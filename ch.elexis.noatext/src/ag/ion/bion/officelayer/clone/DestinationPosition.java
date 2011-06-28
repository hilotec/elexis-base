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
 * Last changes made by $Author: andreas $, $Date: 2006-10-04 14:14:28 +0200 (Mi, 04 Okt 2006) $
 */
package ag.ion.bion.officelayer.clone;

/**
 * Concrete implementation of the IDestinationPosition. With this class
 * its possible to give a newly cloned object a target destiantion, so
 * that it "knows" wherer to be placed.
 * 
 * @author Sebastian Rüsgen
 * 
 * @version $Revision: 10398 $
 */
public class DestinationPosition implements IDestinationPosition {
	
	private Object storeObject = null;
	private Class clazz = null;

  //----------------------------------------------------------------------------
	/**
	 * Constructor of the DestinationPosition.
	 * 
	 * @param storeObject the object to be stored in this class
	 * 
	 * @author Sebastian Rüsgen
	 */
	public DestinationPosition (Object storeObject) {
    this.storeObject = storeObject;
    if (storeObject == null) 
      this.clazz = null;
    else
      this.clazz = storeObject.getClass();
	}
  //----------------------------------------------------------------------------
	/**
	 * Returns the class of the stored object.
	 * 
	 * @return the class type of the stored object.
	 * 
	 * @author Sebastian Rüsgen
	 */
	public Class getType() {
		return clazz;
	}
  //----------------------------------------------------------------------------
	/**
	 * Returns the stored object.
	 * 
	 * @return the object stored.
	 * 
	 * @author Sebastian Rüsgen
	 */
	public Object getDestinationObject() {
		return storeObject;
	}
  //----------------------------------------------------------------------------
}