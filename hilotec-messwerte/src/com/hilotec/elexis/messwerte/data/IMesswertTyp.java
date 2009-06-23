/*******************************************************************************
 * Copyright (c) 2007-2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    A. Kaufmann - initial implementation 
 *    
 * $Id$
 *******************************************************************************/

package com.hilotec.elexis.messwerte.data;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;

public interface IMesswertTyp {
	/**
	 * @return Feldname des Messwertes (interne Verwendung zum Referenzieren
	 *         von Feldern)
	 */
	public abstract String getName();

	/**
	 * @return Beschriftung des Messwertes (zum Anzeigen fuer den User)
	 */
	public abstract String getTitle();

	/**
	 * @return Einheit, die zum Messwert angezeigt werden soll, kann auch leer
	 *         sein.
	 */
	public abstract String getUnit();

	/**
	 * @return Standardwert des Messwerts, wenn er neu angelegt wird
	 */
	public abstract String getDefault();
	
	/**
	 * Standardwert aendern
	 * 
	 * @param def Neuer Standardwert
	 */
	public abstract void setDefault(String def);

	/**
	 * Widget fuer die Darstellung des Messwertes im Editieren-Dialog erstellen
	 * und auch glech mit aktuellem Wert befuellen.
	 * @see saveInput
	 *
	 * @param parent   Eltern-Element in dem das Widget escheinen soll
	 * @param messwert Messwert, der dargestellt werden soll
	 * 
	 * @return Widget
	 */
	public abstract Widget createWidget(Composite parent, Messwert messwert);

	/**
	 * Eingaben, die im Uebergebenen Widget getaetigt wurden, in den
	 * angegebenen Messwert einfuellen. Das uebegebene Widget wurde vorher mit
	 * createWidget() erstellt
	 * @see createWidget
	 * 
	 * @param widget
	 * @param messwert
	 */
	public abstract void saveInput(Widget widget, Messwert messwert);

	/**
	 * Von einem Messwert eine fuer den Benutzer lesbare Form generieren
	 * 
	 * @param messwert Darzustellender Messwert
	 * 
	 * @return String wie er dem Benutzer praesentiert werden kann
	 */
	public abstract String erstelleDarstellungswert(Messwert messwert);
}