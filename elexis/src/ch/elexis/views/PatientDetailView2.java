/*******************************************************************************
 * Copyright (c) 2005, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: PatientDetailView.java 590 2006-07-24 15:40:40Z rgw_ch $
 *******************************************************************************/

package ch.elexis.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.actions.GlobalEvents;
import ch.elexis.actions.GlobalActions;
import ch.elexis.data.Patient;

public class PatientDetailView2 extends ViewPart implements ISaveablePart2{
    public static final String ID="ch.elexis.PatDetail_v2";
    Patientenblatt2 pb;

    @Override
    public void createPartControl(Composite parent)
    {
        parent.setLayout(new FillLayout());
        pb=new Patientenblatt2(parent,getViewSite());

    }

    public void refresh(){
        pb.setPatient((Patient)GlobalEvents.getInstance().getSelectedObject(Patient.class));
        pb.refresh();
    }
    @Override
    public void setFocus()
    {
        // TODO Auto-generated method stub

    }
    /* ******
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2
	 * Wir benötigen das Interface nur, um das Schliessen einer View zu verhindern,
	 * wenn die Perspektive fixiert ist.
	 * Gibt es da keine einfachere Methode?
	 */ 
	public int promptToSaveOnClose() {
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL : ISaveablePart2.NO;
	}
	public void doSave(IProgressMonitor monitor) { /* leer */ }
	public void doSaveAs() { /* leer */}
	public boolean isDirty() {
		return true;
	}
	public boolean isSaveAsAllowed() {
		return false;
	}
	public boolean isSaveOnCloseNeeded() {
		return true;
	}
}
