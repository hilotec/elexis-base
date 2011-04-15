/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: BildImportDialog.java 6014 2010-01-31 19:17:37Z rgw_ch $
 *******************************************************************************/

package ch.elexis.images;

import java.io.ByteArrayOutputStream;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ch.elexis.Desk;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.util.SWTHelper;

public class BildImportDialog extends TitleAreaDialog {
	ImageLoader iml;
	Image img;
	Button bJPEG, bPNG;
	Text Titel;
	Text info;
	public Bild result;
	
	BildImportDialog(Shell shell, ImageLoader iml){
		super(shell);
		this.iml = iml;
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		ret.setLayout(new GridLayout());
		Composite cImage = new Composite(ret, SWT.BORDER);
		cImage.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		img = new Image(Desk.getDisplay(), iml.data[0]);
		cImage.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e){
				GC gc = e.gc;
				gc.drawImage(img, 0, 0);
			}
			
		});
		
		Group gFormat = new Group(ret, SWT.BORDER);
		gFormat.setText(Messages.BildImportDialog_StorageFormat);
		bJPEG = new Button(gFormat, SWT.RADIO);
		bJPEG.setText(Messages.BildImportDialog_JPEG_Description);
		bPNG = new Button(gFormat, SWT.RADIO);
		bPNG.setText(Messages.BildImportDialog_PNG_Description);
		gFormat.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		gFormat.setLayout(new GridLayout());
		bJPEG.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		bPNG.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		new Label(ret, SWT.NONE).setText(Messages.BildImportDialog_TitleOfImage);
		Titel = new Text(ret, SWT.BORDER);
		Titel.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		new Label(ret, SWT.NONE).setText(Messages.BildImportDialog_DescriptionOfImage);
		info = new Text(ret, SWT.BORDER | SWT.MULTI);
		info.setText("\n"); //$NON-NLS-1$
		info.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		return ret;
	}
	
	@Override
	public void create(){
		super.create();
		getShell().setText(Messages.BildImportDialog_ImportCaption);
		setTitle(Messages.BildImportDialog_ImportTitle);
		setMessage(Messages.BildImportDialog_ImportMessage);
		setTitleImage(Desk.getImage(Desk.IMG_LOGO48));
	}
	
	@Override
	protected void okPressed(){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int format = SWT.IMAGE_PNG;
		if (bJPEG.getSelection()) {
			format = SWT.IMAGE_JPEG;
		}
		iml.save(baos, format);
		result =
			new Bild(ElexisEventDispatcher.getSelectedPatient(), Titel.getText(), baos
				.toByteArray());
		result.set("Info", info.getText()); //$NON-NLS-1$
		img.dispose();
		super.okPressed();
	}
	
	@Override
	protected void cancelPressed(){
		img.dispose();
		super.cancelPressed();
	}
}
