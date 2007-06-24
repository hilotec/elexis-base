/*******************************************************************************
 * Copyright (c) 2005, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: KontaktSelektor.java 2530 2007-06-18 14:34:12Z danlutz $
 *******************************************************************************/

package ch.elexis.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.jobs.Job;
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

import ch.elexis.Hub;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.actions.JobPool;
import ch.elexis.actions.ListLoader;
import ch.elexis.actions.AbstractDataLoaderJob.FilterProvider;
import ch.elexis.data.BezugsKontakt;
import ch.elexis.data.Fall;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.util.CommonViewer;
import ch.elexis.util.DefaultControlFieldProvider;
import ch.elexis.util.DefaultLabelProvider;
import ch.elexis.util.LazyContentProvider;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.SimpleWidgetProvider;
import ch.elexis.util.ViewerConfigurer;
import ch.elexis.util.CommonViewer.DoubleClickListener;

public class KontaktSelektor extends TitleAreaDialog implements DoubleClickListener{
	//private Class clazz;
	CommonViewer cv;
	ViewerConfigurer vc;
	private String title;
	private String message;
	private Object selection;
	ListLoader dataloader;
	Button bAll,bPersons,bOrgs;
	KontaktFilter fp;
	FilterButtonAdapter fba;
	int	type;

	boolean showBezugsKontakt = false;
	private ListViewer bezugsKontaktViewer = null;
	private boolean isSelecting = false;
	
	@SuppressWarnings("unchecked")
	public KontaktSelektor(Shell parentShell, Class which, String t, String m) {
		super(parentShell);
		//clazz=which;
		cv=new CommonViewer();
		fba=new FilterButtonAdapter();
		title=t;
		message=m;
		dataloader=(ListLoader)JobPool.getJobPool().getJob(which.getSimpleName());
		if(dataloader==null){
			dataloader=new ListLoader(which.getSimpleName(),new Query(which),new String[]{"Bezeichnung1"});
			   Hub.jobPool.addJob(dataloader);
		        dataloader.setPriority(Job.SHORT);
		        dataloader.setUser(true);
		        dataloader.schedule();
		}
		fp=new KontaktFilter(0,dataloader.getQuery());
	}

	public KontaktSelektor(Shell parentShell, Class which, String t, String m, boolean showBezugsKontakt) {
		this(parentShell, which, t, m);
		
		this.showBezugsKontakt = showBezugsKontakt;
	}
	
	@Override
	public boolean close() {
		cv.removeDoubleClickListener(this);
		cv.dispose();
		return super.close();
	}


	/* (Kein Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(final Composite parent) {
		//SashForm ret=new SashForm(parent,SWT.NONE);
		Composite ret=new Composite(parent,SWT.NONE);
		ret.setLayout(new GridLayout());
		ret.setLayoutData(SWTHelper.getFillGridData(1,true,1,true));
		
		if (showBezugsKontakt) {
			new Label(ret, SWT.NONE).setText("Bezugskontakte");
			bezugsKontaktViewer = new ListViewer(ret, SWT.SINGLE);
			bezugsKontaktViewer.getControl().setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
			
			bezugsKontaktViewer.setContentProvider(new IStructuredContentProvider() {
				public Object[] getElements(Object inputElement) {
					Fall f = GlobalEvents.getSelectedFall();
					if (f != null) {
						Patient patient = f.getPatient();
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
						
						Fall[] faelle = patient.getFaelle();
						for (Fall fall : faelle) {
							Kontakt kontakt;
							
							kontakt = fall.getGarant();
							if (kontakt != null && kontakt.exists()) {
								if (!addedKontakte.contains(kontakt.getId())) {
									elements.add(kontakt);
									addedKontakte.add(kontakt.getId());
								}
							}
							
							kontakt = fall.getKostentraeger();
							if (kontakt != null && kontakt.exists()) {
								if (!addedKontakte.contains(kontakt.getId())) {
									elements.add(kontakt);
									addedKontakte.add(kontakt.getId());
								}
							}

							kontakt = fall.getArbeitgeber();
							if (kontakt != null && kontakt.exists()) {
								if (!addedKontakte.contains(kontakt.getId())) {
									elements.add(kontakt);
									addedKontakte.add(kontakt.getId());
								}
							}
						}
						
						return elements.toArray();
					}
					
					return new Object[] {};
				}
				
				public void dispose() {
					// nothing to do
				}

				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
					//nothing to do
				}
			});
			bezugsKontaktViewer.setLabelProvider(new DefaultLabelProvider());
			bezugsKontaktViewer.setInput(this);
			
			bezugsKontaktViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					if (isSelecting) {
						return;
					}
					
					IStructuredSelection sel = (IStructuredSelection) cv.getViewerWidget().getSelection();
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
		
		vc=new ViewerConfigurer(
		   new LazyContentProvider(cv,dataloader, null),
		   new DefaultLabelProvider(),
		   new DefaultControlFieldProvider(cv,new String[]{"Kuerzel","Bezeichnung1"}),
		   new ViewerConfigurer.ButtonProvider(){

			public Button createButton(final Composite parent) {
				Button ret=new Button(parent,SWT.PUSH);
				ret.setText("Neu erstellen...");
				ret.addSelectionListener(new SelectionAdapter(){

					@Override
					public void widgetSelected(SelectionEvent e) {
						KontaktDetailDialog kdd=new KontaktDetailDialog(parent.getShell(),new String[]{vc.getControlFieldProvider().getValues()[1],""});
						kdd.open();
					}
					
				});
				return ret;
			}

			public boolean isAlwaysEnabled() {
				return false;
			}},
		   new SimpleWidgetProvider(SimpleWidgetProvider.TYPE_LAZYLIST,SWT.NONE,cv)
		);
		Composite types=new Composite(ret,SWT.BORDER);
		types.setLayout(new FillLayout());
		types.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		bAll=new Button(types,SWT.RADIO);
		bPersons=new Button(types,SWT.RADIO);
		bOrgs=new Button(types,SWT.RADIO);
		bAll.setSelection(true);
		bAll.setText("Alle");
		bPersons.setText("Personen");
		bOrgs.setText("Organisationen");
		bAll.addSelectionListener(fba);
		bPersons.addSelectionListener(fba);
		bOrgs.addSelectionListener(fba);
		cv.create(vc,ret,SWT.NONE,"1");
		GridData gd=SWTHelper.getFillGridData(1,true,1,true);
		gd.heightHint=100;
		cv.getViewerWidget().getControl().setLayoutData(gd);
		setTitle(title);
		setMessage(message);
        ((LazyContentProvider)vc.getContentProvider()).startListening();
        cv.addDoubleClickListener(this);
        //cv.getViewerWidget().addFilter(filter);
        dataloader.addFilterProvider(fp);

        if (showBezugsKontakt) {
        	cv.getViewerWidget().addSelectionChangedListener(new ISelectionChangedListener() {
        		public void selectionChanged(SelectionChangedEvent event) {
        			if (isSelecting) {
        				return;
        			}
        			
        			if (bezugsKontaktViewer != null) {
    					IStructuredSelection sel = (IStructuredSelection) bezugsKontaktViewer.getSelection();
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
	public void create() {
		super.create();
		getShell().setText("Kontakt auswählen");
	}


	/* (Kein Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#cancelPressed()
	 */
	@Override
	protected void cancelPressed() {
		selection=null;
		((LazyContentProvider)vc.getContentProvider()).stopListening();
		super.cancelPressed();
	}

	private Object getBezugsKontaktSelection() {
		Object bezugsKontakt = null;

		if (bezugsKontaktViewer != null) {
			IStructuredSelection sel = (IStructuredSelection) bezugsKontaktViewer.getSelection();
			if (sel.size() > 0) {
				bezugsKontakt = sel.getFirstElement();
			}
		}
		
		return bezugsKontakt;
	}
	
	/* (Kein Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
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
			Object[] sel=cv.getSelection();
			if((sel!=null) && (sel.length>0)){
				selection=sel[0];
			}else{
				selection=null;
			}
		}
		((LazyContentProvider)vc.getContentProvider()).stopListening();
		cv.removeDoubleClickListener(this);
		super.okPressed();
	}


	public void doubleClicked(PersistentObject obj, CommonViewer cv) {
		okPressed();
	}

	class FilterButtonAdapter extends SelectionAdapter{
		@Override
		public void widgetSelected(SelectionEvent e) {
			if(((Button)e.getSource()).getSelection()){
				if(bPersons.getSelection()){
					fp.setType(1);
				}else if(bOrgs.getSelection()){
					fp.setType(2);
				}else{
					fp.setType(0);
				}
				dataloader.invalidate();
				cv.notify(CommonViewer.Message.update);
			}
		}
	}
	class KontaktFilter implements FilterProvider{
		int type;
		Query qbe;
		
		KontaktFilter(int t, Query q){
			type=t;
			qbe=q;
		}
		void setType(int t){
			type=t;
		}
		public void applyFilter() {
			if(type==1){
				qbe.add("istPerson", "=", "1");						
			}else if(type==2){
				qbe.add("istPerson", "=", "0");						
			}
		}			
	
	}
}
