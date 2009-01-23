/*******************************************************************************
 * Copyright (c) 2005-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: KontaktSelektor.java 5026 2009-01-23 17:32:50Z rgw_ch $
 *******************************************************************************/

package ch.elexis.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import ch.elexis.Desk;
import ch.elexis.actions.FlatDataLoader;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.actions.PersistentObjectLoader.QueryFilter;
import ch.elexis.data.BezugsKontakt;
import ch.elexis.data.Fall;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Person;
import ch.elexis.data.Query;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.viewers.CommonViewer;
import ch.elexis.util.viewers.DefaultControlFieldProvider;
import ch.elexis.util.viewers.DefaultLabelProvider;
import ch.elexis.util.viewers.SimpleWidgetProvider;
import ch.elexis.util.viewers.ViewerConfigurer;
import ch.elexis.util.viewers.CommonViewer.DoubleClickListener;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

public class KontaktSelektor extends TitleAreaDialog implements DoubleClickListener {
	// Name, Vorname, gebdat, strasse, plz, ort, tel, zusatz, fax, email
	public static final int HINTSIZE = 12;
	
	public static final int HINT_NAME = 0;
	public static final int HINT_FIRSTNAME = 1;
	public static final int HINT_BIRTHDATE = 2;
	public static final int HINT_STREET = 3;
	public static final int HINT_ZIP = 4;
	public static final int HINT_PLACE = 5;
	public static final int HINT_PHONE = 6;
	public static final int HINT_ADD = 7;
	public static final int HINT_FAX = 8;
	public static final int HINT_MAIL = 9;
	public static final int HINT_SEX = 10;
	public static final int HINT_PATIENT = 11;
	
	// private Class clazz;
	CommonViewer cv;
	ViewerConfigurer vc;
	private String title;
	private String message;
	private Object selection;
	Button bAll, bPersons, bOrgs;
	KontaktFilter fp;
	FilterButtonAdapter fba;
	String[] hints;
	// int type;
	
	boolean showBezugsKontakt = false;
	String extraText = null;
	private ListViewer bezugsKontaktViewer = null;
	private boolean isSelecting = false;
	private FlatDataLoader kl;
	
	@SuppressWarnings("unchecked")
	public KontaktSelektor(Shell parentShell, Class which, String t, String m){
		super(parentShell);
		// clazz=which;
		cv = new CommonViewer();
		fba = new FilterButtonAdapter();
		title = t;
		message = m;
		/*
		 * dataloader=(ListLoader)JobPool.getJobPool().getJob(which.getSimpleName());
		 * if(dataloader==null){ dataloader=new ListLoader(which.getSimpleName(),new
		 * Query(which),new String[]{"Bezeichnung1","Bezeichnung2"});
		 * Hub.jobPool.addJob(dataloader); dataloader.setPriority(Job.SHORT);
		 * dataloader.setUser(true); dataloader.schedule(); }
		 */
		kl = new FlatDataLoader(cv, new Query<Kontakt>(Kontakt.class));
		fp = new KontaktFilter(0);
	}
	
	public KontaktSelektor(Shell parentShell, Class<? extends PersistentObject> which, String t,
		String m, boolean showBezugsKontakt){
		this(parentShell, which, t, m);
		
		this.showBezugsKontakt = showBezugsKontakt;
	}
	
	public KontaktSelektor(Shell parentShell, Class<? extends PersistentObject> which, String t,
		String m, String extra){
		this(parentShell, which, t, m);
		extraText = extra;
	}
	
	@Override
	public boolean close(){
		cv.removeDoubleClickListener(this);
		cv.dispose();
		return super.close();
	}
	
	/**
	 * Provide a few hints in case the user clicks "Neu erstellen". The hints is an array of up to
	 * 10 Strings as used in KontaktErfassenDialog
	 * 
	 * @param hints
	 *            Name, Vorname, gebdat, strasse, plz, ort, tel, zusatz, fax, email
	 */
	public void setHints(String[] h){
		this.hints = h;
		for (int i = 0; i < hints.length; i++) { // make KontaktErfassenDialog happy
			if (hints[i] == null) {
				hints[i] = "";
			}
		}
		if (!StringTool.isNothing(hints[HINT_BIRTHDATE])) {
			TimeTool tt = new TimeTool();
			if (tt.set(hints[HINT_BIRTHDATE])) {
				hints[HINT_BIRTHDATE] = tt.toString(TimeTool.DATE_GER);
			} else {
				hints[HINT_BIRTHDATE] = "";
			}
		}
		if (!StringTool.isNothing(hints[HINT_SEX])) {
			if (hints[HINT_SEX].toLowerCase().startsWith("m")) {
				hints[HINT_SEX] = Person.MALE;
			} else {
				hints[HINT_SEX] = Person.FEMALE;
			}
		}
	}
	
	/*
	 * (Kein Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(final Composite parent){
		// SashForm ret=new SashForm(parent,SWT.NONE);
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayout(new GridLayout());
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		
		if (showBezugsKontakt) {
			new Label(ret, SWT.NONE).setText("Bezugskontakte");
			bezugsKontaktViewer = new ListViewer(ret, SWT.SINGLE);
			bezugsKontaktViewer.getControl().setLayoutData(
				SWTHelper.getFillGridData(1, true, 1, false));
			
			bezugsKontaktViewer.setContentProvider(new IStructuredContentProvider() {
				public Object[] getElements(Object inputElement){
					Patient patient = GlobalEvents.getSelectedPatient();
					if (patient != null) {
						ArrayList<PersistentObject> elements = new ArrayList<PersistentObject>();
						ArrayList<String> addedKontakte = new ArrayList<String>();
						
						// add the patient itself
						elements.add(patient);
						addedKontakte.add(patient.getId());
						
						List<BezugsKontakt> bezugsKontakte = patient.getBezugsKontakte();
						if (bezugsKontakte != null) {
							for (BezugsKontakt bezugsKontakt : bezugsKontakte) {
								elements.add(bezugsKontakt);
								addedKontakte.add(bezugsKontakt.get("otherID"));
							}
						}
						
						// required contacts of biling system
						Fall[] faelle = patient.getFaelle();
						for (Fall fall : faelle) {
							String reqs = fall.getRequirements();
							if (reqs != null) {
								for (String req : reqs.split(";")) {
									final String[] r = req.split(":");
									
									// no valid entry
									if (r.length < 2) {
										continue;
									}
									
									// only consider contacts
									if (r[1].equals("K")) {
										String kontaktID = fall.getInfoString(r[0]);
										if (!kontaktID.startsWith("**ERROR")) {
											Kontakt kontakt = Kontakt.load(kontaktID);
											if (kontakt.isValid()) {
												elements.add(kontakt);
												addedKontakte.add(kontakt.getId());
											}
										}
									}
								}
							}
						}
						
						return elements.toArray();
					}
					
					return new Object[] {};
				}
				
				public void dispose(){
				// nothing to do
				}
				
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput){
				// nothing to do
				}
			});
			bezugsKontaktViewer.setLabelProvider(new DefaultLabelProvider());
			bezugsKontaktViewer.setInput(this);
			
			bezugsKontaktViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event){
					if (isSelecting) {
						return;
					}
					
					IStructuredSelection sel =
						(IStructuredSelection) cv.getViewerWidget().getSelection();
					if (sel.size() > 0) {
						isSelecting = true;
						cv.getViewerWidget().setSelection(new StructuredSelection(), false);
						isSelecting = false;
					}
				}
			});
		} else {
			bezugsKontaktViewer = null;
		}
		
		if (showBezugsKontakt) {
			new Label(ret, SWT.NONE).setText("Andere Kontakte");
		}
		if (extraText != null) {
			new Label(ret, SWT.WRAP).setText(extraText);
		}
		vc = new ViewerConfigurer(
		// new LazyContentProvider(cv,dataloader, null),
			kl, new DefaultLabelProvider(), new DefaultControlFieldProvider(cv, new String[] {
				"Kuerzel", "Bezeichnung1"
			}), new ViewerConfigurer.ButtonProvider() {
				
				public Button createButton(final Composite parent){
					Button ret = new Button(parent, SWT.PUSH);
					ret.setText("Neu erstellen...");
					ret.addSelectionListener(new SelectionAdapter() {
						
						@Override
						public void widgetSelected(SelectionEvent e){
							if (hints == null) {
								hints = new String[3];
								hints[0] = vc.getControlFieldProvider().getValues()[1];
							}
							KontaktErfassenDialog ked =
								new KontaktErfassenDialog(parent.getShell(), hints);
							ked.open();
							selection = ked.getResult();
							okPressed();
							// cv.getViewerWidget().refresh();
							// cv.getViewerWidget().setSelection(new StructuredSelection(kr), true);
						}
						
					});
					return ret;
				}
				
				public boolean isAlwaysEnabled(){
					return false;
				}
			}, new SimpleWidgetProvider(SimpleWidgetProvider.TYPE_LAZYLIST, SWT.NONE, cv));
		Composite types = new Composite(ret, SWT.BORDER);
		types.setLayout(new FillLayout());
		types.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		bAll = new Button(types, SWT.RADIO);
		bPersons = new Button(types, SWT.RADIO);
		bOrgs = new Button(types, SWT.RADIO);
		bAll.setSelection(true);
		bAll.setText("Alle");
		bPersons.setText("Personen");
		bOrgs.setText("Organisationen");
		bAll.addSelectionListener(fba);
		bPersons.addSelectionListener(fba);
		bOrgs.addSelectionListener(fba);
		cv.create(vc, ret, SWT.NONE, "1");
		GridData gd = SWTHelper.getFillGridData(1, true, 1, true);
		gd.heightHint = 100;
		cv.getViewerWidget().getControl().setLayoutData(gd);
		setTitle(title);
		setMessage(message);
		vc.getContentProvider().startListening();
		cv.addDoubleClickListener(this);
		// cv.getViewerWidget().addFilter(filter);
		kl.addQueryFilter(fp);
		
		if (showBezugsKontakt) {
			cv.getViewerWidget().addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event){
					if (isSelecting) {
						return;
					}
					
					if (bezugsKontaktViewer != null) {
						IStructuredSelection sel =
							(IStructuredSelection) bezugsKontaktViewer.getSelection();
						if (sel.size() > 0) {
							isSelecting = true;
							bezugsKontaktViewer.setSelection(new StructuredSelection(), false);
							isSelecting = false;
						}
					}
				}
			});
		}
		
		return ret;
	}
	
	public Object getSelection(){
		return selection;
	}
	
	@Override
	public void create(){
		super.create();
		getShell().setText("Kontakt auswählen");
	}
	
	/*
	 * (Kein Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#cancelPressed()
	 */
	@Override
	protected void cancelPressed(){
		selection = null;
		vc.getContentProvider().stopListening();
		super.cancelPressed();
	}
	
	private Object getBezugsKontaktSelection(){
		Object bezugsKontakt = null;
		
		if (bezugsKontaktViewer != null) {
			IStructuredSelection sel = (IStructuredSelection) bezugsKontaktViewer.getSelection();
			if (sel.size() > 0) {
				bezugsKontakt = sel.getFirstElement();
			}
		}
		
		return bezugsKontakt;
	}
	
	/*
	 * (Kein Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed(){
		
		Object bKSel = getBezugsKontaktSelection();
		if (bKSel instanceof Kontakt) {
			selection = bKSel;
		} else if (bKSel instanceof BezugsKontakt) {
			BezugsKontakt bezugsKontakt = (BezugsKontakt) bKSel;
			Kontakt kontakt = Kontakt.load(bezugsKontakt.get("otherID"));
			if (kontakt.exists()) {
				selection = kontakt;
			}
		} else {
			if (selection == null) {
				Object[] sel = cv.getSelection();
				if ((sel != null) && (sel.length > 0)) {
					selection = sel[0];
				} else {
					selection = null;
				}
			}
		}
		vc.getContentProvider().stopListening();
		cv.removeDoubleClickListener(this);
		super.okPressed();
	}
	
	public void doubleClicked(PersistentObject obj, CommonViewer cv){
		okPressed();
	}
	
	class FilterButtonAdapter extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e){
			if (((Button) e.getSource()).getSelection()) {
				if (bPersons.getSelection()) {
					fp.setType(1);
				} else if (bOrgs.getSelection()) {
					fp.setType(2);
				} else {
					fp.setType(0);
				}
				cv.notify(CommonViewer.Message.update);
			}
		}
	}
	
	static class KontaktFilter implements QueryFilter {
		int type;
		
		KontaktFilter(int t){
			type = t;
		}
		
		void setType(int t){
			type = t;
		}
		
		public void apply(Query<? extends PersistentObject> qbe){
			if (type == 1) {
				qbe.add("istPerson", "=", "1");
			} else if (type == 2) {
				qbe.add("istPerson", "=", "0");
			}
		}
		
	}
	
	public static Kontakt showInSync(Class<? extends Kontakt> clazz, String title, String message,
		String extra){
		InSync rn = new InSync(clazz, title, message, extra, null);
		Desk.getDisplay().syncExec(rn);
		return rn.ret;
		
	}
	
	public static Kontakt showInSync(Class<? extends Kontakt> clazz, String title, String message){
		InSync rn = new InSync(clazz, title, message, null, null);
		Desk.getDisplay().syncExec(rn);
		return rn.ret;
		
	}
	
	public static Kontakt showInSync(Class<? extends Kontakt> clazz, String title, String message,
		String extra, String[] hints){
		InSync rn = new InSync(clazz, title, message, extra, hints);
		Desk.getDisplay().syncExec(rn);
		return rn.ret;
		
	}
	
	public static Kontakt showInSync(Class<? extends Kontakt> clazz, String title, String message,
		String[] hints){
		InSync rn = new InSync(clazz, title, message, null, hints);
		Desk.getDisplay().syncExec(rn);
		return rn.ret;
		
	}
	
	private static class InSync implements Runnable {
		Kontakt ret;
		String title, message;
		Class<? extends Kontakt> clazz;
		String extra;
		String[] hints;
		
		InSync(Class<? extends Kontakt> clazz, String title, String message, String extra,
			String[] hints){
			this.title = title;
			this.message = message;
			this.clazz = clazz;
			this.extra = extra;
			this.hints = hints;
		}
		
		public void run(){
			Shell shell = Desk.getDisplay().getActiveShell();
			KontaktSelektor ksl = new KontaktSelektor(shell, clazz, title, message, extra);
			if (hints != null) {
				ksl.setHints(hints);
			}
			if (ksl.open() == Dialog.OK) {
				ret = (Kontakt) ksl.getSelection();
			} else {
				ret = null;
			}
		}
		
	}
}
