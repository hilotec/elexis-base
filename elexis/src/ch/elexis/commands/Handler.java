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
 *  $Id: Handler.java 4430 2008-09-22 17:22:09Z rgw_ch $
 *******************************************************************************/
package ch.elexis.commands;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.IParameterValues;
import org.eclipse.core.commands.ParameterValuesException;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

import ch.rgw.tools.StringTool;

/**
 * Simplified Handler for execution of Commands
 * 
 * @author gerry
 * 
 */
public class Handler {
	private static HashMap<String, Object> paramMap = new HashMap<String, Object>();
	public static final String DEFAULTPARAM="ch.elexis.commands.defaultParameter";
	
	
	/**
	 * Execute a Command with a single Object as Parameter and a ProgressMonitor
	 * @param origin The ViewSite the Command was originated
	 * @param commandID the ID of the command as defined in plugin.xml
	 * @param param the arbitrary parameter
	 * @return the return value of the Command's execute method
	 */
	public static Object executeWithProgress(IViewSite origin, String commandID, Object param, IProgressMonitor monitor){
		HashMap<String,Object> hp=new HashMap<String,Object>();
		hp.put("param", param);
		hp.put("monitor", monitor);
		return execute(origin,commandID,hp);
	}
	/**
	 * Execute a Command with a single Object as Parameter
	 * @param origin The ViewSite the Command was originated
	 * @param commandID the ID of the command as defined in plugin.xml
	 * @param param the arbitrary parameter
	 * @return the return value of the Command's execute method
	 */
	public static Object execute(IViewSite origin, String commandID, Object param){
		HashMap<String,Object> hp=new HashMap<String,Object>();
		if(param!=null){
			hp.put("param", param);
		}
		return execute(origin,commandID,hp);
	}
	
	/**
	 * Return the given single Parameter. This is to be called from within
	 * the execute method of the command
	 * @param eev the ExecutionEnvironment of the command
	 * @return the Object as given from the caller to execute(...) or null if 
	 * no such object was given
	 */
	@SuppressWarnings("unchecked")
	public static Object getParam(ExecutionEvent eev){
		Map<String,String> params=eev.getParameters();
		String np=params.get(Handler.DEFAULTPARAM);
		if(np!=null){
			HashMap<String, Object> map=(HashMap<String, Object>) getParam(np);
			if(map!=null){
				return map.get("param");
			}
		}
		return null;
	}
	
	/**
	 * get the ProgressMonitor supplied by the caller
	 * @param eev the ExecutionEnvironment of the Command
	 * @return the monitor if any or null if none was given.
	 */
	@SuppressWarnings("unchecked")
	public static IProgressMonitor getMonitor(ExecutionEvent eev){
		Map<String,String> params=eev.getParameters();
		String np=params.get(Handler.DEFAULTPARAM);
		HashMap<String, Object> map=(HashMap<String, Object>) getParam(np);
		if(map!=null){
			return (IProgressMonitor)map.get("monitor");
		}
		return null;
	}
	/**
	 * Return a named parameter
	 * @param paramName
	 * @return
	 */
	public static Object getParam(String paramName){
		Object ret=paramMap.get(paramName);
		if(ret!=null){
			paramMap.remove(paramName);
		}
		return ret;
	}
	
	private static Object execute(IViewSite origin, String commandID, Map<String, Object> params){
		if (origin == null) {
			// Hub.plugin.getWorkbench().getWorkbenchWindows();
		}
		IHandlerService handlerService = (IHandlerService) origin.getService(IHandlerService.class);
		ICommandService cmdService = (ICommandService) origin.getService(ICommandService.class);
		try {
			Command command = cmdService.getCommand(commandID);
			String name = StringTool.unique("CommandHandler");
			paramMap.put(name, params);
			Parameterization px = new Parameterization(new DefaultParameter(), name);
			ParameterizedCommand parmCommand =
				new ParameterizedCommand(command, new Parameterization[] {
					px
				});
			
			return handlerService.executeCommand(parmCommand, null);
			
		} catch (Exception ex) {
			throw new RuntimeException("add.command not found");
		}
	}
	
	static class DefaultParameter implements IParameter {
		
		public String getId(){
			return DEFAULTPARAM;
		}
		
		public String getName(){
			return "param";
		}
		
		public IParameterValues getValues() throws ParameterValuesException{
			return new IParameterValues() {
				
				public Map getParameterValues(){
					return new HashMap<String, String>();
				}
			};
			
		}
		
		public boolean isOptional(){
			return true;
		}
		
	}

	
}
