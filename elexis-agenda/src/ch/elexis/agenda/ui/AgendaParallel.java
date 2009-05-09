/*******************************************************************************
 * Copyright (c) 2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Sponsoring:
 * 	 mediX Notfallpaxis, diepraxen Stauffacher AG, Zürich
 * 
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: AgendaParallel.java 5283 2009-05-09 16:45:09Z rgw_ch $
 *******************************************************************************/

package ch.elexis.agenda.ui;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import ch.elexis.Hub;
import ch.elexis.agenda.data.IPlannable;
import ch.elexis.agenda.preferences.PreferenceConstants;
import ch.elexis.util.Plannables;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.StringTool;

/**
 * A View to display ressources side by side in the same view.
 * 
 * @author gerry
 * 
 */
public class AgendaParallel extends BaseView {
	private static final String DEFAULT_PIXEL_PER_MINUTE="1.0";
	
	private Label[] labels;
	private ProportionalSheet sheet;
	
	
	public AgendaParallel(){
		
	}
	
	@Override
	protected void create(Composite parent){
		ScrolledComposite bounding = new ScrolledComposite(parent, SWT.V_SCROLL);
		bounding.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		sheet=new ProportionalSheet(bounding,this);
		bounding.setContent(sheet);
		
	}
	
	@Override
	protected IPlannable getSelection(){
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected void refresh(){
		String resources=Hub.localCfg.get(PreferenceConstants.AG_RESOURCESTOSHOW, StringTool.join(agenda.getResources(),","));
		if(resources!=null){
			String[] toShow=resources.split(",");
			for(int i=0;i<toShow.length;i++){
				List<IPlannable> termine=Plannables.loadTermine(toShow[i], agenda.getActDate());
				sheet.addAppointments(termine, i);
			}
			sheet.recalc();
		}
	}

	public static double getPixelPerMinute(){
		String ppm=Hub.localCfg.get(PreferenceConstants.AG_PIXEL_PER_MINUTE, DEFAULT_PIXEL_PER_MINUTE);
		try{
			double ret=Double.parseDouble(ppm);
			return ret;
		}catch(NumberFormatException ne){
			Hub.localCfg.set(PreferenceConstants.AG_PIXEL_PER_MINUTE, DEFAULT_PIXEL_PER_MINUTE);
			return Double.parseDouble(DEFAULT_PIXEL_PER_MINUTE);
		}
	}
}
