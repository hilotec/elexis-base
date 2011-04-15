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
 *  $Id: LaborPrefs.java 3862 2008-05-05 16:14:14Z rgw_ch $
 *******************************************************************************/

package ch.elexis.preferences;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.admin.AccessControlDefaults;
import ch.elexis.data.LabItem;
import ch.elexis.data.LabResult;
import ch.elexis.data.Query;
import ch.elexis.dialogs.EditLabItem;
import ch.elexis.util.SWTHelper;

public class LaborPrefs extends PreferencePage implements IWorkbenchPreferencePage {
	
	// DynamicListDisplay params;
	// Composite definition;
	// FormToolkit tk;
	private TableViewer tv;
	private Table table;
	int sortC = 1;
	private String[] headers =
		{
			Messages.LaborPrefs_lab, Messages.LaborPrefs_name, Messages.LaborPrefs_short,
			Messages.LaborPrefs_type, Messages.LaborPrefs_unit, Messages.LaborPrefs_refM,
			Messages.LaborPrefs_refF, Messages.LaborPrefs_sortmode
		};
	private int[] colwidth = {
		100, 100, 50, 50, 50, 100, 100, 100
	};
	
	public LaborPrefs(){
		super(Messages.LaborPrefs_labTitle);
	}
	
	protected Control createContents(Composite parn){
		// parn.setLayout(new FillLayout());
		noDefaultAndApplyButton();
		
		Composite ret = new Composite(parn, SWT.NONE);
		ret.setLayout(new GridLayout());
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		table = new Table(ret, SWT.SINGLE | SWT.V_SCROLL | SWT.FULL_SELECTION);
		for (int i = 0; i < headers.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.LEFT);
			tc.setText(headers[i]);
			tc.setWidth(colwidth[i]);
			tc.setData(i);
			tc.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e){
					sortC = (Integer) ((TableColumn) e.getSource()).getData();
					tv.refresh(true);
				}
				
			});
		}
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		tv = new TableViewer(table);
		tv.setContentProvider(new IStructuredContentProvider() {
			
			public Object[] getElements(Object inputElement){
				return LabItem.getLabItems().toArray();
			}
			
			public void dispose(){}
			
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput){}
			
		});
		tv.setLabelProvider(new LabListLabelProvider());
		tv.addDoubleClickListener(new IDoubleClickListener() {
			
			public void doubleClick(DoubleClickEvent event){
				IStructuredSelection sel = (IStructuredSelection) tv.getSelection();
				Object o = sel.getFirstElement();
				if (o instanceof LabItem) {
					LabItem li = (LabItem) o;
					EditLabItem eli = new EditLabItem(getShell(), li);
					eli.create();
					if (eli.open() == Dialog.OK) {
						tv.refresh();
					}
				}
			}
			
		});
		tv.setSorter(new ViewerSorter() {
			
			@Override
			public int compare(Viewer viewer, Object e1, Object e2){
				LabItem li1 = (LabItem) e1;
				LabItem li2 = (LabItem) e2;
				String s1 = "", s2 = ""; //$NON-NLS-1$ //$NON-NLS-2$
				switch (sortC) {
				case 0:
					s1 = li1.getLabor().getLabel();
					s2 = li2.getLabor().getLabel();
					break;
				case 2:
					s1 = li1.getKuerzel();
					s2 = li2.getKuerzel();
					break;
				case 3:
					s1 = li1.getTyp().toString();
					s2 = li2.getTyp().toString();
					break;
				case 7:
					s1 = li1.getGroup();
					s2 = li2.getGroup();
					break;
				default:
					s1 = li1.getName();
					s2 = li2.getName();
				}
				int res = s1.compareToIgnoreCase(s2);
				if (res == 0) {
					return li1.getPrio().compareToIgnoreCase(li2.getPrio());
				}
				return res;
			}
			
		});
		tv.setInput(this);
		Composite buttons = new Composite(ret, SWT.BORDER);
		RowLayout rl = new RowLayout();
		rl.justify = true;
		buttons.setLayout(rl);
		Button bNewItem = new Button(buttons, SWT.PUSH);
		bNewItem.setText(Messages.LaborPrefs_labValue);
		bNewItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				EditLabItem eli = new EditLabItem(getShell(), null);
				eli.create();
				if (eli.open() == Dialog.OK) {
					tv.refresh();
				}
			}
			
		});
		Button bDelItem = new Button(buttons, SWT.PUSH);
		bDelItem.setText(Messages.LaborPrefs_deleteItem);
		bDelItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e){
				IStructuredSelection sel = (IStructuredSelection) tv.getSelection();
				Object o = sel.getFirstElement();
				if (o instanceof LabItem) {
					LabItem li = (LabItem) o;
					Query<LabResult> qbe = new Query<LabResult>(LabResult.class);
					qbe.add("ItemID", "=", li.getId()); //$NON-NLS-1$ //$NON-NLS-2$
					List<LabResult> list = qbe.execute();
					for (LabResult po : list) {
						po.delete();
					}
					li.delete();
					tv.remove(o);
				}
			}
		});
		Button bDelAllItems = new Button(buttons, SWT.PUSH);
		bDelAllItems.setText(Messages.LaborPrefs_deleteAllItems);
		bDelAllItems.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e){
				if (SWTHelper.askYesNo(Messages.LaborPrefs_deleteReallyAllItems,
					Messages.LaborPrefs_deleteAllExplain)) {
					Query<LabItem> qbli = new Query<LabItem>(LabItem.class);
					List<LabItem> items = qbli.execute();
					for (LabItem li : items) {
						Query<LabResult> qbe = new Query<LabResult>(LabResult.class);
						qbe.add("ItemID", "=", li.getId()); //$NON-NLS-1$ //$NON-NLS-2$
						List<LabResult> list = qbe.execute();
						for (LabResult po : list) {
							po.delete();
						}
						li.delete();
					}
					tv.refresh();
				}
			}
		});
		if (Hub.acl.request(AccessControlDefaults.DELETE_LABITEMS) == false) {
			bDelAllItems.setEnabled(false);
		}
		return ret;
	}
	
	static class LabListLabelProvider extends LabelProvider implements ITableLabelProvider {
		
		public Image getColumnImage(Object element, int columnIndex){
			// TODO Automatisch erstellter Methoden-Stub
			return null;
		}
		
		public String getColumnText(Object element, int columnIndex){
			LabItem li = (LabItem) element;
			switch (columnIndex) {
			case 0:
				return li.getLabor() == null ? Messages.LaborPrefs_unkown : li.getLabor()
					.getLabel();
			case 1:
				return li.getName();
			case 2:
				return li.getKuerzel();
			case 3:
				LabItem.typ typ = li.getTyp();
				if (typ == LabItem.typ.NUMERIC) {
					return Messages.LaborPrefs_numeric;
				} else if (typ == LabItem.typ.TEXT) {
					return Messages.LaborPrefs_alpha;
				} else if (typ == LabItem.typ.DOCUMENT) {
					return Messages.LaborPrefs_document;
				}
				return Messages.LaborPrefs_absolute;
			case 4:
				return li.getEinheit();
			case 5:
				return li.get("RefMann"); //$NON-NLS-1$
			case 6:
				return li.getRefW();
			case 7:
				return li.getGroup() + " - " + li.getPrio(); //$NON-NLS-1$
			default:
				return "?col?"; //$NON-NLS-1$
			}
		}
		
	};
	
	public void init(IWorkbench workbench){
	// Nothing to initialize
	}
	
}
