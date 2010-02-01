/*******************************************************************************
 * Copyright (c) 2006-2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: Eigenartikel.java 6044 2010-02-01 15:18:50Z rgw_ch $
 *******************************************************************************/

package ch.elexis.data;

import ch.rgw.tools.JdbcLink;

public class Eigenartikel extends Artikel {

	public static final String TYPNAME = "Eigenartikel";

	@Override
	protected String getConstraint() {
		return new StringBuilder(Artikel.FLD_TYP).append(Query.EQUALS).append(
				JdbcLink.wrap(TYPNAME)).toString();
	}

	protected void setConstraint() {
		set(Artikel.FLD_TYP, TYPNAME);
	}

	@Override
	public String getCodeSystemName() {
		return TYPNAME;
	}

	@Override
	public String getLabel() {
		return get(Artikel.FLD_NAME);
	}

	@Override
	public String getCode() {
		return get(Artikel.FLD_SUB_ID);
	}

	public String getGroup() {
		return checkNull(get(Artikel.FLD_CODECLASS));
	}

	public static Eigenartikel load(String id) {
		return new Eigenartikel(id);
	}

	protected Eigenartikel() {
	}

	protected Eigenartikel(String id) {
		super(id);
	}

	@Override
	public boolean isDragOK() {
		return true;
	}
}
