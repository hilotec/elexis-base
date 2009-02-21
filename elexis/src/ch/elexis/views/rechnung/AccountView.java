/*******************************************************************************
 * Copyright (c) 2006-2009, Daniel Lutz and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Daniel Lutz - initial implementation
 *    Gerry Weirich - adapted to use the new AccountTransaction-class
 *    				  actions added
 *    
 *  $Id: AccountView.java 5170 2009-02-21 19:44:23Z rgw_ch $
 *******************************************************************************/

package ch.elexis.views.rechnung;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.actions.BackgroundJob;
import ch.elexis.actions.GlobalActions;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.actions.BackgroundJob.BackgroundJobListener;
import ch.elexis.actions.GlobalEvents.ActivationListener;
import ch.elexis.actions.GlobalEvents.SelectionListener;
import ch.elexis.data.AccountTransaction;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.data.Rechnung;
import ch.elexis.dialogs.AddBuchungDialog;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;
import ch.rgw.tools.Money;

/**
 * This view shows the current patient's account
 */

public class AccountView extends ViewPart implements SelectionListener, ActivationListener,
		ISaveablePart2 {
	
	public static final String ID = "ch.elexis.views.rechnung.AccountView";
	
	private static final String ACCOUNT_EXCESS_JOB_NAME = "Guthaben berechnen";
	private BackgroundJob accountExcessJob;
	
	private FormToolkit tk;
	private Form form;
	private Label balanceLabel;
	private Label excessLabel;
	private TableViewer accountViewer;
	
	private Patient actPatient;
	
	private Action addPaymentAction, removePaymentAction;
	
	// column indices
	private static final int DATE = 0;
	private static final int AMOUNT = 1;
	private static final int BILL = 2;
	private static final int REMARKS = 3;
	
	private static final String[] COLUMN_TEXT = {
		"Datum", // DATE
		"Betrag", // AMOUNT
		"Rechnung", // BILL
		"Bemerkungen", // REMARKS
	};
	
	private static final int[] COLUMN_WIDTH = {
		80, // DATE
		80, // AMOUNT
		80, // BILL
		160, // REMARKS
	};
	
	public void createPartControl(Composite parent){
		initializeJobs();
		
		parent.setLayout(new FillLayout());
		tk = Desk.getToolkit();
		form = tk.createForm(parent);
		form.getBody().setLayout(new GridLayout(1, false));
		
		// account infos
		Composite accountArea = tk.createComposite(form.getBody());
		accountArea.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		accountArea.setLayout(new GridLayout(3, false));
		tk.createLabel(accountArea, "Konto:");
		tk.createLabel(accountArea, "Kontostand:");
		balanceLabel = tk.createLabel(accountArea, "");
		balanceLabel.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		tk.createLabel(accountArea, ""); // dummy
		tk.createLabel(accountArea, "Rechnungsguthaben:");
		excessLabel = tk.createLabel(accountArea, "");
		excessLabel.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		// account entries
		accountViewer = new TableViewer(form.getBody(), SWT.SINGLE | SWT.FULL_SELECTION);
		Table table = accountViewer.getTable();
		table.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		tk.adapt(table);
		
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		// columns
		TableColumn[] tc = new TableColumn[COLUMN_TEXT.length];
		for (int i = 0; i < COLUMN_TEXT.length; i++) {
			tc[i] = new TableColumn(table, SWT.NONE);
			tc[i].setText(COLUMN_TEXT[i]);
			tc[i].setWidth(COLUMN_WIDTH[i]);
		}
		
		accountViewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement){
				if (actPatient == null) {
					return new Object[] {
						"Kein Patient ausgewählt."
					};
				}
				Query<AccountTransaction> qa =
					new Query<AccountTransaction>(AccountTransaction.class);
				qa.add("PatientID", "=", actPatient.getId());
				qa.orderBy(false, new String[] {
					"Datum"
				});
				return qa.execute().toArray();
				
				/*
				 * List<AccountEntry> entries = new ArrayList<AccountEntry>();
				 * 
				 * StringBuilder sql = new StringBuilder();
				 * sql.append("SELECT DATUM, BETRAG, BEMERKUNG FROM KONTO ");
				 * sql.append("  WHERE PatientID = "); sql.append(actPatient.getWrappedId());
				 * sql.append("  ORDER BY DATUM"); Stm stm =
				 * PersistentObject.getConnection().getStatement();
				 * 
				 * try { ResultSet res = stm.query(sql.toString()); while (res.next()) { String
				 * sDate = res.getString(1); String sAmount = res.getString(2); String remarks =
				 * res.getString(3);
				 * 
				 * TimeTool date = new TimeTool(sDate); Money amount = new Money(0);
				 * amount.addCent(sAmount);
				 * 
				 * AccountEntry entry = new AccountEntry(date, amount, remarks); entries.add(entry);
				 * } } catch (Exception ex) { ExHandler.handle(ex); return new Object[0]; } finally
				 * { PersistentObject.getConnection().releaseStatement(stm); }
				 * 
				 * 
				 * return entries.toArray();
				 */
			}
			
			public void dispose(){
			// nothing to do
			}
			
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput){
			// nothing to do
			}
		});
		accountViewer.setLabelProvider(new ITableLabelProvider() {
			public void addListener(ILabelProviderListener listener){
			// nothing to do
			}
			
			public void removeListener(ILabelProviderListener listener){
			// nothing to do
			}
			
			public void dispose(){
			// nothing to do
			}
			
			public String getColumnText(Object element, int columnIndex){
				if (!(element instanceof AccountTransaction)) {
					return "";
				}
				
				AccountTransaction entry = (AccountTransaction) element;
				String text = "";
				
				switch (columnIndex) {
				case DATE:
					text = entry.get("Datum");
					break;
				case AMOUNT:
					text = entry.getAmount().getAmountAsString();
					break;
				case BILL:
					Rechnung rechnung = entry.getRechnung();
					if (rechnung != null && rechnung.exists()) {
						text = rechnung.getNr();
					} else {
						text = "";
					}
					break;
				case REMARKS:
					text = entry.getRemark();
					break;
				}
				
				return text;
			}
			
			public Image getColumnImage(Object element, int columnIndex){
				return null;
			}
			
			public boolean isLabelProperty(Object element, String property){
				return false;
			}
		});
		
		// viewer.setSorter(new NameSorter());
		accountViewer.setInput(getViewSite());
		
		/*
		 * makeActions(); hookContextMenu(); hookDoubleClickAction(); contributeToActionBars();
		 */
		makeActions();
		ViewMenus menu = new ViewMenus(getViewSite());
		menu.createToolbar(addPaymentAction /* do not use yet ,removePaymentAction */);
		removePaymentAction.setEnabled(false);
		GlobalEvents.getInstance().addActivationListener(this, this);
		accountViewer.addSelectionChangedListener(GlobalEvents.getInstance().getDefaultListener());
	}
	
	private void initializeJobs(){
		accountExcessJob = new AccountExcessJob(ACCOUNT_EXCESS_JOB_NAME);
		accountExcessJob.addListener(new BackgroundJobListener() {
			public void jobFinished(BackgroundJob j){
				setKontoText();
			}
		});
		accountExcessJob.schedule();
	}
	
	private void finishJobs(){
		accountExcessJob.cancel();
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus(){
		accountViewer.getControl().setFocus();
	}
	
	@Override
	public void dispose(){
		finishJobs();
		GlobalEvents.getInstance().removeActivationListener(this, this);
		accountViewer.removeSelectionChangedListener(GlobalEvents.getInstance()
			.getDefaultListener());
		super.dispose();
	}
	
	private void setPatient(Patient patient){
		actPatient = patient;
		
		// start calculating account excess
		accountExcessJob.invalidate();
		accountExcessJob.schedule();
		
		String title = "";
		if (actPatient != null) {
			title = actPatient.getLabel();
		} else {
			title = "Kein Patient ausgewählt";
		}
		form.setText(title);
		
		setKontoText();
		accountViewer.refresh();
		
		form.layout();
	}
	
	// maybe called from foreign thread
	private void setKontoText(){
		// check wheter the labels are valid, since we may be called
		// from a different thread
		if (balanceLabel.isDisposed() || excessLabel.isDisposed()) {
			return;
		}
		
		String balanceText = "";
		String excessText = "";
		
		if (actPatient != null) {
			balanceText = actPatient.getKontostand().getAmountAsString();
			
			if (accountExcessJob.isValid()) {
				Object jobData = accountExcessJob.getData();
				if (jobData instanceof Money) {
					Money accountExcess = (Money) jobData;
					excessText = accountExcess.getAmountAsString();
				}
			}
		}
		
		balanceLabel.setText(balanceText);
		excessLabel.setText(excessText);
	}
	
	/*
	 * SelectionListener methods
	 */

	public void selectionEvent(PersistentObject obj){
		if (obj instanceof Patient) {
			Patient selectedPatient = (Patient) obj;
			
			setPatient(selectedPatient);
		} else if (obj instanceof AccountTransaction) {
			removePaymentAction.setEnabled(true);
		}
	}
	
	public void clearEvent(Class template){
		if (template.equals(Patient.class)) {
			setPatient(null);
		} else if (template.equals(AccountTransaction.class)) {
			removePaymentAction.setEnabled(false);
		}
	}
	
	/*
	 * ActivationListener
	 */

	public void activation(boolean mode){
	// nothing to do
	}
	
	public void visible(boolean mode){
		if (mode == true) {
			GlobalEvents.getInstance().addSelectionListener(this);
			
			Patient patient = GlobalEvents.getSelectedPatient();
			setPatient(patient);
		} else {
			GlobalEvents.getInstance().removeSelectionListener(this);
			
			setPatient(null);
		}
	};
	
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
	
	/*
	 * class AccountEntry { TimeTool date; Money amount; String remarks;
	 * 
	 * AccountEntry(TimeTool date, Money amount, String remarks) { this.date = date; this.amount =
	 * amount; this.remarks = remarks;
	 * 
	 * if (remarks == null) { remarks = ""; } } }
	 */

	private void makeActions(){
		addPaymentAction = new Action("Buchung/Zahlung hinzufügen") {
			{
				setToolTipText("Einen Betrag als Zahlung eingeben");
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_ADDITEM));
			}
			
			@Override
			public void run(){
				if (new AddBuchungDialog(getViewSite().getShell(), actPatient).open() == Dialog.OK) {
					setPatient(actPatient);
				}
			}
		};
		removePaymentAction = new Action("Buchung/Zahlung löschen") {
			{
				setToolTipText("Markierte Buchung löschen");
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_DELETE));
			}
			
			@Override
			public void run(){
				AccountTransaction at =
					(AccountTransaction) GlobalEvents.getInstance().getSelectedObject(
						AccountTransaction.class);
				if (at != null) {
					if (SWTHelper
						.askYesNo(
							"Zahlung wirklich löschen?",
							"Bitte beachten Sie, dass manuelles Löschen von Zahlungen zu Buchhaltungsfehlern führen kann. Wirklich löschen?")) {
						at.delete();
						setPatient(actPatient);
					}
				}
			}
			
		};
	}
	
	class AccountExcessJob extends BackgroundJob {
		public AccountExcessJob(String name){
			super(name);
		}
		
		public IStatus execute(IProgressMonitor monitor){
			if (AccountView.this.actPatient != null) {
				result = actPatient.getAccountExcess();
			} else {
				result = null;
			}
			
			// return new Status(IStatus.OK, Hub.PLUGIN_ID, IStatus.OK, "Daten geladen", null);
			return Status.OK_STATUS;
		}
		
		public int getSize(){
			return 1;
		}
	}
}