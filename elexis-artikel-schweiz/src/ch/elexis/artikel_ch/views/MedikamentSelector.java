/*******************************************************************************
 * Copyright (c) 2006-2007, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: MedikamentSelector.java 3452 2007-12-14 08:27:03Z michael_imhof $
 *******************************************************************************/

package ch.elexis.artikel_ch.views;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;

import ch.elexis.actions.AbstractDataLoaderJob;
import ch.elexis.actions.JobPool;
import ch.elexis.actions.ListLoader;
import ch.elexis.artikel_ch.data.ArtikelFactory;
import ch.elexis.artikel_ch.data.Medikament;
import ch.elexis.data.Query;
import ch.elexis.util.CommonViewer;
import ch.elexis.util.DefaultControlFieldProvider;
import ch.elexis.util.LazyContentProvider;
import ch.elexis.util.SimpleWidgetProvider;
import ch.elexis.util.ViewerConfigurer;
import ch.elexis.views.artikel.ArtikelContextMenu;
import ch.elexis.views.artikel.ArtikelLabelProvider;
import ch.elexis.views.codesystems.CodeSelectorFactory;

public class MedikamentSelector extends CodeSelectorFactory {
	AbstractDataLoaderJob dataloader;
	
	public MedikamentSelector() {
		dataloader=(AbstractDataLoaderJob) JobPool.getJobPool().getJob("Medikamente");
		if(dataloader==null){
			dataloader=new ListLoader("Medikamente",new Query<Medikament>(Medikament.class),new String[]{"Name"});
			JobPool.getJobPool().addJob(dataloader);
		}
		JobPool.getJobPool().activate("Medikamente",Job.SHORT);
	}

	@Override
	public ViewerConfigurer createViewerConfigurer(CommonViewer cv) {
		new ArtikelContextMenu((Medikament)new ArtikelFactory().createTemplate(Medikament.class),cv);
		return new ViewerConfigurer(
				new LazyContentProvider(cv,dataloader,null),
				new ArtikelLabelProvider(),
				new MedikamentControlFieldProvider(cv, new String[]{"Name"}),
				new ViewerConfigurer.DefaultButtonProvider(),
				new SimpleWidgetProvider(SimpleWidgetProvider.TYPE_LAZYLIST, SWT.NONE,null)
		);
	}

	@Override
	public Class getElementClass() {
		return Medikament.class;
	}

	@Override
	public void dispose() {
		
	}

	@Override
	public String getCodeSystemName() {
		return "Medikamente";
	}

}
