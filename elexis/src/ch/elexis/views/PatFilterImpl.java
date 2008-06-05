/*******************************************************************************
 * Copyright (c) 2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: PatFilterImpl.java 4006 2008-06-05 16:17:52Z rgw_ch $
 *******************************************************************************/

package ch.elexis.views;

import java.util.List;

import ch.elexis.data.Artikel;
import ch.elexis.data.Etikette;
import ch.elexis.data.Fall;
import ch.elexis.data.IDiagnose;
import ch.elexis.data.IVerrechenbar;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Prescription;
import ch.elexis.data.Query;
import ch.elexis.data.Script;
import ch.elexis.data.Verrechnet;
import ch.elexis.views.PatListFilterBox.IPatFilter;

/**
 * Default implementation of IPatFilter. Will be called after all other filters
 * returned DONT_HANDLE
 * @author Gerry
 
 */
public class PatFilterImpl implements IPatFilter {

	public int accept(Patient p, PersistentObject o){
		if(o instanceof Kontakt){
			// 
		}else if(o instanceof IVerrechenbar){
			IVerrechenbar iv=(IVerrechenbar)o;
			Fall[] faelle=p.getFaelle();
			for(Fall fall:faelle){
				Konsultation[] konsen=fall.getBehandlungen(false);
				for(Konsultation k:konsen){
					List<Verrechnet> lv=k.getLeistungen();
					for(Verrechnet v:lv){
						if(v.getVerrechenbar().equals(iv)){
							return ACCEPT;
						}
					}
				}
			}
			return REJECT;

		}else if(o instanceof IDiagnose){
			IDiagnose diag=(IDiagnose)o;
			Fall[] faelle=p.getFaelle();
			for(Fall fall:faelle){
				Konsultation[] konsen=fall.getBehandlungen(false);
				for(Konsultation k:konsen){
					List<IDiagnose> id=k.getDiagnosen();
					if(id.contains(diag)){
						return ACCEPT;
					}
				}
			}
			return REJECT;
		}else if(o instanceof Artikel){
			Query<Prescription> qbe=new Query<Prescription>(Prescription.class);
			qbe.add("PatientID", "=", p.getId());
			qbe.add("ArtikelID", "=",o.getId());
			if(qbe.execute().size()>0){
				return ACCEPT;
			}
			return REJECT;
		}else if(o instanceof Prescription){
			Artikel art=((Prescription)o).getArtikel();
			Query<Prescription> qbe=new Query<Prescription>(Prescription.class);
			qbe.add("PatientID", "=", p.getId());
			qbe.add("ArtikelID", "=", art.getId());
			if(qbe.execute().size()>0){
				return ACCEPT;
			}
			return REJECT;
		}else if(o instanceof Etikette){
			List<Etikette> etis=p.getEtiketten();
			Etikette e=(Etikette)o;
			if(etis.contains(e)){
				return ACCEPT;
			}
			return REJECT;
		}else if(o instanceof Script){
			Object ret;
			try {
				Script script=(Script)o;
				ret = script.execute(p);
				if(ret instanceof Integer){
					return (Integer)ret;
				}

			} catch (Exception e) {
				return FILTER_FAULT;
			}
		}
		return DONT_HANDLE;
	}

	
}
