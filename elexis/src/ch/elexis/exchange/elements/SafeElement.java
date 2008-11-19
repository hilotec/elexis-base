/*******************************************************************************
 * Copyright (c) 2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: SafeElement.java 4673 2008-11-09 17:01:26Z rgw_ch $
 *******************************************************************************/

package ch.elexis.exchange.elements;

import org.jdom.Element;
import org.jdom.Namespace;

import ch.rgw.tools.StringTool;

public class SafeElement extends Element {
	public SafeElement(String name, Namespace ns){
		super(name, ns);
	}
	
	public SafeElement(String name){
		super(name);
	}
	
	/**
	 * This sets an attribute in a safe manner: If the value is null, some useful action is taken
	 * instead og throwing an exception
	 * 
	 * @param name
	 *            name of the attribute
	 * @param value
	 *            value of the attribute
	 * @param defaultValue
	 *            default to use if value resolves to null. If defaultValue is null, the attirbute
	 *            will not be set at all.
	 */
	public void setAttributeEx(String name, String value, String defaultValue){
		if (StringTool.isNothing(name)) {
			return;
		}
		if (StringTool.isNothing(value)) {
			if (defaultValue == null) {
				return;
			} else {
				setAttribute(name, defaultValue);
			}
		} else {
			setAttribute(name, value);
		}
	}
}