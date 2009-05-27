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
 * $Id: Leistungsblock.java 5320 2009-05-27 16:51:14Z rgw_ch $
 *******************************************************************************/

package ch.elexis.data;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;

import ch.elexis.Hub;
import ch.rgw.compress.CompEx;
import ch.rgw.tools.ExHandler;

public class Leistungsblock extends PersistentObject implements ICodeElement {
	public static final String LEISTUNGEN = "Leistungen";
	public static final String MANDANT_ID = "MandantID";
	public static final String NAME = "Name";
	public static final String XIDDOMAIN = "www.xid.ch/id/elexis_leistungsblock";
	
	static {
		addMapping("LEISTUNGSBLOCK", NAME, MANDANT_ID, LEISTUNGEN);
		Xid.localRegisterXIDDomainIfNotExists(XIDDOMAIN, "Leistungsblock", Xid.ASSIGNMENT_LOCAL
			| Xid.QUALITY_GUID);
	}
	
	public Leistungsblock(String Name, Mandant m){
		create(null);
		String[] f = new String[] {
			NAME, MANDANT_ID
		};
		set(f, Name, m.getId());
	}
	
	public String getName(){
		return checkNull(get(NAME));
	}
	/**
	 * return a List of elements contained in this block will never return null, but the list might
	 * be empty
	 * 
	 * @return a possibly empty list of ICodeElements
	 */
	public List<ICodeElement> getElements(){
		return load();
	}
	
	/**
	 * Add an ICodeElement to this block
	 * 
	 * @param v
	 *            an Element
	 */
	public void addElement(ICodeElement v){
		if (v != null) {
			List<ICodeElement> lst = load();
			int i = 0;
			for (ICodeElement ice : lst) {
				if (ice.getCode().compareTo(v.getCode()) > 0) {
					break;
				}
				i++;
			}
			lst.add(i, v);
			flush(lst);
		}
	}
	
	public void removeElement(ICodeElement v){
		if (v != null) {
			List<ICodeElement> lst = load();
			lst.remove(v);
			flush(lst);
		}
	}
	
	/**
	 * Move a CodeElement inside the block
	 * 
	 * @param v
	 *            the element to move
	 * @param offset
	 *            offset to move. negative values move up, positive down
	 */
	public void moveElement(ICodeElement v, int offset){
		if (v != null) {
			List<ICodeElement> lst = load();
			int idx = lst.indexOf(v);
			if (idx != -1) {
				int npos = idx + offset;
				if (npos < 0) {
					npos = 0;
				} else if (npos >= lst.size()) {
					npos = lst.size() - 1;
				}
				ICodeElement el = lst.remove(idx);
				lst.add(npos, el);
				flush(lst);
			}
		}
	}
	
	@Override
	public String storeToString(){
		return toString(load());
	}
	
	public String toString(List<ICodeElement> lst){
		StringBuilder st = new StringBuilder();
		for (ICodeElement v : lst) {
			st.append(((PersistentObject) v).storeToString()).append(",");
		}
		return st.toString().replaceFirst(",$", "");
		
	}
	
	@Override
	public String getLabel(){
		return get(NAME);
	}
	
	public String getText(){
		return get(NAME);
	}
	
	public String getCode(){
		return get(NAME);
	}
	
	@Override
	protected String getTableName(){
		return "LEISTUNGSBLOCK";
	}
	
	public static Leistungsblock load(String id){
		return new Leistungsblock(id);
	}
	
	protected Leistungsblock(String id){
		super(id);
	}
	
	protected Leistungsblock(){}
	
	private boolean flush(List<ICodeElement> lst){
		try {
			if (lst == null) {
				lst = new ArrayList<ICodeElement>();
			}
			String storable = toString(lst);
			setBinary(LEISTUNGEN, CompEx.Compress(storable, CompEx.ZIP));
			return true;
		} catch (Exception ex) {
			ExHandler.handle(ex);
		}
		return false;
	}
	
	private List<ICodeElement> load(){
		ArrayList<ICodeElement> lst = new ArrayList<ICodeElement>();
		try {
			lst = new ArrayList<ICodeElement>();
			byte[] compressed = getBinary(LEISTUNGEN);
			if (compressed != null) {
				String storable = new String(CompEx.expand(compressed), "UTF-8");
				for (String p : storable.split(",")) {
					lst.add((ICodeElement) Hub.poFactory.createFromString(p));
				}
			}
		} catch (Exception ex) {
			ExHandler.handle(ex);
		}
		return lst;
	}
	
	@Deprecated
	public boolean isEmpty(){
		byte[] comp = getBinary(LEISTUNGEN);
		return (comp == null);
	}
	
	public String getCodeSystemName(){
		return "Block";
	}
	
	public String getCodeSystemCode(){
		return "999";
	}
	
	@Override
	public boolean isDragOK(){
		return true;
	}
	
	public List<IAction> getActions(Verrechnet kontext){
		
		return null;
	}
}
