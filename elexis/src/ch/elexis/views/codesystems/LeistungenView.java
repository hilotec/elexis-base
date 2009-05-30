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
 *  $Id: LeistungenView.java 5331 2009-05-30 13:01:05Z rgw_ch $
 *******************************************************************************/

package ch.elexis.views.codesystems;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Hub;
import ch.elexis.actions.GlobalActions;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.actions.GlobalEvents.ActivationListener;
import ch.elexis.data.Eigenartikel;
import ch.elexis.data.ICodeElement;
import ch.elexis.data.Leistungsblock;
import ch.elexis.util.SWTHelper;
import ch.elexis.views.artikel.EigenartikelSelektor;
import ch.elexis.views.codesystems.CodeSelectorFactory.cPage;
import ch.rgw.tools.StringTool;

public class LeistungenView extends ViewPart implements ActivationListener, ISaveablePart2 {
	
	private static final String CAPTION_ERROR = Messages.getString("LeistungenView.error"); //$NON-NLS-1$
	public final static String ID = "ch.elexis.LeistungenView"; //$NON-NLS-1$
	public CTabFolder ctab;
	CTabItem selected;
	
	public LeistungenView(){

	}
	
	@Override
	public void createPartControl(Composite parent){
		
		parent.setLayout(new GridLayout());
		ctab = new CTabFolder(parent, SWT.BOTTOM);
		ctab.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		ctab.setSimple(false);
		ctab.setMRUVisible(true);
		ctab.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e){
				if (selected != null) {
					cPage page = (cPage) selected.getControl();
					if (page == null) {
						SWTHelper.alert(CAPTION_ERROR, "cPage=null"); //$NON-NLS-1$
					}
					page.cv.getConfigurer().getControlFieldProvider().clearValues();
				}
				selected = ctab.getSelection();
				((cPage) selected.getControl()).refresh();
				setFocus();
			}
			
		});
		CodeSelectorFactory cs = new BlockSelector();
		CTabItem ct = new CTabItem(ctab, SWT.NONE);
		ICodeElement ics = (ICodeElement) Hub.poFactory.createTemplate(Leistungsblock.class);
		if (ics == null) {
			SWTHelper.alert(CAPTION_ERROR, "ICodeElement=null"); //$NON-NLS-1$
			throw new NullPointerException("ICodeElement; LeistungenView"); //$NON-NLS-1$
		}
		if (StringTool.isNothing(ics.getCodeSystemName())) {
			SWTHelper.alert(CAPTION_ERROR, "CodeSystemname=null"); //$NON-NLS-1$
		}
		ct.setText(ics.getCodeSystemName());
		ct.setData(ics);
		// cPage page=new cPage(ctab, getViewSite(),ics,cs);
		BlockSelector.bsPage bspage = new BlockSelector.bsPage(ctab, cs);
		ct.setControl(bspage);
		cs = new EigenartikelSelektor();
		ct = new CTabItem(ctab, SWT.NONE);
		ics = (ICodeElement) Hub.poFactory.createTemplate(Eigenartikel.class);
		ct.setText(ics.getCodeSystemName());
		ct.setData(ics);
		cPage page = new cPage(ctab, getViewSite(), ics, cs);
		ct.setControl(page);
		CodeSelectorFactory.makeTabs(ctab, getViewSite(), "ch.elexis.Verrechnungscode"); //$NON-NLS-1$
		GlobalEvents.getInstance().addActivationListener(this, this);
	}
	
	public void dispose(){
		GlobalEvents.getInstance().removeActivationListener(this, this);
		super.dispose();
	}
	
	@Override
	public void setFocus(){
		if (selected != null) {
			((cPage) selected.getControl()).cv.getConfigurer().getControlFieldProvider().setFocus();
		}
	}
	
	void swapTabs(int iLeft, int iRight){
		CTabItem ctLeft = ctab.getItem(iLeft);
		CTabItem ctRight = ctab.getItem(iRight);
		String t = ctLeft.getText();
		Control c = ctLeft.getControl();
		ctLeft.setText(ctRight.getText());
		ctLeft.setControl(ctRight.getControl());
		ctRight.setText(t);
		ctRight.setControl(c);
	}
	
	public void activation(boolean mode){
		if (mode == false) {
			if (selected != null) {
				cPage page = (cPage) selected.getControl();
				page.cv.getConfigurer().getControlFieldProvider().clearValues();
			}
			// remove any ICodeSelectiorTarget, since it's no more needed
			GlobalEvents.getInstance().removeCodeSelectorTarget();
		} else {
			if (selected != null) {
				cPage page = (cPage) selected.getControl();
				page.refresh();
				
			}
			
		}
		
	}
	
	public void visible(boolean mode){}
	
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
}
