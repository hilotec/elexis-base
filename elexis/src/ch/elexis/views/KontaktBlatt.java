/*******************************************************************************
 * Copyright (c) 2005-2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: KontaktBlatt.java 5324 2009-05-29 15:30:24Z rgw_ch $
 *******************************************************************************/

package ch.elexis.views;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.StringConstants;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.actions.GlobalEvents.ActivationListener;
import ch.elexis.actions.GlobalEvents.SelectionListener;
import ch.elexis.admin.AccessControlDefaults;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Organisation;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Person;
import ch.elexis.data.Xid;
import ch.elexis.data.Xid.XIDDomain;
import ch.elexis.dialogs.AnschriftEingabeDialog;
import ch.elexis.dialogs.KontaktExtDialog;
import ch.elexis.util.LabeledInputField;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.LabeledInputField.AutoForm;
import ch.elexis.util.LabeledInputField.InputData;
import ch.elexis.util.LabeledInputField.InputData.Typ;
import ch.rgw.tools.StringTool;

public class KontaktBlatt extends Composite implements SelectionListener, ActivationListener{
	private static final String MOBIL = Messages.getString("KontaktBlatt.MobilePhone"); //$NON-NLS-1$
	private static final String VORNAME = Messages.getString("KontaktBlatt.FirstName"); //$NON-NLS-1$
	private static final String NAME = Messages.getString("KontaktBlatt.LastName"); //$NON-NLS-1$
	private static final String TEL_DIREKT = Messages.getString("KontaktBlatt.OhoneDirect"); //$NON-NLS-1$
	private static final String ANSPRECHPERSON = Messages.getString("KontaktBlatt.ContactPerson"); //$NON-NLS-1$
	private static final String ZUSATZ = Messages.getString("KontaktBlatt.Addidtional"); //$NON-NLS-1$
	private static final String BEZEICHNUNG = Messages.getString("KontaktBlatt.Name"); //$NON-NLS-1$
	static final String[] types={"istOrganisation","istLabor","istPerson","istPatient","istAnwender","istMandant"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	static final String[] typLabels={Messages.getString("KontaktBlatt.Organization"),Messages.getString("KontaktBlatt.Laboratory"),Messages.getString("KontaktBlatt.Person"),Messages.getString("KontaktBlatt.Patient"),Messages.getString("KontaktBlatt.User"),Messages.getString("KontaktBlatt.Mandator")}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	private Button[] bTypes=new Button[types.length];
	private TypButtonAdapter tba=new TypButtonAdapter();
	private IViewSite site;
	private ScrolledForm form;
	private FormToolkit tk;
	AutoForm afDetails;
	
	static final InputData[] def=new InputData[]{
		new InputData(Messages.getString("KontaktBlatt.Bez1"),Kontakt.NAME1,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.Bez2"),Kontakt.NAME2,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.Bez3"),Kontakt.NAME3,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.Sex"),Person.SEX,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.Street"),"Strasse",Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.Zip"),Kontakt.ZIP,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.Place"),Kontakt.PLACE,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.Country"),Kontakt.COUNTRY,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.Phone1"),Kontakt.PHONE1,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.Phone2"),Kontakt.PHONE2,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.Mobile"),Kontakt.MOBILEPHONE,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.Fax"),Kontakt.FAX,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.Mail"),Kontakt.E_MAIL,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.www"),Kontakt.WEBSITE,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.shortLabel"),Kontakt.KUERZEL,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.remark"),Kontakt.REMARK,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.title"),Person.TITLE,Typ.STRING,null), //$NON-NLS-1$
		new InputData(Messages.getString("KontaktBlatt.extid"),"UUID",new LabeledInputField.IContentProvider(){ //$NON-NLS-1$ //$NON-NLS-2$

			public void displayContent(PersistentObject po, InputData ltf) {
				StringBuilder sb=new StringBuilder();
				Xid xid=po.getXid();
				String dom=Xid.getSimpleNameForXIDDomain(xid.getDomain());
				sb.append(dom).append(": ").append(xid.getDomainId()); //$NON-NLS-1$
				ltf.setText(sb.toString());
			}

			public void reloadContent(PersistentObject po, InputData ltf) {
				ArrayList<String> extFlds=new ArrayList<String>();
				Kontakt k=(Kontakt)po;
				for(String dom:Xid.getXIDDomains()){
					XIDDomain xd=Xid.getDomain(dom);
					if((k.istPerson() && xd.isDisplayedFor(Person.class)) || 
						(k.istOrganisation() && xd.isDisplayedFor(Organisation.class)))	{
						extFlds.add(Xid.getSimpleNameForXIDDomain(dom)+"="+dom); //$NON-NLS-1$
					}
				}
				KontaktExtDialog dlg=new KontaktExtDialog(Desk.getTopShell(),(Kontakt) po,extFlds.toArray(new String[0]));
				dlg.open();
				
			}
			
		}),
	};
	private Kontakt actKontakt;
	private Label lbAnschrift;
	
	public KontaktBlatt(Composite parent, int style, IViewSite vs){
		super(parent,style);
		site=vs;
		tk=Desk.getToolkit();
		setLayout(new FillLayout());
		form=tk.createScrolledForm(this);
		Composite body=form.getBody();
		body.setLayout(new GridLayout());
		Composite cTypes=tk.createComposite(body,SWT.BORDER);
		for(int i=0;i<types.length;i++){
			bTypes[i]=tk.createButton(cTypes,typLabels[i],SWT.CHECK);
			bTypes[i].addSelectionListener(tba);
			bTypes[i].setData(types[i]);
		}
		cTypes.setLayoutData(SWTHelper.getFillGridData(1,true,1,false));
		cTypes.setLayout(new FillLayout());
		
		Composite bottom=tk.createComposite(body);
		bottom.setLayout(new FillLayout());
		bottom.setLayoutData(SWTHelper.getFillGridData(1,true,1,true));
		actKontakt=(Kontakt)GlobalEvents.getInstance().getSelectedObject(Kontakt.class);
		afDetails=new AutoForm(bottom,def);
		Composite cAnschrift=tk.createComposite(body);
		cAnschrift.setLayout(new GridLayout(2, false));
		cAnschrift.setLayoutData(SWTHelper.getFillGridData(1,true,1,false));
		Hyperlink hAnschrift=tk.createHyperlink(cAnschrift,Messages.getString("KontaktBlatt.Postal"),SWT.NONE); //$NON-NLS-1$
		hAnschrift.addHyperlinkListener(new HyperlinkAdapter(){

			@Override
			public void linkActivated(HyperlinkEvent e) {
				new AnschriftEingabeDialog(getShell(),actKontakt).open();
				GlobalEvents.getInstance().fireSelectionEvent(actKontakt);
			}
			
		});
		lbAnschrift=tk.createLabel(cAnschrift,StringConstants.EMPTY,SWT.WRAP);
		lbAnschrift.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		//GlobalEvents.getInstance().addSelectionListener(this);
		GlobalEvents.getInstance().addActivationListener(this, site.getPart());
	}
	
	public void selectionEvent(PersistentObject obj) {
		if(obj instanceof Kontakt){
			if(!isEnabled()){
				setEnabled(true);
			}
			actKontakt=(Kontakt)obj;
			afDetails.reload(actKontakt);
			String[] ret=new String[types.length];
			actKontakt.get(types,ret);
			for(int i=0;i<types.length;i++){
				bTypes[i].setSelection((ret[i]==null) ? false : StringTool.one.equals( ret[i] ));
				if(Hub.acl.request(AccessControlDefaults.KONTAKT_MODIFY)==false){
					bTypes[i].setEnabled(false);
				}
			}
			if(bTypes[0].getSelection()==true){
				def[0].setLabel(BEZEICHNUNG);
				def[1].setLabel(ZUSATZ);
				def[2].setLabel(ANSPRECHPERSON);
				def[3].setEditable(false);
				def[3].setText(StringConstants.EMPTY);
				def[10].setLabel(TEL_DIREKT);
			}else{
				def[0].setLabel(NAME);
				def[1].setLabel(VORNAME);
				def[2].setLabel(ZUSATZ);
				def[3].setEditable(true);
				def[10].setLabel(MOBIL);
			}
			lbAnschrift.setText(actKontakt.getPostAnschrift(false));
			form.reflow(true);
		}
		
	}
	@Override
	public void dispose(){
		GlobalEvents.getInstance().removeActivationListener(this, site.getPart());
		super.dispose();
	}
	private final class TypButtonAdapter extends SelectionAdapter {
		ArrayList<String> alTypes=new ArrayList<String>();
		ArrayList<String> alValues=new ArrayList<String>();
		@Override
		public void widgetSelected(SelectionEvent e) {
			Button b=(Button)e.getSource();
			String type=(String)b.getData();
			
			if(b.getSelection()==true){
				if(type.equals("istOrganisation")){ //$NON-NLS-1$
					select("1","x","0","0","0","0"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
					def[0].setLabel(BEZEICHNUNG);
					def[1].setLabel(ZUSATZ);
					def[2].setLabel(ANSPRECHPERSON);
					def[3].setText(""); //$NON-NLS-1$
					def[10].setLabel(TEL_DIREKT);
				}else if(type.equals("istLabor")){ //$NON-NLS-1$
					select("1","1","0","0","0","0"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
					def[0].setLabel(BEZEICHNUNG);
					def[1].setLabel(ZUSATZ);
					def[2].setLabel(Messages.getString("KontaktBlatt.LabAdmin")); //$NON-NLS-1$
					def[10].setLabel(TEL_DIREKT);
				}else{
					def[0].setLabel(NAME);
					def[1].setLabel(VORNAME);
					def[2].setLabel(ZUSATZ);
					def[10].setLabel(MOBIL);
					if("istPerson".equals( type )){ //$NON-NLS-1$
						select("0","0","1","x","x","x"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
					}else if(type.equals("istPatient")){ //$NON-NLS-1$
						select("0","0","1","1","x","x"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
					}else if(type.equals("istAnwender")){ //$NON-NLS-1$
						select("0","0","1","x","1","x"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
					}else if(type.equals("istMandant")){ //$NON-NLS-1$
						select("0","0","1","x","1","1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
					}
				}
			}else{
				actKontakt.set(type,"0"); //$NON-NLS-1$
			}
		}
		void select(String... fields){
			alTypes.clear();
			alValues.clear();
			for(int i=0;i<fields.length;i++){
				if(fields[i].equals("x")){ //$NON-NLS-1$
					continue;
				}
				alTypes.add(types[i]);
				alValues.add(fields[i]);
				bTypes[i].setSelection(fields[i].equals("1")); //$NON-NLS-1$
			}
			actKontakt.set(alTypes.toArray(new String[0]),alValues.toArray(new String[0]));
		}
	}
	public void activation(boolean mode) {
		if(GlobalEvents.getInstance().getSelectedObject(Kontakt.class)==null){
			setEnabled(false);
		}else{
			setEnabled(true);
		}
		
	}

	public void visible(boolean mode) {
		if(mode==true){
			selectionEvent(GlobalEvents.getInstance().getSelectedObject(Kontakt.class));
			GlobalEvents.getInstance().addSelectionListener(this);
		}else{
			GlobalEvents.getInstance().removeSelectionListener(this);
		}
		
	}

	public void clearEvent(Class<? extends PersistentObject> template) {
		setEnabled(false);
		
	}

}
