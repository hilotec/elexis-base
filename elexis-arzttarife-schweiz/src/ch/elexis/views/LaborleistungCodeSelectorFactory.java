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
 * $Id: LaborleistungCodeSelectorFactory.java 5097 2009-02-06 11:58:59Z rgw_ch $
 *******************************************************************************/

package ch.elexis.views;

import org.eclipse.swt.SWT;

import ch.elexis.actions.FlatDataLoader;
import ch.elexis.data.LaborLeistung;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.util.viewers.CommonViewer;
import ch.elexis.util.viewers.DefaultControlFieldProvider;
import ch.elexis.util.viewers.DefaultLabelProvider;
import ch.elexis.util.viewers.SimpleWidgetProvider;
import ch.elexis.util.viewers.ViewerConfigurer;
import ch.elexis.views.codesystems.CodeSelectorFactory;

public class LaborleistungCodeSelectorFactory extends CodeSelectorFactory {
	// private AbstractDataLoaderJob dataloader;
	private ViewerConfigurer vc;
	private FlatDataLoader fdl;
	
	public LaborleistungCodeSelectorFactory(){}
	
	@Override
	public ViewerConfigurer createViewerConfigurer(CommonViewer cv){
		fdl = new FlatDataLoader(cv, new Query<LaborLeistung>(LaborLeistung.class));
		fdl.setOrderField("Text");
		vc =
			new ViewerConfigurer(fdl, new DefaultLabelProvider(), new DefaultControlFieldProvider(
				cv, new String[] {
					"Code", "Text"}), //$NON-NLS-1$ //$NON-NLS-2$
				new ViewerConfigurer.DefaultButtonProvider(), new SimpleWidgetProvider(
					SimpleWidgetProvider.TYPE_LAZYLIST, SWT.NONE, null));
		return vc;
	}
	
	@Override
	public Class<? extends PersistentObject> getElementClass(){
		return LaborLeistung.class;
	}
	
	@Override
	public void dispose(){

	}
	
	@Override
	public String getCodeSystemName(){
		return "Analysetarif"; //$NON-NLS-1$
	}
	
	@Override
	public PersistentObject findElement(String code){
		String id = new Query<LaborLeistung>(LaborLeistung.class).findSingle("Code", "=", code);
		if (id != null) {
			return LaborLeistung.load(id);
		}
		return null;
	}
	
}
