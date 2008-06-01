/*******************************************************************************
 * Copyright (c) 2007, Daniel Lutz and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Daniel Lutz - initial implementation
 *    
 *  $Id: UserCasePreferences.java 3991 2008-06-01 13:32:22Z rgw_ch $
 *******************************************************************************/
package ch.elexis.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.data.Fall;
import ch.rgw.IO.InMemorySettings;

/**
 * User specific settings: Case defaults
 */
public class UserCasePreferences extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {
	
	public static final String ID = "ch.elexis.preferences.UserCasePreferences";

	public UserCasePreferences() {
		super(GRID);
		setPreferenceStore(new SettingsPreferenceStore(new InMemorySettings()));
		setDescription("Fälle");
	}

	@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor(PreferenceConstants.USR_DEFCASELABEL, "Standard-Bezeichnung", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.USR_DEFCASEREASON, "Standard-Grund", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.USR_DEFLAW, "Standard-Abrechnungssystem", getFieldEditorParent()));
    }

	public void init(IWorkbench workbench) {
		getPreferenceStore().setValue(PreferenceConstants.USR_DEFCASELABEL, Fall.getDefaultCaseLabel());
		getPreferenceStore().setValue(PreferenceConstants.USR_DEFCASEREASON, Fall.getDefaultCaseReason());
		getPreferenceStore().setValue(PreferenceConstants.USR_DEFLAW, Fall.getDefaultCaseLaw());
    }

        @Override
	public boolean performOk() {
		super.performOk();

		Hub.userCfg.set(PreferenceConstants.USR_DEFCASELABEL, getPreferenceStore().getString(PreferenceConstants.USR_DEFCASELABEL));
		Hub.userCfg.set(PreferenceConstants.USR_DEFCASEREASON, getPreferenceStore().getString(PreferenceConstants.USR_DEFCASEREASON));
		Hub.userCfg.set(PreferenceConstants.USR_DEFLAW, getPreferenceStore().getString(PreferenceConstants.USR_DEFLAW));

        return true;
	}
    
	@Override
    protected void performDefaults()
    {  
       this.initialize();
    }
}
