package com.hilotec.elexis.messwerte.views;

import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import ch.elexis.selectors.ActiveControl;
import ch.elexis.selectors.ActiveControlListener;
import ch.elexis.selectors.TextField;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.TimeTool;

import com.hilotec.elexis.messwerte.data.Messung;
import com.hilotec.elexis.messwerte.data.MessungTyp;
import com.hilotec.elexis.messwerte.data.Messwert;
import com.hilotec.elexis.messwerte.data.Panel;
import com.hilotec.elexis.messwerte.data.typen.IMesswertTyp;
import com.tiff.common.ui.datepicker.DatePickerCombo;

public class MessungBearbeitenWithLayout extends TitleAreaDialog {
	private Messung messung;
	private DatePickerCombo dateWidget;
	private List<Messwert> messwerte;

	public MessungBearbeitenWithLayout(Shell shell, Messung m) {
		super(shell);
		messung = m;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		MessungTyp typ = messung.getTyp();
		ScrolledComposite scroll = new ScrolledComposite(parent, SWT.BORDER
				| SWT.V_SCROLL | SWT.H_SCROLL);
		scroll.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		Composite comp = new Composite(scroll, SWT.NONE);
		scroll.setContent(comp);

		comp.setLayout(new GridLayout());
		comp.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));

		dateWidget = new DatePickerCombo(comp, SWT.NONE);
		dateWidget.setDate(new TimeTool(messung.getDatum()).getTime());
		dateWidget.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		messwerte=messung.getMesswerte();
		createComposite(typ.getPanel(),comp);
		comp.setSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return scroll;
	}

	public Composite createComposite(Panel p, Composite parent){
		Composite ret=new Composite(parent,SWT.NONE);
		//ret.setBackground(Desk.getColor(Desk.COL_BLUE));
		if(p.getType().equals("plain")){
			ret.setLayout(new FillLayout());
		}else if(p.getType().equals("grid")){
			String cols=p.getAttribute("columns");
			if(cols==null){
				ret.setLayout(new GridLayout());
			}else{
				ret.setLayout(new GridLayout(Integer.parseInt(cols),false));
			}
		}else if(p.getType().equals("field")){
			String fieldref=p.getAttribute("ref");
			Messwert mw=getMesswert(fieldref);
			if(mw!=null){
				int flags=0;
				if(p.getAttribute("editable").equals("false")){
					flags|=TextField.READONLY;
				}
				IMesswertTyp dft = mw.getTyp();
				String labelText = dft.getTitle();
				TextField tf=new TextField(ret,flags,labelText);
				tf.setText(mw.getDarstellungswert());
				tf.setData("messwert", mw);
				String validPattern=p.getAttribute("validpattern");
				if(validPattern!=null){
					String invalidMsg=p.getAttribute("invalidmessage");
					tf.setValidPattern(validPattern, invalidMsg==null ? "ungültige Eingabe" : invalidMsg);
				}
				tf.addListener(new ActiveControlListener() {
					
					@Override
					public void titleClicked(ActiveControl field) {
						
					}
					
					@Override
					public void invalidContents(ActiveControl field) {
						setMessage(field.getErrMsg());
					}
					
					@Override
					public void contentsChanged(ActiveControl ac) {
						Messwert messwert=(Messwert)ac.getData("messwert");
						messwert.setWert(ac.getText());
						
					}
				});
				setLayoutData(tf);

			}
		}
		for(Panel panel:p.getPanels()){
			setLayoutData(createComposite(panel,ret));
		}
		return ret;
	}
	
	public Messwert getMesswert(String name){
		for(Messwert m:messwerte){
			if(m.getName().equals(name)){
				return m;
			}
		}
		return null;
	}
	private void setLayoutData(Control c){
		if(c.getParent().getLayout() instanceof GridLayout){
			c.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		}
		c.pack();
	}

	@Override
	protected void okPressed() {
		// TODO Auto-generated method stub
		super.okPressed();
	}

	@Override
	public void create() {
		super.create();
		getShell().setText("Messung bearbeiten");
		setTitle(messung.getTyp().getTitle());
	}

}
