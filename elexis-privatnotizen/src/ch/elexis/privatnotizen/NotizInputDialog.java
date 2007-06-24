/*******************************************************************************
 * Copyright (c) 2006, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: NotizInputDialog.java 808 2006-08-28 04:54:26Z rgw_ch $
 *******************************************************************************/

package ch.elexis.privatnotizen;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import ch.elexis.Desk;
import ch.elexis.util.SWTHelper;

public class NotizInputDialog extends TitleAreaDialog {
	private Privatnotiz mine;
	private Text textField;
	public NotizInputDialog(Shell shell, Privatnotiz note) {
		super(shell);
		mine=note;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		textField=new Text(parent,SWT.MULTI|SWT.BORDER);
		textField.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		textField.setText(mine.getText());
		return textField;
	}

	@Override
	public void create() {
		super.create();
		setTitle("Notiz zum KG Eintrag");
		getShell().setText("Privatnotiz");
		setTitleImage(Desk.theImageRegistry.get(Desk.IMG_LOGO48));
		setMessage("Geben Sie hier private Notizen zu diesem KG-Eintrag ein");
	}

	@Override
	protected void okPressed() {
		mine.setText(textField.getText());
		super.okPressed();
	}
	
}
