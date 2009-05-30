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
 * $Id: CodeDetailView.java 5331 2009-05-30 13:01:05Z rgw_ch $
 *******************************************************************************/
package ch.elexis.views.codesystems;

import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.actions.GlobalActions;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.actions.GlobalEvents.ActivationListener;
import ch.elexis.actions.GlobalEvents.SelectionListener;
import ch.elexis.data.PersistentObject;
import ch.elexis.util.Extensions;
import ch.elexis.util.ImporterPage;
import ch.elexis.util.ViewMenus;
import ch.elexis.util.viewers.CommonViewer;
import ch.elexis.util.viewers.ViewerConfigurer;
import ch.elexis.views.IDetailDisplay;
import ch.rgw.tools.ExHandler;

public class CodeDetailView extends ViewPart implements SelectionListener, ActivationListener,
		ISaveablePart2 {
	public final static String ID = "ch.elexis.codedetailview"; //$NON-NLS-1$
	private CTabFolder ctab;
	private IAction importAction;
	private ViewMenus viewmenus;
	private Hashtable<String, ImporterPage> importers;
	
	@Override
	public void createPartControl(Composite parent){
		parent.setLayout(new FillLayout());
		ctab = new CTabFolder(parent, SWT.NONE);
		importers = new Hashtable<String, ImporterPage>();
		addCustomBlocksPage();
		importers.put(ctab.getItem(0).getText(), new BlockImporter());
		
		addPagesFor("ch.elexis.Diagnosecode"); //$NON-NLS-1$
		addPagesFor("ch.elexis.Verrechnungscode"); //$NON-NLS-1$
		if (ctab.getItemCount() > 0) {
			ctab.setSelection(0);
			
		}
		ctab.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				CTabItem top = ctab.getSelection();
				if (top != null) {
					String t = top.getText();
					importAction.setEnabled(importers.get(t) != null);
					MasterDetailsPage page = (MasterDetailsPage) top.getControl();
					ViewerConfigurer vc = page.cv.getConfigurer();
					vc.getControlFieldProvider().setFocus();
				}
			}
			
		});
		makeActions();
		viewmenus = new ViewMenus(getViewSite());
		viewmenus.createMenu(importAction /* ,deleteAction */);
		GlobalEvents.getInstance().addSelectionListener(this);
		GlobalEvents.getInstance().addActivationListener(this, this);
		
	}
	
	private void addCustomBlocksPage(){
		BlockSelector cs = new BlockSelector();
		BlockDetailDisplay bdd = new BlockDetailDisplay();
		MasterDetailsPage page = new MasterDetailsPage(ctab, cs, bdd);
		CTabItem ct = new CTabItem(ctab, SWT.NONE);
		ct.setText(bdd.getTitle());
		ct.setControl(page);
		ct.setData(bdd);
		page.sash.setWeights(new int[] {
			30, 70
		});
	}
	
	private void makeActions(){
		importAction = new Action(Messages.getString("CodeDetailView.importActionTitle")) { //$NON-NLS-1$
				@Override
				public void run(){
					CTabItem it = ctab.getSelection();
					if (it != null) {
						ImporterPage top = importers.get(it.getText());
						if (top != null) {
							ImportDialog dlg = new ImportDialog(getViewSite().getShell(), top);
							dlg.create();
							dlg.setTitle(top.getTitle());
							dlg.setMessage(top.getDescription());
							dlg.getShell().setText(
								Messages.getString("CodeDetailView.importerCaption")); //$NON-NLS-1$
							if (dlg.open() == Dialog.OK) {
								top.run(false);
							}
						}
					}
					
				}
				
			};
		
	}
	
	private class ImportDialog extends TitleAreaDialog {
		ImporterPage importer;
		
		public ImportDialog(Shell parentShell, ImporterPage i){
			super(parentShell);
			importer = i;
		}
		
		@Override
		protected Control createDialogArea(Composite parent){
			return importer.createPage(parent);
		}
		
	}
	
	private void addPagesFor(String point){
		List<IConfigurationElement> list = Extensions.getExtensions(point);
		for (IConfigurationElement ce : list) {
			try {
				System.out.println(ce.getName());
				if ("Artikel".equals(ce.getName())) { //$NON-NLS-1$
					continue;
				}
				IDetailDisplay d =
					(IDetailDisplay) ce.createExecutableExtension("CodeDetailDisplay"); //$NON-NLS-1$
				CodeSelectorFactory cs =
					(CodeSelectorFactory) ce.createExecutableExtension("CodeSelectorFactory"); //$NON-NLS-1$
				String a = ce.getAttribute("ImporterClass"); //$NON-NLS-1$
				ImporterPage ip = null;
				if (a != null) {
					ip = (ImporterPage) ce.createExecutableExtension("ImporterClass"); //$NON-NLS-1$
					if (ip != null) {
						importers.put(d.getTitle(), ip);
					}
				}
				MasterDetailsPage page = new MasterDetailsPage(ctab, cs, d);
				CTabItem ct = new CTabItem(ctab, SWT.NONE);
				ct.setText(d.getTitle());
				ct.setControl(page);
				ct.setData(d);
				
			} catch (Exception ex) {
				ExHandler.handle(ex);
				MessageBox mb = new MessageBox(getViewSite().getShell(), SWT.ICON_ERROR | SWT.OK);
				mb.setText(Messages.getString("CodeDetailView.errorCaption")); //$NON-NLS-1$
				mb.setMessage(Messages.getString("CodeDetailView.errorBody") + ce.getName() + ":\n" //$NON-NLS-1$ //$NON-NLS-2$
					+ ex.getLocalizedMessage());
				mb.open();
			}
		}
	}
	
	@Override
	public void setFocus(){
		if (ctab.getItemCount() > 0) {
			ctab.setFocus();
		}
	}
	
	public void selectionEvent(PersistentObject obj){
		if (obj != null) {
			CTabItem top = ctab.getSelection();
			if (top != null) {
				IDetailDisplay ids = (IDetailDisplay) top.getData();
				Class cl = ids.getElementClass();
				String o1 = obj.getClass().getName();
				String o2 = cl.getName();
				if (o1.equals(o2)) {
					ids.display(obj);
				}
			}
		}
	}
	
	class MasterDetailsPage extends Composite {
		SashForm sash;
		CommonViewer cv;
		
		MasterDetailsPage(Composite parent, CodeSelectorFactory master, IDetailDisplay detail){
			super(parent, SWT.NONE);
			setLayout(new FillLayout());
			sash = new SashForm(this, SWT.NONE);
			cv = new CommonViewer();
			cv.create(master.createViewerConfigurer(cv), sash, SWT.NONE, getViewSite());
			cv.getViewerWidget().addSelectionChangedListener(
				GlobalEvents.getInstance().getDefaultListener());
			/* Composite page= */detail.createDisplay(sash, getViewSite());
			cv.getConfigurer().getContentProvider().startListening();
			
		}
		
	}
	
	@Override
	public void dispose(){
		GlobalEvents.getInstance().removeSelectionListener(this);
		GlobalEvents.getInstance().removeActivationListener(this, this);
		if ((ctab != null) && (!ctab.isDisposed())) {
			for (CTabItem ct : ctab.getItems()) {
				((MasterDetailsPage) ct.getControl()).cv
					.getViewerWidget()
					.removeSelectionChangedListener(GlobalEvents.getInstance().getDefaultListener());
				((MasterDetailsPage) ct.getControl()).cv.getConfigurer().getContentProvider()
					.stopListening();
			}
		}
		
	}
	
	/** Vom ActivationListener */
	public void activation(boolean mode){
		CTabItem top = ctab.getSelection();
		if (top != null) {
			MasterDetailsPage page = (MasterDetailsPage) top.getControl();
			ViewerConfigurer vc = page.cv.getConfigurer();
			if (mode == true) {
				vc.getControlFieldProvider().setFocus();
			} else {
				vc.getControlFieldProvider().clearValues();
			}
		}
		
	}
	
	public void visible(boolean mode){

	}
	
	public void clearEvent(Class template){
	// TODO Auto-generated method stub
	
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
	
	public void doSave(IProgressMonitor monitor){ /* leer */}
	
	public void doSaveAs(){ /* leer */}
	
	public boolean isDirty(){
		return true;
	}
	
	public boolean isSaveAsAllowed(){
		return false;
	}
	
	public boolean isSaveOnCloseNeeded(){
		return true;
	}
}
