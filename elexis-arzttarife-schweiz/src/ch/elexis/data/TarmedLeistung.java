/*******************************************************************************
 * Copyright (c) 2005-2006, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: TarmedLeistung.java 2740 2007-07-07 14:08:00Z rgw_ch $
 *******************************************************************************/

package ch.elexis.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Comparator;
import java.util.Hashtable;

import org.eclipse.jface.viewers.IFilter;

import ch.elexis.util.IOptifier;
import ch.elexis.util.PlatformHelper;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.*;
import ch.rgw.tools.JdbcLink.Stm;

/**
 * Implementation des Tarmed-Systems. Besteht aus den eigentlichen Leistungen,
 * statischen Methoden zum auslesen der Textformen der einzelnen Codes, einem
 * Validator und einem Mandantenfilter.
 * @author gerry
 *
 */
public class TarmedLeistung extends VerrechenbarAdapter{
    Hashtable<String,String> ext;
    public static TarmedComparator tarmedComparator;
    public static TarmedOptifier tarmedOptifier;
    public static final TimeTool INFINITE = new TimeTool("19991231");
    
    static{
    	String checkExist=PersistentObject.j.queryString("SELECT * FROM TARMED WHERE ID LIKE '10%'");
    	if(checkExist==null){
    		String filepath=PlatformHelper.getBasePath("ch.elexis.arzttarife_ch")+File.separator+"ch"+File.separator+"elexis"+
    			File.separator+"data"+File.separator+"createDB.script";
    		try {
				FileInputStream fis=new FileInputStream(filepath);
				Stm stm=PersistentObject.j.getStatement();
				stm.execScript(fis, true, true);
			} catch (FileNotFoundException e) {
				ExHandler.handle(e);
				SWTHelper.showError("Kann Tarmed-Datenbank nicht erstellen", "create-Script nicht gefunden in "+filepath);
			}
    		
    	}
        addMapping("TARMED","Parent","DigniQuali","DigniQuanti", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                "Sparte","Text=tx255","Name=tx255","Nick=Nickname", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                "GueltigVon=S:D:GueltigVon","GueltigBis=S:D:GueltigBis" //$NON-NLS-1$ //$NON-NLS-2$
        );
        tarmedComparator=new TarmedComparator();
        tarmedOptifier=new TarmedOptifier();
    }
    
    
    /** Text zu einem Code der qualitativen Dignität holen */
    public static String getTextForDigniQuali(String dql){
        if(dql==null){
            return ""; //$NON-NLS-1$
        }
        return checkNull(j.queryString("SELECT titel FROM TARMED_DEFINITIONEN WHERE SPALTE='DIGNI_QUALI' AND KUERZEL="+JdbcLink.wrap(dql))); //$NON-NLS-1$
    }
    /** Kurz-Code für eine qualitative Dignität holen */
    public static String getCodeForDigniQuali(String kurz){
        if(kurz==null){
            return ""; //$NON-NLS-1$
        }
        return checkNull(j.queryString("SELECT KUERZEL FROM TARMED_DEFINITIONEN WHERE SPALTE='DIGNI_QUALI' AND TITEL="+JdbcLink.wrap(kurz))); //$NON-NLS-1$
    }
    
    
    /** Text für einen Code für quantitative Dignität holen */
    public static String getTextForDigniQuanti(String dqn){
        if(dqn==null){
            return ""; //$NON-NLS-1$
        }
        return checkNull(j.queryString("SELECT titel FROM TARMED_DEFINITIONEN WHERE SPALTE='DIGNI_QUANTI' AND KUERZEL="+JdbcLink.wrap(dqn))); //$NON-NLS-1$
    }
    
    /** Text für einen Sparten-Code holen */
    public static String getTextForSparte(String sparte){
        if(sparte==null){
            return ""; //$NON-NLS-1$
        }
        return checkNull(j.queryString("SELECT titel FROM TARMED_DEFINITIONEN WHERE SPALTE='SPARTE' AND KUERZEL="+JdbcLink.wrap(sparte))); //$NON-NLS-1$
    }
    
    /** Text für eine Anästhesie-Risikoklasse holen */
    public static String getTextForRisikoKlasse(String klasse){
        if(klasse==null){
            return ""; //$NON-NLS-1$
        }
        return checkNull(j.queryString("SELECT titel FROM TARMED_DEFINITIONEN WHERE SPALTE='ANAESTHESIE' AND KUERZEL="+JdbcLink.wrap(klasse))); //$NON-NLS-1$
    }
    
    /** Text für einen ZR_EINHEIT-Code holen (Sitzung, Monat usw.) */
    public static String getTextForZR_Einheit(String einheit){
        if(einheit==null){
            return ""; //$NON-NLS-1$
        }
        return checkNull(j.queryString("SELECT titel FROM TARMED_DEFINITIONEN WHERE SPALTE='ZR_EINHEIT' AND KUERZEL="+JdbcLink.wrap(einheit))); //$NON-NLS-1$
    }
    
    /** Alle Codes für Quantitative Dignität holen */
    public static String[] getDigniQuantiCodes(){
    	return null;
    }
    
    /** Konstruktor wird nur vom Importer gebraucht */
    public TarmedLeistung(String code, String parent, String DigniQuali, String DigniQuanti, String sparte){
        create(code);
        j.exec("INSERT INTO TARMED_EXTENSION (CODE) VALUES ("+getWrappedId()+")"); //$NON-NLS-1$ //$NON-NLS-2$
        set(new String[]{"Parent","DigniQuali","DigniQuanti","Sparte"},parent,DigniQuali,DigniQuanti,sparte); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
    public String[] getDisplayedFields(){
		return new String[]{"ID","Text"}; //$NON-NLS-1$ //$NON-NLS-2$
	}
    @Override
    public String getLabel()
    {
        return getId()+" "+getText(); //$NON-NLS-1$
    }

    @Override
    protected String getTableName()
    {
        return "TARMED"; //$NON-NLS-1$
    }

    /** Code liefern */
    public String getCode()
    {   
        return getId();
    }

    /** Text liefern */
    public String getText()
    {
        return get("Text"); //$NON-NLS-1$
    }
    /** Text setzen (wird nur vom Importer gebraucht */
    public void setText(String tx){
        set("Text",tx); //$NON-NLS-1$
    }
    
    /** Erweiterte Informationen laden */
    @SuppressWarnings("unchecked") //$NON-NLS-1$
	public Hashtable<String,String> loadExtension(){
        Stm stm=j.getStatement();
        ResultSet res=stm.query("SELECT limits FROM TARMED_EXTENSION WHERE CODE="+getWrappedId()); //$NON-NLS-1$
        try{
            if(res.next()){
                byte[] in=res.getBytes(1);
                if((in==null)|| (in.length==0)){
                    ext=new Hashtable<String,String>();
                }else{
                    ext=StringTool.fold(in,StringTool.GLZ,null);
                }
            }
        }catch(Exception ex){
            ExHandler.handle(ex);
            ext=new Hashtable<String,String>();
        }finally{
            j.releaseStatement(stm);
           
        }
        return ext;
    }
    
    /** Erweiterte Informationen rückspeichern */
    public void flushExtension(){
        if(ext!=null){
            byte[] flat=StringTool.flatten(ext,StringTool.GLZ,null);
            PreparedStatement preps=j.prepareStatement("UPDATE TARMED_EXTENSION SET limits=? WHERE CODE="+getWrappedId()); //$NON-NLS-1$
            try{
                preps.setBytes(1,flat);
                preps.execute();
            }
            catch(Exception ex){
                ExHandler.handle(ex);
            }
        }
    }
    
    /** Medizinische Interpretation auslesen */
    public String getMedInterpretation(){
        return checkNull(j.queryString("SELECT med_interpret FROM TARMED_EXTENSION WHERE CODE="+getWrappedId())); //$NON-NLS-1$
    }
    
    /** Medizinische Interpretation setzen (Wird nur vom Importer gebraucht) */
    public void setMedInterpretation(String text){
        j.exec("UPDATE TARMED_EXTENSION SET med_interpret="+JdbcLink.wrap(text)+" WHERE CODE="+getWrappedId()); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    /** Technische Interpretation auslesen */
    public String getTechInterpretation(){
        return checkNull(j.queryString("SELECT tech_interpret FROM TARMED_EXTENSION WHERE CODE="+getWrappedId())); //$NON-NLS-1$
    }
    /** Technische Intepretation setzen (Wird nur vom Importer gebraucht */
    public void setTechInterpretation(String text){
        j.exec("UPDATE TARMED_EXTENSION SET tech_interpret="+JdbcLink.wrap(text)+" WHERE CODE="+getWrappedId()); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    /** Qualitative Dignität holen (als code)*/
    public String getDigniQuali(){
        return checkNull(get("DigniQuali")); //$NON-NLS-1$
    }
    /** Qualitative Dignität als Text holen */
    public String getDigniQualiAsText(){
        return checkNull(getTextForDigniQuali(get("DigniQuali"))); //$NON-NLS-1$
    }
    
    /** Qualitative Dinität setzen (Wird nur vom Importer gebraucht) */ 
    public void setDigniQuali(String dql){
        set("DigniQuali",dql); //$NON-NLS-1$
    }
    
    /** Quantitative Dignität als code holen */
    public String getDigniQuanti()
    {   return checkNull(get("DigniQuanti")); //$NON-NLS-1$
    }
    
    /** Quantitative Dignität als Text holen */
    public String getDigniQuantiAsText(){
        return checkNull(getTextForDigniQuanti(get("DigniQuanti"))); //$NON-NLS-1$
    }
    
    /** Sparte holen (als Code) */
    public String getSparte(){
        return checkNull(get("Sparte")); //$NON-NLS-1$
    }
    /** Sparte als Text holen */
    public String getSparteAsText(){
        return checkNull(getTextForSparte(get("Sparte"))); //$NON-NLS-1$
    }
    
    /** Name des verwendeten Codesystems holen (liefert immer "Tarmed") */
    public String getCodeSystemName()
    {
        return "Tarmed"; //$NON-NLS-1$
    }

    protected TarmedLeistung(String id){
        super(id);
    }
    public TarmedLeistung(){/* leer */}
    
    /** Eine Position einlesen */
    public static TarmedLeistung load(String id){
        return new TarmedLeistung(id);
    }
    
    /** Eine Position vom code einlesen */
    public static IVerrechenbar getFromCode(String code)
    {
        return new TarmedLeistung(code);
    }
    
    
    /**
     * Konfigurierbarer Filter für die Anzeige des Tarmed-Codebaums in Abhängigkeit
     * vom gewählten Mandanten (Nur zur Dignität passende Einträge anzeigen) 
     * @author gerry
     */
     
    public static class MandantFilter implements IFilter{

        
        MandantFilter(Mandant m){
        	
        }

        public boolean select(Object object)
        {
            if (object instanceof TarmedLeistung) {
                 /*TarmedLeistung tl = (TarmedLeistung) object;*/
                return true;
            }
            return false;
        }
       
        
    }
 
    /**
     * Komparator zum Sortieren der Codes. Es wird einfach nach Codeziffer
     * sortiert.
     * Wirft eine ClassCastException, wenn die Objekte nicht TarmedLeistungen
     * sind.
     * @author gerry
     */
    static class TarmedComparator implements Comparator{

		public int compare(Object o1, Object o2) {
				TarmedLeistung tl1 = (TarmedLeistung) o1;
				TarmedLeistung tl2 = (TarmedLeistung) o2;
				return tl1.getCode().compareTo(tl2.getCode());
		}
		
	}
    
    public IOptifier getOptifier() {
		return tarmedOptifier;
	}

	public Comparator getComparator() {
		return tarmedComparator;
	}

	public IFilter getFilter(Mandant m) {
		return new MandantFilter(m);
	}

  
    @Override
    public boolean isDragOK()
    {
        return (!StringTool.isNothing(getDigniQuali().trim()));
    }
    public int getAL(){
    	loadExtension();
    	return (int)Math.round(checkZeroDouble(ext.get("TP_AL"))*100); //$NON-NLS-1$
    }
    public int getTL(){
    	loadExtension();
    	return (int)Math.round(checkZeroDouble(ext.get("TP_TL"))*100); //$NON-NLS-1$
    }
    /** Preis der Leistung in Rappen 
    public int getPreis(TimeTool date, String subgroup)
    {
        loadExtension();
        String t=ext.get("TP_TL");
        String a=ext.get("TP_AL");
        double tl=0.0;
        double al=0.0;
        try{
            tl= (t==null) ? 0.0 : Double.parseDouble(t);
        }catch(NumberFormatException ex){
            tl=0.0;
        }
        try{
            al= (a==null) ? 0.0 : Double.parseDouble(a);
        }catch(NumberFormatException ex){
            al=0.0;
        }
        double tp=getVKMultiplikator(date, subgroup)*100;
        return (int)Math.round((tl+al)*tp);
    }
    */
    public int getMinutes(){
    	loadExtension();
    	double min=checkZeroDouble(ext.get("LSTGIMES_MIN")); //$NON-NLS-1$
    	min+=checkZeroDouble(ext.get("VBNB_MIN")); //$NON-NLS-1$
    	min+=checkZeroDouble(ext.get("BEFUND_MIN")); //$NON-NLS-1$
    	min+=checkZeroDouble(ext.get("WECHSEL_MIN")); //$NON-NLS-1$
    	return (int)Math.round(min);
    }
    public String getExclusion(){
    	loadExtension();
    	return checkNull(ext.get("exclusion")); //$NON-NLS-1$
    }
	public int getTP(TimeTool date, String subgroup) {
		loadExtension();
        String t=ext.get("TP_TL"); //$NON-NLS-1$
        String a=ext.get("TP_AL"); //$NON-NLS-1$
        double tl=0.0;
        double al=0.0;
        try{
            tl= (t==null) ? 0.0 : Double.parseDouble(t);
        }catch(NumberFormatException ex){
            tl=0.0;
        }
        try{
            al= (a==null) ? 0.0 : Double.parseDouble(a);
        }catch(NumberFormatException ex){
            al=0.0;
        }
		return (int)Math.round((tl+al)*100.0);
	}
	public double getFactor(TimeTool date, String subgroup) {
        return getVKMultiplikator(date, subgroup);
	}
	
	/**
	 * Returns the GueltigVon value
	 * @return the GueltigVon value as a TimeTool object, or null if the value is
	 * not defined
	 */
	public TimeTool getGueltigVon() {
		String value = get("GueltigVon");
		if (!StringTool.isNothing(value)) {
			return new TimeTool(value);
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the GueltigBis value
	 * @return the GueltigBis value as a TimeTool object, or null if the value is
	 * not defined
	 */
	public TimeTool getGueltigBis() {
		String value = get("GueltigBis");
		if (!StringTool.isNothing(value)) {
			return new TimeTool(value);
		} else {
			return null;
		}
	}
}
