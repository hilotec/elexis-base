/*******************************************************************************
 * Copyright (c) 2007-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: ICalTransfer.java 4780 2008-12-09 18:10:22Z rgw_ch $
 *******************************************************************************/

package ch.elexis.agenda.data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.agenda.Messages;
import ch.elexis.agenda.preferences.PreferenceConstants;
import ch.elexis.data.Query;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

import com.tiff.common.ui.datepicker.DatePickerCombo;

/**
 * Convert appointments to and frim ICal-Format
 * @author gerry
 *
 */
public class ICalTransfer {
	String[] bereiche;
	final String NOFILESELECTED = " - keine Datei ausgewählt -";
	String typ = Termin.typStandard();
	String status = Termin.statusStandard();
	Button bFile;
	Combo cbTypes, cbStates;
	Button bAll;
	Label lFilename;
	
	public ICalTransfer(){
		bereiche =
			Hub.globalCfg.get(PreferenceConstants.AG_BEREICHE, Messages.TagesView_14).split(",");
	}

	/**
	 * Export a range of appointments to ICal. The Method ends with a Call to an ICalExportDialog
	 * to let the user decide where to store the resulting file.
	 * @param von Begin of the range to export (null: today)
	 * @param bis end of the range to export (null: today)
	 * @param bereich agenda mandator whose appointments should be exported
	 */
	public void doExport(TimeTool von, TimeTool bis, String bereich){
		if (von == null) {
			von = new TimeTool();
		}
		if (bis == null) {
			bis = new TimeTool();
		}
		if (bereich == null) {
			bereich = bereiche[0];
		}
		new ICalExportDlg(von, bis, bereich).open();
	}
	
	/**
	 * Import an ICal-File
	 * @param bereich agenda mandator to whom the ical should be imported
	 */
	public void doImport(String bereich){
		if (bereich == null) {
			bereich = bereiche[0];
		}
		new ICalImportDlg(bereich).open();
	}
	
	class ICalImportDlg extends TitleAreaDialog {
		String m;
		FileDialog fileDialog;
		
		public ICalImportDlg(String bereich){
			super(Desk.getTopShell());
			m = bereich;
		}
		
		@Override
		protected Control createDialogArea(Composite parent){
			Composite ret = new Composite(parent, SWT.NONE);
			ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
			ret.setLayout(new GridLayout(2, false));
			bFile = new Button(ret, SWT.PUSH);
			lFilename = new Label(ret, SWT.NONE);
			lFilename.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
			lFilename.setText(NOFILESELECTED);
			bFile.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
			bFile.setText("Datei für Import...");
			bFile.addSelectionListener(new SelectionAdapter() {
				
				@Override
				public void widgetSelected(SelectionEvent e){
					fileDialog = new FileDialog(getShell(), SWT.OPEN);
					fileDialog.setFilterExtensions(new String[] {
						"*.ics"
					});
					fileDialog.setFilterNames(new String[] {
						"iCal Dateien"
					});
					String file = fileDialog.open();
					if (file == null) {
						lFilename.setText(NOFILESELECTED);
						getButton(IDialogConstants.OK_ID).setEnabled(false);
					} else {
						lFilename.setText(file);
						getButton(IDialogConstants.OK_ID).setEnabled(true);
					}
				}
				
			});
			new Label(ret, SWT.NONE).setText("Termintyp vorgeben");
			cbTypes = new Combo(ret, SWT.SINGLE | SWT.READ_ONLY);
			new Label(ret, SWT.NONE).setText("Terminstatus vorgeben");
			cbStates = new Combo(ret, SWT.SINGLE | SWT.READ_ONLY);
			bAll = new Button(ret, SWT.CHECK);
			bAll.setText("Neue Termine für alle Bereiche eintragen");
			bAll.setLayoutData(SWTHelper.getFillGridData(2, true, 1, false));
			cbTypes.setItems(Termin.TerminTypes);
			cbStates.setItems(Termin.TerminStatus);
			cbTypes.setText(Termin.typStandard());
			cbStates.setText(Termin.statusStandard());
			cbTypes.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
			cbStates.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
			return ret;
		}
		
		@Override
		public void create(){
			super.create();
			getShell().setText("Termine importieren");
			setTitle("iCal nach Agenda importieren");
			setMessage("Wählen Sie bitte eine iCal-Datei zum Importieren aus");
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
		
		@Override
		protected void okPressed(){
			if (!lFilename.getText().equals(NOFILESELECTED)) {
				CalendarBuilder cb = new CalendarBuilder();
				String[] b = new String[1];
				b[0] = m;
				if (bAll.getSelection()) {
					b = bereiche;
				}
				
				try {
					Calendar cal = cb.build(new FileInputStream(lFilename.getText()));
					TimeTool ttFrom = new TimeTool();
					TimeTool ttUntil = new TimeTool();
					Date dt = null;
					List<Component> comps = cal.getComponents();
					for (Component comp : comps) {
						if (comp instanceof VEvent) { // TimeZone not supported
							VEvent event = (VEvent) comp;
							PropertyList props = event.getProperties();
							DtStart start = event.getStartDate();
							DtEnd end = event.getEndDate();
							if (start != null) {
								dt = start.getDate();
								ttFrom.setTimeInMillis(dt.getTime());
							} else {
								ttFrom.set(new TimeTool());
							}
							if (end != null) {
								dt = end.getDate();
								ttUntil.setTimeInMillis(dt.getTime());
							} else {
								ttUntil.set(ttFrom);
							}
							Termin termin = null;
							Uid uid = event.getUid();
							String uuid = StringTool.unique("agendaimport");
							if (uid != null) {
								uuid = uid.getValue();
								termin = Termin.load(uuid);
							}
							if (termin == null || (!termin.exists())) {
								for (String ber : b) {
									termin =
										new Termin(uuid, ber, ttFrom
											.toString(TimeTool.DATE_COMPACT), Termin
											.TimeInMinutes(ttFrom), Termin.TimeInMinutes(ttUntil),
											typ, status);
									uuid = StringTool.unique("agendaimport");
								}
							} else {
								termin.set(new String[] {
									"BeiWem", "Tag", "Beginn", "Dauer", "Typ", "Status"
								}, new String[] {
									m, ttFrom.toString(TimeTool.DATE_COMPACT),
									Integer.toString(Termin.TimeInMinutes(ttFrom)),
									Integer.toString(ttFrom.secondsTo(ttUntil) / 60), typ, status
								});
								
							}
							Summary summary = event.getSummary();
							if (summary != null) {
								termin.setText(summary.getValue());
							}
							Description desc = event.getDescription();
							if (desc != null) {
								termin.setGrund(desc.getValue());
							}
						}
					}
				} catch (ParserException e) {
					ExHandler.handle(e);
					SWTHelper.showError("Datenformat falsch",
						"Dies scheint keine gültige iCal-Datei zu sein");
				} catch (Exception ex) {
					ExHandler.handle(ex);
					SWTHelper.showError("Lesefehler", "Konnte Datei " + bFile.getText()
						+ " nicht lesen.");
				}
			}
			super.okPressed();
		}
		
	}
	
	class ICalExportDlg extends TitleAreaDialog {
		
		TimeTool von, bis;
		String m;
		DatePickerCombo dpVon, dpBis;
		Combo cbBereiche;
		FileDialog fileDialog;
		Label lFile;
		
		public ICalExportDlg(TimeTool from, TimeTool until, String bereich){
			super(Desk.getTopShell());
			von = new TimeTool(from);
			bis = new TimeTool(until);
			m = bereich;
			
		}
		
		@Override
		protected Control createDialogArea(Composite parent){
			Composite ret = new Composite(parent, SWT.NONE);
			ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
			ret.setLayout(new GridLayout(3, false));
			new Label(ret, SWT.NONE).setText("Von");
			new Label(ret, SWT.NONE).setText("Bis");
			new Label(ret, SWT.NONE).setText("Bereich");
			dpVon = new DatePickerCombo(ret, SWT.NONE);
			dpBis = new DatePickerCombo(ret, SWT.NONE);
			cbBereiche = new Combo(ret, SWT.NONE);
			cbBereiche.setItems(bereiche);
			cbBereiche.setText(m);
			Button bChose = new Button(ret, SWT.PUSH);
			bChose.setText("Datei");
			bChose.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e){
					fileDialog = new FileDialog(getShell(), SWT.SAVE);
					fileDialog.setFilterExtensions(new String[] {
						"*.ics"
					});
					fileDialog.setFilterNames(new String[] {
						"iCal Dateien"
					});
					String fileName = fileDialog.open();
					if (fileName == null) {
						lFile.setText(NOFILESELECTED);
						getButton(IDialogConstants.OK_ID).setEnabled(false);
					} else {
						lFile.setText(fileName);
						getButton(IDialogConstants.OK_ID).setEnabled(true);
					}
				}
				
			});
			lFile = new Label(ret, SWT.NONE);
			lFile.setLayoutData(SWTHelper.getFillGridData(2, true, 1, false));
			lFile.setText(NOFILESELECTED);
			dpVon.setDate(von.getTime());
			dpBis.setDate(bis.getTime());
			return ret;
			
		}
		
		@Override
		public void create(){
			super.create();
			getShell().setText("Agenda exportieren");
			setTitle("Agenda-Bereich exportieren");
			setMessage("Wählen Sie bitte Beginndatum, Enddatum und Bereich für den Export");
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
		
		@Override
		protected void okPressed(){
			von.setTimeInMillis(dpVon.getDate().getTime());
			bis.setTimeInMillis(dpBis.getDate().getTime());
			Query<Termin> qbe = new Query<Termin>(Termin.class);
			qbe.add("Tag", ">=", von.toString(TimeTool.DATE_COMPACT));
			qbe.add("Tag", "<=", bis.toString(TimeTool.DATE_COMPACT));
			qbe.add("BeiWem", "=", m);
			List<Termin> termine = qbe.execute();
			TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
			TimeZone timezone = registry.getTimeZone("Europe/Zurich");
			VTimeZone tz = timezone.getVTimeZone();
			Calendar calendar = new Calendar();
			calendar.getProperties().add(new ProdId("-//ch.elexis//Elexis v" + Hub.Version));
			calendar.getProperties().add(Version.VERSION_2_0);
			calendar.getProperties().add(CalScale.GREGORIAN);
			for (Termin t : termine) {
				if ((t.getStartMinute() == 0) && (t.getType().equals(Termin.typReserviert()))) {
					continue;
				}
				if ((t.getStartMinute() + t.getDurationInMinutes() == (23 * 60) + 59)
					&& (t.getType().equals(Termin.typReserviert()))) {
					continue;
				}
				TimeTool tt = new TimeTool(t.getStartTime());
				DateTime start = new DateTime(tt.getTime());
				tt.addMinutes(t.getDurationInMinutes());
				DateTime end = new DateTime(tt.getTime());
				VEvent vTermin = new VEvent(start, end, t.getPersonalia());
				vTermin.getProperties().add(tz.getTimeZoneId());
				Uid uid = new Uid(t.getId());
				vTermin.getProperties().add(uid);
				Description desc = new Description(t.getGrund());
				vTermin.getProperties().add(desc);
				calendar.getComponents().add(vTermin);
			}
			try {
				FileOutputStream fout = new FileOutputStream(lFile.getText());
				CalendarOutputter outputter = new CalendarOutputter();
				outputter.output(calendar, fout);
				
			} catch (Exception ex) {
				ExHandler.handle(ex);
				SWTHelper.alert("I/O-Fehler", "Konnte Datei " + lFile.getText()
					+ " nicht schreiben");
			}
			super.okPressed();
		}
		
	}
}
