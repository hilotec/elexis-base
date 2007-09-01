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
 *  $Id: DefaultOutputter.java 3054 2007-09-01 16:36:23Z rgw_ch $
 *******************************************************************************/

package ch.elexis.views.rechnung;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import ch.elexis.data.Fall;
import ch.elexis.data.Rechnung;
import ch.elexis.util.IRnOutputter;
import ch.elexis.util.Result;
import ch.elexis.util.SWTHelper;

/**
 * This outputter takes the output target from the case's billing syste,
 * @author Gerry
 *
 */
public class DefaultOutputter implements IRnOutputter {
	private ArrayList<IRnOutputter> configured=new ArrayList<IRnOutputter>();
	
	public boolean canBill(Fall fall) {
		return fall.getOutputter().canBill(fall);
	}

	public boolean canStorno(Rechnung rn) {
		return rn.getFall().getOutputter().canStorno(rn);
	}

	public Control createSettingsControl(Composite parent) {
		Label lbl=new Label(parent,SWT.WRAP);
		lbl.setText("Für jede Rechnung das zum Abrechnungssystem des Falls gehörende\nStandard-Ausgabeziel wählen.");
		return lbl;
	}

	public Result<Rechnung> doOutput(TYPE type, Collection<Rechnung> rnn) {
		Result<Rechnung> res=new Result<Rechnung>(null);
		for(Rechnung rn:rnn){
			Fall fall=rn.getFall();
			final IRnOutputter iro=fall.getOutputter();
			if(!configured.contains(iro)){
				SWTHelper.SimpleDialog dlg=new SWTHelper.SimpleDialog(new SWTHelper.IControlProvider(){
					public Control getControl(Composite parent) {
						return iro.createSettingsControl(parent);

					}});
				if(dlg.open()==Dialog.OK){
					configured.add(iro);
				}else{
					continue;
				}
			}
			res.add(fall.getOutputter().doOutput(type, Arrays.asList(new Rechnung[]{rn})));
		}
		return null;
	}

	public String getDescription() {
		return "Fall-Standardausgabe";
	}

}
