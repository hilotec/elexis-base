/*******************************************************************************
 * Copyright (c) 2007, G. Weirich
 * All rights reserved.    
 * $Id: RegisterAction.java 121 2007-06-10 21:24:54Z Gerry $
 *******************************************************************************/

package ch.elexis.connect.abxmicros;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.preferences.SettingsPreferenceStore;
import ch.elexis.rs232.Connection;
import ch.elexis.util.SWTHelper;

public class Preferences extends PreferencePage implements
		IWorkbenchPreferencePage {
	
	public static final String MICROS_BASE="connectors/abxmicros/";
	public static final String PORT=MICROS_BASE+"port";
	public static final String PARAMS=MICROS_BASE+"params";
	
	Combo ports;
	Text speed,data,stop;
	Button parity;
	public Preferences() {
		super("Reflotron");
		setPreferenceStore(new SettingsPreferenceStore(Hub.localCfg));
	}
	@Override
	protected Control createContents(Composite parent) {
		String[] param=Hub.localCfg.get(PARAMS, "9600,8,n,1").split(",");
		
		Composite ret=new Composite(parent,SWT.NONE);
		ret.setLayout(new GridLayout(2,false));
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		new Label(ret,SWT.NONE).setText("Com-Port");
		ports=new Combo(ret,SWT.SINGLE);
		Connection conn=new Connection(null);
		ports.setItems(conn.getComPorts());
		ports.setText(Hub.localCfg.get(PORT, "COM1"));
		new Label(ret,SWT.NONE).setText("Geschwindigkeit");
		speed=new Text(ret,SWT.BORDER);
		speed.setText(param[0]);
		new Label(ret,SWT.NONE).setText("Datenbits");
		data=new Text(ret,SWT.BORDER);
		data.setText(param[1]);
		new Label(ret,SWT.NONE).setText("Parity");
		parity=new Button(ret,SWT.CHECK);
		parity.setSelection(!param[2].equalsIgnoreCase("n"));
		new Label(ret,SWT.NONE).setText("Stopbits");
		stop=new Text(ret,SWT.BORDER);
		stop.setText(param[3]);
		return ret;
	}

	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public boolean performOk() {
		StringBuilder sb=new StringBuilder();
		sb.append(speed.getText()).append(",")
			.append(data.getText()).append(",")
			.append(parity.getSelection() ? "y" : "n").append(",")
			.append(stop.getText());
		Hub.localCfg.set(PARAMS, sb.toString());
		Hub.localCfg.set(PORT, ports.getText());
		Hub.localCfg.flush();
		return super.performOk();
	}
}
