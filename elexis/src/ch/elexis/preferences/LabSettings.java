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
 *  $Id: LabSettings.java 3433 2007-12-10 16:52:26Z rgw_ch $
 *******************************************************************************/

package ch.elexis.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.admin.AccessControlDefaults;
import ch.elexis.preferences.inputs.PrefAccessDenied;

public class LabSettings extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {
	public static final String KEEP_UNSEEN_LAB_RESULTS="lab/keepUnseen";
	
	
	public LabSettings() {
		super(GRID);
		setPreferenceStore(new SettingsPreferenceStore(Hub.globalCfg));
	}

		@Override
	protected Control createContents(final Composite parent) {
		if(Hub.acl.request(AccessControlDefaults.LAB_SEEN)){		
			return super.createContents(parent);
		}else{
			return new PrefAccessDenied(parent);
		}
	}

		@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor(KEEP_UNSEEN_LAB_RESULTS,"Neue Laborwerte anzeigen (Tage)"
					,getFieldEditorParent()));
		
	}

	public void init(final IWorkbench workbench) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean performOk() {
		Hub.userCfg.flush();
		return super.performOk();
	}

	
}
