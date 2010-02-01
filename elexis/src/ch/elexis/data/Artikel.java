/*******************************************************************************
 * Copyright (c) 2005-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 * $Id: Artikel.java 6044 2010-02-01 15:18:50Z rgw_ch $
 *******************************************************************************/
package ch.elexis.data;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.elexis.Hub;
import ch.elexis.preferences.PreferenceConstants;
import ch.elexis.util.Log;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.Money;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

/**
 * Ein Artikel ist ein Objekt, das im Lager vorhanden ist oder sein sollte oder einem Patienten
 * verordnet werden kann
 */
public class Artikel extends VerrechenbarAdapter {
	public static final String FLD_EAN = "EAN";
	public static final String FLD_CODECLASS = "Codeclass";
	public static final String XID_PHARMACODE = "www.xid.ch/id/pharmacode/ch";
	public static final String FLD_SUB_ID = "SubID";
	public static final String ARTIKEL = "Artikel";
	public static final String FLD_LIEFERANT_ID = "LieferantID";
	public static final String FLD_PHARMACODE = "Pharmacode";
	public static final String ANBRUCH = "Anbruch";
	public static final String MINBESTAND = "Minbestand";
	public static final String MAXBESTAND = "Maxbestand";
	public static final String VERKAUFSEINHEIT = "Verkaufseinheit";
	public static final String VERPACKUNGSEINHEIT = "Verpackungseinheit";
	public static final String ISTBESTAND = "Istbestand";
	public static final String FLD_VK_PREIS = "VK_Preis";
	public static final String FLD_EK_PREIS = "EK_Preis";
	public static final String EIGENNAME = "Eigenname";
	public static final String FLD_TYP = "Typ";
	public static final String FLD_NAME = "Name";
	public static final String TABLENAME = "ARTIKEL";
	public static Pattern NAME_VE_PATTERN = Pattern.compile(".+ ([0-9]+) Stk.*");
	
	@Override
	protected String getTableName() {
		return TABLENAME;
	}
	
	public String getXidDomain() {
		return XID_PHARMACODE;
	}
	
	static {
		addMapping(TABLENAME, FLD_LIEFERANT_ID, FLD_NAME, MAXBESTAND, MINBESTAND,
			ISTBESTAND, FLD_EK_PREIS, FLD_VK_PREIS, FLD_TYP, FLD_EXTINFO, FLD_EAN, FLD_SUB_ID,
			"Eigenname=Name_intern", FLD_CODECLASS, "Klasse");
		Xid.localRegisterXIDDomainIfNotExists(XID_PHARMACODE, "Pharmacode", Xid.ASSIGNMENT_REGIONAL);
	}
	
	/**
	 * This implementation of PersistentObject#load is special in that it tries to load the actual
	 * appropriate subclass
	 */
	public static Artikel load(final String id) {
		if (id == null) {
			return null;
		}
		Artikel ret = new Artikel(id);
		if (!ret.exists()) {
			return ret;
		}
		String clazz = ret.get("Klasse");
		if (!StringTool.isNothing(clazz)) {
			try {
				ret = (Artikel) Hub.poFactory.createFromString(clazz + "::"
					+ id);
			} catch (Exception ex) {
				log.log("Fehlerhafter Leistungscode " + clazz + "::" + id, Log.ERRORS);
			}
		}
		return ret;
	}
	
	/**
	 * Einen neuen Artikel mit vorgegebenen Parametern erstellen
	 * 
	 * @param Name
	 * @param Typ
	 */
	public Artikel(final String Name, final String Typ) {
		create(null);
		set(new String[] { FLD_NAME, FLD_TYP }, new String[] { Name, Typ });
	}
	
	public Artikel(final String Name, final String Typ, final String subid) {
		create(null);
		set(new String[] { FLD_NAME, FLD_TYP, FLD_SUB_ID }, Name, Typ, subid);
	}
	
	@Override
	public String getLabel() {
		if (!exists()) {
			return "(" + getName() + ")";
		}
		return getInternalName();
	}
	
	public String[] getDisplayedFields() {
		return new String[] { FLD_TYP, FLD_NAME };
	}
	
	/**
	 * Den internen Namen setzen. Dieser ist vom Anwender frei wählbar und erscheint in der
	 * Artikelauswahl und auf der Rechnung.
	 * 
	 * @param nick
	 *            Der "Spitzname"
	 */
	public void setInternalName(final String nick) {
		set(EIGENNAME, nick);
	}
	
	/**
	 * Den internen Namen holen
	 * 
	 * @return
	 */
	public String getInternalName() {
		String ret = get(EIGENNAME);
		if (StringTool.isNothing(ret)) {
			ret = getName();
		}
		return ret;
	}
	
	/**
	 * Den offiziellen namen holen
	 * 
	 * @return
	 */
	public String getName() {
		return checkNull(get(FLD_NAME));
	}
	
	/**
	 * Den "echten" Namen setzen. Dies ist der offizielle Name des Artikels, wie er beispielsweise
	 * in Katalogen aufgeführt ist. Dieser sollte normalerweise nicht geändert werden.
	 * 
	 * @param name
	 *            der neue "echte" Name
	 */
	public void setName(final String name) {
		set(FLD_NAME, name);
	}
	
	/**
	 * Basis-Einkaufspreis in Rappen pro Einheit
	 * 
	 * @return
	 */
	public Money getEKPreis() {
		try {
			return new Money(checkZero(get(FLD_EK_PREIS)));
		} catch (Throwable ex) {
			Hub.log.log("Fehler beim Einlesen von EK für " + getLabel(), Log.ERRORS);
		}
		return new Money();
		
	}
	
	/**
	 * Basis-Verkaufspreis in Rappen pro Einheit
	 * 
	 * @return
	 */
	public Money getVKPreis() {
		try {
			return new Money(checkZero(get(FLD_VK_PREIS)));
		} catch (Throwable ex) {
			Hub.log.log("Fehler beim Einlesen von VK für " + getLabel(), Log.ERRORS);
		}
		return new Money();
		
	}
	
	/**
	 * Einkaufspreis setzen. Das sollte normalerweise nur der Importer tun
	 * 
	 * @param preis
	 */
	public void setEKPreis(final Money preis) {
		set(FLD_EK_PREIS, preis.getCentsAsString());
	}
	
	/**
	 * Den Verkaufspreis setzen. Das sollte bei gesetztlich festgelegten Artikeln nur der Importer
	 * tun.
	 * 
	 * @param preis
	 */
	public void setVKPreis(final Money preis) {
		set(FLD_VK_PREIS, preis.getCentsAsString());
	}
	
	/**
	 * Herausfinden, wieviele Packungen wir noch auf Lager haben
	 * 
	 * @return den Istbestand
	 */
	public int getIstbestand() {
		try {
			return checkZero(get(ISTBESTAND));
		} catch (Throwable ex) {
			Hub.log.log("Fehler beim Einlesen von istbestand für " + getLabel(), Log.ERRORS);
		}
		return 0;
	}
	
	/**
	 * Versuche, die Verpakcungseinheit herauszufinden. Entweder haben wir sie im Artikeldetail
	 * angegeben, dann ist es trivial, oder vielleicht steht im Namen etwas wie xx Stk.
	 * 
	 * @return einen educated guess oder 0 (unknown)
	 */
	public int guessVE() {
		int ret = getVerpackungsEinheit();
		if (ret == 0) {
			String name = getName();
			Matcher matcher = NAME_VE_PATTERN.matcher(name);
			if (matcher.matches()) {
				String num = matcher.group(1);
				try {
					return Integer.parseInt(num);
				} catch (Exception ex) {
					
				}
			}
		}
		return ret;
	}
	
	/**
	 * Herausfinden, wieviele Exemplare wir noch auf Lager haben (Istbestand * Verpackungseinheit)
	 * 
	 * @return Zahl der Einzelabgaben, die noch gemacht werden können
	 */
	public int getTotalCount() {
		int pack = getIstbestand();
		int VE = getPackungsGroesse();
		if (VE == 0) {
			return pack;
		}
		int AE = getAbgabeEinheit();
		if (AE < VE) {
			return (pack * VE) + (getBruchteile() * AE);
		}
		return pack;
	}
	
	/**
	 * Eingestellten Höchstebestand holen
	 * 
	 * @return Wieviele Packungen der Anwender maximal auf Lager haben will
	 */
	public int getMaxbestand() {
		try {
			return checkZero(get(MAXBESTAND));
		} catch (Throwable ex) {
			Hub.log.log("Fehler beim Einlesen von Maxbestand für " + getLabel(), Log.ERRORS);
		}
		return 0;
	}
	
	/**
	 * Eingestellten Mindestbestand holen
	 * 
	 * @return Wieviele Packungen der Anwender mindestens auf Lager haben will.
	 */
	public int getMinbestand() {
		try {
			return checkZero(get(MINBESTAND));
		} catch (Throwable ex) {
			Hub.log.log("Fehler beim Einlesen von Minbestand für " + getLabel(), Log.ERRORS);
		}
		return 0;
	}
	
	/**
	 * Höchstbestand setzen
	 * 
	 * @param s
	 *            Wieviele Packungen der Anwender höchstens auf Lager haben will
	 */
	public void setMaxbestand(final int s) {
		String sl = checkLimit(s);
		if (sl != null) {
			set(MAXBESTAND, sl);
		}
	}
	
	/**
	 * Mindestbestand setzen
	 * 
	 * @param s
	 *            Wieviele Packungen der Anwender mindestens auf Lager haben will
	 */
	public void setMinbestand(final int s) {
		String sl = checkLimit(s);
		if (sl != null) {
			set(MINBESTAND, sl);
		}
	}
	
	/**
	 * Istbestand setzen. Wenn INVENTORY_CHECK_ILLEGAL-VALUES gesetzt ist, erscheint eine Warnung,
	 * wenn der Istbestand unter null komt.
	 * 
	 * @param s
	 *            Wieviele Packungen tatsächlich auf Lager sind
	 */
	public void setIstbestand(final int s) {
		String sl = null;
		if (Hub.globalCfg.get(PreferenceConstants.INVENTORY_CHECK_ILLEGAL_VALUES, true)) {
			sl = checkLimit(s);
		} else {
			sl = Integer.toString(s);
		}
		if (sl != null) {
			set(ISTBESTAND, sl);
		}
	}
	
	/**
	 * Wieviele Abgabeeinheiten aus einer angebrochenen Packung sind da
	 * 
	 * @return Zahl der Abgabeheinheiten aus der angebrochenen Packung
	 */
	public int getBruchteile() {
		return checkZero(getExt(ANBRUCH));
	}
	
	/**
	 * Prüfen, ob der Lagerbestand ungültig ist
	 * 
	 * @param s
	 * @return
	 */
	private String checkLimit(final int s) {
		String str = Integer.toString(s);
		if (s > -1 && s < 1001) {
			return str;
		}
		if (isLagerartikel()) {
			SWTHelper.showError("Ungültiger Lagerbestand", "Der Lagerbestand ist auf "
				+ str + ". Bitte einen Wert zwischen 0 und 1000 eingeben.");
		}
		return null;
	}
	
	/**
	 * Prüfen, ob ein Artikel ein ALgerartikel ist
	 * 
	 * @return true wenn ja
	 */
	public boolean isLagerartikel() {
		if ((getMinbestand() > 0) || (getMaxbestand() > 0)) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Alle Lagerartikel holen.
	 * 
	 * @return
	 */
	public static List<Artikel> getLagerartikel() {
		Query<Artikel> qbe = new Query<Artikel>(Artikel.class);
		qbe.add(MINBESTAND, ">", "0");
		qbe.or();
		qbe.add(MAXBESTAND, ">", "0");
		qbe.orderBy(false, new String[] { FLD_NAME });
		List<Artikel> l = qbe.execute();
		return l == null ? new ArrayList<Artikel>(0) : l;
	}
	
	/**
	 * Eine Abgabeeinheit eines Lagerartikels abgeben. Nörogenfalls wird eine neue Packung
	 * angebrochen.
	 * 
	 * @param n
	 */
	@SuppressWarnings("unchecked")
	public void einzelAbgabe(final int n) {
		Hashtable<String, String> ext = getHashtable(FLD_EXTINFO);
		int anbruch = checkZero(ext.get(ANBRUCH));
		int ve = checkZero(ext.get(VERKAUFSEINHEIT));
		int vk = checkZero(ext.get(VERPACKUNGSEINHEIT));
		if (vk == 0) {
			if (ve != 0) {
				vk = ve;
				ext.put(VERKAUFSEINHEIT, Integer.toString(vk));
				setHashtable(FLD_EXTINFO, ext);
			}
		}
		if (ve == 0) {
			if (vk != 0) {
				ve = vk;
				ext.put(VERPACKUNGSEINHEIT, Integer.toString(ve));
				setHashtable(FLD_EXTINFO, ext);
			}
		}
		int num = n * ve;
		if (vk == ve) {
			setIstbestand(getIstbestand() - n);
		} else {
			int rest = anbruch - num;
			while (rest < 0) {
				rest = rest + vk;
				setIstbestand(getIstbestand() - 1);
			}
			ext.put(ANBRUCH, Integer.toString(rest));
			setHashtable(FLD_EXTINFO, ext);
		}
	}
	
	/**
	 * Eine Einzelabgabe wieder einbuchen
	 * 
	 * @param n
	 */
	@SuppressWarnings("unchecked")
	public void einzelRuecknahme(final int n) {
		Hashtable<String, String> ext = getHashtable(FLD_EXTINFO);
		int anbruch = checkZero(ext.get(ANBRUCH));
		int ve = checkZero(ext.get(VERKAUFSEINHEIT));
		int vk = checkZero(ext.get(VERPACKUNGSEINHEIT));
		int num = n * ve;
		if (vk == ve) {
			setIstbestand(getIstbestand() + n);
		} else {
			int rest = anbruch + num;
			while (rest > vk) {
				rest = rest - vk;
				setIstbestand(getIstbestand() + 1);
			}
			ext.put(ANBRUCH, Integer.toString(rest));
			setHashtable(FLD_EXTINFO, ext);
		}
	}
	
	public String getEAN() {
		String ean = get(FLD_EAN);
		return ean;
	}
	
	public void setEAN(String ean) {
		set(FLD_EAN, ean);
	}
	
	@SuppressWarnings("unchecked")
	public String getPharmaCode() {
		Hashtable ext = getHashtable(FLD_EXTINFO);
		return checkNull((String) ext.get(FLD_PHARMACODE));
	}
	
	public Kontakt getLieferant() {
		return Kontakt.load(get(FLD_LIEFERANT_ID));
	}
	
	public void setLieferant(final Kontakt l) {
		set(FLD_LIEFERANT_ID, l.getId());
	}
	
	@SuppressWarnings("unchecked")
	public int getVerpackungsEinheit() {
		Hashtable ext = getHashtable(FLD_EXTINFO);
		return checkZero((String) ext.get(VERPACKUNGSEINHEIT));
	}
	
	@SuppressWarnings("unchecked")
	public int getVerkaufseinheit() {
		Hashtable ext = getHashtable(FLD_EXTINFO);
		return checkZero((String) ext.get(VERKAUFSEINHEIT));
	}
	
	public int getPackungsGroesse() {
		return checkZero(getExt(VERPACKUNGSEINHEIT));
	}
	
	public int getAbgabeEinheit() {
		return checkZero(getExt(VERKAUFSEINHEIT));
	}
	
	@SuppressWarnings("unchecked")
	public void setExt(final String name, final String value) {
		Hashtable h = getHashtable(FLD_EXTINFO);
		if (value == null) {
			h.remove(name);
		} else {
			h.put(name, value);
		}
		setHashtable(FLD_EXTINFO, h);
	}
	
	@SuppressWarnings("unchecked")
	public String getExt(final String name) {
		Hashtable h = getHashtable(FLD_EXTINFO);
		return checkNull((String) h.get(name));
	}
	
	protected Artikel(final String id) {
		super(id);
	}
	
	protected Artikel() {
	}
	
	/************************ Verrechenbar ************************/
	@Override
	public String getCode() {
		return getId();
	}
	
	@Override
	public String getText() {
		return getInternalName();
	}
	
	@Override
	public String getCodeSystemName() {
		return ARTIKEL;
	}
	
	@SuppressWarnings("unchecked")
	public int getPreis(final TimeTool dat, final Fall fall) {
		double vkt = checkZeroDouble(get(FLD_VK_PREIS));
		Hashtable ext = getHashtable(FLD_EXTINFO);
		double vpe = checkZeroDouble((String) ext.get(VERPACKUNGSEINHEIT));
		double vke = checkZeroDouble((String) ext.get(VERKAUFSEINHEIT));
		if ((vpe > 0.0) && (vke > 0.0) && (vpe != vke)) {
			return (int) Math.round(vke * (vkt / vpe));
		} else {
			return (int) Math.round(vkt);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Money getKosten(final TimeTool dat) {
		double vkt = checkZeroDouble(get(FLD_EK_PREIS));
		Hashtable ext = getHashtable(FLD_EXTINFO);
		double vpe = checkZeroDouble((String) ext.get(VERPACKUNGSEINHEIT));
		double vke = checkZeroDouble((String) ext.get(VERKAUFSEINHEIT));
		if (vpe != vke) {
			return new Money((int) Math.round(vke * (vkt / vpe)));
		} else {
			return new Money((int) Math.round(vkt));
		}
	}
	
	public int getTP(final TimeTool date, final Fall fall) {
		return getPreis(date, fall);
	}
	
	public double getFactor(final TimeTool date, final Fall fall) {
		return 1.0;
	}
	
	@Override
	protected String[] getExportFields() {
		return new String[] { FLD_EAN, FLD_SUB_ID, FLD_LIEFERANT_ID, "Klasse",
			FLD_NAME, MAXBESTAND, MINBESTAND, ISTBESTAND, FLD_EK_PREIS, FLD_VK_PREIS,
			FLD_TYP, FLD_CODECLASS, FLD_EXTINFO };
	}
	
	@Override
	protected String getExportUIDValue() {
		String pharmacode = getExt(FLD_PHARMACODE);
		String ean = get(FLD_EAN);
		return ean + "_" + pharmacode;
	}
}
