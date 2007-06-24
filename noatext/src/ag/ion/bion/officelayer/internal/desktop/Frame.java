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
 * Last changes made by $Author: andreas $, $Date: 2006/10/04 12:14:27 $
 */
package ag.ion.bion.officelayer.internal.desktop;

import ag.ion.bion.officelayer.application.connection.IOfficeConnection;
import ag.ion.bion.officelayer.desktop.GlobalCommands;
import ag.ion.bion.officelayer.desktop.IFrame;
import ag.ion.bion.officelayer.util.Assert;

import ag.ion.noa.NOAException;

import ag.ion.noa.frame.IDispatch;
import ag.ion.noa.frame.IDispatchDelegate;
import ag.ion.noa.frame.ILayoutManager;

import ag.ion.noa.internal.frame.Dispatch;
import ag.ion.noa.internal.frame.DispatchWrapper;
import ag.ion.noa.internal.frame.LayoutManager;

import com.sun.star.beans.XPropertySet;

import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.FrameSearchFlag;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatchProviderInterception;
import com.sun.star.frame.XDispatchProviderInterceptor;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XInterceptorInfo;
import com.sun.star.frame.XLayoutManager;

import com.sun.star.uno.UnoRuntime;

import com.sun.star.util.URL;
import com.sun.star.util.XURLTransformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * OpenOffice.org frame.
 * 
 * @author Andreas Br�ker
 * @author Markus Kr�ger
 * @version $Revision: 1.1 $
 */
public class Frame implements IFrame {

  private XFrame 						xFrame 						= null;
  private IOfficeConnection officeConnection 	= null;
  private DispatchProviderInterceptor dispatchProviderInterceptor = null; 
  
  private Map delegatesMap = null;
  
  private TreeSet disabledCommandURLs = null; 
  
  //----------------------------------------------------------------------------
  /**
   * Internal dispatch provider interceptor.
   * 
   * @author Andreas Br�ker
   * @version $Revision: 1.1 $
   * @date 15.08.2006
   */
  private class DispatchProviderInterceptor implements XDispatchProviderInterceptor, XInterceptorInfo {

  	private XDispatchProvider slaveDispatchProvider = null;
  	private XDispatchProvider masterDispatchProvider = null;
  	
    //----------------------------------------------------------------------------
    /**
     * Returns slave dispatch provider.
     * 
     * @return slave dispatch provider
     * 
     * @author Andreas Br�ker
     * @date 15.08.2006
     */
		public XDispatchProvider getSlaveDispatchProvider() {
			return slaveDispatchProvider;
		}
    //----------------------------------------------------------------------------
    /**
     * Sets slave dispatch provider.
     * 
     * @param slaveDispatchProvider slave dispatch provider to be set
     * 
     * @author Andreas Br�ker
     * @date 15.08.2006
     */
		public void setSlaveDispatchProvider(XDispatchProvider slaveDispatchProvider) {
			this.slaveDispatchProvider = slaveDispatchProvider;
		}
    //----------------------------------------------------------------------------
    /**
     * Returns master dispatch provider.
     * 
     * @return master dispatch provider
     * 
     * @author Andreas Br�ker
     * @date 15.08.2006
     */
		public XDispatchProvider getMasterDispatchProvider() {
			return masterDispatchProvider;
		}
    //----------------------------------------------------------------------------
    /**
     * Sets master dispatch provider.
     * 
     * @param masterDispatchProvider master dispatch provider to be set
     * 
     * @author Andreas Br�ker
     * @date 15.08.2006
     */
		public void setMasterDispatchProvider(XDispatchProvider masterDispatchProvider) {
			this.masterDispatchProvider = masterDispatchProvider;
		}
    //----------------------------------------------------------------------------
    /**
     * Queries for a dispatch. Returns null if the dispatch
     * can not be provided.
     * 
     * @param targetFrameName name of the target frame
     * @param searchFlags search flags to be used
     * 
     * @return dispatch or null if the dispatch
     * can not be provided
     * 
     * @author Andreas Br�ker
     * @date 15.08.2006
     */
		public XDispatch queryDispatch(URL url, String targetFrameName, int searchFlags) {
			String commandURL = url.Complete;
			if(isDispatchDisabled(commandURL))
				return null;
						
			IDispatchDelegate dispatchDelegate = null;
			XDispatch xDispatch = null;
			if(delegatesMap != null) {
				dispatchDelegate = (IDispatchDelegate)delegatesMap.get(commandURL);
			}
			if(slaveDispatchProvider != null) {
				xDispatch = slaveDispatchProvider.queryDispatch(url, targetFrameName, searchFlags);
			}
			if(dispatchDelegate != null)
				return new DispatchWrapper(dispatchDelegate, xDispatch);
			else
				return xDispatch;
		}
    //----------------------------------------------------------------------------
    /**
     * Returns dispatches for the submitted dispatch descriptors.
     * 
     * @return dispatches for the submitted dispatch descriptors
     * 
     * @author Andreas Br�ker
     * @date 15.08.2006
     */
		public XDispatch[] queryDispatches(DispatchDescriptor[] dispatchDescriptors) {
		  List list = new ArrayList();
      for(int i=0, n=dispatchDescriptors.length; i<n; i++) {
        DispatchDescriptor dispatchDescriptor = dispatchDescriptors[i];
        XDispatch dispatch = queryDispatch(dispatchDescriptor.FeatureURL, dispatchDescriptor.FrameName, dispatchDescriptor.SearchFlags);
        if(dispatch != null)
          list.add(dispatch);
      }
      return (XDispatch[])list.toArray(new XDispatch[list.size()]);
		}
    //----------------------------------------------------------------------------
    /**
     * Returns intercepted dispatch URLs.
     * 
     * @return intercepted dispatch URLs
     * 
     * @author Andreas Br�ker
     * @date 15.08.2006
     */
		public String[] getInterceptedURLs() {
			if(delegatesMap == null && disabledCommandURLs == null)
				return new String[0];
			
			String[] commandURLs = null;
			if(disabledCommandURLs != null)
				commandURLs = (String[])disabledCommandURLs.toArray(new String[disabledCommandURLs.size()]);
			if(delegatesMap != null) {
				if(commandURLs == null)
					commandURLs = (String[])delegatesMap.keySet().toArray(new String[delegatesMap.size()]);
				else {
					String[] temp = new String[commandURLs.length + delegatesMap.size()];
					System.arraycopy(commandURLs, 0, temp, 0, commandURLs.length);
					String[] newURLs = (String[])delegatesMap.keySet().toArray(new String[delegatesMap.size()]);
					System.arraycopy(newURLs, 0, temp, commandURLs.length, newURLs.length);
					commandURLs = temp;
				}
			}
			return commandURLs;
		}
    //----------------------------------------------------------------------------
  	
  }
  //----------------------------------------------------------------------------
  
  //----------------------------------------------------------------------------
  /**
   * Constructs new Frame.
   * 
   * @param xFrame OpenOffice.org XFrame interface to be used
   * @param officeConnection office connection to be used
   * 
   * @author Andreas Br�ker
   */
  public Frame(XFrame xFrame, IOfficeConnection officeConnection) {
    Assert.isNotNull(xFrame, XFrame.class, this);
    Assert.isNotNull(officeConnection, IOfficeConnection.class, this);
    
    this.xFrame = xFrame;
    this.officeConnection = officeConnection;
  }
  //----------------------------------------------------------------------------
  /**
   * Returns OpenOffice.org XFrame interface. This method 
   * is not part of the public API.
   * 
   * @return OpenOffice.org XFrame interface
   * 
   * @author Andreas Br�ker
   */
  public XFrame getXFrame() {
    return xFrame;
  }
  //----------------------------------------------------------------------------
  /**
   * Returns layout manager of the frame. Returns null if a layout manager
   * is not available.
   * 
   * @return layout manager of the frame or null if a layout manager
   * is not available
   * 
   * @throws NOAException if the layout manager can not be requested
   * 
   * @author Andreas Br�ker
   * @date 2006/02/05
   */
  public ILayoutManager getLayoutManager() throws NOAException {
  	try {
  		XPropertySet propertySet = (XPropertySet)UnoRuntime.queryInterface(XPropertySet.class, xFrame);
  		Object object = propertySet.getPropertyValue("LayoutManager");
  		XLayoutManager layoutManager = (XLayoutManager)UnoRuntime.queryInterface(XLayoutManager.class, object);
  		if(layoutManager != null)
  			return new LayoutManager(layoutManager);
  	}
  	catch(Throwable throwable) {
  		throw new NOAException(throwable);
  	}
  	return null;
  }  
  //----------------------------------------------------------------------------
  /**
   * Disables the command dispatch related to the submitted command URL.
   * In order to activate the dispatches you need to call {@link IFrame#updateDispatches()}.
   * 
   * @param commandURL URL of the command
   * 
   * @see GlobalCommands
   * 
   * @author Andreas Br�ker
   * @author Markus Kr�ger
   * @date 07.07.2006
   */
  public void disableDispatch(String commandURL) {
  	if(commandURL == null)
  		return;
  	
  	if(disabledCommandURLs == null)
  		disabledCommandURLs = new TreeSet();
  	disabledCommandURLs.add(commandURL);
  }  
  //----------------------------------------------------------------------------
  /**
   * Adds a dispatch delegate for the submitted command URL.
   * In order to activate the dispatches you need to call {@link IFrame#updateDispatches()}.
   * 
   * @param commandURL command URL to be used
   * @param dispatchDelegate dispatch delegate to be added
   * 
   * @author Andreas Br�ker
   * @author Markus Kr�ger
   * @date 07.07.2006
   */
  public void addDispatchDelegate(String commandURL, IDispatchDelegate dispatchDelegate) {
  	if(commandURL == null || dispatchDelegate == null)
  		return;
  	
  	if(delegatesMap == null)
  		delegatesMap = new HashMap(5);
  	delegatesMap.put(commandURL, dispatchDelegate);
  }
  //----------------------------------------------------------------------------
  /**
   * Removes the dispatch delegate for the submitted command URL.
   * In order to activate the dispatches you need to call {@link IFrame#updateDispatches()}.
   * 
   * @param commandURL command URL to be used
   * 
   * @author Andreas Br�ker
   * @author Markus Kr�ger
   * @date 07.07.2006
   */
  public void removeDispatchDelegate(String commandURL) {
  	if(delegatesMap == null || commandURL == null)
  		return;
  	
  	delegatesMap.remove(commandURL);
  }
  //----------------------------------------------------------------------------
  /**
   * Returns dispatch for the submitted command URL.
   * 
   * @param commandURL command URL of the dispatch
   * 
   * @return dispatch for the submitted command URL
   * 
   * @throws NOAException if a dispatch for the submitted command URL
   * can not be provided
   * 
   * @author Andreas Br�ker
   * @date 14.06.2006
   */
  public IDispatch getDispatch(String commandURL) throws NOAException {
  	if(commandURL == null)
  		throw new NOAException("The command URL is not valid.");
  	try {  		
	  	XDispatchProvider xDispatchProvider = (XDispatchProvider)UnoRuntime.queryInterface(XDispatchProvider.class, xFrame);
	  	URL[] urls = new URL[1];
	  	urls[0] = new URL();
	  	urls[0].Complete = commandURL;
	  	XURLTransformer xURLTranformer = (XURLTransformer)UnoRuntime.queryInterface(XURLTransformer.class, officeConnection.createService("com.sun.star.util.URLTransformer"));
	  	xURLTranformer.parseStrict(urls);
	  	XDispatch xDispatch = xDispatchProvider.queryDispatch(urls[0], "", FrameSearchFlag.GLOBAL);	  	
	  	if(xDispatch == null)
	  		throw new NOAException("The command URL is not valid");
	  	return new Dispatch(xDispatch, urls[0]);
  	}
  	catch(Throwable throwable) {
  		throw new NOAException(throwable);
  	}
  }
  //----------------------------------------------------------------------------
  /**
   * Closes the frame.
   * 
   * @author Andreas Br�ker
   */
  public void close() {
    xFrame.dispose();
  }
  //----------------------------------------------------------------------------
  /**
   * Focuses the frame.
   * 
   * @author Markus Kr�ger
   */
  public void setFocus() {
    xFrame.getContainerWindow().setFocus();
    xFrame.activate();
  }
  //----------------------------------------------------------------------------
  /**
   * Updates the current dispatches. 
   * 
   * @author Andreas Br�ker
   * @author Markus Kr�ger
   * @date 07.07.2006
   */
  public void updateDispatches() {
    if(dispatchProviderInterceptor != null) {
      XDispatchProviderInterception xDispatchProviderInterception = (XDispatchProviderInterception)UnoRuntime.queryInterface(XDispatchProviderInterception.class, xFrame);
      if(xDispatchProviderInterception != null) {
        xDispatchProviderInterception.releaseDispatchProviderInterceptor(dispatchProviderInterceptor);
        dispatchProviderInterceptor = null;
      }
    }
      
    if(dispatchProviderInterceptor == null) {
      XDispatchProviderInterception xDispatchProviderInterception = (XDispatchProviderInterception)UnoRuntime.queryInterface(XDispatchProviderInterception.class, xFrame);
      if(xDispatchProviderInterception != null) {
        dispatchProviderInterceptor = new DispatchProviderInterceptor();
        xDispatchProviderInterception.registerDispatchProviderInterceptor(dispatchProviderInterceptor);
      }
    }    
  }  
  //----------------------------------------------------------------------------  
  /**
   * Returns information whether the submitted command URL is disabled.
   * 
   * @param commandURL command URL to be used
   * 
   * @return information whether the submitted command URL is disabled
   * 
   * @author Andreas Br�ker
   * @date 15.08.2006
   */
  private boolean isDispatchDisabled(String commandURL) {
  	if(disabledCommandURLs == null)
  		return false;
  	return disabledCommandURLs.contains(commandURL);
  }  
  //----------------------------------------------------------------------------
  
}