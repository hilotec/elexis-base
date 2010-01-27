/*******************************************************************************
 * Copyright (c) 2006-2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 * $Id: GlobalEvents.java 5970 2010-01-27 16:43:04Z rgw_ch $
 *******************************************************************************/

package ch.elexis.actions;

import java.util.Hashtable;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.data.Anwender;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.util.Log;
import ch.elexis.views.codesystems.ICodeSelectorTarget;
import ch.rgw.tools.Tree;

/**
 * Central management and distribution of events. To get informed about selection changes, register
 * as GlobalEvents.Listener. To send a message about changed selection, call fireSelectionEvent.
 * GlobalEvents will distribute the event among all listeners.
 * 
 * To get informed about activation or deactivation of a workbench part, register as
 * ActivationListener.
 * 
 * To get informed when a change of the databse occurs, register as BackingStoreListener.
 * 
 * To get informed about creation, deletion or modification of PersistentObject, register as
 * ObjectListener
 * 
 * To be informed when a user change occurs, register as UserListener
 * 
 * Zentrale Verarbeitung und Verteilung von Ereignissen. Wer z.B. über eine Änderung der Auswahl
 * informiert werden möchte, registriert sich als GlobalEvent.Listener. Wer selber eine Nachricht
 * senden möchte, ruft "fireEvent" auf. Dadurch wird die Nachricht an alle Listeners weitergeleitet.
 * Wer informiert werden will, ob eine bestimmte Part aktiviert oder deaktiviert wurde, registriert
 * sich als ActivationListener Wer über eine Änderung einer Datenbasis informiert werden will (um
 * z.B. eine Liste neu einzulesen), registriert sich als BackingStoreListener Wer informiert werden
 * will, wenn der Anwender oder der Mandant sich ändert, registriert sich als UserListener
 * 
 * @author Gerry
 * 
 * @deprecated use ElexisEventDispatcher
 */

@Deprecated
public class GlobalEvents {
	private static Log log = Log.get("GlobalEvents"); //$NON-NLS-1$
	
	private final LinkedList<SelectionListener> selectionListeners;
	private final LinkedList<BackingStoreListener> storeListeners;
	private final LinkedList<ObjectListener> objectListeners;
	private final LinkedList<UserListener> userListeners;
	
	private ICodeSelectorTarget codeSelectorTarget = null;
	private static GlobalEvents theInstance;
	
	private GlobalEvents(){
		selectionListeners = new LinkedList<SelectionListener>();
		storeListeners = new LinkedList<BackingStoreListener>();
	
		objectListeners = new LinkedList<ObjectListener>();
		userListeners = new LinkedList<UserListener>();
	
	}
	
	/**
	 * There is only one GlobalEvents-Object, which can be accessed only here.
	 * 
	 * Singleton. Kann nur über diese Funktion bezogen werden
	 * 
	 * @return
	 */
	private static GlobalEvents getInstance(){
		if (theInstance == null) {
			theInstance = new GlobalEvents();
		}
		return theInstance;
	}
	
	/**
	 * Convenience-Methoden
	 * 
	 */
	/*
	public static Patient getSelectedPatient(){
		return (Patient) getInstance().getSelectedObject(Patient.class);
	}
	
	public static Fall getSelectedFall(){
		return (Fall) getInstance().getSelectedObject(Fall.class);
	}
	
	public static Konsultation getSelectedKons(){
		return (Konsultation) getInstance().getSelectedObject(Konsultation.class);
	}
	*/
	
	/**
	 * Einen ObjectListener hinzufügen. ObjectListeners werden informiert, wenn ein PersistentObject
	 * geändert, neu erstellt oder gelöscht wird.
	 * 
	 * @param o
	 *            ein ObjectListener oder ObjectListenerAdapter
	 */
	
	public void addObjectListener(final ObjectListener o){
		objectListeners.add(o);
	}
	
	/**
	 * Einen ObjectListener entfernen. Dies muss unbedingt spätestens bei Dispose gemacht werden.
	 * 
	 * @param o
	 *            ein ObjectListener, der zuvor mit addObjectListener hinzugefügt wurde
	 */
	public void removeObjectListener(final ObjectListener o){
		objectListeners.remove(o);
	}
	
	/**
	 * Einen SelectionListener hinzufügen. Selection;Listeners werden informiert, wenn der Anwender
	 * ein Objekt auswählt
	 * 
	 * @param l
	 *            den Listener
	 * @param win
	 *            das Fenster, das beobachtet werden soll.
	 
	public void addSelectionListener(final SelectionListener l){
		selectionListeners.add(l);
	}
	*/
	/**
	 * Einen listener entfernen. Dies muss unbedingt bei dispose gemacht werden, da es sonst beim
	 * nächsten Aufrufversuch eine Exception gibt.
	 * 
	 * @param l
	 */
	public void removeSelectionListener(final SelectionListener l /* ,IWorkbenchWindow win */){
		selectionListeners.remove(l);
	}
	
	/**
	 * Add a listener that will be informed, as user or mandantor changes
	 
	public void addUserListener(final UserListener l){
		userListeners.add(l);
	}
	*/
	/**
	 * remove a previously added UserListener
	 * 
	 * @param l
	 */
	public void removeUserListener(final UserListener l){
		userListeners.remove(l);
	}
	
	/**
	 * add a listener that will be informed, as Members of a given class should be reloiaded from
	 * the database
	 
	public void addBackingStoreListener(final BackingStoreListener l){
		storeListeners.add(l);
	}
	*/
	/**
	 * remove a previously added BackingStoreIstener
	 * 
	 * @param l
	 */
	public void removeBackingStoreListener(final BackingStoreListener l){
		storeListeners.remove(l);
	}
	
	
	/**
	 * Die Lebenszustands-Änderung eines Objekts anzeigen
	 * 
	 */
	public enum CHANGETYPE {
		update, delete, create
	};
	
	private void fireObjectEvent(final PersistentObject o, final CHANGETYPE type){
		Desk.getDisplay().asyncExec(new Runnable() {
			
			public void run(){
				for (ObjectListener ol : objectListeners) {
					switch (type.ordinal()) {
					case 0:
						ol.objectChanged(o);
						break;
					case 1:
						ol.objectDeleted(o);
						break;
					case 2:
						ol.objectCreated(o);
						break;
					}
				}
			}
		});
	}
	
	/**
	 * Die Änderung einer Auswahl anzeigen. Im Fall eines Events aus der Patient-Fall-behandlung
	 * Kette wird die Synchronisation jeweils hergestellt.
	 * 
	 * @param selected
	 *            das neu ausgewählte Element
	 * @param win
	 *            wo die Auswahl erfolgte
	 */
	
	public void fireSelectionEvent(final PersistentObject po){
		ElexisEventDispatcher.fireSelectionEvent(po);
	}
	private void oldfireSelectionEvent(final PersistentObject selected /* , IWorkbenchWindow win */){
		
		if (selected == null) {
			log.log("fireSelectionEvent mit Null Objekt ", Log.DEBUGMSG); //$NON-NLS-1$
		} else {
			log
			.log(
				"fireSelectionEvent: " + selected.getClass().getName() + "::" + selected.getId(), Log.DEBUGMSG); //$NON-NLS-1$ //$NON-NLS-2$
		}
		// TODO Das ist unbefriedigend. Lieber Abhängigkeit Pat/Fall/Kons beim Klienten auflösen
		if (selected instanceof Patient) {
			Hub.setWindowText((Patient) selected);
			Fall f = (Fall) SelectionTracker.getObject(Fall.class);
			if ((f == null) || (f.getPatient() == null)
					|| (!f.getPatient().getId().equals(selected.getId()))) {
				/*
				 * Konsultation b=((Patient)selected).getLetzteKons(); if(b!=null){ f=b.getFall();
				 * doDispatchEvent(win,f); doDispatchEvent(win,b); }
				 */
				clearSelection(Fall.class);
			}
		} else if (selected instanceof Fall) {
			Patient pat = (Patient) SelectionTracker.getObject(Patient.class);
			Fall fall = (Fall) selected;
			if ((pat == null) || (!pat.getId().equals(fall.getPatient().getId()))) {
				doDispatchEvent(fall.getPatient());
				SelectionTracker.clearObject(Konsultation.class);
				
			}
		} else if (selected instanceof Konsultation) {
			Fall fb = ((Konsultation) selected).getFall();
			Fall fo = (Fall) SelectionTracker.getObject(Fall.class);
			if ((fo != null) && !fo.getId().equals(fb.getId())) {
				doDispatchEvent(fb);
				doDispatchEvent(fb.getPatient());
			}
		}
		doDispatchEvent(selected);
		
	}
	
	static PersistentObject sourceObj;
	
	private void doDispatchEvent(final PersistentObject obj){
		if ((obj == null) || (sourceObj == obj)) {
			return;
		}
		
		log.log("doDispatch: " + obj.getClass().getName() + "::" + obj.getId(), Log.DEBUGMSG); //$NON-NLS-1$ //$NON-NLS-2$
		
		// Endlosschleife vermeiden.
		sourceObj = obj;
		if (SelectionTracker.change(obj) || true) {
			for (SelectionListener l : selectionListeners) {
				l.selectionEvent(obj);
			}
		}
		sourceObj = null;
	}
	
	/**
	 * Eine Information schicken, dass der Datenbestand eines bestimmten Typs verändert wurde.
	 * 
	 * @param clazz
	 */
	@SuppressWarnings("unchecked")
	private void oldfireUpdateEvent(final Class clazz){
		for (BackingStoreListener lis : storeListeners) {
			lis.reloadContents(clazz);
		}
	}
	public void fireUpdateEvent(final Class clazz){
		ElexisEventDispatcher.reload(clazz);
	}
	
	/**
	 * Die Information senden, dass entweder der Anwender oder der Mandant geändert haben
	 */
	public void fireUserEvent(){
		/*
		for (UserListener l : userListeners) {
			l.UserChanged();
		}
		*/
		ElexisEventDispatcher.getInstance().fire(new ElexisEvent(Hub.actUser, Anwender.class, ElexisEvent.EVENT_USER_CHANGED));
	}
	
	/**
	 * Ein SelectionListener dient dazu, sich über die Auswahl eines PersistentObjects informieren
	 * zu lassen
	 * 
	 * @author Gerry
	 * 
	 */
	public interface SelectionListener {
		public void selectionEvent(PersistentObject obj);
		
		public void clearEvent(Class<? extends PersistentObject> template);
	}
	
	/**
	 * Der BackingStoreListener wird dann informiert, wenn die Datenbasis neu eingelesen werden
	 * sollte (z.B. weil Änderungen erfolgt sein könnten) Es ist nicht garantiert, dass tatsächlich
	 * Änderungen erfilgt sind.
	 * 
	 * @author Gerry
	 * 
	 */
	public interface BackingStoreListener {
		public void reloadContents(Class<? extends PersistentObject> clazz);
	}
	
	/**
	 * Findet den im aktuellen Fenster gerade selektierten Patienten oder null, wenn kein Patient
	 * selektiert ist
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public PersistentObject getSelectedObject(final Class template){
		return SelectionTracker.getObject(template);
	}
	
	public void clearSelection(final Class<? extends PersistentObject> template /*
	 * ,
	 * IWorkbenchWindow
	 * win
	 */){
		log.log("clearSelection: " + template.getName(), Log.DEBUGMSG); //$NON-NLS-1$
		
		SelectionTracker.clearObject(template);
		Desk.getDisplay().syncExec(new Runnable() {
			public void run(){
				for (SelectionListener l : selectionListeners) {
					l.clearEvent(template);
				}
			}
		});
	}
	
	private static class SelectionTracker {
		static SelectionTracker tracker;
		@SuppressWarnings("unchecked")
		Hashtable<Class, PersistentObject> objects = new Hashtable<Class, PersistentObject>();
		
		@SuppressWarnings("unchecked")
		static PersistentObject getObject(/* IWorkbenchWindow win, */final Class template){
			// SelectionTracker t=find(win);
			return tracker == null ? null : tracker.objects.get(template);
		}
		
		@SuppressWarnings("unchecked")
		static void clearObject(/* IWorkbenchWindow win, */final Class template){
			// SelectionTracker t=find(win);
			if (tracker != null) {
				tracker.objects.remove(template);
			}
		}
		
		static boolean change(final PersistentObject sel){
			if (sel == null) {
				return false;
			}
			if (tracker == null) {
				tracker = new SelectionTracker();
			}
			PersistentObject old = tracker.objects.get(sel.getClass());
			if ((old != null) && (old == sel)) {
				return false;
			}
			tracker.objects.put(sel.getClass(), sel);
			return true;
		}
	}
	
	public interface UserListener {
		public void UserChanged();
	}
	
	public interface ObjectListener {
		public void objectChanged(PersistentObject o);
		
		public void objectCreated(PersistentObject o);
		
		public void objectDeleted(PersistentObject o);
	}
	
	public static class ObjectListenerAdapter implements ObjectListener {
		
		public void objectChanged(final PersistentObject o){}
		
		public void objectCreated(final PersistentObject o){}
		
		public void objectDeleted(final PersistentObject o){}
		
	}
	
	
	
}
