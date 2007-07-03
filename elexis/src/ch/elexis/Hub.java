/*******************************************************************************
 * Copyright (c) 2005-2007, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *    $Id: Hub.java 2696 2007-07-03 12:49:04Z rgw_ch $
 *******************************************************************************/

package ch.elexis;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import ch.elexis.actions.*;
import ch.elexis.admin.AccessControl;
import ch.elexis.data.*;
import ch.elexis.preferences.PreferenceConstants;
import ch.elexis.preferences.PreferenceInitializer;
import ch.elexis.util.Log;
import ch.elexis.util.PlatformHelper;
import ch.elexis.util.SWTHelper;
import ch.rgw.IO.*;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;
import ch.rgw.tools.VersionInfo;

/**
 * Diese Klasse ist der OSGi-Activator und steuert somit Start und Ende 
 * der Anwendung. Ganz früh (vor dem Initialisieren der anwendung) und 
 * ganz spät (unmittelbar vor dem Entfernen der Anwendung) notwendige
 * Schritte müssen hier durchgeführt werden.  
 * Ausserdem werden hier globale Variablen und Konstanten angelegt.
 */
public class Hub extends AbstractUIPlugin {
	// Globale Konstanten
	public static final String PLUGIN_ID="ch.elexis"; //$NON-NLS-1$
	public static final String COMMAND_PREFIX=PLUGIN_ID+".commands."; //$NON-NLS-1$
	static final String neededJRE="1.5.0"; //$NON-NLS-1$
    public static final String Version="1.0.0"; //$NON-NLS-1$
    public static final String DBVersion="1.5.0"; //$NON-NLS-1$
    static final String[] mine={"ch.elexis","ch.rgw"}; //$NON-NLS-1$ //$NON-NLS-2$
    private static List<ShutdownJob> shutdownJobs=new LinkedList<ShutdownJob>();
            
    //Globale Variable
    /** Das Singleton-Objekt dieser Klasse */
	public static Hub plugin;
    
    /** Lokale Einstellungen (Werden in der Registry bzw. ~/.java gespeichert) */
	public static Settings localCfg;
    
    /** Globale Einstellungen (Werden in der Datenbank gespeichert) */
	public static Settings globalCfg;
	
	/** Anwenderspezifische Einstellungen (Werden in der Datenbank gespeichert) */
	public static Settings userCfg;
	
	/** Mandantspezifische EInstellungen (Werden in der Datenbank gespeichert) */
	public static Settings mandantCfg;
   
    /** Zentrale Logdatei */
	public static Log log;
    
    /** Globale Aktionen */
	public static GlobalActions mainActions;
	
	/** Der aktuell angemeldete Anwender */
	public static Anwender actUser;
	
	/** Der Mandant, auf dessen namen die aktuellen Handlungen gehen */
	public static Mandant actMandant;

	/** Die zentrale Zugriffskontrolle */
	public static AccessControl acl=new AccessControl();
	
	/** Der Initialisierer für die Voreinstellungen */
	public static PreferenceInitializer pin;
	
	/** Hintergrundjobs zum Nachladen von Daten */
    public static JobPool jobPool;
    
    /** Factory für interne PersistentObjects */
    public static PersistentObjectFactory poFactory;
	   
    /** Heartbeat */
    public static Heartbeat heart;
    
    public Hub() {
		plugin = this;

		// Log und Exception-Handler initialisieren
		log=Log.get("Elexis startup"); //$NON-NLS-1$
 		localCfg=new SysSettings(SysSettings.USER_SETTINGS,Desk.class);
 		userCfg=localCfg; // Damit Anfragen auf userCfg bei nicht eingeloggtem User keine NPE werfen

 		// initialize log with default configuration
 		initializeLog(localCfg);

		// Kommandozeile lesen und lokale Konfiguration einlesen
 		localCfg=new SysSettings(SysSettings.USER_SETTINGS,Desk.class);
		String[] args=Platform.getApplicationArgs();
		for(String s:args){
			if(s.startsWith("--use-config=")){ //$NON-NLS-1$
				String[] c=s.split("="); //$NON-NLS-1$
				log.log(Messages.Hub_12+c[1],Log.INFOS);
				localCfg=localCfg.getBranch(c[1], true);
				
				// initialize log with special configuration
				initializeLog(localCfg);
				break;
			}
		}
		
		// Exception handler initialiseren, Output wie log, auf eigene Klassen begrenzen
        ExHandler.setOutput(localCfg.get(PreferenceConstants.ABL_LOGFILE,"")); //$NON-NLS-1$
        ExHandler.setClasses(mine);
        
        // Java Version prüfen
        VersionInfo vI=new VersionInfo(System.getProperty("java.version","0.0.0")); //$NON-NLS-1$ //$NON-NLS-2$
        log.log("Elexis "+Version+", build "+getRevision(true)+Messages.Hub_19 + //$NON-NLS-1$ //$NON-NLS-2$
                Messages.Hub_20+vI.version(),Log.SYNCMARK);

        if(vI.isOlder(neededJRE))
        {   String msg=Messages.Hub_21+neededJRE;
            getLog().log(new Status(Status.ERROR,"ch.elexis", //$NON-NLS-1$
                -1,msg,new Exception(msg)));
            SWTHelper.alert(Messages.Hub_23, msg);
            log.log(msg,Log.FATALS);
        }
        log.log(Messages.Hub_24+getBasePath(),Log.INFOS);
        poFactory=new PersistentObjectFactory();
 		pin=new PreferenceInitializer();
 		pin.initializeDefaultPreferences();
        jobPool=JobPool.getJobPool();
        //pinger=new PingerJob();
        jobPool.addJob(new ListLoader<Patient>("PatientenListe",new Query<Patient>(Patient.class),new String[]{"Name","Vorname"})); //$NON-NLS-1$
        //jobPool.addJob(new ListLoader<Plz>("Plz",new Query(Plz.class),new String[]{"Plz","Ort"}));

	}
    
    /*
     * called by constructor
     */
    private void initializeLog(Settings cfg) {
		String logfileName = cfg.get(PreferenceConstants.ABL_LOGFILE, "elexis.log"); //$NON-NLS-1$
		int maxLogfileSize = -1;
		try {
			String defaultValue = new Integer(Log.DEFAULT_LOGFILE_MAX_SIZE).toString();
			String value = cfg.get(PreferenceConstants.ABL_LOGFILE_MAX_SIZE, defaultValue);
			maxLogfileSize = Integer.parseInt(value.trim());
		} catch (NumberFormatException ex) {
			// do nothing
		}

		Log.setLevel(cfg.get(PreferenceConstants.ABL_LOGLEVEL,Log.ERRORS));
		Log.setOutput(logfileName, maxLogfileSize);
		Log.setAlertLevel(cfg.get(PreferenceConstants.ABL_LOGALERT,Log.ERRORS));
    }

	/**
	 * Hier stehen Aktionen, die ganz früh, noch vor dem Starten der Workbench,
	 * durchgeführt werden sollen.
	 */
	public void start(BundleContext context) throws Exception {
        //log.log("Basedir: "+getBasePath(),Log.DEBUGMSG);
    	super.start(context);
    	heart=Heartbeat.getInstance();
	}

	/**
	 * Programmende
	 */
	public void stop(BundleContext context) throws Exception {
		heart.stop();
		JobPool.getJobPool().dispose();
		if(Hub.actUser!=null){
			Anwender.logoff();
		}
        if(globalCfg!=null){
        	// We should not flush acl's at this point, since this might overwrite other client's settings
        	// acl.flush(); 
            globalCfg.flush();
        }
		PersistentObject.disconnect();
		globalCfg=null;
        super.stop(context);
		plugin = null;
		if((shutdownJobs!=null) && (shutdownJobs.size()>0)){
			Shell shell=new Shell(Display.getDefault());
			MessageDialog dlg=new MessageDialog(shell,"Elexis: Konfiguration",Dialog.getDefaultImage(),"Bitte schalten Sie den PC nicht aus und warten Sie mit Elexis-Neustart, bis diese Nachricht verschwindet",
				 SWT.ICON_INFORMATION,new String[]{"Abbruch"},0);
			dlg.setBlockOnOpen(false);
			dlg.open();
			for(ShutdownJob job:shutdownJobs){
				job.doit();
			}
			dlg.close();
		}
	}

	public static void setMandant(Mandant m){
		if(actMandant!=null){
			//Hub.mandantCfg.dump(null);
			mandantCfg.flush();
		}
		if(m==null){
			if((mainActions!=null) && (mainActions.mainWindow!=null) && (mainActions.mainWindow.getShell()!=null)){
				mandantCfg=userCfg;
			}
		}else{
			mandantCfg=new SqlSettings(PersistentObject.getConnection(),"USERCONFIG","Param","Value","UserID="+m.getWrappedId()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		actMandant=m;
		setWindowText(null);
		GlobalEvents.getInstance().fireSelectionEvent(Hub.actMandant);
		GlobalEvents.getInstance().fireUpdateEvent(Mandant.class);
	}
	
	public static void setWindowText(Patient pat){
		StringBuilder sb=new StringBuilder();
			sb.append("Elexis -");
			if(Hub.actUser==null){
				sb.append("Kein Anwender eingeloggt - ");
			}else{
				sb.append(" ").append(Hub.actUser.getLabel());
			}
			if(Hub.actMandant==null){
				sb.append(" Kein Mandant ");
				
			}else{
				sb.append(" / ").append(Hub.actMandant.getLabel());
			}
			if(pat==null){
				pat=GlobalEvents.getSelectedPatient();
			}
			if(pat==null){
				sb.append("  -  Kein Patient ausgewählt");
			}else{
				String nr=pat.getPatCode();
				int act=new TimeTool().get(TimeTool.YEAR);
				int patg=new TimeTool(pat.getGeburtsdatum()).get(TimeTool.YEAR);
				int alter=act-patg;
				sb.append("  / ").append(pat.getLabel())
					.append("(").append(alter).append(") - ")
					.append("[").append(nr).append("]");
				
				if(Reminder.findForPatient(pat,Hub.actUser).size()!=0){
					sb.append("    *** Reminders *** ");
				}
			}
			if(mainActions.mainWindow!=null){
				Shell shell=mainActions.mainWindow.getShell();
				if((shell!=null) && (!shell.isDisposed())){
					mainActions.mainWindow.getShell().setText(sb.toString());		
				}
			}
	}
	
	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("ch.elexis", path); //$NON-NLS-1$
	}
    public static String getId(){
    	return "Elexis v."+Version+", r."+getRevision(false)+" "+System.getProperty("os.name")+"/"+System.getProperty("os.version");
    }
	
	/** Revisionsnummer und Erstellungsdatum dieser Instanz ermitteln. Dazu wird 
	 *  die beim letzten Commit von Subversion geänderte Variable LastChangedRevision
	 *  untersucht, und fürs Datum das von ANT beim build eingetragene Datum gesucht.
	 *  Wenn diese Instanz nicht von ANT erstellt wurde, handelt es sich um eine
	 *  Entwicklerversion, welche unter Eclipse-Kontrolle abläuft.
	 */
    public static String getRevision(boolean withdate)
    {
    	String SVNREV="$LastChangedRevision: 2696 $"; //$NON-NLS-1$
        String res=SVNREV.replaceFirst("\\$LastChangedRevision:\\s*([0-9]+)\\s*\\$","$1"); //$NON-NLS-1$ //$NON-NLS-2$
        if(withdate==true){
      	  	File base=new File(getBasePath()+"/rsc/compiletime.txt");
      	  	if(base.canRead()){
      	  		String dat=FileTool.readFile(base);
      	  		if(dat.equals("@TODAY@")){
                    res+=Messages.Hub_38;      	  			
      	  		}else{
      	  			res+=", "+new TimeTool(dat+"00").toString(TimeTool.FULL_GER);
      	  		}
      	  	}else{
      	  		res+=",compiletime not known";
      	  	}
        }
        return res;
    }
    /*
    @SuppressWarnings("deprecation") //$NON-NLS-1$
	public static String getBasePath(){
    	URL url=null;
		try {
			url = Platform.asLocalURL(new URL(FileTool.getClassPath(Hub.class)));
			File f=new File(url.getPath());
			return f.getParentFile().getParentFile().getParentFile().getParent();
		} catch (MalformedURLException e) {
			ExHandler.handle(e);
		} catch (IOException e) {
			ExHandler.handle(e);
		}
		
    	return ""; //$NON-NLS-1$
    }
    */
   
    public static String getBasePath(){
    	return PlatformHelper.getBasePath(PLUGIN_ID);
  	}
    
    public static List<Anwender>getUserList(){
    	Query<Anwender> qbe=new Query<Anwender>(Anwender.class);
    	return qbe.execute();
    }
    public static List<Mandant>getMandantenList(){
    	Query<Mandant> qbe=new Query<Mandant>(Mandant.class);
    	return qbe.execute();
    }
    public static Shell getActiveShell(){
    	return plugin.getWorkbench().getActiveWorkbenchWindow().getShell();
    }
    
    /**
     * A job that executes during sstop() of the plugin (that means after the workbench
     * is shut down
     * @author gerry
     *
     */
    public interface ShutdownJob{
    	/**
    	 * do whatever you like
    	 */
    	public void doit() throws Exception;
    }
    public static void addShutdownJob(ShutdownJob job){
    	if(!shutdownJobs.contains(job)){
    		shutdownJobs.add(job);
    	}
    }
}
