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
 *    $Id: CodeSelectorFactory.java 2883 2007-07-24 05:17:52Z rgw_ch $
 *******************************************************************************/
package ch.elexis.eigendiagnosen.views;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;

import ch.elexis.actions.JobPool;
import ch.elexis.actions.LazyTreeLoader;
import ch.elexis.data.Query;
import ch.elexis.eigendiagnosen.data.Eigendiagnose;
import ch.elexis.util.CommonViewer;
import ch.elexis.util.DefaultControlFieldProvider;
import ch.elexis.util.SimpleWidgetProvider;
import ch.elexis.util.TreeContentProvider;
import ch.elexis.util.ViewerConfigurer;

public class CodeSelectorFactory extends
		ch.elexis.views.codesystems.CodeSelectorFactory {
	private LazyTreeLoader<Eigendiagnose> dataloader;
	private static final String LOADER_NAME="Eigendiagnosen";
	

	@SuppressWarnings("unchecked")
	public CodeSelectorFactory(){
		dataloader=(LazyTreeLoader<Eigendiagnose>) JobPool.getJobPool().getJob(LOADER_NAME); //$NON-NLS-1$
		
		if(dataloader==null){
			dataloader=new LazyTreeLoader<Eigendiagnose>(LOADER_NAME,new Query<Eigendiagnose>(Eigendiagnose.class),"parent",new String[]{"Kuerzel","Text"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			dataloader.setParentField("Kuerzel");
			JobPool.getJobPool().addJob(dataloader);
		}
		JobPool.getJobPool().activate(LOADER_NAME,Job.SHORT); //$NON-NLS-1$

	}
	@Override
	public ViewerConfigurer createViewerConfigurer(CommonViewer cv) {
		ViewerConfigurer vc=new ViewerConfigurer(
				new TreeContentProvider(cv,dataloader),
				new ViewerConfigurer.TreeLabelProvider(),
				new DefaultControlFieldProvider(cv, new String[]{"Kuerzel","Text"}), //$NON-NLS-1$
				new ViewerConfigurer.DefaultButtonProvider(),
				new SimpleWidgetProvider(SimpleWidgetProvider.TYPE_TREE, SWT.NONE,null)
				);
		return vc;

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getCodeSystemName() {
		return Eigendiagnose.CODESYSTEM_NAME;
	}

	@Override
	public Class getElementClass() {
		return Eigendiagnose.class;
	}

}
