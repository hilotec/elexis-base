/*******************************************************************************
 * Copyright (c) 2005-2006, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *    $Id: ArtikelSelektor.java 2508 2007-06-08 14:32:48Z danlutz $
 *******************************************************************************/

package ch.elexis.views.artikel;

import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.actions.GlobalActions;
import ch.elexis.data.Artikel;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.dialogs.ArtikelDetailDialog;
import ch.elexis.preferences.PreferenceConstants;
import ch.elexis.util.*;
import ch.elexis.views.codesystems.CodeSelectorFactory;
import ch.rgw.tools.ExHandler;

public class ArtikelSelektor extends ViewPart implements ISaveablePart2{
	public static final String ID="ch.elexis.ArtikelSelektor";
	CTabFolder ctab;
	TableViewer tv;
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout());
		ctab=new CTabFolder(parent,SWT.NONE);
		ctab.setLayoutData(SWTHelper.getFillGridData(1,true,1,true));
		java.util.List<IConfigurationElement> list=Extensions.getExtensions("ch.elexis.Verrechnungscode");
		for(IConfigurationElement ice:list){
			if("Artikel".equals(ice.getName())){
				try{
					CodeSelectorFactory cs=(CodeSelectorFactory)ice.createExecutableExtension("CodeSelectorFactory");
					CTabItem ci=new CTabItem(ctab,SWT.NONE);
					CommonViewer cv=new CommonViewer();
					ViewerConfigurer vc=cs.createViewerConfigurer(cv);
					Composite c=new Composite(ctab,SWT.NONE);
					c.setLayout(new GridLayout());
					cv.create(vc,c,SWT.V_SCROLL,getViewSite());
					ci.setControl(c);
					ci.setData(cv);
					ci.setText(cs.getCodeSystemName());
					cv.addDoubleClickListener(new CommonViewer.DoubleClickListener(){

						public void doubleClicked(PersistentObject obj, CommonViewer cv) {
							new ArtikelDetailDialog(getViewSite().getShell(),obj).open();
							
						}});
					vc.getContentProvider().startListening();
				}catch(Exception ex){
					ExHandler.handle(ex);
				}
			}
		}
		CTabItem ci=new CTabItem(ctab,SWT.NONE);
		Composite c=new Composite(ctab,SWT.NONE);
		c.setLayout(new GridLayout());
		ci.setControl(c);
		ci.setText("Lagerartikel");
		Table table=new Table(c,SWT.SIMPLE|SWT.V_SCROLL);
		table.setLayoutData(SWTHelper.getFillGridData(1,true,1,true));
		tv=new TableViewer(table);
		tv.setContentProvider(new IStructuredContentProvider(){

			public Object[] getElements(Object inputElement) {
				/*
				Query<Artikel> qbe=new Query<Artikel>(Artikel.class);
				qbe.add("Minbestand","<>","0");
				qbe.or();
				qbe.add("Istbestand","<>","0");
				List<Artikel> l=qbe.execute();
				return l.toArray();
				*/
				return Artikel.getLagerartikel().toArray();
			}

			public void dispose() {	}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			
		});
		//tv.setLabelProvider(new LagerLabelProvider());
		tv.setLabelProvider(new ArtikelLabelProvider());
		tv.addDragSupport(DND.DROP_COPY,new Transfer[]{TextTransfer.getInstance()},new DragSourceAdapter(){
			 public void dragStart(DragSourceEvent event)
	            {
				 	IStructuredSelection sel=(IStructuredSelection) tv.getSelection();
	                if(sel==null || sel.isEmpty()){
	                    event.doit=false;
	                }
	                Object s=sel.getFirstElement();
	                if(s instanceof PersistentObject){
	                    PersistentObject po = (PersistentObject) s;
	                    event.doit=po.isDragOK();
	                }else{
	                    event.doit=false;
	                }
	            }

	            public void dragSetData(DragSourceEvent event)
	            {
	            	IStructuredSelection isel=(IStructuredSelection) tv.getSelection();
	                StringBuilder sb=new StringBuilder();
	                Object[] sel=isel.toArray();
	                for(Object s:sel){
	                	if(s instanceof PersistentObject){
	                        sb.append(((PersistentObject)s).storeToString()).append(",");
	                    }
	                    else{
	                        sb.append("error").append(",");
	                    }
	                }
	                event.data=sb.toString().replace(",$","");
	            }
			
		});
		tv.addDoubleClickListener(new IDoubleClickListener(){

			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel=(IStructuredSelection) tv.getSelection();
				if((sel!=null) && (!sel.isEmpty())){
					Artikel art=(Artikel)sel.getFirstElement();
					new ArtikelDetailDialog(getViewSite().getShell(),art).open();
				}
			}
			
		});
		tv.setInput(getViewSite());
	}

	@Override
	public void setFocus() {
		// TODO Automatisch erstellter Methoden-Stub

	}
	@Override
	public void dispose(){
		
	}
	
	// replaced by ArtikelLabelProvider
	
	class LagerLabelProvider extends DefaultLabelProvider implements ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			if(element instanceof Artikel){
				return null;
			}else{
				return Desk.theImageRegistry.get(Desk.IMG_ACHTUNG);
			}
		}

		public String getColumnText(Object element, int columnIndex) {
			if(element instanceof Artikel){
				Artikel art=(Artikel)element;
				String ret=art.getInternalName();
				if(art.isLagerartikel()){
					ret+=" ("+Integer.toString(art.getTotalCount())+")";
				}
				return ret;
			}
			return super.getColumnText(element, columnIndex);
		}
		
	}
	/* ******
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2
	 * Wir benötigen das Interface nur, um das Schliessen einer View zu verhindern,
	 * wenn die Perspektive fixiert ist.
	 * Gibt es da keine einfachere Methode?
	 */ 
	public int promptToSaveOnClose() {
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL : ISaveablePart2.NO;
	}
	public void doSave(IProgressMonitor monitor) { /* leer */ }
	public void doSaveAs() { /* leer */}
	public boolean isDirty() {
		return true;
	}
	public boolean isSaveAsAllowed() {
		return false;
	}
	public boolean isSaveOnCloseNeeded() {
		return true;
	}
}
