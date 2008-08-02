/*******************************************************************************
 * Copyright (c) 2006-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: FindingElement.java 4224 2008-08-02 19:12:53Z rgw_ch $
 *******************************************************************************/

package ch.elexis.exchange.elements;

import java.util.List;

import org.jdom.Element;

import ch.elexis.data.LabItem;
import ch.elexis.data.LabResult;
import ch.elexis.data.Labor;
import ch.elexis.exchange.XChangeContainer;
import ch.elexis.exchange.XIDHandler;
import ch.rgw.tools.TimeTool;

@SuppressWarnings("serial")
public class FindingElement extends XChangeElement{
	public static final String ENCLOSING="findings";
	public static final String XMLNAME="finding";
	public static final String ATTR_NAME="name";
	public static final String ATTR_NORMRANGE="normRange";
	public static final String ATTR_TYPE="type";
	public static final String ATTR_UNITS="unit";
	public static final String ATTR_GROUP="group";
	
	public static final String ELEMENT_XID="xid";
	public static final String XIDBASE="www.xid.ch/labitems/";

	public static final String TYPE_NUMERIC="numeric";
	public static final String TYPE_TEXT="text";
	public static final String TYPE_IMAGE="image";
	public static final String TYPE_ABSOLUTE="absolute";
	
	
	
	public String getXMLName(){
		return XMLNAME;
	}
	
	FindingElement(XChangeContainer p){
		super(p);
	}
	
	
	
	
	FindingElement(XChangeContainer home, LabItem li){
		super(home);

		setAttribute(ATTR_NAME, li.getKuerzel());
			if(li.getTyp().equals(LabItem.typ.NUMERIC)){
			setAttribute(ATTR_TYPE,TYPE_NUMERIC);
			setAttribute(ATTR_NORMRANGE,li.getRefM());		// TODO anpassen
			setAttribute(ATTR_UNITS,li.getEinheit());
			
		}else if(li.getTyp().equals(LabItem.typ.ABSOLUTE)){
			setAttribute(ATTR_TYPE,TYPE_ABSOLUTE);
		}else if(li.getTyp().equals(LabItem.typ.TEXT)){
			setAttribute(ATTR_TYPE,TYPE_TEXT);
		}
		setAttribute(ATTR_GROUP,li.getGroup());
		Element eXid=home.xidHandler.createXidElement(li, home.getNamespace());
		addContent(eXid);
	}
	
	/*
	public FindingElement(XChangeContainer parent, Patient p){
		this(parent);
		Kontakt kLab=null; //parent.findContact(getAttributeValue("lab"));
		Result<String> ret=new Result<String>("OK");
		if(kLab==null){
			ret.add(Log.ERRORS, 0, "Couldnt find lab", getAttributeValue("lab"), true);
			return ;
		}
		String classification=getAttributeValue("classification");
		String param=getAttributeValue("param");
		String unit=getAttributeValue("unit");
		String type=getAttributeValue("type");
		LabItem.typ typ;
		if(type.equalsIgnoreCase("number")){
			typ=LabItem.typ.NUMERIC;
		}else if(type.equalsIgnoreCase("text")){
			typ=LabItem.typ.TEXT;
		}else{
			typ=LabItem.typ.ABSOLUTE;
		}
		Query<LabItem> qli=new Query<LabItem>(LabItem.class);
		qli.startGroup();
		qli.add("kuerzel", "=", param);
		qli.or();
		qli.add("titel", "=", param);
		qli.endGroup();
		qli.and();
		qli.add("LaborID", "=", kLab.getId());
		qli.add("Einheit", "=", unit);
		List<LabItem> ll=qli.execute();
		LabItem li;
		String ref=getAttributeValue("normrange");
		
		if(ll.size()<1){
			li=new LabItem(param,param,kLab,ref,ref,unit,typ,"import","1");
		}else{
			li=ll.get(0);
			if(p.getGeschlecht().equals("m")){
				li.setRefM(ref);
			}else{
				li.setRefW(ref);
			}
		}
		TimeTool tt=new TimeTool(getAttributeValue("date"));
		LabResult lr=new LabResult(p,tt,li,getChildText("result",XChangeContainerImpl.ns),"");
	}
	*/
}
