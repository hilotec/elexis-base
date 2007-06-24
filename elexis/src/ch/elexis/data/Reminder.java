/*******************************************************************************
 * Copyright (c) 2005-2007, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: Reminder.java 2347 2007-05-07 14:57:30Z rgw_ch $
 *******************************************************************************/

package ch.elexis.data;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.eclipse.jface.viewers.IFilter;

import ch.elexis.Hub;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

/**
 * Ein Reminder ist eine Erinnerung an etwas. Ein Reminder ist an einen Kontakt gebunden.
 * Ein Reminder hat ein Fälligkeitsdatum und einen Status
 * Es gibt mehrere Typen von Remindern:<ul>
 * <li>Nachricht: Es erscheint am und nach dem Fälligkeitsdatum eine Nachricht auf dem
 * Bildschirm, die den Reminder anzeigt. Dies solange, bis der status auf "erledigt" oder
 * "unerledigt" gesetzt wird.</li>
 * <li>Brief: Es wird am Fälligkeitsdatum ein Brief mit gegebener Vorlage zum angegebenen Kontakt erstellt.</li>
 * </ul>
 * @author Gerry
 *
 */
public class Reminder extends PersistentObject implements Comparable{

	@Override
	protected String getTableName() {
		return "REMINDERS";
	}
	static{
		addMapping("REMINDERS","IdentID","Creator=OriginID","Due=S:D:DateDue","Status","Typ",
				"Params","Message","Responsible", 
				"Responsibles=JOINT:ResponsibleID:ReminderID:REMINDERS_RESPONSIBLE_LINK");
	}
	public enum Typ{anzeigeTodoPat,anzeigeTodoAll,anzeigeOeffnen,anzeigeProgstart,brief}
	public static final String[] TypText={"Anzeige nur beim Patienten","Immer in Pendenzen anzeigen",
		"Popup beim Auswählen des Patienten", "Popup beim Einloggen",
		"Brief erstellen"};
	public enum Status{geplant,faellig,ueberfaellig,erledigt,unerledigt}
	
	Reminder(){/* leer */}
	private Reminder(String id){
		super(id);
	}
	/** Einen neuen Reminder erstellen */
	public Reminder(Kontakt ident,String due, Typ typ, String params,String msg){
		create(null);
		if(ident==null){
			ident=Hub.actUser;
		}
		set(new String[]{"IdentID","Creator","Due","Status","Typ","Params","Message"},
			new String[]{ident.getId(),Hub.actUser.getId(),due,Byte.toString((byte)Status.geplant.ordinal()),
				Byte.toString((byte)typ.ordinal()),
				params,msg});
	}
	
	/** Einen Reminder anhand seiner ID aus der Datenbank einlesen */
	public static Reminder load(String id){
		return new Reminder(id);
	}
	public String getLabel(){
		Kontakt k=Kontakt.load(get("IdentID"));
		
		StringBuilder sb=new StringBuilder();
		sb.append(get("Due")).append(" (").append(k.get("Bezeichnung1")).append("): ")
		.append(get("Message"));
		return sb.toString();
	}
	public Typ getTyp(){
		String t=get("Typ");
		if(StringTool.isNothing(t)){
			t="1";
		}
		Typ ret=Typ.values()[Byte.parseByte(t)];
		return ret;
	}
	public Status getStatus(){
		String t=get("Status");
		if(StringTool.isNothing(t)){
			t="0";
		}
		Status ret=Status.values()[Byte.parseByte(t)];
		if(ret==Status.geplant){
			TimeTool now=new TimeTool();
			now.chop(3);
			TimeTool mine=getDateDue();
			if(now.isEqual(mine)){
				return Status.faellig;
			}
			if(now.isAfter(mine)){
				return Status.ueberfaellig;
			}
		}
		return ret;
	}
	
	public String getMessage(){
		return checkNull(get("Message"));
	}
	
	public void setStatus(Status s){
		set("Status", Byte.toString((byte)s.ordinal()));
	}
	public TimeTool getDateDue(){
		TimeTool ret=new TimeTool(get("Due"));
		ret.chop(3);
		return ret;
	}
	public boolean isDue(){
		TimeTool now=new TimeTool();
		TimeTool mine=getDateDue();
		if(mine.isEqual(now)){
			return true;
		}
		return false;
	}
	
	public boolean isOverdue(){
		TimeTool now=new TimeTool();
		TimeTool mine=getDateDue();
		if(mine.isBefore(now)){
			return true;
		}
		return false;
	}
	
	public List<Anwender> getResponsibles(){
		List<String[]> lResp=getList("Responsibles",new String[0]);
		ArrayList<Anwender> ret=new ArrayList<Anwender>(lResp.size());
		for(String[] r:lResp){
			ret.add(Anwender.load(r[0]));
		}
		return ret;
	}
	
	public Anwender getCreator(){
		return Anwender.load(checkNull(get("Creator")));
	}
	/**
	 * Alle heute (oder vor heute) fälligen Reminder holen
	 * @return eine Liste aller fälligen Reminder
	 */
	public static List<Reminder> findForToday(){
		Query<Reminder> qbe=new Query<Reminder>(Reminder.class);
		qbe.add("Due","<=",new TimeTool().toString(TimeTool.DATE_COMPACT));
		qbe.add("Status","<>",Integer.toString(Status.erledigt.ordinal()));
		List<Reminder> ret=qbe.execute();
		return ret;
	}
	
	/**
	 * Alle Reminder zu einem Patienten holen
	 * @param p der Patient 
	 * @param responsible der Verantwortliche oder null: Alle
	 * @return eine Liste aller offenen Reminder dieses Patienten
	 */
	public static List<Reminder> findForPatient(Patient p, Kontakt responsible){
		Query<Reminder> qbe=new Query<Reminder>(Reminder.class);
		qbe.add("IdentID","=",p.getId());
		qbe.add("Status","<>",Integer.toString(Status.erledigt.ordinal()));
		qbe.add("Due","<=",new TimeTool().toString(TimeTool.DATE_COMPACT));
		if(responsible!=null){
			qbe.startGroup();
			qbe.add("Responsible", "=", responsible.getId());
			qbe.or();
			qbe.add("Responsible", "", null);
			qbe.endGroup();
		}
		return qbe.execute();
	}
	
	/**
	 * Alle Reminder holen, die beim Progammstart gezeigt werden sollen
	 * @return
	 */
	public static List<Reminder> findToShowOnStartup(Anwender a){
		Query<Reminder> qbe=new Query<Reminder>(Reminder.class);
		qbe.add("Due","<=",new TimeTool().toString(TimeTool.DATE_COMPACT));
		qbe.add("Status","<>",Integer.toString(Status.erledigt.ordinal()));
		qbe.add("Typ","=",Integer.toString(Typ.anzeigeProgstart.ordinal()));
		return qbe.execute();
	}
	
	/**
	 * Alle Reminder holen, die bei einem bestimmten Patienten für einen bestimmten
	 * Anwender fällig sind
	 * @param p der Patient
	 * @param a der Anwender
	 * @param bOnlyPopup nur die zeigen, die den Typ "Bei Auswahl popup" haben.
	 * @return eine Liste der fälligen Reminder dieses Patienten
	 */
	public static List<Reminder> findRemindersDueFor(Patient p, Anwender a, boolean bOnlyPopup){
		final SortedSet<Reminder> r4a=a.getReminders(p);
		List<Reminder> ret=new ArrayList<Reminder>(r4a.size());
		TimeTool today=new TimeTool();
		for(Reminder r:r4a){
			if(r.getDateDue().isAfter(today)){
				continue;
			}
			if(r.getStatus()==Status.erledigt){
				continue;
			}
			if((bOnlyPopup==true) && (r.getTyp()!=Typ.anzeigeOeffnen)){
				continue;
			}
			ret.add(r);
		}
		
		return ret;
	}
	public Patient getKontakt() {
		Patient ret=Patient.load(get("IdentID"));
		if(ret!=null && ret.exists()){
			return ret;
		}
		return null;
	}
	/**
	 * The comparator is used when reminders are inserted chronologically in a sorted set. 
	 * To allow multiple different reminders at the same day, we use the
	 * id to differentiate reminders with identical dates.
	 */
	public int compareTo(Object o) {
		if(o instanceof Reminder){
			Reminder r=(Reminder)o;
			int i=getDateDue().compareTo(r.getDateDue());
			if(i==0){
				return getId().compareTo(r.getId());
			}else{
				return i;
			}
		}
		return 0;
	}
	
	@Override
	public boolean delete() {
		j.exec("DELETE FROM REMINDERS_RESPONSIBLE_LINK WHERE ReminderID="+getWrappedId());
		return super.delete();
	}
	
}
