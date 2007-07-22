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
 *    $Id: NeuerFallDialog.java 2866 2007-07-22 17:30:40Z rgw_ch $
 *******************************************************************************/

package ch.elexis.dialogs;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import ch.elexis.actions.GlobalEvents;
import ch.elexis.data.Fall;
import ch.elexis.data.Patient;
import ch.elexis.views.FallDetailBlatt2;

public class NeuerFallDialog extends TitleAreaDialog {
	Fall fall;
	Patient pat;
	
	public NeuerFallDialog(Shell shell, Fall f){
		super(shell);
		fall=f;
		if(fall==null){
			pat=GlobalEvents.getSelectedPatient();
			fall=pat.neuerFall(Messages.getString("NeuerFallDialog.0"), Messages.getString("NeuerFallDialog.1"), Messages.getString("NeuerFallDialog.2")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			GlobalEvents.getInstance().fireSelectionEvent(fall);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		FallDetailBlatt2 fdb=new FallDetailBlatt2(parent);
		return fdb;
	}

	@Override
	public void create() {
		super.create();
		setMessage(Messages.getString("NeuerFallDialog.enterCaseData")); //$NON-NLS-1$
		setTitle(Messages.getString("NeuerFallDialog.createNewCase")); //$NON-NLS-1$
		getShell().setText(Messages.getString("NeuerFallDialog.newCase")); //$NON-NLS-1$
	}

	@Override
	protected void okPressed() {
		// TODO Auto-generated method stub
		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		fall.delete();
		//GlobalEvents.getInstance().clearSelection(Fall.class, null);
		GlobalEvents.getInstance().fireUpdateEvent(Fall.class);
		super.cancelPressed();
	}
	
	
}
