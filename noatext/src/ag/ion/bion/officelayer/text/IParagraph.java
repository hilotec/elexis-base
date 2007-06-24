/****************************************************************************
 * ubion.ORS - The Open Report Suite                                        *
 *                                                                          *
 * ------------------------------------------------------------------------ *
 *                                                                          *
 * Subproject: NOA (Nice Office Access)                                     *
 *                                                                          *
 *                                                                          *
 * The Contents of this file are made available subject to                  *
 * the terms of GNU Lesser General Public License Version 2.1.              *
 *                                                                          * 
 * GNU Lesser General Public License Version 2.1                            *
 * ======================================================================== *
 * Copyright 2003-2005 by IOn AG                                            *
 *                                                                          *
 * This library is free software; you can redistribute it and/or            *
 * modify it under the terms of the GNU Lesser General Public               *
 * License version 2.1, as published by the Free Software Foundation.       *
 *                                                                          *
 * This library is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU        *
 * Lesser General Public License for more details.                          *
 *                                                                          *
 * You should have received a copy of the GNU Lesser General Public         *
 * License along with this library; if not, write to the Free Software      *
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,                    *
 * MA  02111-1307  USA                                                      *
 *                                                                          *
 * Contact us:                                                              *
 *  http://www.ion.ag                                                       *
 *  info@ion.ag                                                             *
 *                                                                          *
 ****************************************************************************/
 
/*
 * Last changes made by $Author: andreas $, $Date: 2006/10/04 12:14:22 $
 */
package ag.ion.bion.officelayer.text;

import ag.ion.bion.officelayer.clone.ICloneServiceProvider;

/**
 * Paragraph of a text document.
 * 
 * @author Andreas Br�ker
 * @version $Revision: 1.1 $
 */
public interface IParagraph extends ITextContent, ICloneServiceProvider {

  //----------------------------------------------------------------------------
  /**
   * Returns properties of the paragraph.
   * 
   * @return properties of the paragraph
   * 
   * @author Andreas Br�ker
   */
  public IParagraphProperties getParagraphProperties();
  //----------------------------------------------------------------------------
  /**
   * Returns character properties belonging to the paragraph
   * 
   * @return characterproperties of the paragraph
   * 
   * @author Sebastian R�sgen
   */
  public ICharacterProperties getCharacterProperties();
  //----------------------------------------------------------------------------
  /**
   * Gets the property store of the paragraph
   * 
   * @return the paragprah property store
   * 
   * @author Sebastian R�sgen
   */
  public IParagraphPropertyStore getParagraphPropertyStore() throws TextException;
  //----------------------------------------------------------------------------
  /**
   * Gets the character property store of the paragraph
   * 
   * @return the paragprah's character property store
   * 
   * @author Sebastian R�sgen
   */
  public ICharacterPropertyStore getCharacterPropertyStore() throws TextException;
  //----------------------------------------------------------------------------
  /**
   * Gets the text contained in this pragraph
   * 
   * @return the paragraph text
   * 
   * @author Sebastian R�sgen 
   */
  public String getParagraphText() throws TextException;
  //---------------------------------------------------------------------------- 
  /**
   * Sets new text to the paragraph.
   * 
   * @param text the text that should be placed
   * 
   * @author Sebastian R�sgen
   */
  public void setParagraphText(String text);
  //----------------------------------------------------------------------------
}