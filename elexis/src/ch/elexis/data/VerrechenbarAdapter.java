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
 * $Id: VerrechenbarAdapter.java 5970 2010-01-27 16:43:04Z rgw_ch $
 *******************************************************************************/

package ch.elexis.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IFilter;

import ch.elexis.Desk;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.dialogs.AddElementToBlockDialog;
import ch.elexis.util.IOptifier;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.Money;
import ch.rgw.tools.TimeTool;
import ch.rgw.tools.JdbcLink.Stm;

public abstract class VerrechenbarAdapter extends PersistentObject implements IVerrechenbar {
	
	protected IAction addToBlockAction;
	
	@Override
	public String getLabel(){
		return getText();
	}
	
	@Override
	protected abstract String getTableName();
	
	public String getCode(){
		return null;
	}
	
	public String getCodeSystemName(){
		return null;
	}
	
	public String getText(){
		return null;
	}
	
	public IOptifier getOptifier(){
		return optifier;
	}
	
	public Comparator<IVerrechenbar> getComparator(){
		return comparator;
	}
	
	public IFilter getFilter(final Mandant m){
		return ifilter;
	}
	
	public List<IAction> getActions(Verrechnet kontext){
		ArrayList<IAction> actions = new ArrayList<IAction>(1);
		if (addToBlockAction == null) {
			makeActions(this);
		}
		actions.add(addToBlockAction);
		return actions;
	}
	
	public void setVKMultiplikator(final TimeTool von, TimeTool bis, final double factor,
		final String typ){
		StringBuilder sql = new StringBuilder();
		String eoue = new TimeTool(TimeTool.END_OF_UNIX_EPOCH).toString(TimeTool.DATE_COMPACT);
		if (bis == null) {
			bis = new TimeTool(TimeTool.END_OF_UNIX_EPOCH);
		}
		String from = von.toString(TimeTool.DATE_COMPACT);
		Stm stm = getConnection().getStatement();
		sql.append("UPDATE VK_PREISE SET DATUM_BIS=").append(JdbcLink.wrap(from)).append(
			" WHERE (DATUM_BIS=").append(JdbcLink.wrap(eoue)).append(
			" OR DATUM_BIS='99991231') AND TYP=").append(JdbcLink.wrap(typ));
		stm.exec(sql.toString());
		sql.setLength(0);
		sql.append("INSERT INTO VK_PREISE (DATUM_VON,DATUM_BIS,MULTIPLIKATOR,TYP) VALUES (")
			.append(JdbcLink.wrap(von.toString(TimeTool.DATE_COMPACT))).append(",").append(
				JdbcLink.wrap(bis.toString(TimeTool.DATE_COMPACT))).append(",").append(
				JdbcLink.wrap(Double.toString(factor))).append(",").append(JdbcLink.wrap(typ))
			.append(");");
		stm.exec(sql.toString());
		getConnection().releaseStatement(stm);
	}
	
	public double getVKMultiplikator(final TimeTool date, final String typ){
		return getMultiplikator(date, "VK_PREISE", typ);
	}
	
	public double getVKMultiplikator(final TimeTool date, final Fall fall){
		return getMultiplikator(date, "VK_PREISE", fall.getAbrechnungsSystem());
	}
	
	public double getEKMultiplikator(final TimeTool date, final Fall fall){
		return getMultiplikator(date, "EK_PREISE", fall.getAbrechnungsSystem());
	}
	
	private double getMultiplikator(final TimeTool date, final String table, final String typ){
		String actdat = JdbcLink.wrap(date.toString(TimeTool.DATE_COMPACT));
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT MULTIPLIKATOR FROM ").append(table).append(" WHERE TYP=").append(
			JdbcLink.wrap(typ)).append(" AND DATUM_VON <=").append(actdat).append(
			" AND DATUM_BIS >").append(actdat);
		String res = getConnection().queryString(sql.toString() + " AND ID=" + getWrappedId());
		if (res == null) {
			res = getConnection().queryString(sql.toString());
		}
		return res == null ? 1.0 : Double.parseDouble(res);
	}
	
	public Money getKosten(final TimeTool dat){
		return new Money(0);
	}
	
	public int getMinutes(){
		return 0;
	}
	
	protected VerrechenbarAdapter(final String id){
		super(id);
	}
	
	public String getCodeSystemCode(){
		return "999";
	}
	
	protected VerrechenbarAdapter(){
		makeActions(this);
	}
	
	private void makeActions(final ICodeElement el){
		addToBlockAction = new Action("Zu Leistungsblock...") {
			@Override
			public void run(){
				AddElementToBlockDialog adb = new AddElementToBlockDialog(Desk.getTopShell());
				if (adb.open() == Dialog.OK) {
					ICodeElement ice =
						(ICodeElement) ElexisEventDispatcher.getSelected(el.getClass());
					Leistungsblock lb = adb.getResult();
					lb.addElement(ice);
					ElexisEventDispatcher.reload(Leistungsblock.class);
				}
			}
		};
	}
}
