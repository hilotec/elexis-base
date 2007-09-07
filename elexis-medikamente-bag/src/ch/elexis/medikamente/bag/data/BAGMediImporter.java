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
 *  $Id: BAGMediImporter.java 3106 2007-09-07 05:14:37Z rgw_ch $
 *******************************************************************************/
package ch.elexis.medikamente.bag.data;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;

import ch.elexis.data.Artikel;
import ch.elexis.data.Query;
import ch.elexis.importers.ExcelWrapper;
import ch.elexis.util.ImporterPage;

public class BAGMediImporter extends ImporterPage {

	public BAGMediImporter() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Composite createPage(Composite parent) {
		FileBasedImporter fbi=new FileBasedImporter(parent,this);
		return fbi;
	}

	@Override
	public IStatus doImport(IProgressMonitor monitor) throws Exception {
		ExcelWrapper ew=new ExcelWrapper();
		if(ew.load(results[0], 1)){
			int f=ew.getFirstRow();
			int l=ew.getLastRow();
			monitor.beginTask("Import BAG-Medikamente", l-f);
			for(int i=f;i<l;i++){
				List<String> row=ew.getRow(i);
				importUpdate(row.toArray(new String[0]));
				monitor.worked(1);
			}
			monitor.done();
			return Status.OK_STATUS;
		}
		return Status.CANCEL_STATUS;
	}

	/**
	 * Import a medicament from one row of the BAG-Medi file
	 * @param row
	 * @return
	 */
	public static boolean importUpdate(String[] row){
		String pharmacode=row[2];
		Query<Artikel> qbe=new Query<Artikel>(Artikel.class);
		String id=qbe.findSingle("SubID", "=", pharmacode);
		BAGMedi imp;
		if(id==null){
			imp=new BAGMedi(row[7],pharmacode);
		}else{
			imp=BAGMedi.load(id);
			imp.update(row);
		}
			
		return true;
	}

	@Override
	public String getDescription() {
		return "Import Medikamentenliste BAG";
	}

	@Override
	public String getTitle() {
		return "Medi-BAG";
	}

}
