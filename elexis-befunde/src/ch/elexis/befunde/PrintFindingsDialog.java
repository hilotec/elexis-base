/*******************************************************************************
 * Copyright (c) 2007, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: PrintFindingsDialog.java 2516 2007-06-12 15:56:07Z rgw_ch $
 *******************************************************************************/
package ch.elexis.befunde;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import ch.elexis.Hub;
import ch.elexis.data.Brief;
import ch.elexis.text.ITextPlugin;
import ch.elexis.text.TextContainer;
import ch.elexis.text.ITextPlugin.ICallback;
import ch.elexis.util.SWTHelper;

public class PrintFindingsDialog extends TitleAreaDialog implements ICallback {
	String[][] fields;
	
	public PrintFindingsDialog(Shell parentShell, String[][] fields) {
		super(parentShell);
		this.fields=fields;
	}

			@Override
	protected Control createDialogArea(Composite parent) {
		Composite ret=new Composite(parent,SWT.NONE);
		TextContainer text=new TextContainer(getShell());
		ret.setLayout(new FillLayout());
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		text.getPlugin().createContainer(ret, this);
		text.getPlugin().showMenu(false);
		text.getPlugin().showToolbar(false);
		text.createFromTemplateName(null, "Messwerte", Brief.UNKNOWN, Hub.actUser, "Messwerte");
		text.getPlugin().setFont("Helvetica", SWT.NORMAL, 9);
		text.getPlugin().insertTable("[Tabelle]", ITextPlugin.FIRST_ROW_IS_HEADER, fields, null);
		return ret;
	}

	@Override
	public void create() {
		super.create();
		getShell().setText("Messwerte");
		setTitle("Messwerte drucken");
		setMessage("Dies druckt die aktuell angezeigte Seite der Messwerte");
		getShell().setSize(900,700);
	}

	@Override
	protected void okPressed() {
		super.okPressed();
	}

	public void save() {
		// TODO Auto-generated method stub
		
	}

	public boolean saveAs() {
		// TODO Auto-generated method stub
		return false;
	}
		
}
