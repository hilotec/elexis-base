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
 * $Id: IKonsExtension.java 6194 2010-03-14 12:13:27Z rgw_ch $
 *******************************************************************************/

package ch.elexis.util;

import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.custom.StyleRange;

import ch.elexis.text.EnhancedTextField;
import ch.elexis.text.IRichTextDisplay;

/**
 * Erweiterung für Konsultationseinträge. Wird vom Extensionpoint KonsExtensions benötigt Eine
 * KonsExtension kann Textstellen umformatieren, kann Hyperlinks einfügen und kann Einträge für das
 * Popup-Menu der Konsultationsanzeige definieren. Die IKonsExtension wird zunächst beim Analysieren
 * der KonsExtension mit connect() initialisiert. Dann wird sie beim Rendern des Texts im
 * KonsDetailView für jedes von ihr deklarierte xref-tag einmal aufgerufen (doLayout). Sie kann da
 * "true" zurückgeben um anzuzeigen, dass sie auf Mausklicks reagieren will, oder false, wenn es nur
 * um Layout ohne Klickaktivität geht. Falls sie auf doLayout "true" zurückgegeben hat, wird sie
 * immer dann via doXref aufgerufe, wenn der Benutzer den von ihr gesetzten Link anklickt.
 * Schliesslich wird die IKonsExtension immer dann aufgerufen, wenn der Anwender das Kontext- menu
 * des Textfelds anzeigen will (rechte Maustaste). Wenn getAction eine IAction zurückliefert, dann
 * wird diese ins Kontextmenu eingebunden. Wenn getAction null zurückliefert, erfolgt keine
 * Veränderung des Kontxtmenüs. Referenzimplementation: ch.elexis.privatnotizen
 * 
 * @author gerry
 * 
 */
public interface IKonsExtension extends IExecutableExtension {
	
	/**
	 * diese KonsExtension mit einem EnhancedTextField verknüpfen
	 * 
	 * @param tf
	 *            das TextField, an das diese Extension gebunden wird
	 * @return einen Namen, der diese Extension eindeutig identifiziert
	 */
	public String connect(IRichTextDisplay tf);
	
	/**
	 * Einen Querverweis für die Darstellung layouten
	 * 
	 * @param n
	 *            eine StyleRange zum beliebig bearbeiten
	 * @param provider
	 *            den Provider-String, den diese IKonsExtension dem Extension-Point angegeben hat
	 * @param id
	 *            die ID, die die IKonsExtension dieser Textstelle zugewiesen hat
	 * @return true wenn der Text auch als Hyperlink funktionieren soll
	 */
	public boolean doLayout(StyleRange n, String provider, String id);
	
	/**
	 * Aktion für einen Querverweis auslösen (wurde angeklickt)
	 * 
	 * @param refProvider
	 *            Provider-String
	 * @param refID
	 *            ID für die angeklickte Textstelle
	 * @return false wenn bei der Aktion ein Fehler auftrat
	 */
	public boolean doXRef(String refProvider, String refID);
	
	/**
	 * Transportable Repräsentation des eingebetteten Inhalts liefern
	 * 
	 * @param refProvider
	 *            Provider-String
	 * @param refID
	 *            ID für das betreffende Object
	 * @return ein MimePart mit dem Objekt, oder null, wenn die Extension keine Tranpsortform hat
	 */
	// public MimePart doRender(String refProvider, String refID);
	
	// public boolean doImport(MimePart object, int pos, String title);
	/**
	 * Actions für diese Extension holen. z.B. für Kontextmenu
	 */
	public IAction[] getActions();
	
	/**
	 * Ein Object wurde eingefügt, z.B. mit drag&drop
	 * 
	 * @param o
	 *            eingefügtes Object
	 */
	public void insert(Object o, int pos);
	
	/**
	 * Anwender hat eine XRef gelöscht -> ggf. damit verbundene Daten müssen jetzt entfernt werden
	 */
	public void removeXRef(String refProvider, String refID);
}
