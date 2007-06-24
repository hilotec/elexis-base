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

import ag.ion.bion.officelayer.text.table.IFormulaService;
import ag.ion.bion.officelayer.text.table.ITextTableCellPropertyStore;

/**
 * Cell of a table in a text document.
 * 
 * @author Andreas Br�ker
 * @author Markus Kr�ger
 * @version $Revision: 1.1 $
 */
public interface ITextTableCell extends ITextComponent, ICloneServiceProvider {
  
  /** The cell is empty. */
  public static final int TYPE_EMPTY    = 99;
  /** The cell contains a value. */
  public static final int TYPE_VALUE    = 100;
  /** The cell conains text. */
  public static final int TYPE_TEXT     = 101;
  /** The cell contains a formula. */
  public static final int TYPE_FORMULA  = 102;  
  
  //----------------------------------------------------------------------------
  /**
   * Returns name of the cell.
   * 
   * @return name of the cell
   * 
   * @author Andreas B�ker
   */
  public ITextTableCellName getName();  
  //----------------------------------------------------------------------------
  /**
   * Returns text table of the cell.
   * 
   * @return text table of the cell
   * 
   * @throws TextException if the text table is not available
   * 
   * @author Andreas Br�ker
   */
  public ITextTable getTextTable() throws TextException;  
  //----------------------------------------------------------------------------
  /**
   * Returns content type.
   * 
   * @return content type
   * 
   * @author Andreas Br�ker
   */
  public int getContentType();  
  //----------------------------------------------------------------------------
  /**
   * Returns text service.
   * 
   * @return text service
   * 
   * @author Andreas Br�ker
   */
  public ITextService getTextService();
  //----------------------------------------------------------------------------
  /**
   * Returns text table cell properties.
   * 
   * @return text table cell properties
   * 
   * @author Andreas Br�ker
   */
  public ITextTableCellProperties getProperties();
  //----------------------------------------------------------------------------
  /**
   * Returns text table cell character properties.
   * 
   * @return text table cell character properties
   * 
   * @author Markus Kr�ger
   */
  public ICharacterProperties getCharacterProperties();
  //----------------------------------------------------------------------------
  /**
   * Returns formula service.
   * 
   * @return formula service
   * 
   * @author Miriam Sutter
   */
  public IFormulaService getFormulaService();
  //----------------------------------------------------------------------------
  /**
   * Sets the formula.
   * 
   * @param formula formula
   * 
   * @author Miriam Sutter
   */
  public void setFormula(String formula);
  //----------------------------------------------------------------------------
  /**
   * Returns related page style of the text table cell.
   * 
   * @return related page style of the text table cell
   * 
   * @throws TextException if the page style is not available
   * 
   * @author Andreas Br�ker
   */
  public IPageStyle getPageStyle() throws TextException;  
  //----------------------------------------------------------------------------
  /**
   * Returns the property store of this cell.
   * 
   * @param verticalPosition vertical position to be used
   * @param horizontalPosition horizontal position to be used
   * 
   * @return property store of the cell
   * 
   * @throws TextException if the cell is not available
   * 
   * @author Sebastian R�sgen
   */
  public ITextTableCellPropertyStore getCellPropertyStore (int verticalPosition, int horizontalPosition) throws TextException;
  //----------------------------------------------------------------------------
  /**
   * Returns the character property store of this cell.
   * 
   * @return character property store of the cell
   * 
   * @throws TextException if property store could not be returned
   * 
   * @author Markus Kr�ger
   */
  public ICharacterPropertyStore getCharacterPropertyStore() throws TextException;
  //----------------------------------------------------------------------------
  /**
   * Gets the value of the cell.
   * 
   * @return the value of the cell
   * 
   * @author Sebastian R�sgen
   */
  public double getValue();
  //----------------------------------------------------------------------------  
  /**
   * Sets the value of the cell.
   * 
   * @param value the value to be set in the table
   * 
   * @author SebastianR�sgen
   */
  public void setValue(double value);
  //----------------------------------------------------------------------------
  /**
   * Returns the page number of the cell, returns -1 if page number
   * could not be determined.
   * 
   * @return the page number of the cell, returns -1 if page number
   * could not be determined
   * 
   * @author Markus Kr�ger
   */
  public short getPageNumber();  
  //----------------------------------------------------------------------------

}