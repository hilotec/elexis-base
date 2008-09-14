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
 *  $Id: ResultElement.java 4414 2008-09-14 20:08:24Z rgw_ch $
 *******************************************************************************/

package ch.elexis.exchange.elements;

import java.util.List;

import org.jdom.Element;


import ch.elexis.data.LabResult;
import ch.elexis.exchange.XChangeContainer;
import ch.elexis.util.XMLTool;
import ch.rgw.tools.TimeTool;

@SuppressWarnings("serial")
public class ResultElement extends XChangeElement {
	public static final String XMLNAME="result";
	public static final String ATTR_DATE="timestamp";
	public static final String ATTR_NORMAL="isNormal";
	public static final String ATTR_LABITEM="findingRef";
	public static final String ELEMENT_META="meta";
	public static final String ATTRIB_CREATOR="creator";
	public static final String ELEMENT_IMAGE="image";
	public static final String ELEMENT_TEXTRESULT="textResult";
	public static final String ELEMENT_DOCRESULT="documentRef";

	
	@Override
	public String getXMLName() {
		return XMLNAME;
	}
	
	public ResultElement(XChangeContainer parent){
		super(parent);
	}

	public static ResultElement addResult(MedicalElement me, LabResult lr){
		List<FindingElement> findings=me.getAnalyses();
		for(FindingElement fe:findings){
			if(fe.getXid().getID().equals(XMLTool.idToXMLID(lr.getItem().getId()))){
				ResultElement re=new ResultElement(me.getContainer(),lr);
				me.addAnalyse(re);
				return re;
			}
		}
		FindingElement fe=new FindingElement(me.getContainer(),lr.getItem());
		me.addFindingItem(fe);
		ResultElement re=new ResultElement(me.getContainer(),lr);
		me.addAnalyse(re);
		return re;
	}
	private ResultElement(XChangeContainer home, LabResult lr){
		super(home);
		setAttribute("id",XMLTool.idToXMLID(lr.getId()));
		setAttribute(ATTR_DATE, new TimeTool(lr.getDate()).toString(TimeTool.DATETIME_XML));
		setAttribute(ATTR_LABITEM, ch.elexis.util.XMLTool.idToXMLID(lr.getItem().getId()));
		Element eResult=new Element(ELEMENT_TEXTRESULT,home.getNamespace());
		eResult.setText(lr.getResult());
		addContent(eResult);
		// setAttribute(ATTR_NORMAL,);	// TODO
		home.addChoice(this, lr.getLabel(), lr);
	}
}
