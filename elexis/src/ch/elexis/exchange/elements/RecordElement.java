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
 *  $Id: RecordElement.java 4173 2008-07-24 10:25:05Z rgw_ch $
 *******************************************************************************/

package ch.elexis.exchange.elements;

import java.util.List;

import org.jdom.Element;

import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.elexis.exchange.XChangeContainer;
import ch.elexis.text.Samdas;
import ch.elexis.util.Result;
import ch.rgw.tools.TimeTool;
import ch.rgw.tools.VersionedResource;
import ch.rgw.tools.VersionedResource.ResourceItem;

@SuppressWarnings("serial")
public class RecordElement extends XChangeElement{
	public static final String XMLNAME="record";
	
	public String getXMLName(){
		return XMLNAME;
	}
	public RecordElement(XChangeContainer c){
		super(c);
	}
	
	
	public RecordElement(XChangeContainer c, Konsultation k){
		super(c);
		
		setAttribute("date",new TimeTool(k.getDatum()).toString(TimeTool.DATE_ISO));
		Kontakt kMandant=k.getMandant();
		if(kMandant==null){
			setAttribute("responsible","unknown");
		}else{
			ContactElement cMandant=c.addContact(kMandant);
			setAttribute("responsible",cMandant.getAttributeValue("id"));
		}
		setID(k.getId());
		VersionedResource vr=k.getEintrag();
    	ResourceItem entry=vr.getVersion(vr.getHeadVersion());
    	if(entry!=null){
	    	setAttribute("author",entry.remark);
	   	
			Samdas samdas=new Samdas(k.getEintrag().getHead());
			Element sRecord=samdas.getRecordElement();
			if(sRecord!=null){
				//Samdas.Record record=new Samdas.Record(sRecord);
				Element sText=sRecord.getChild("text",Samdas.ns);
				if(sText!=null){
					Element eText=new Element("text",getContainer().getNamespace());
					eText.addContent(sText.getText());
					addContent(eText);
				}
			}
    	}
	}
	
	/*
	public Result<String> doImport(Patient p){
		XChangeContainer parent=getContainer();
		String kid=getAttributeValue("responsible");
		String auth=getAttributeValue("author");
		Fall[] faelle=p.getFaelle();
		Fall fall;
		if((faelle==null) || (faelle.length==0)){
			fall=p.neuerFall("Import", "unbekannt", "unbekannt");
		}else{
			fall=faelle[0];
		}
		Konsultation k=fall.neueKonsultation();
		parent.putExtToIntIDMapping(getAttributeValue("id"),k.getId());
		Kontakt kResp=parent.findContact(kid);
		kResp.set("istMandant", "1");
		k.set("MandantID",kid);
		k.setDatum(getAttributeValue("date"), true);
		Samdas samdas=new Samdas();
		Samdas.Record rec=samdas.getRecord();
		Element eText=getChild("text", XChangeContainerImpl.ns);
		if(eText!=null){
			String text=eText.getText();
			if(text!=null){
				rec.setText(text);
			}else {
				rec.setText("");
			}
		}
		
		VersionedResource ve=VersionedResource.load(null);
		ve.update(samdas.toString(), auth);
		k.setEintrag(ve, true);
		
		// TODO parent.anamnesis.doImport(this, k);
		return new Result<String>("OK");
	}
	*/
	
	@SuppressWarnings("unchecked")
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("\nEintrag vom ").append(getAttr("date")).append(" erstellt von ")
			.append(getAttr("author")).append("\n");
		List<Element> children=getChildren();
		if(children!=null){
			for(Element child:children){
				if(child.getName().equals("text")){
					continue;
				}
				sb.append(child.getName()).append(":\n");
				sb.append(child.getText()).append("\n");
			}
		}
		Element eText=getChild("text");
		if(eText!=null){
			String text=eText.getText();
			sb.append(text).append("\n------------------------------\n");
		}
		return sb.toString();
	}
}
