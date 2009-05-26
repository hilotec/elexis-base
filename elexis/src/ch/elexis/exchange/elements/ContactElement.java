/*******************************************************************************
 * Copyright (c) 2006-2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: ContactElement.java 5319 2009-05-26 14:55:24Z rgw_ch $
 *******************************************************************************/

package ch.elexis.exchange.elements;

import java.util.List;

import org.jdom.Element;

import ch.elexis.data.Kontakt;
import ch.elexis.data.Organisation;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Person;
import ch.elexis.exchange.KontaktMatcher;
import ch.elexis.exchange.XChangeContainer;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

/**
 * A Contact can contain elements of the types address, connection, and medical
 * 
 * @author Gerry
 * 
 */
@SuppressWarnings("serial")
public class ContactElement extends XChangeElement {
	public static final String XMLNAME = "contact"; //$NON-NLS-1$
	public static final String ATTR_BIRTHDATE = "birthdate"; //$NON-NLS-1$
	public static final String ATTR_FIRSTNAME = "firstname"; //$NON-NLS-1$
	public static final String ATTR_MIDDLENAME = "middlename"; //$NON-NLS-1$
	public static final String ATTR_LASTNAME = "lastname"; //$NON-NLS-1$
	public static final String ATTR_SEX = "sex"; //$NON-NLS-1$
	public static final String ATTR_SALUTATION = "salutation"; //$NON-NLS-1$
	public static final String ATTR_TITLE = "title"; //$NON-NLS-1$
	public static final String ATTR_TYPE = "type"; //$NON-NLS-1$
	public static final String ATTR_SHORTNAME = "shortname"; //$NON-NLS-1$
	public static final String ELEM_XID = "xid"; //$NON-NLS-1$
	public static final String ELEM_ADDRESS = "address"; //$NON-NLS-1$
	public static final String VALUE_PERSON = "person"; //$NON-NLS-1$
	public static final String VALUE_ORGANIZATION = "organization"; //$NON-NLS-1$
	public static final String VALUE_MALE = "male"; //$NON-NLS-1$
	public static final String VALUE_FEMALE = "female"; //$NON-NLS-1$
	
	public ContactElement(XChangeContainer home){
		super(home);
	}
	
	public ContactElement(XChangeContainer home, Element el){
		super(home, el);
	}
	
	public void add(AddressElement ae){
		super.add(ae);
	}
	
	public void add(ContactRefElement ce){
		super.add(ce);
	}
	
	public void add(MedicalElement me){
		super.add(me);
	}
	
	public List<AddressElement> getAddresses(){
		return (List<AddressElement>) getChildren(ELEM_ADDRESS, AddressElement.class);
	}
	
	/**
	 * Create a ContactElement from a Kontakt
	 * 
	 * @param parent
	 * @param k
	 */
	public ContactElement(XChangeContainer parent, Kontakt k){
		super(parent);
		XidElement eXid = new XidElement(parent, k);
		add(eXid);
		if (k.istPerson()) {
			Person p = Person.load(k.getId());
			setAttribute(ATTR_TYPE, VALUE_PERSON);
			setAttribute(ATTR_LASTNAME, p.getName());
			setAttribute(ATTR_FIRSTNAME, p.getVorname());
			if (p.getGeschlecht().startsWith("m")) { //$NON-NLS-1$
				setAttribute(ATTR_SEX, VALUE_MALE);
			} else {
				setAttribute(ATTR_SEX, VALUE_FEMALE);
			}
			String gebdat = p.getGeburtsdatum();
			if (!StringTool.isNothing(gebdat)) {
				setAttribute(ATTR_BIRTHDATE, new TimeTool(gebdat).toString(TimeTool.DATE_ISO));
			}
			
		} else {
			setAttribute(ATTR_TYPE, VALUE_ORGANIZATION);
			setAttribute(ATTR_LASTNAME, k.getLabel());
		}
		add(new AddressElement(parent, k.getAnschrift(), "default")); //$NON-NLS-1$
		parent.addMapping(this, k);
	}
	
	public List<ContactRefElement> getAssociations(){
		List<ContactRefElement> ret =
			(List<ContactRefElement>) getChildren("connection", ContactRefElement.class); //$NON-NLS-1$
		return ret;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.getString("ContactElement.Name")).append(getAttr(ATTR_LASTNAME)).append(StringTool.lf); //$NON-NLS-1$
		sb.append(Messages.getString("ContactElement.vorname")).append(getAttr(ATTR_FIRSTNAME)); //$NON-NLS-1$
		String middle = getAttr(ATTR_MIDDLENAME);
		if (middle.length() > 0) {
			sb.append(StringTool.space).append(middle);
		}
		sb.append(Messages.getString("ContactElement.gebdat")); //$NON-NLS-1$
		TimeTool geb = new TimeTool(getAttr(ATTR_BIRTHDATE));
		sb.append(geb.toString(TimeTool.DATE_GER)).append(StringTool.lf);
		sb.append("PID: ").append(getAttr(ID)).append(StringTool.lf+StringTool.lf); //$NON-NLS-1$ //$NON-NLS-2$
		List<AddressElement> addresses = getAddresses();
		for (AddressElement adr : addresses) {
			sb.append(adr.toString()).append(StringTool.lf);
		}
		return sb.toString();
	}
	
	@Override
	public String getXMLName(){
		return XMLNAME;
	}
	
	public PersistentObject doImport(PersistentObject context){
		XidElement eXid = getXid();
		Kontakt ret = null;
		if (eXid != null) {
			List<PersistentObject> cands = eXid.findObject();
			if (cands.size() == 0) {
				AddressElement ae = null;
				List<AddressElement> lae = getAddresses();
				if (lae.size() > 0) {
					if (lae.size() == 1) {
						ae = lae.get(0);
					} else {
						for (AddressElement adr : lae) {
							if (adr.getAttr(AddressElement.ATTR_DESCRIPTION).equalsIgnoreCase(
								AddressElement.VALUE_DEFAULT)) {
								ae = adr;
								break;
							}
						}
						if (ae == null) {
							ae = lae.get(0);
						}
					}
					
				}
				String strasse = null;
				String plz = null;
				String ort = null;
				String natel = null;
				if (ae != null) {
					strasse = ae.getAttr(AddressElement.ATTR_STREET);
					plz = ae.getAttr(AddressElement.ATTR_ZIP);
					ort = ae.getAttr(AddressElement.ATTR_CITY);
				}
				if (getAttr(ATTR_TYPE).equalsIgnoreCase(VALUE_PERSON)) {
					String s = getAttr(ATTR_SEX).equals(VALUE_MALE) ? Person.MALE : Person.FEMALE;
					ret =
						KontaktMatcher.findPerson(getAttr(ATTR_LASTNAME), getAttr(ATTR_FIRSTNAME),
							getAttr(ATTR_BIRTHDATE), s, strasse, plz, ort, natel,
							KontaktMatcher.CreateMode.CREATE);
					
				} else {
					ret =
						KontaktMatcher.findOrganisation(getAttr(ATTR_LASTNAME),
							getAttr(ATTR_FIRSTNAME), strasse, plz, ort,
							KontaktMatcher.CreateMode.CREATE);
				}
			} else if (cands.size() == 1) {
				if (getAttr(ATTR_TYPE).equalsIgnoreCase(VALUE_PERSON)) {
					ret = Person.load(cands.get(0).getId());
				} else {
					ret = Organisation.load(cands.get(0).getId());
				}
			}
			MedicalElement me =
				(MedicalElement) getChild(MedicalElement.XMLNAME, MedicalElement.class);
			me.doImport(ret);
		}
		return ret;
	}
	
}
