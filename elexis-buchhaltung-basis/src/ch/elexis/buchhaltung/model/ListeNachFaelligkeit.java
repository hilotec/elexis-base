/*******************************************************************************
 * Copyright (c) 2008, G. Weirich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: ListeNachFaelligkeit.java 1058 2008-12-23 09:09:52Z  $
 *******************************************************************************/
package ch.elexis.buchhaltung.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import util.DateTool;

import ch.elexis.data.Fall;
import ch.elexis.data.Patient;
import ch.elexis.data.Query;
import ch.elexis.data.Rechnung;
import ch.elexis.data.RnStatus;
import ch.unibe.iam.scg.archie.annotations.GetProperty;
import ch.unibe.iam.scg.archie.annotations.SetProperty;
import ch.unibe.iam.scg.archie.model.AbstractDataProvider;
import ch.unibe.iam.scg.archie.model.SetDataException;
import ch.unibe.iam.scg.archie.ui.FieldTypes;

/**
 * An AbstractDataProvider that calculates income due at a given date
 * 
 * @author gerry
 * 
 */
public class ListeNachFaelligkeit extends AbstractDataProvider {
	private static final String NAME = "Rechnungen nach Fälligkeitsdatum";
	private static final String DUE_AFTER_TEXT = "Fällig nach Tagen";
	private static final String DUE_DATE_TEXT = "Stichtag";
	private int dueAfter;
	private DateTool stichTag=new DateTool();
	
	public ListeNachFaelligkeit(){
		super(NAME);
		
	}
	
	@SetProperty(name = DUE_DATE_TEXT)
	public void setStichtag(String stichtag) throws SetDataException{
		stichTag = new DateTool(stichtag);
	}
	
	@GetProperty(name = DUE_DATE_TEXT, fieldType = FieldTypes.TEXT_DATE, index = -2 )
	public String getStichtag(){
		return stichTag.toString(DateTool.DATE_GER);
	}

	@GetProperty(name = DUE_AFTER_TEXT, fieldType = FieldTypes.TEXT_NUMERIC)
	public int getDueAfter(){
		return dueAfter;
	}
	
	@SetProperty(name = DUE_AFTER_TEXT)
	public void setDueAfter(int date){
		dueAfter = date;
	}
	
		
	@Override
	protected IStatus createContent(IProgressMonitor monitor){
		int totalwork = 1000000;
		monitor.beginTask(NAME, totalwork);
		monitor.subTask("Datenbankabfrage");
		Query<Rechnung> qbe = new Query<Rechnung>(Rechnung.class);
		qbe.add("RnStatus", "<>", Integer.toString(RnStatus.BEZAHLT));
		List<Rechnung> rnn = qbe.execute();
		monitor.worked(1000);
		int step = totalwork / rnn.size();
		monitor.subTask("Analysiere Rechnungen");
		ArrayList<Comparable<?>[]> result = new ArrayList<Comparable<?>[]>();
		for (Rechnung rn : rnn) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			if (RnStatus.isActive(rn.getStatus())) {
			DateTool date = new DateTool(rn.getDatumRn());
				date.addDays(dueAfter);
				if (date.isBefore(stichTag)) {
					Comparable<?>[] row = new Comparable[dataSet.getHeadings().size()];
					Fall fall=rn.getFall();
					if(fall!=null){
						Patient pat=fall.getPatient();
						if(pat!=null){
							row[0]=pat.getPatCode();
							row[1] = rn.getNr();
							row[2] = new DateTool(date);
							row[3] = rn.getBetrag().getAmountAsString();
							result.add(row);
						}
					}
					
					
				}
			}
			monitor.worked(step);
		}
		this.dataSet.setContent(result);
		monitor.done();
		return Status.OK_STATUS;
	}
	
	@Override
	protected List<String> createHeadings(){
		List<String> ret = new ArrayList<String>();
		ret.add("Patient-Nr");
		ret.add("Rechnungs Nr.");
		ret.add("Fällig am");
		ret.add("Betrag");
		return ret;
	}
	
	@Override
	public String getDescription(){
		return NAME;
	}
	
}
