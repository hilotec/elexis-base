/*******************************************************************************
 * Copyright (c) 2005-2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: MediDetailDialog.java 5328 2009-05-30 06:53:39Z rgw_ch $
 *******************************************************************************/

package ch.elexis.dialogs;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import ch.elexis.data.Prescription;
import ch.elexis.util.SWTHelper;

public class MediDetailDialog extends TitleAreaDialog {
	Prescription art;
	Text dosis;
	Text einnahme;
	
	public MediDetailDialog(Shell shell, Prescription a){
		super(shell);
		art = a;
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		ret.setLayout(new GridLayout());
		new Label(ret, SWT.NONE).setText(Messages.getString("MediDetailDialog.dosage")); //$NON-NLS-1$
		dosis = new Text(ret, SWT.BORDER);
		dosis.setText(art.getDosis());
		new Label(ret, SWT.NONE).setText(Messages.getString("MediDetailDialog.prescription")); //$NON-NLS-1$
		einnahme = new Text(ret, SWT.MULTI);
		einnahme.setText(art.getBemerkung());
		einnahme.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		return ret;
	}
	
	@Override
	public void create(){
		super.create();
		setTitle(art.getArtikel().getLabel());
		setMessage(Messages.getString("MediDetailDialog.pleaseEnterPrescription")); //$NON-NLS-1$
		getShell().setText(Messages.getString("MediDetailDialog.articleDetail")); //$NON-NLS-1$
		
	}
	
	@Override
	protected void okPressed(){
		art.setDosis(dosis.getText());
		art.set(Prescription.REMARK, einnahme.getText());
		super.okPressed();
	}
	
}
