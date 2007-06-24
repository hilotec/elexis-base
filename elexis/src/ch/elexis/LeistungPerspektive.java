/*******************************************************************************
 * Copyright (c) 2005-2006, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: LeistungPerspektive.java 1129 2006-10-19 11:20:27Z rgw_ch $
 *******************************************************************************/
package ch.elexis;

import org.eclipse.swt.SWT;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import ch.elexis.preferences.PreferenceConstants;
import ch.elexis.views.Starter;
import ch.elexis.views.codesystems.CodeDetailView;
import ch.elexis.views.codesystems.LeistungenView;

/**
 * Anzeige der Detailviews aller Leistungstypen.  
 * @author gerry
 *
 */
public class LeistungPerspektive implements IPerspectiveFactory {
	public static final String ID="ch.elexis.LeistungPerspektive"; //$NON-NLS-1$
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		layout.setFixed(false);
		if(Hub.localCfg.get(PreferenceConstants.SHOWSIDEBAR,"true").equals("true")){ //$NON-NLS-1$ //$NON-NLS-2$
			layout.addStandaloneView(Starter.ID,false,SWT.LEFT,0.1f,editorArea);
		}
		layout.addPlaceholder(LeistungenView.ID,SWT.LEFT,0.3f,editorArea);
		layout.addView(CodeDetailView.ID,SWT.RIGHT,0.8f,editorArea);
		layout.addShowViewShortcut(CodeDetailView.ID);

	}

}
