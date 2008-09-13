/*******************************************************************************
 * Copyright (c) 2006-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: RechnungsListeView.java 4411 2008-09-13 20:47:59Z rgw_ch $
 *******************************************************************************/
package ch.elexis.views.rechnung;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.actions.GlobalEvents.BackingStoreListener;
import ch.elexis.data.Fall;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Rechnung;
import ch.elexis.preferences.PreferenceConstants;
import ch.elexis.util.CommonViewer;
import ch.elexis.util.Money;
import ch.elexis.util.MoneyInput;
import ch.elexis.util.NumberInput;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.SimpleWidgetProvider;
import ch.elexis.util.Tree;
import ch.elexis.util.ViewMenus;
import ch.elexis.util.ViewerConfigurer;

/**
 * Display a listing of all bills selected after several user selectable criteria. The selected bills
 * can be modified or exported.
 * @author gerry
 *
 */
public class RechnungsListeView extends ViewPart implements BackingStoreListener{
	public final static String ID="ch.elexis.RechnungsListeView"; //$NON-NLS-1$
	
	CommonViewer cv;
    ViewerConfigurer vc;
    RnActions actions;
    RnContentProvider cntp;
    RnControlFieldProvider cfp;

    Text tPat, tRn, tSum, tOpen;
    NumberInput niDaysTo1st, niDaysTo2nd, niDaysTo3rd;
    MoneyInput mi1st,mi2nd,mi3rd;
    SelectionListener mahnWizardListener;
    FormToolkit tk=Desk.getToolkit();
    
	
	@Override
	public void createPartControl(final Composite p) {
		p.setLayout(new GridLayout());
		//SashForm sash=new SashForm(p,SWT.VERTICAL);
		Composite comp=new Composite(p,SWT.NONE);
		comp.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		comp.setLayout(new GridLayout());
		cv=new CommonViewer();
    	cntp=new RnContentProvider(this,cv);
    	cfp=new RnControlFieldProvider();
    	vc=new ViewerConfigurer(
    			cntp,
    			new ViewerConfigurer.TreeLabelProvider(),
    			cfp,
    			new ViewerConfigurer.DefaultButtonProvider(),
    			new SimpleWidgetProvider(SimpleWidgetProvider.TYPE_TREE,SWT.V_SCROLL|SWT.MULTI,cv)
    	);
    	//rnFilter=FilterFactory.createFilter(Rechnung.class,"Rn Nummer","Name","Vorname","Betrag");
    	cv.create(vc,comp,SWT.BORDER,getViewSite());
    	
    	Composite bottom=new Composite(comp,SWT.NONE);

    	RowLayout rowLayout = new RowLayout();
 		rowLayout.wrap = false;
 		rowLayout.pack = true;
 		rowLayout.justify = true;
 		rowLayout.fill=true;
 		rowLayout.type = SWT.HORIZONTAL;
 		rowLayout.marginLeft = 0;
 		rowLayout.marginTop = 0;
 		rowLayout.marginRight = 0;
 		rowLayout.marginBottom = 0;
 		rowLayout.spacing = 5;

 		mahnWizardListener=new SelectionAdapter(){
			@Override
			public void widgetSelected(final SelectionEvent e) {
				Hub.mandantCfg.set(PreferenceConstants.RNN_DAYSUNTIL1ST,niDaysTo1st.getValue());
				Hub.mandantCfg.set(PreferenceConstants.RNN_DAYSUNTIL2ND,niDaysTo2nd.getValue());
				Hub.mandantCfg.set(PreferenceConstants.RNN_DAYSUNTIL3RD,niDaysTo3rd.getValue());
				Hub.mandantCfg.set(PreferenceConstants.RNN_AMOUNT1ST, mi1st.getMoney(false).getAmountAsString());
				Hub.mandantCfg.set(PreferenceConstants.RNN_AMOUNT2ND, mi2nd.getMoney(false).getAmountAsString());
				Hub.mandantCfg.set(PreferenceConstants.RNN_AMOUNT3RD, mi3rd.getMoney(false).getAmountAsString());
				// Hub.mandantCfg.dump(null);
			}
 			
 		};
 		
    	bottom.setLayout(rowLayout);
    	Form fSum=tk.createForm(bottom);
    	Form fWizard=tk.createForm(bottom);
    	fSum.setText(Messages.getString("RechnungsListeView.sum")); //$NON-NLS-1$
    	fWizard.setText("Mahnungen-Automatik");
    	//Composite cSum=new Composite(comp,SWT.NONE);
    	Composite cSum=fSum.getBody();
    	cSum.setLayout(new GridLayout(2,false));
    	//fSum.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
    	tk.createLabel(cSum,Messages.getString("RechnungsListeView.patInList")); //$NON-NLS-1$
    	tPat=tk.createText(cSum,"",SWT.BORDER|SWT.READ_ONLY); //$NON-NLS-1$
    	tPat.setLayoutData(new GridData(100,SWT.DEFAULT));
    	tk.createLabel(cSum,Messages.getString("RechnungsListeView.accountsInList")); //$NON-NLS-1$
    	tRn=tk.createText(cSum,"",SWT.BORDER|SWT.READ_ONLY); //$NON-NLS-1$
    	tRn.setLayoutData(new GridData(100,SWT.DEFAULT));
    	tk.createLabel(cSum,Messages.getString("RechnungsListeView.sumInList")); //$NON-NLS-1$
    	tSum=SWTHelper.createText(tk, cSum, 1, SWT.BORDER|SWT.READ_ONLY);
    	tSum.setLayoutData(new GridData(100,SWT.DEFAULT));
    	tk.createLabel(cSum,Messages.getString("RechnungsListeView.paidInList"));  //$NON-NLS-1$
    	tOpen=SWTHelper.createText(tk,cSum, 1, SWT.BORDER|SWT.READ_ONLY);
    	tOpen.setLayoutData(new GridData(100,SWT.DEFAULT));
    	Composite cW=fWizard.getBody();
    	cW.setLayout(new GridLayout(4,true));
    	
    	tk.createLabel(cW, "Abstand in Tagen");

    	niDaysTo1st=new NumberInput(cW,"1. Mahnung");
    	niDaysTo1st.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	niDaysTo1st.getControl().addSelectionListener(mahnWizardListener);
    	niDaysTo1st.setValue(Hub.mandantCfg.get(PreferenceConstants.RNN_DAYSUNTIL1ST, 30));
    	niDaysTo2nd=new NumberInput(cW,"2. Mahnung");
    	niDaysTo2nd.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	niDaysTo2nd.getControl().addSelectionListener(mahnWizardListener);
    	niDaysTo2nd.setValue(Hub.mandantCfg.get(PreferenceConstants.RNN_DAYSUNTIL2ND, 10));
    	niDaysTo3rd=new NumberInput(cW,"3. Mahnung");
    	niDaysTo3rd.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	niDaysTo3rd.getControl().addSelectionListener(mahnWizardListener);
    	niDaysTo3rd.setValue(Hub.mandantCfg.get(PreferenceConstants.RNN_DAYSUNTIL3RD, 5));
    	tk.createLabel(cW, "Gebühr");
    	mi1st=new MoneyInput(cW,"1. Mahnung");
    	mi1st.addSelectionListener(mahnWizardListener);
    	mi1st.setMoney(Hub.mandantCfg.get(PreferenceConstants.RNN_AMOUNT1ST, new Money().getAmountAsString()));
    	mi2nd=new MoneyInput(cW,"2. Mahnung");
    	mi2nd.addSelectionListener(mahnWizardListener);
    	mi2nd.setMoney(Hub.mandantCfg.get(PreferenceConstants.RNN_AMOUNT2ND, new Money().getAmountAsString()));
    	mi3rd=new MoneyInput(cW,"3. Mahnung");
    	mi3rd.addSelectionListener(mahnWizardListener);
    	mi3rd.setMoney(Hub.mandantCfg.get(PreferenceConstants.RNN_AMOUNT3RD, new Money().getAmountAsString()));
    	
    	GlobalEvents.getInstance().addBackingStoreListener(this);
    	cv.getViewerWidget().getControl().setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
    	ViewMenus menu=new ViewMenus(getViewSite());
    	actions=new RnActions(this);
    	menu.createToolbar(actions.rnExportAction,actions.mahnWizardAction, actions.rnFilterAction,actions.reloadAction);
    	menu.createMenu(actions.expandAllAction,actions.collapseAllAction,actions.printListeAction, actions.addAccountExcessAction);
    	MenuManager mgr=new MenuManager();
    	mgr.setRemoveAllWhenShown(true);
    	mgr.addMenuListener(new RnMenuListener(this));
    	cv.setContextMenu(mgr);
    	cntp.startListening();
	}

	@Override
	public void dispose(){
		cntp.stopListening();
		super.dispose();
	}
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	public void reloadContents(final Class clazz) {
		if(clazz.equals(Rechnung.class)){
			cv.notify(CommonViewer.Message.update);
		}
		
	}

	@SuppressWarnings("unchecked") 
	List<Rechnung> createList(){
		IStructuredSelection sel=(IStructuredSelection)cv.getViewerWidget().getSelection();
		List<Tree> at=sel.toList();
		List<Rechnung> ret=new LinkedList<Rechnung>();
		for(Tree<PersistentObject> t:at){
			if(t.contents instanceof Patient){
				for(Tree<PersistentObject>tp:t.getChildren()){
					for(Tree<PersistentObject> tf:tp.getChildren()){
						ret.add((Rechnung)tf.contents);
					}
				}
			}else if(t.contents instanceof Fall){
				for(Tree<PersistentObject> tr:t.getChildren()){
					ret.add((Rechnung)tr.contents);
				}
			}else if(t.contents instanceof Rechnung){
				Rechnung r=(Rechnung)t.contents;
				ret.add(r);
			}
		}
		return ret;
	}
}
