/*******************************************************************************
 * Copyright (c) 2006, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: ReplaceCallback.java 23 2006-03-24 15:36:01Z rgw_ch $
 *******************************************************************************/


package ch.elexis.text;

public interface ReplaceCallback {
	public String replace(String in);
}
