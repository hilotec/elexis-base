/*******************************************************************************
 * Copyright (c) 2006-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: RnDetailView.java 4258 2008-08-11 13:59:37Z rgw_ch $
 *******************************************************************************/
package ch.elexis.views.rechnung;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.util.SWTHelper;

public class RnDetailView extends ViewPart {
	public final static String ID="ch.elexis.RechnungsDetailView"; //$NON-NLS-1$
	RechnungsBlatt blatt;
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout());
		blatt=new RechnungsBlatt(parent,getViewSite());
		blatt.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));

	}

	@Override
	public void setFocus() {
		blatt.setFocus();
	}

	
}
