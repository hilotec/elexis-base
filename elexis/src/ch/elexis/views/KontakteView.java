/*******************************************************************************
 * Copyright (c) 2005-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 * $Id: KontakteView.java 6044 2010-02-01 15:18:50Z rgw_ch $
 *******************************************************************************/

package ch.elexis.views;

import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.StringConstants;
import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.FlatDataLoader;
import ch.elexis.actions.GlobalActions;
import ch.elexis.actions.PersistentObjectLoader;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Organisation;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Person;
import ch.elexis.data.Query;
import ch.elexis.dialogs.KontaktErfassenDialog;
import ch.elexis.util.ViewMenus;
import ch.elexis.util.viewers.CommonViewer;
import ch.elexis.util.viewers.DefaultControlFieldProvider;
import ch.elexis.util.viewers.DefaultLabelProvider;
import ch.elexis.util.viewers.SimpleWidgetProvider;
import ch.elexis.util.viewers.ViewerConfigurer;
import ch.elexis.util.viewers.ViewerConfigurer.ControlFieldListener;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.StringTool;

public class KontakteView extends ViewPart implements ControlFieldListener, ISaveablePart2 {
	public static final String ID = "ch.elexis.Kontakte"; //$NON-NLS-1$
	private CommonViewer cv;
	private ViewerConfigurer vc;
	IAction dupKontakt, delKontakt, createKontakt;
	PersistentObjectLoader loader;
	
	private final String[] fields = {
		Kontakt.FLD_SHORT_LABEL + Query.EQUALS + Messages.getString("KontakteView.shortLabel"), //$NON-NLS-1$
		Kontakt.FLD_NAME1 + Query.EQUALS + Messages.getString("KontakteView.text1"), //$NON-NLS-1$
		Kontakt.FLD_NAME2 + Query.EQUALS + Messages.getString("KontakteView.text2"), //$NON-NLS-1$
		Kontakt.FLD_STREET + Query.EQUALS + Messages.getString("KontakteView.street"), //$NON-NLS-1$
		Kontakt.FLD_ZIP + Query.EQUALS + Messages.getString("KontakteView.zip"), //$NON-NLS-1$
		Kontakt.FLD_PLACE + Query.EQUALS + Messages.getString("KontakteView.place")}; //$NON-NLS-1$
	private ViewMenus menu;
	
	public KontakteView(){}
	
	@Override
	public void createPartControl(Composite parent){
		parent.setLayout(new FillLayout());
		cv = new CommonViewer();
		loader = new FlatDataLoader(cv, new Query<Kontakt>(Kontakt.class));
		vc =
			new ViewerConfigurer(loader, new KontaktLabelProvider(),
				new DefaultControlFieldProvider(cv, fields),
				new ViewerConfigurer.DefaultButtonProvider(), new SimpleWidgetProvider(
					SimpleWidgetProvider.TYPE_LAZYLIST, SWT.NONE, null));
		cv.create(vc, parent, SWT.NONE, getViewSite());
		makeActions();
		cv.setObjectCreateAction(getViewSite(), createKontakt);
		menu = new ViewMenus(getViewSite());
		menu.createViewerContextMenu(cv.getViewerWidget(), delKontakt, dupKontakt);
		menu.createMenu(GlobalActions.printKontaktEtikette);
		menu.createToolbar(GlobalActions.printKontaktEtikette);
		vc.getContentProvider().startListening();
		vc.getControlFieldProvider().addChangeListener(this);
		cv.addDoubleClickListener(new CommonViewer.DoubleClickListener() {
			public void doubleClicked(PersistentObject obj, CommonViewer cv){
				try {
					KontaktDetailView kdv =
						(KontaktDetailView) getSite().getPage().showView(KontaktDetailView.ID);
					kdv.kb.catchElexisEvent(new ElexisEvent(obj, obj.getClass(),
						ElexisEvent.EVENT_SELECTED));
				} catch (PartInitException e) {
					ExHandler.handle(e);
				}
				
			}
		});
	}
	
	public void dispose(){
		vc.getContentProvider().stopListening();
		vc.getControlFieldProvider().removeChangeListener(this);
		super.dispose();
	}
	
	@Override
	public void setFocus(){
		vc.getControlFieldProvider().setFocus();
	}
	
	public void changed(HashMap<String, String> values){
		ElexisEventDispatcher.clearSelection(Kontakt.class);
	}
	
	public void reorder(String field){
		loader.setOrderField(field);
	}
	
	/**
	 * ENTER has been pressed in the control fields, select the first listed patient
	 */
	// this is also implemented in PatientenListeView
	public void selected(){
		StructuredViewer viewer = cv.getViewerWidget();
		Object[] elements = cv.getConfigurer().getContentProvider().getElements(viewer.getInput());
		
		if (elements != null && elements.length > 0) {
			Object element = elements[0];
			/*
			 * just selecting the element in the viewer doesn't work if the control fields are not
			 * empty (i. e. the size of items changes): cv.setSelection(element, true); bug in
			 * TableViewer with style VIRTUAL? work-arount: just globally select the element without
			 * visual representation in the viewer
			 */
			if (element instanceof PersistentObject) {
				// globally select this object
				ElexisEventDispatcher.fireSelectionEvent((PersistentObject) element);
			}
		}
	}
	
	/*
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2 Wir benötigen das
	 * Interface nur, um das Schliessen einer View zu verhindern, wenn die Perspektive fixiert ist.
	 * Gibt es da keine einfachere Methode?
	 */
	public int promptToSaveOnClose(){
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL
				: ISaveablePart2.NO;
	}
	
	public void doSave(IProgressMonitor monitor){ /* leer */
	}
	
	public void doSaveAs(){ /* leer */
	}
	
	public boolean isDirty(){
		return true;
	}
	
	public boolean isSaveAsAllowed(){
		return false;
	}
	
	public boolean isSaveOnCloseNeeded(){
		return true;
	}
	
	private void makeActions(){
		delKontakt = new Action(Messages.getString("KontakteView.delete")) { //$NON-NLS-1$
			@Override
			public void run(){
				Object[] o = cv.getSelection();
				if (o != null) {
					Kontakt k = (Kontakt) o[0];
					k.delete();
					cv.getConfigurer().getControlFieldProvider().fireChangedEvent();
				}
			}
		};
		dupKontakt = new Action(Messages.getString("KontakteView.duplicate")) { //$NON-NLS-1$
			@Override
			public void run(){
				Object[] o = cv.getSelection();
				if (o != null) {
					Kontakt k = (Kontakt) o[0];
					Kontakt dup;
					if (k.istPerson()) {
						Person p = Person.load(k.getId());
						dup =
							new Person(p.getName(), p.getVorname(), p.getGeburtsdatum(), p
								.getGeschlecht());
					} else {
						Organisation org = Organisation.load(k.getId());
						dup =
							new Organisation(org.get(Organisation.FLD_NAME1), org
								.get(Organisation.FLD_NAME2));
					}
					dup.setAnschrift(k.getAnschrift());
					cv.getConfigurer().getControlFieldProvider().fireChangedEvent();
					// cv.getViewerWidget().refresh();
				}
			}
		};
		createKontakt = new Action(Messages.getString("KontakteView.create")) { //$NON-NLS-1$
			@Override
			public void run(){
				String[] flds = cv.getConfigurer().getControlFieldProvider().getValues();
				String[] predef =
					new String[] {
					flds[1], flds[2], StringConstants.EMPTY, StringConstants.EMPTY,
					flds[3], flds[4], flds[5]
				};
				KontaktErfassenDialog ked =
					new KontaktErfassenDialog(getViewSite().getShell(), predef);
				ked.open();
			}
		};
	}
	
	class KontaktLabelProvider extends DefaultLabelProvider {
		
		@Override
		public String getText(Object element){
			String[] fields =
				new String[] {
				Kontakt.FLD_NAME1, Kontakt.FLD_NAME2, Kontakt.FLD_NAME3, Kontakt.FLD_STREET, Kontakt.FLD_ZIP,
				Kontakt.FLD_PLACE, Kontakt.FLD_PHONE1
			};
			String[] values=new String[fields.length];
			((Kontakt)element).get(fields, values);
			return StringTool.join(values, StringConstants.COMMA);
		}
		
	}
}
