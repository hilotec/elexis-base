/*******************************************************************************
 * Copyright (c) 2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: MultilineFieldEditor.java 1281 2006-11-14 16:59:49Z rgw_ch $
 *******************************************************************************/
package ch.elexis.dialogs;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.AbstractElementListSelectionDialog;
import org.eclipse.ui.dialogs.FilteredList;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;

import ch.elexis.Desk;
import ch.elexis.data.DBImage;
import ch.elexis.data.Query;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;

public class ImageChooser extends AbstractElementListSelectionDialog{
	
	private Object[] fElements;
	private Hyperlink hl;
	private Text tTitle;
	private static String NOFILESELECTED="(Bitte Datei wählen)";
	private Button bDB, bFile;
	private DBImage result;
	
	public DBImage getSelection(){
		return result;
	}
	
	public ImageChooser(Shell shell){
		super(shell,new LabelProvider(){
			@Override
			public Image getImage(Object element) {
				if(element instanceof DBImage){
					return ((DBImage)element).getImage();
				}
				return null;
			}

			@Override
			public String getText(Object element) {
				if(element instanceof DBImage){
					return ((DBImage)element).getName();
				}
				return "?";
			}}
		);
	}
	/**
     * Sets the elements of the list.
     * @param elements the elements of the list.
     */
    public void setElements(Object[] elements) {
        fElements = elements;
    }

    /*
     * @see SelectionStatusDialog#computeResult()
     */
    protected void computeResult() {
    	if(bDB.getSelection()){
    		setResult(Arrays.asList(getSelectedElements()));
    		Object[] sel=getResult();
    		if(sel!=null && sel.length>0){
    			result=(DBImage)sel[0];
    		}else{
    			result=null;
    		}
    	}
    }

    private Menu createMenu(Control parent){
    	Menu ret=new Menu(parent);
    	MenuItem item=new MenuItem(ret,SWT.NONE);
    	item.setText("Löschen");
    	item.addSelectionListener(new SelectionAdapter(){
    		@Override
    		public void widgetSelected(SelectionEvent e){
    			Object[] oo=getSelectedElements();
    			if(oo!=null && oo.length>0){
    				if(SWTHelper.askYesNo("Wirklich Bilder löschen", "Wirklich ausgewählte Bilder unwiderruflich löschen?")){
    					for(Object o:oo){
    						((DBImage)o).delete();
    					}
    				}
    			}
    		}
    	});
    	return ret;
    }
    /*
     * @see Dialog#createDialogArea(Composite)
     */
    protected Control createDialogArea(Composite parent) {
        Composite ret = (Composite) super.createDialogArea(parent);
        bDB=new Button(ret,SWT.RADIO);
        bDB.setText("Bild aus Datenbank verwenden");
        createMessageArea(ret);
        createFilterText(ret);
        FilteredList list=createFilteredList(ret);
        list.setMenu(createMenu(list));
        list.addSelectionListener(new SelectionAdapter(){

			@Override
			public void widgetSelected(SelectionEvent e) {
				bFile.setSelection(false);
				bDB.setSelection(true);
			}
        	
        });
        new Label(ret,SWT.SEPARATOR|SWT.HORIZONTAL).setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		bFile=new Button(ret,SWT.RADIO);
		bFile.setText("Oder Bild aus Datei erstellen und importieren");
        Composite cBottom=new Composite(ret,SWT.BORDER);
		cBottom.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		cBottom.setLayout(new GridLayout(2,false));
		new Label(cBottom,SWT.NONE).setText("Datei");
		new Label(cBottom,SWT.NONE).setText("Bildtitel");
		hl=new Hyperlink(cBottom,SWT.NONE);
		tTitle=new Text(cBottom,SWT.BORDER);
		hl.addHyperlinkListener(new HyperlinkAdapter(){

			@Override
			public void linkActivated(HyperlinkEvent e) {
				bFile.setSelection(true);
				bDB.setSelection(false);
				FileDialog fd=new FileDialog(getShell(),SWT.OPEN);
				fd.setFilterExtensions(new String[]{"*.ico","*.png","*.gif","*.jpg","*.*"});
				fd.setFilterNames(new String[]{"Icon-Dateien","Portable Network Graphics",
						"Grafics Interchange Format", "JPEG","Alle Dateien"});
				String filename=fd.open();
				if(filename!=null){
					hl.setText(filename);
					getOkButton().setEnabled(true);
				}
			}
			
		});
		hl.setText(NOFILESELECTED);
		hl.setForeground(Desk.theColorRegistry.get(Desk.COL_BLUE));
		hl.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		tTitle.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		tTitle.addKeyListener(new KeyAdapter(){

			@Override
			public void keyPressed(KeyEvent e) {
				bFile.setSelection(true);
				bDB.setSelection(false);
			}
			
		});
		bDB.setSelection(true);
		
		/*
		final LabeledInputField liTitle=new LabeledInputField(gBottom,"Titel für das Bild");
		liTitle.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		Button bFile=new Button(gBottom,SWT.PUSH);
		GridData gd=new GridData(SWT.RIGHT,SWT.BOTTOM,false,false);
		bFile.setLayoutData(gd);
		bFile.setText("Datei...");
		bFile.addSelectionListener(new SelectionAdapter(){

			@Override
			public void widgetSelected(SelectionEvent e) {
				if(liTitle.getText().length()==0){
					//setErrorMessage("Geben Sie bitte einen Titel für die Grafik ein");
					return;
				}
				FileDialog fd=new FileDialog(getShell(),SWT.OPEN);
				fd.setFilterExtensions(new String[]{"*.ico","*.png","*.gif","*.jpg","*.*"});
				fd.setFilterNames(new String[]{"Icon-Dateien","Portable Network Graphics",
						"Grafics Interchange Format", "JPEG","Alle Dateien"});
				String filename=fd.open();
				if(filename!=null){
					try{
						File file=new File(filename);
						DBImage dbimg=new DBImage(liTitle.getText()+":"+file.getName(),new FileInputStream(file));
					}catch(Exception ex){
						ExHandler.handle(ex);
					}
				}
				super.widgetSelected(e);
			}
			
		});
		*/
		Query<DBImage> qbe=new Query<DBImage>(DBImage.class);
		List<DBImage> imgs=qbe.execute();
		if(imgs!=null){
			fElements=imgs.toArray();
		}else{
			fElements=new Object[0];
		}
        setListElements(fElements);
        setSelection(getInitialElementSelections().toArray());
        return ret;
    }
	
    @Override
    public void okPressed(){
    	if(bFile.getSelection()){
    		String fname=hl.getText();
    		if(!fname.equals(NOFILESELECTED)){
				try{
					File file=new File(fname);
					result=new DBImage(tTitle.getText()+":"+file.getName(),new FileInputStream(file));
				}catch(Exception ex){
					ExHandler.handle(ex);
				}
    		}
    	}
		super.okPressed();
    }

	@Override
	public void create(){
		super.create();
		getShell().setText("Bild aus Datenbank auswählen");
		setMessage("Bitte wählen Sie ein Bild oder erstellen Sie eines aus einer Datei");
		setTitle("Bildauswahl");
	}
	
}
