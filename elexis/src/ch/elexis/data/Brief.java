/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 *  $Id: Brief.java 6051 2010-02-02 17:25:48Z rgw_ch $
 *******************************************************************************/
package ch.elexis.data;

import ch.elexis.StringConstants;
import ch.elexis.text.XrefExtension;
import ch.rgw.compress.CompEx;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

/**
 * Ein Brief ist ein mit einem externen Programm erstelles Dokument. (Im Moment immer
 * OpenOffice.org). Die Klasse Briefe mit der Tabelle Briefe enthält dabei die Meta-Informationen,
 * während die private Klasse contents mit der Tabelle HEAP die eigentlichen Dokumente als black
 * box, nämlich im Binärformat des erstellenden Programms, enthält. Ein Brief bezieht sich immer auf
 * eine bestimmte Konsultation, zu der er erstellt wurde.
 * 
 * @author Gerry
 * 
 */
public class Brief extends PersistentObject {
	public static final String MIME_TYPE = "MimeType";
	public static final String DATE_MODIFIED = "modifiziert";
	public static final String DATE = "Datum";
	public static final String TYPE = "Typ";
	public static final String KONSULTATION_ID = "BehandlungsID";
	public static final String DESTINATION_ID = "DestID";
	public static final String SENDER_ID = "AbsenderID";
	public static final String PATIENT_ID = "PatientID";
	public static final String SUBJECT = "Betreff";
	public static final String TABLENAME = "BRIEFE";
	public static final String TEMPLATE = "Vorlagen";
	public static final String AUZ = "AUF-Zeugnis";
	public static final String RP = "Rezept";
	public static final String UNKNOWN = "Allg.";
	public static final String LABOR = "Labor";
	public static final String BESTELLUNG = "Bestellung";
	public static final String RECHNUNG = "Rechnung";
	public static final String SYSTEMPLATE = "Systemvorlagen";
	
	public static final String MIMETYPE_OO2 = "application/vnd.oasis.opendocument.text";
	
	@Override
	protected String getTableName(){
		return TABLENAME;
	}
	
	static {
		addMapping(TABLENAME, SUBJECT, PATIENT_ID, DATE_FIELD, SENDER_ID, DESTINATION_ID,
			KONSULTATION_ID, TYPE, "modifiziert=S:D:modifiziert", "geloescht", MIME_TYPE,
			"gedruckt=S:D:gedruckt", "Path");
	}
	
	protected Brief(){/* leer */
	}
	
	protected Brief(String id){
		super(id);
	}
	
	/** Einen Brief anhand der ID aus der Datenbank laden */
	public static Brief load(String id){
		return new Brief(id);
	}
	
	/** Einen neuen Briefeintrag erstellen */
	public Brief(String Betreff, TimeTool Datum, Kontakt Absender, Kontakt dest, Konsultation bh,
		String typ){
		getConnection().setAutoCommit(false);
		try {
			super.create(null);
			if (Datum == null) {
				Datum = new TimeTool();
			}
			String pat = StringTool.leer, bhdl = StringTool.leer;
			if (bh != null) {
				bhdl = bh.getId();
				pat = bh.getFall().getPatient().getId();
			}
			String dst = "";
			if (dest != null) {
				dst = dest.getId();
			}
			String dat = Datum.toString(TimeTool.DATE_GER);
			set(new String[] {
				SUBJECT, PATIENT_ID, DATE, SENDER_ID, DATE_MODIFIED, DESTINATION_ID,
				KONSULTATION_ID, TYPE, "geloescht"
			}, new String[] {
				Betreff, pat, dat, Absender == null ? StringTool.leer : Absender.getId(), dat, dst, bhdl, typ,
						StringConstants.ZERO
			});
			new contents(this);
			getConnection().commit();
		} catch (Throwable ex) {
			ExHandler.handle(ex);
			getConnection().rollback();
		} finally {
			getConnection().setAutoCommit(true);
		}
	}
	
	public void setPatient(Person k){
		set(PATIENT_ID, k.getId());
	}
	
	public void setTyp(String typ){
		set(TYPE, typ);
	}
	
	public String getTyp(){
		String t = get(TYPE);
		if (t == null) {
			return "Brief";
		}
		return t;
	}
	
	/** Speichern als Text */
	public boolean save(String cnt){
		contents c = contents.load(getId());
		c.save(cnt);
		set(DATE_MODIFIED, new TimeTool().toString(TimeTool.DATE_COMPACT));
		return true;
	}
	
	/** Speichern in Binärformat */
	public boolean save(byte[] in, String mimetype){
		if (in != null) {
			// if(mimetype.equalsIgnoreCase(MIMETYPE_OO2)){
			contents c = contents.load(getId());
			c.save(in);
			set(DATE_MODIFIED, new TimeTool().toString(TimeTool.DATE_COMPACT));
			set(MIME_TYPE, mimetype);
			return true;
			// }
			// return false;
		}
		return false;
	}
	
	/** Binärformat laden */
	public byte[] loadBinary(){
		contents c = contents.load(getId());
		return c.getBinary();
	}
	
	/** Textformat laden */
	public String read(){
		contents c = contents.load(getId());
		return c.read();
	}
	
	/** Mime-Typ des Inhalts holen */
	public String getMimeType(){
		String gm = get(MIME_TYPE);
		if (StringTool.isNothing(gm)) {
			return MIMETYPE_OO2;
		}
		return gm;
	}
	
	public static boolean canHandle(String mimetype){
		/*
		 * if(mimetype.equalsIgnoreCase(MIMETYPE_OO2)){ return true; }
		 */
		return true;
	}
	
	public boolean delete(){
		getConnection().exec("UPDATE HEAP SET deleted='1' WHERE ID=" + getWrappedId());
		String konsID = get(KONSULTATION_ID);
		if (!StringTool.isNothing(konsID) && (!konsID.equals("SYS"))) {
			Konsultation kons = Konsultation.load(konsID);
			if ((kons != null) && (kons.isEditable(false))) {
				kons.removeXRef(XrefExtension.providerID, getId());
			}
		}
		return super.delete();
	}
	
	public BriefAusgabe logOutput(IOutputter outputter){
		return new BriefAusgabe(this,outputter);
	}
	/** Einen Brief unwiederruflich löschen */
	public boolean remove(){
		getConnection().setAutoCommit(false);
		try {
			getConnection().exec("DELETE FROM HEAP WHERE ID=" + getWrappedId());
			getConnection().exec("DELETE FROM BRIEFE WHERE ID=" + getWrappedId());
			getConnection().commit();
		} catch (Throwable ex) {
			ExHandler.handle(ex);
			getConnection().rollback();
			return false;
		} finally {
			getConnection().setAutoCommit(true);
		}
		return true;
	}
	
	public String getBetreff(){
		return checkNull(get(SUBJECT));
	}
	
	public void setBetreff(String nBetreff){
		set(SUBJECT, nBetreff);
	}
	
	public String getDatum(){
		return get(DATE);
	}
	
	public Kontakt getAdressat(){
		String dest = get(DESTINATION_ID);
		return dest == null ? null : Kontakt.load(dest);
	}
	
	public Person getPatient(){
		Person pat = Person.load(get(PATIENT_ID));
		if ((pat != null) && (pat.state() > INVALID_ID)) {
			return pat;
		}
		return null;
	}
	
	public String getLabel(){
		return checkNull(get(DATE)) + StringTool.space + checkNull(get(SUBJECT));
	}
	
	private static class contents extends PersistentObject {
		private static final String CONTENTS = "inhalt";
		static final String CONTENT_TABLENAME = "HEAP";
		
		static {
			addMapping(CONTENT_TABLENAME, CONTENTS);
		}
		
		private contents(Brief br){
			create(br.getId());
		}
		
		private contents(String id){
			super(id);
		}
		
		byte[] getBinary(){
			return getBinary(CONTENTS);
		}
		
		private String read(){
			byte[] raw = getBinary();
			if (raw != null) {
				byte[] ret = CompEx.expand(raw);
				return StringTool.createString(ret);
			}
			return "";
		}
		
		private void save(String contents){
			byte[] comp = CompEx.Compress(contents, CompEx.BZIP2);
			setBinary(CONTENTS, comp);
		}
		
		private void save(byte[] contents){
			setBinary(CONTENTS, contents);
		}
		
		@Override
		public String getLabel(){
			return getId();
		}
		
		static contents load(String id){
			return new contents(id);
		}
		
		@Override
		protected String getTableName(){
			return CONTENT_TABLENAME;
		}
		
	}
}
