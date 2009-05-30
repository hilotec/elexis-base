/*******************************************************************************
 * Copyright (c) 2005, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    D. Lutz    - adapted for importing data from other databases
 *    
 *    $Id: DBImportSecondPage.java 5331 2009-05-30 13:01:05Z rgw_ch $
 *******************************************************************************/

package ch.elexis.wizards;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import ch.elexis.util.SWTHelper;

public class DBImportSecondPage extends WizardPage {
	Text name,pwd;
	public DBImportSecondPage(String pageName) {
		super("Credentials"); //$NON-NLS-1$
		setTitle(Messages.getString("DBImportSecondPage.userDetails")); //$NON-NLS-1$
		setMessage(Messages.getString("DBImportSecondPage.enterUsername")+ //$NON-NLS-1$
				Messages.getString("DBImportSecondPage.enterPassword")); //$NON-NLS-1$
	}

	public DBImportSecondPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		// TODO Automatisch erstellter Konstruktoren-Stub
	}
	
	public void createControl(Composite parent) {
		DBImportWizard wiz=(DBImportWizard) getWizard();
		Composite form = new Composite(parent,SWT.NONE);
		form.setLayout(new GridLayout(1,false));
		name=new Text(form,SWT.BORDER);
		name.setLayoutData(SWTHelper.getFillGridData(1,true,1,false));
		pwd=new Text(form,SWT.BORDER);
		pwd.setLayoutData(SWTHelper.getFillGridData(1,true,1,false));
		if((wiz.preset!=null) && (wiz.preset.length==5)){
			name.setText(wiz.preset[3]);
			pwd.setText(wiz.preset[4]);
		}
		setControl(form);
	}

}
