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
 * $Id: Artikel.java 2497 2007-06-08 09:50:16Z danlutz $
 *******************************************************************************/
package ch.elexis.data;

import java.util.Hashtable;
import java.util.List;

import ch.elexis.Hub;
import ch.elexis.util.Log;
import ch.elexis.util.Money;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

/**
 * Ein Artikel ist ein Objekt, das im Lager vorhanden ist oder sein sollte oder einem
 * Patienten verordnet werden kann
 */
public class Artikel extends VerrechenbarAdapter{
	public static final String TABLENAME="ARTIKEL";
	
	protected String getTableName() {
		return TABLENAME;
	}
	static{
		addMapping(TABLENAME,"LieferantID","Name","Maxbestand","Minbestand",
				"Istbestand","EK_Preis","VK_Preis","Typ","ExtInfo",
				"SubID","Eigenname=Name_intern","Codeclass","Klasse");
	}
    /**
     * This implementation of PersistentObject#load is special in that it tries to load
     * the actual appropriate subclass
     */
	public static Artikel load(String id){
		if(id==null){
			return null;
		}
	   	Artikel ret=new Artikel(id);
	   	if(!ret.exists()){
	   		return null;
	   	}
	   	String clazz=ret.get("Klasse");
	   	if(!StringTool.isNothing(clazz)){
		   	 try{
	             ret= (Artikel)Hub.poFactory.createFromString(clazz+"::"+id);
		   	 }catch(Exception ex){
		            log.log("Fehlerhafter Leistungscode "+clazz+"::"+id,Log.ERRORS);
		     }
	   	}
        return ret;
	}
    /**
     * Einen neuen Artikel mit vorgegebenen Parametern erstellen
     * @param Name
     * @param Typ
     */
	public Artikel(String Name,String Typ){
		create(null);
		set(new String[]{"Name","Typ"},new String[]{Name,Typ});
	}
	public Artikel(String Name, String Typ, String subid){
		create(null);
		set(new String[]{"Name","Typ","SubID"},Name,Typ,subid);
	}
	public String getLabel(){
		return getInternalName();
	}
	public String[] getDisplayedFields(){
		return new String[]{"Typ","Name"};
	}
	/**
	 * Den internen Namen setzen. Dieser ist vom Anwender frei wählbar und erscheint
	 * in der Artikelauswahl und auf der Rechnung.
	 * @param nick Der "Spitzname"
	 */
	public void setInternalName(String nick){
		set("Eigenname",nick);
	}
	/**
	 * Den internen Namen holen
	 * @return
	 */
	public String getInternalName(){
		String ret=get("Eigenname");
		if(StringTool.isNothing(ret)){
			ret=getName();
		}
		return ret;
	}
	/**
	 * Den offiziellen namen holen
	 * @return
	 */
	public String getName(){
		return checkNull(get("Name"));
	}
	/**
	 * Den "echten" Namen setzen. DIes ist der offizielle Name des Artikels, wie
	 * er beispielsweise in Katalogen aufgeführt ist. Dieser sollte normalerweise 
	 * nicht geändert werden.
	 * @param name der neue "echte" Name
	 */
	public void setName(String name){
		set("Name",name);
	}
	
	/** 
	 * Basis-Einkaufspreis in Rappen pro Einheit
	 * @return
	 */
	public int getEKPreis(){
		try{
			return checkZero(get("EK_Preis"));
		}catch(Throwable ex){
			Hub.log.log("Fehler beim Einlesen von EK für "+getLabel(),Log.ERRORS);
		}
		return 0;

	}
	/**
	 * Basis-Verkaufspreis in Rappen pro Einheit
	 * @return
	 */
	public int getVKPreis(){
		try{
			return checkZero(get("VK_Preis"));
		}catch(Throwable ex){
			Hub.log.log("Fehler beim Einlesen von VK für "+getLabel(),Log.ERRORS);
		}
		return 0;

	}
	public int getIstbestand(){
		try{
			return checkZero(get("Istbestand"));
		}catch(Throwable ex){
			Hub.log.log("Fehler beim Einlesen von istbestand für "+getLabel(),Log.ERRORS);
		}
		return 0;
	}
	
	public int getTotalCount(){
		int pack=getIstbestand();
		int VE=getPackungsGroesse();
		if(VE==0){
			return pack;
		}
		int AE=getAbgabeEinheit();
		if(AE<VE){
			return (pack*VE)+(getBruchteile()*AE);
		}
		return pack;
	}
	public int getPackungsGroesse(){
		return checkZero(getExt("Verpackungseinheit"));
	}
	public int getAbgabeEinheit(){
		return checkZero(getExt("Verkaufseinheit"));
	}
	public int getMaxbestand(){
		try{
			return checkZero(get("Maxbestand"));
		}catch(Throwable ex){
			Hub.log.log("Fehler beim Einlesen von Maxbestand für "+getLabel(),Log.ERRORS);
		}
		return 0;
	}
	public int getMinbestand(){
		try{
			return checkZero(get("Minbestand"));
		}catch(Throwable ex){
			Hub.log.log("Fehler beim Einlesen von Minbestand für "+getLabel(),Log.ERRORS);
		}
		return 0;
	}
	public void setMaxbestand(int s){
		set("Maxbestand",Integer.toString(s));
	}
	public void setMinbestand(int s){
		set("Minbestand",Integer.toString(s));
	}
	public void setIstbestand(int s){
		set("Istbestand",Integer.toString(s));
	}
	public int getBruchteile(){
		return checkZero(getExt("Anbruch"));
	}
	
	public boolean isLagerartikel() {
		if (getMinbestand() > 0 || getMaxbestand() > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public static List<Artikel> getLagerartikel() {
		Query<Artikel> qbe=new Query<Artikel>(Artikel.class);
		qbe.add("Minbestand",">","0");
		qbe.or();
		qbe.add("Maxbestand",">","0");
		qbe.orderBy(false, new String[] {"Name"});
		List<Artikel> l=qbe.execute();
		return l;
	}
	
	@SuppressWarnings("unchecked")
	public void einzelAbgabe(int n){
		Hashtable<String,String> ext=getHashtable("ExtInfo");
		int anbruch=checkZero((String)ext.get("Anbruch"));
		int ve=checkZero((String)ext.get("Verkaufseinheit"));
		int vk=checkZero((String)ext.get("Verpackungseinheit"));
		if(vk==0){
			if(ve!=0){
				vk=ve;
				ext.put("Verkaufseinheit",Integer.toString(vk));
				setHashtable("ExtInfo",ext);
			}
		}
		if(ve==0){
			if(vk!=0){
				ve=vk;
				ext.put("Verpackungseinheit",Integer.toString(ve));
				setHashtable("ExtInfo",ext);
			}
		}
		int num=n*ve;
		if(vk==ve){
			setIstbestand(getIstbestand()-n);
		}else{
			int rest=anbruch-num;
			while(rest<0){
				rest=rest+vk;
				setIstbestand(getIstbestand()-1);
			}
			ext.put("Anbruch",Integer.toString(rest));
			setHashtable("Extinfo",ext);
		}
	}
	@SuppressWarnings("unchecked")
	public void einzelRuecknahme(int n){
		Hashtable<String,String> ext=getHashtable("ExtInfo");
		int anbruch=checkZero((String)ext.get("Anbruch"));
		int ve=checkZero((String)ext.get("Verkaufseinheit"));
		int vk=checkZero((String)ext.get("Verpackungseinheit"));
		int num=n*ve;
		if(vk==ve){
			setIstbestand(getIstbestand()+n);
		}else{
			int rest=anbruch+num;
			while(rest>vk){
				rest=rest-vk;
				setIstbestand(getIstbestand()+1);
			}
			ext.put("Anbruch",Integer.toString(rest));
			setHashtable("Extinfo",ext);
		}
	}
	public String getEAN(){
		Hashtable ext=getHashtable("ExtInfo");
		return (String)ext.get("EAN");
	}
	public String getPharmaCode(){
		Hashtable ext=getHashtable("ExtInfo");
		return checkNull((String)ext.get("Pharmacode"));
	}
	public Kontakt getLieferant(){
		return Kontakt.load(get("LieferantID"));
	}
	public void setLieferant(Kontakt l){
		set("LieferantID",l.getId());
	}
	public int getVerpackungsEinheit(){
		Hashtable ext=getHashtable("ExtInfo");
		return checkZero((String)ext.get("Verpackungseinheit"));
	}
	public int getVerkaufseinheit(){
		Hashtable ext=getHashtable("ExtInfo");
		return checkZero((String)ext.get("Verkaufseinheit"));
	}
	@SuppressWarnings("unchecked")
	public void setExt(String name, String value){
		Hashtable h=getHashtable("ExtInfo");
		h.put(name,value);
		setHashtable("ExtInfo",h);
	}
	public String getExt(String name){
		Hashtable h=getHashtable("ExtInfo");
		return checkNull((String)h.get(name));
	}
	protected Artikel(String id){
		super(id);
	}
	protected 
	Artikel(){
	}
	
	
	/************************ Verrechenbar ************************/
	public String getCode() { return getId();}
	public String getText() { return get("Name");}
	public String getCodeSystemName() { return "Artikel";}
	
	public int getPreis(TimeTool dat, String subgroup) {
		double vkt= checkZeroDouble(get("VK_Preis"));
		Hashtable ext=getHashtable("ExtInfo");
		double vpe= checkZeroDouble((String)ext.get("Verpackungseinheit"));
		double vke= checkZeroDouble((String)ext.get("Verkaufseinheit"));
		if(vpe!=vke){
			return (int)Math.round(vke*(vkt/vpe));
		}else{
			return (int)Math.round(vkt);
		}
	}
	public Money getKosten(TimeTool dat){
		double vkt= checkZeroDouble(get("EK_Preis"));
		Hashtable ext=getHashtable("ExtInfo");
		double vpe= checkZeroDouble((String)ext.get("Verpackungseinheit"));
		double vke= checkZeroDouble((String)ext.get("Verkaufseinheit"));
		if(vpe!=vke){
			return new Money((int)Math.round(vke*(vkt/vpe)));
		}else{
			return new Money((int)Math.round(vkt));
		}
	}
	public int getTP(TimeTool date, String subgroup) {
		return getPreis(date,subgroup);
	}
	public double getFactor(TimeTool date, String subgroup) {
		return 1.0;
	}
}
