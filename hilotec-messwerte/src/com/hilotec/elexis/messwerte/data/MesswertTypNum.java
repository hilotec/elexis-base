/*******************************************************************************
 * Copyright (c) 2009, A. Kaufmann and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    A. Kaufmann - initial implementation 
 *    
 * $Id: MesswertTypNum.java 5386 2009-06-23 11:34:17Z rgw_ch $
 *******************************************************************************/

package com.hilotec.elexis.messwerte.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swt.widgets.Text;

import ch.elexis.util.SWTHelper;

/**
 * @author Antoine Kaufmann
 */
public class MesswertTypNum extends MesswertBase implements IMesswertTyp {
	double defVal = 0.0;
	
	public MesswertTypNum(String n, String t, String u) {
		super(n, t, u);
	}
	
	public String erstelleDarstellungswert(Messwert messwert) {
		return messwert.getWert();
	}

	public String getDefault() {
		return Double.toString(defVal);
	}

	public void setDefault(String str) {
		defVal = Double.parseDouble(str);
	}
	
	public Widget createWidget(Composite parent, Messwert messwert) {
		Text text = SWTHelper.createText(parent, 1, SWT.NONE);
		text.setText(messwert.getWert());
		return text;
	}
	
	public void saveInput(Widget widget, Messwert messwert) {
		Text text = (Text) widget;
		messwert.setWert(text.getText());
	}
}