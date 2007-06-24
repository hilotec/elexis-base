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
 *  $Id: XrefExtension.java 2130 2007-03-19 16:33:45Z rgw_ch $
 *******************************************************************************/
package ch.elexis.text;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.ui.PartInitException;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.data.Brief;
import ch.elexis.data.PersistentObject;
import ch.elexis.util.IKonsExtension;
import ch.elexis.views.TextView;
import ch.rgw.tools.ExHandler;

public class XrefExtension implements IKonsExtension {
	public static final String providerID="ch.elexis.text.DocXRef";
	EnhancedTextField tx;
	
	public String connect(EnhancedTextField tf) {
		tx=tf;
		return providerID;
	}

	
	public boolean doLayout(StyleRange n, String provider, String id) {
		n.background=Desk.theColorRegistry.get("hellblau");
		n.foreground=Desk.theColorRegistry.get("grau20");
		return true;
	}

	public boolean doXRef(String refProvider, String refID) {
		try {
			TextView tv=(TextView)Hub.plugin.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(TextView.ID);
			tv.openDocument(Brief.load(refID));
			return true;
		} catch (PartInitException e) {
			ExHandler.handle(e);
		}
		return false;
	}

	public IAction[] getActions() {
		// TODO Auto-generated method stub
		return null;
	}

	public void removeXRef(String refProvider, String refID) {
		// TODO Auto-generated method stub

	}

	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		// TODO Auto-generated method stub

	}


	public void insert(Object o, int pos) {
		
	}


}
