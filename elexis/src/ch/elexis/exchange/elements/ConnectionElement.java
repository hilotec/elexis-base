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
 *  $Id: ConnectionElement.java 2618 2007-06-24 10:08:05Z rgw_ch $
 *******************************************************************************/

package ch.elexis.exchange.elements;

import org.jdom.Element;

import ch.elexis.exchange.XChangeContainer;

public class ConnectionElement extends XChangeElement {

	public ConnectionElement(XChangeContainer parent, String type, String id){
		super(parent);
		e=new Element("connection",XChangeContainer.ns);
		setType(type);
		setId(id);
	}
	
	public ConnectionElement(XChangeContainer parent, Element el){
		super(parent,el);
	}
	
	public void setType(String type){
		e.setAttribute("type", type);
	}
	
	public String getType(){
		return getAttr("type");
	}
	
	public void setId(String id){
		e.setAttribute("id", id);
	}
	
	public String getId(){
		return getAttr("id");
	}
}
