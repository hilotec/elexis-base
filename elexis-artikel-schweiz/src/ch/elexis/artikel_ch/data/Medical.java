/*******************************************************************************
 * Copyright (c) 2006-2007, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: Medical.java 5932 2010-01-14 22:30:04Z rgw_ch $
 *******************************************************************************/
package ch.elexis.artikel_ch.data;

import ch.elexis.data.Artikel;

public class Medical extends Artikel{
	@Override
	protected String getConstraint() {
		return "Typ='Medical'"; //$NON-NLS-1$
	}
	protected void setConstraint(){
		set("Typ","Medical"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	@Override
	public String getCodeSystemName() {
			return "Medicals"; //$NON-NLS-1$
	}
	@Override
	public String getCode() {
		return getPharmaCode();
	}
	public static Medical load(String id){
		return new Medical(id);
	}
	protected Medical(String id){
		super (id);
	}
	protected Medical(){}
	@Override
	public boolean isDragOK() {
		return true;
	}
	
	public String getLabel(){
		return get("Name"); //$NON-NLS-1$
	}

}