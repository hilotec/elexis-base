/*******************************************************************************
 * Copyright (c) 2005-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: Patient.java 3824 2008-04-21 07:52:20Z rgw_ch $
 *******************************************************************************/
package ch.elexis.data;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import ch.elexis.Hub;
import ch.elexis.admin.AccessControlDefaults;
import ch.elexis.util.Money;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;
import ch.rgw.tools.JdbcLink.Stm;
import ch.rgw.tools.TimeTool.TimeFormatException;

/**
 * Ein Patient ist eine Person (und damit auch ein Kontakt), mit folgenden zusätzlichen Eigenschaften <ul>
 * <li> Anamnesen : PA, SA, FA </li>
 * <li> Fixe Diagnosen</li>
 * <li> Fixe Medikation</li>
 * <li> Risiken</li>
 * <li> Einer Liste der Fälle, die zu diesem Patienten existieren </li>
 * <li> Einer Liste der Garanten, die diesem Patienten zugeordnet wurden </li>
 * <li> Einer Liste aller Forderungen und Zahlungen im Verkehr mit diesem Patienten </li>
 * </ul>
 * 
 * @author gerry
 *
 */
public class Patient extends Person{
	static{
		addMapping("KONTAKT",
				"Diagnosen       	=S:C:Diagnosen",
				"PersAnamnese   	=S:C:PersAnamnese",
				"SystemAnamnese	 	=S:C:SysAnamnese",
				"FamilienAnamnese	=S:C:FamAnamnese",
				"Risiken",
				"Allergien",
				"Faelle				=LIST:PatientID:FAELLE:DatumVon",
				"Garanten			=JOINT:GarantID:PatientID:PATIENT_GARANT_JOINT:ch.elexis.data.Kontakt",
				"Dauermedikation	=JOINT:ArtikelID:PatientID:PATIENT_ARTIKEL_JOINT:ch.elexis.data.Artikel",
				"Konto				=LIST:PatientID:KONTO",
				"Gruppe",
				"PatientNr","istPatient"
		);
	}

	public String getDiagnosen(){
		return get("Diagnosen");
	}
	public String getPersAnamnese(){
		return get("PersAnamnese");
	}
	public String getSystemAnamnese(){
		return get("Systemanamnese");
	}
 
    protected Patient(){/* leer */  }
    @Override
	public boolean isValid(){
		if(!super.isValid()){
			return false;
		}
		String geb=(get("Geburtsdatum"));
		if(geb.equals("WERT?")){
			return false;
		}
		String g=get("Geschlecht");
		if(g.equals("m") || g.equals("w")){
			return true;
		}
		return false;
    }
    
    /**
     * Dieser oder der folgende Konstruktor sollte normalerweise verwendet werden, um einen neuen, bisher
     * noch nicht in der Datenbank vorhandenen Patienten anzulegen.
     * @param Name
     * @param Vorname
     * @param Geburtsdatum Als String in Notation dd.mm.jj
     * @param s	Geschlecht m oder w
     */
    public Patient(final String Name, final String Vorname, final String Geburtsdatum, final String s)
    {
    	super(Name, Vorname,Geburtsdatum,s);
    }
    
    /**
     * This constructor is more critical than the previous one
     * @param name will be checked for non-alphabetic characters
     * @param vorname will be checked for non alphabetiic characters
     * @param gebDat will be checked for unplausible values
     * @param s will be checked for undefined values
     * @throws TimeFormatException
     */
    public Patient(final String name, final String vorname, final TimeTool gebDat, final String s) throws PersonDataException{
    	super(name, vorname, gebDat,s);
    }
	/**
	 * Eine Liste aller zu diesem Patient gehörenden Fälle liefern
	 * @return Array mit allen Fällen (das die Länge null haben kann)
	 */
	public Fall[] getFaelle() {
		List<String> cas=getList("Faelle",true);
		Fall[] ret=new Fall[cas.size()];
		int i=0;
		for(String id : cas){
			ret[i++]=Fall.load(id);
		}
		return ret;
	}
	/**
	 * Fixmedikation dieses Patienten einlesen
	 * @return ein Array aus {@link Prescription.java}Prescriptions
	 */
	public Prescription[] getFixmedikation(){
		Query<Prescription> qbe=new Query<Prescription>(Prescription.class);
		qbe.add("PatientID","=",getId());
		qbe.add("RezeptID", "", null);
		String today=new TimeTool().toString(TimeTool.DATE_COMPACT);
		qbe.startGroup();
		qbe.add("DatumBis",">=" , today);
		qbe.or();
		qbe.add("DatumBis", "", null);
		qbe.endGroup();
		List<Prescription> l=qbe.execute();
		return l.toArray(new Prescription[0]);
	}
	
	/**
	 * Fixmedikation als Text 
	 * @return
	 */
	public String getMedikation(){
		Prescription[] pre=getFixmedikation();
		StringBuilder sb=new StringBuilder();
		for(Prescription p:pre){
			sb.append(p.getLabel()).append("\n");
		}
		return sb.toString();
	}
	/**
	 * Die neueste Konsultation dieses Patienten holen, soweit eruierbar
	 * @param create: eine Kons erstellen, falls keine existiert
	 * @return die letzte Konsultation oder null
	 */
	
    public Konsultation getLetzteKons(final boolean create){
    	if(Hub.actMandant==null){
    		SWTHelper.showError("Kein Mandant angemeldet", "Es ist kein Mandant angemeldet.");
    		return null;
    	}
    	Query<Konsultation> qbe=new Query<Konsultation>(Konsultation.class);
    	qbe.add("MandantID", "=", Hub.actMandant.getId());
    	//qbe.add("Datum", "=", new TimeTool().toString(TimeTool.DATE_COMPACT));

    	Fall[] faelle=getFaelle();
    	if((faelle==null) || (faelle.length==0)){
    		return create ? createFallUndKons() : null;
    	}
    	qbe.startGroup();
    	boolean termInserted=false;
    	for(Fall fall:faelle){
    		if(fall.isOpen()){
    			qbe.add("FallID", "=", fall.getId());
    			qbe.or();
    			termInserted=true;
    		}
    	}
    	if(!termInserted){
    		return create ? createFallUndKons() : null;
    	}
    	qbe.endGroup();
    	qbe.orderBy(true, "Datum");
    	List<Konsultation> list=qbe.execute();
    	if((list==null) || list.isEmpty()){
    		return null;
    	}else{
    		return list.get(0);
    	}
    }
    
    public Konsultation createFallUndKons(){
    	Fall fall=neuerFall(Fall.getDefaultCaseLabel(), Fall.getDefaultCaseReason(), Fall.getDefaultCaseLaw());
    	Konsultation k=fall.neueKonsultation();
    	k.setMandant(Hub.actMandant);
    	return k;
    }
    /*
        String lb=checkNull((String)getInfoElement("LetzteBehandlung"));
        if(!lb.equals("")){
        	Konsultation ret= Konsultation.load(lb);
        	if(ret!=null){
        		return ret;
        	}
        }
        
        Konsultation last=null;
        Fall[] faelle=getFaelle();
        for(Fall fall:faelle){
            Konsultation[] b=fall.getBehandlungen(true);
            if((b==null)|| (b.length==0)){
            	continue;
            }
            if(last==null){
                last=b[0];
            }else{
                if(last.compareTo(b[0])<0){
                    last=b[0];
                }
            }
        }
        if(last!=null){
            setInfoElement("LetzteBehandlung",last.getId());
        }else{
        	setInfoElement("LetzteBehandlung","");
        }
        return last;
        
    }
    */
	/** 
	 * Einen neuen Fall erstellen und an den Patienten binden 
	 * @return der eben erstellte Fall oder null bei Fehler
	 * */
	public Fall neuerFall(final String Bezeichnung, final String grund, final String Abrechnungsmethode)
	{
		Fall fall=new Fall(getId(),Bezeichnung,grund,Abrechnungsmethode);
		return fall;
	}
	
	/** 
	 * Einen Kurzcode, der diesen Patienten identifiziert, zurückliefern. 
	 * Der Kurzcode kann je nach Voreinstellung eine eindeutige, jeweils nur einmal
	 * vergebene Nummer sein, oder ein aus den Personalien gebildetes Kürzel.
	 * Dieser Code kann beispielsweise als Index für die Archivierung der KG's
	 * in Papierform verwendet werden.
	 * @return einen String, (der eine Zahl sein kann), und der innerhalb dieser
	 * Installation eindeutig ist.
	 */
	public String getPatCode(){
		String rc=get("PatientNr");
		if(!StringTool.isNothing(rc)){
			return rc;
		}
		if(Hub.globalCfg.get("PatIDMode","number").equals("number")){
			while(true){
				String lockid=PersistentObject.lock("PatNummer",true);
				String pid=j.queryString("SELECT WERT FROM CONFIG WHERE PARAM='PatientNummer'");
				if(StringTool.isNothing(pid)){
					pid="0";
					j.exec("INSERT INTO CONFIG (PARAM,WERT) VALUES ('PatientNummer','0')");
				}
				int lastNum=Integer.parseInt(pid)+1;
				rc=Integer.toString(lastNum);
				j.exec("UPDATE CONFIG set wert='"+rc+"' where param='PatientNummer'");
				PersistentObject.unlock("PatNummer",lockid);
				String exists=j.queryString("SELECT ID FROM KONTAKT WHERE PatientNr="+JdbcLink.wrap(rc));
				if(exists==null){
					break;
				}
			}
		}else{
			String[] ret=new String[3];
			if(get(new String[]{"Name","Vorname","Geburtsdatum"},ret)==true){
				StringBuffer code=new StringBuffer(12); 
				if((ret[0]!=null) && (ret[0].length()>1)){
					code.append(ret[0].substring(0,2));
				}
				if((ret[1]!=null) && (ret[1].length()>1)){
					code.append(ret[1].substring(0,2));
				}
				if((ret[2]!=null) && (ret[2].length()==10)){
					int quersumme=Integer.parseInt(ret[2].substring(8));
					quersumme+=Integer.parseInt(ret[2].substring(3,5));
					quersumme+=Integer.parseInt(ret[2].substring(0,2));
					code.append(Integer.toString(quersumme));
					//code.append(ret[2].substring(8)).append(ret[2].substring(3,5)).append(ret[2].substring(0,2));
				}
				rc=code.toString();
				Query<Kontakt> qbe=new Query<Kontakt>(Kontakt.class);
				qbe.add("PatientNr","LIKE",rc+"%");
				List<Kontakt> list=qbe.execute();
				if(!list.isEmpty()){
					int l=list.size()+1;
					code.append("-").append(l);
					rc=code.toString();
				}
			}
		}
		set("PatientNr",rc);
		return rc;
	}

	public Money getKontostand(){
		StringBuilder sql=new StringBuilder();
		sql.append("SELECT betrag FROM KONTO WHERE PatientID=").append(getWrappedId());
		Stm stm=j.getStatement();
		Money konto=new Money();
		try{
			ResultSet res=stm.query(sql.toString());
			while(res.next()){
				int buchung=res.getInt(1);
				konto.addCent(buchung);
			}
			return konto;
		}catch(Exception ex){
			ExHandler.handle(ex);
			return null;
		}finally{
			j.releaseStatement(stm);
		}
	}
  
    /**
     * Calculates a possibly available account excess. (This value may be
     * added to a bill as prepayment.)
     * <p>
     * Considers all overpaid bills and account transactions not bound to a bill.
     * The garant of the bill must be the patient itself.
     * (Bills not yet paid or partly paid are not considered.)
     * <p>
     * This value is not the same as the current account balance, since we
     * ignore outstanding debts of not yet paid bills.
     * 
     * @return the account excess (may be zero or positive)
     */
    public Money getAccountExcess() {
    	Money prepayment = new Money();
    	
    	// overpaid bills of this patient
    	// TODO do an optimized query over KONTAKT/FALL/RECHNUNG
    	Query<Rechnung> rQuery = new Query<Rechnung>(Rechnung.class);
    	
    	// normally do not display other mandator's balance
    	if(Hub.acl.request(AccessControlDefaults.ACCOUNTING_GLOBAL)==false){
    		rQuery.add("MandantID", "=", Hub.actMandant.getId());
    	}
    	
    	// let the database engine do the filtering
    	Fall[] faelle=getFaelle();
    	if((faelle!=null) && (faelle.length>0)){
    		rQuery.startGroup();
    		for(Fall fall:faelle){
        		rQuery.add("FallID", "=", fall.getId());
        		rQuery.or();
        	}
    		rQuery.endGroup();
    	}
    	
    	List<Rechnung> rechnungen = rQuery.execute();
    	if (rechnungen != null) {
    		for (Rechnung rechnung : rechnungen) {
    			Fall fall=rechnung.getFall();
    			if(fall!=null){	// of course this should never happen
					Query<AccountTransaction> atQuery = new Query<AccountTransaction>(AccountTransaction.class);
					atQuery.add("PatientID", "=", getId());
					atQuery.add("RechnungsID", "=", rechnung.getId());

					List<AccountTransaction> transactions = atQuery.execute();
					if (transactions != null) {
						Money sum = new Money();
						for (AccountTransaction transaction : transactions) {
							sum.addMoney(transaction.getAmount());
						}
						if (sum.getCents() > 0) {
							prepayment.addMoney(sum);
						}
					}
    			}
    		}
    	}
    	
    	// account (sum over all account transactions not assigned to a bill)
    	Query<AccountTransaction> atQuery = new Query<AccountTransaction>(AccountTransaction.class);
    	atQuery.add("PatientID", "=", getId());
    	List<AccountTransaction> transactions = atQuery.execute();
    	if (transactions != null) {
    		Money sum = new Money();
    		for (AccountTransaction transaction : transactions) {
    			Rechnung rechnung = transaction.getRechnung();
    			if ((rechnung == null) || !rechnung.exists()) {
    				sum.addMoney(transaction.getAmount());
    			}
    		}
    		prepayment.addMoney(sum);
    	}
    	
    	return prepayment;
    }

	/** Einen Patienten mit gegebener ID aus der Datenbank einlesen */
	public static Patient load(final String id){
        Patient ret=new Patient(id);
        return ret;
    }
    
	
    private Patient(final String id){
    	super(id);
    }
   
    @Override
    protected String getConstraint()
    {
        return "istPatient='1'";
    }
    @Override
	protected void setConstraint(){
        set(new String[]{"istPatient","istPerson"},"1","1");
    }
	@Override
	/**
	 * Return a short or long label for this Patient
	 * 
	 * This implementation returns "<Vorname> <Name>" for the sort label,
	 * and calls getPersonalia() for the long label.
	 * 
	 * @return a label describing this Patient
	 */
	public String getLabel(final boolean shortLabel) {
		if (shortLabel) {
			return super.getLabel(true);
		} else {
			return getPersonalia();
		}
	}

	/**
	 * We do not allow direct deletion -> use remove instead
	 */
	@Override
	public boolean delete() {
		return delete(false);
	}
	/**
	 * Einen Patienten aus der Datenbank entfernen. Dabei werden auch alle verknüpften Daten
	 * gelöscht (Labor, Rezepte, AUF, Rechnungen etc.)
	 * Plugins, welche patientenspezifische Daten speichern, sollten diese ebenfalls löschen
	 * (sie erhalten einen ObjectEvent)
	 * @param force bei true wird der Patient auf jeden Faöll gelöscht, bei false nur, wenn keine
	 * Fälle von ihm existieren.
	 * @return false wenn der Patient nicht gelöscht werden konnte.
	 */
    public boolean delete(final boolean force){
        Fall[] fl=getFaelle();
        if((fl.length==0) || ((force==true) && (Hub.acl.request(AccessControlDefaults.DELETE_FORCED)==true))){
        	for(Fall f:fl){
        		f.delete(true);
        	}
        	delete_dependent();
        	return super.delete();
        }
        return false;
    }
    private boolean delete_dependent(){
    	for(LabResult lr:new Query<LabResult>(LabResult.class,"PatientID",getId()).execute()){
    		lr.delete();
    	}
    	for(Rezept rp:new Query<Rezept>(Rezept.class,"PatientID",getId()).execute()){
    		rp.delete();
    	}
    	for(Brief br:new Query<Brief>(Brief.class,"PatientID",getId()).execute()){
    		br.delete();
    	}
    	for(AccountTransaction at:new Query<AccountTransaction>(AccountTransaction.class,"PatientID",getId()).execute()){
    		at.delete();
    	}
    	return true;
    }
	@Override
	public boolean isDragOK() {
		return true;
	}
	
	/**
	 * Try get("Foo") from somewhere in Elexis or one of the plugins!
	 * Or use the field [Patient.Foo] in a text template.
	 * @return an aribtrary String as result of the call to get("NameOfMethod")
	 */
	public String getFoo(){
		return "Bar";
				
	}

	public String getAlter(){
		TimeTool now=new TimeTool();
		TimeTool bd=new TimeTool(getGeburtsdatum());
		int jahre=now.get(TimeTool.YEAR)-bd.get(TimeTool.YEAR);
		bd.set(TimeTool.YEAR,now.get(TimeTool.YEAR));
		if(bd.isAfter(now)){
			jahre-=1;
		}
		return Integer.toString(jahre);
	}
	/**
	 * Return all bills of this patient
	 * @return a list of bills of this patient
	 */
	public List<Rechnung> getRechnungen() {
		List<Rechnung> rechnungen = new ArrayList<Rechnung>(); 
		
		Fall[] faelle = getFaelle();
		if ((faelle != null) && (faelle.length > 0)) {
			Query<Rechnung> query = new Query<Rechnung>(Rechnung.class);
			query.insertTrue();
			query.startGroup();
			for (Fall fall : faelle) {
				query.add("FallID", "=", fall.getId());
				query.or();
			}
			query.endGroup();
			
			List<Rechnung> rnList = query.execute();
			if (rnList != null) {
				rechnungen.addAll(rnList);
			}
		}
		
		return rechnungen;
	}
}
