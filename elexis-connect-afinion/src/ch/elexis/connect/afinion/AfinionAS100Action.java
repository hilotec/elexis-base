package ch.elexis.connect.afinion;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.connect.afinion.packages.PackageException;
import ch.elexis.connect.afinion.packages.Record;
import ch.elexis.data.LabItem;
import ch.elexis.data.Labor;
import ch.elexis.data.Patient;
import ch.elexis.data.Query;
import ch.elexis.dialogs.KontaktSelektor;
import ch.elexis.rs232.AbstractConnection;
import ch.elexis.rs232.AbstractConnection.ComPortListener;
import ch.elexis.util.SWTHelper;

public class AfinionAS100Action extends Action implements ComPortListener {
	
	AfinionConnection _ctrl;
	Labor _myLab;
	Thread msgDialogThread;
	Thread infoDialogThread;
	Patient selectedPatient;
	Logger _log;
	Record lastRecord = null;
	
	public AfinionAS100Action(){
		super(Messages.getString("AfinionAS100Action.ButtonName"), AS_CHECK_BOX);
		setToolTipText(Messages.getString("AfinionAS100Action.ToolTip"));
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin("ch.elexis.connect.afinion",
			"icons/afinion.png"));
	}
	
	private void initConnection(){
		if (_ctrl != null && _ctrl.isOpen()) {
			_ctrl.close();
		}
		_ctrl =
			new AfinionConnection(Messages.getString("AfinionAS100Action.ConnectionName"),
				Hub.localCfg.get(Preferences.PORT, Messages
					.getString("AfinionAS100Action.DefaultPort")), Hub.localCfg.get(
					Preferences.PARAMS, Messages.getString("AfinionAS100Action.DefaultParams")),
				this);
		
		_ctrl.setCurrentDate(new GregorianCalendar(2007, 5, 26, 0, 0, 0));
		
		if (Hub.localCfg.get(Preferences.LOG, "n").equalsIgnoreCase("y")) {
			try {
				_log =
					new Logger(System.getProperty("user.home") + File.separator + "elexis"
						+ File.separator + "afinion.log");
			} catch (FileNotFoundException e) {
				SWTHelper.showError(Messages.getString("AfinionAS100Action.LogError.Title"),
					Messages.getString("AfinionAS100Action.LogError.Text"));
				_log = new Logger();
			}
		} else {
			_log = new Logger(false);
		}
	}
	
	@Override
	public void run(){
		if (isChecked()) {
			initConnection();
			_log.logStart();
			String msg = _ctrl.connect();
			if (msg == null) {
				String timeoutStr =
					Hub.localCfg.get(Preferences.TIMEOUT, Messages
						.getString("AfinionAS100Action.DefaultTimeout"));
				int timeout = 20;
				try {
					timeout = Integer.parseInt(timeoutStr);
				} catch (NumberFormatException e) {
					// Do nothing. Use default value
				}
				_ctrl.awaitFrame(Desk.getTopShell(),
					"Daten werden aus dem Afinion AS100 gelesen..", 1, 4, 0, timeout);
				return;
			} else {
				_log.log("Error");
				SWTHelper
					.showError(Messages.getString("AfinionAS100Action.RS232.Error.Title"), msg);
			}
		} else {
			if (_ctrl.isOpen()) {
				_ctrl.sendBreak();
				_ctrl.close();
			}
		}
		setChecked(false);
		_log.logEnd();
	}
	
	public void gotBreak(final AbstractConnection connection){
		connection.close();
		setChecked(false);
		_log.log("Break");
		_log.logEnd();
		SWTHelper.showError(Messages.getString("AfinionAS100Action.RS232.Break.Title"), Messages
			.getString("AfinionAS100Action.RS232.Break.Text"));
	}
	
	/**
	 * Liest Bytes aus einem Bytearray
	 */
	private byte[] subBytes(final byte[] bytes, final int pos, final int length){
		byte[] retVal = new byte[length];
		for (int i = 0; i < length; i++) {
			retVal[i] = bytes[pos + i];
		}
		return retVal;
	}
	
	/**
	 * Einzelner Messwert wird verarbeitet
	 * @param probe
	 */
	private void processRecord(final Record record) {
			Desk.getDisplay().syncExec(new Runnable() {
				
				public void run(){
					selectedPatient = GlobalEvents.getSelectedPatient();
					Patient probePat = null;
					String vorname = null;
					String name = null;
					String patientStr = "Patient: Unbekannt (" + record.getId() + ")\n";
					
					if (record.getId() != null) {
						String patIdStr = record.getId();
						Long patId = null;
						try {
							patId = new Long(patIdStr);
						} catch(NumberFormatException e) {
							// Do nothing
						}
						
						// Patient-ID oder Name?
						Query<Patient> patQuery = new Query<Patient>(Patient.class);
						if (patId != null) {
							patQuery.add(Patient.PATID, "=", patIdStr);
						} else {
							String[] parts = patIdStr.split(",");
							if (parts.length > 1) {
								vorname = parts[1].toUpperCase();
								if (parts[1].length() > 1) {
									vorname = parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1);
								}
								patQuery.add(Patient.FIRSTNAME, "like", vorname + "%");
							}
							if (parts.length > 0) {
								name = parts[0].toUpperCase();
								if (parts[0].length() > 1) {
									name = parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1);
								}
								patQuery.add(Patient.NAME, "like", name + "%");
							}
						}
						List<Patient> patientList = patQuery.execute();
						
						if (patientList.size() == 1) {
							probePat = patientList.get(0);
							patientStr =
								"Patient: " + probePat.getName() + ", " + probePat.getVorname() + " ("
									+ record.getId() + ")\n";
						}
					}
					
					String text = patientStr + "Run: " + record.getRunNr() + "\n" + record.getText();
					
					boolean ok = MessageDialog.openConfirm(Desk.getTopShell(), "Afinion AS100", text);
					if (ok) {
						boolean showSelectionDialog = false;
						if (selectedPatient == null) {
							if (probePat != null) {
								selectedPatient = probePat;
							} else {
								showSelectionDialog = true;
							}
						} else {
							if (probePat == null) {
								showSelectionDialog = true;
							}
						}
						
						if (showSelectionDialog) {
							Desk.getDisplay().syncExec(new Runnable() {
								public void run(){
									// TODO: Filter vorname/name in KontaktSelektor einbauen
									KontaktSelektor ksl =
										new KontaktSelektor(Hub.getActiveShell(), Patient.class, Messages
											.getString("ReflotronSprintAction.Patient.Title"), Messages
											.getString("ReflotronSprintAction.Patient.Text"));
									ksl.create();
									ksl.getShell().setText(
										Messages.getString("ReflotronSprintAction.Patient.Title"));
									if (ksl.open() == org.eclipse.jface.dialogs.Dialog.OK) {
										selectedPatient = (Patient) ksl.getSelection();
									} else {
										selectedPatient = null;
									}
									
								}
							});
						}
						if (selectedPatient != null) {
							try {
								record.write(selectedPatient);
							} catch (PackageException e) {
								SWTHelper.showError(Messages
									.getString("ReflotronSprintAction.ProbeError.Title"), e.getMessage());
							}
						} else {
							SWTHelper.showError(Messages.getString("ReflotronSprintAction.Patient.Title"),
								"Kein Patient ausgewählt!");
						}
					_log.log("Saved");
					GlobalEvents.getInstance().fireUpdateEvent(LabItem.class);
				}
			}
		});
	}
	
	/**
	 * Messagedaten von Afinion wurden gelesen
	 */
	public void gotData(final AbstractConnection connection, final byte[] data){
		_log.logRX(new String(data));
		
		// Record lesen
		int pos = 0;
		int i=0;
		int validRecords = 0;
		while (i < 10) {
			byte[] subbytes = subBytes(data, pos, 256);
			Record tmpRecord = new Record(subbytes);
			if (tmpRecord.isValid()) {
				lastRecord = tmpRecord;
				System.out.println(lastRecord.toString());
				validRecords++;
			}
			pos += 256;
			i++;
		}
		
		if (validRecords > 1) { // Last set of records
			Calendar cal = lastRecord.getCalendar();
			cal.add(Calendar.SECOND, -1);
			_ctrl.setCurrentDate(cal);
		} else {
			_ctrl.close();
		
			if (lastRecord != null) {
				processRecord(lastRecord);
			} else {
				SWTHelper.showInfo("Afinion AS100", "Keine Messdaten vorhanden!");
			}
			
			_log.log("Saved");
			GlobalEvents.getInstance().fireUpdateEvent(LabItem.class);
		}
	}
	
	public void closed() {
		_ctrl.close();
		_log.log("Closed");
		setChecked(false);
		_log.logEnd();
	}
	
	public void cancelled() {
		_ctrl.close();
		_log.log("Cancelled");
		setChecked(false);
		_log.logEnd();
	}
	
	public void timeout(){
		_ctrl.close();
		_log.log("Timeout");
		SWTHelper.showError(Messages.getString("AfinionAS100Action.RS232.Timeout.Title"), Messages
			.getString("AfinionAS100Action.RS232.Timeout.Text"));
		setChecked(false);
		_log.logEnd();
	}
}
