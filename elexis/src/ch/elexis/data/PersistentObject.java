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
 *    $Id: PersistentObject.java 2523 2007-06-16 04:45:04Z rgw_ch $
 *******************************************************************************/

package ch.elexis.data;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.data.cache.SoftCache;
import ch.elexis.preferences.PreferenceConstants;
import ch.elexis.preferences.PreferenceInitializer;
import ch.elexis.preferences.SettingsPreferenceStore;
import ch.elexis.util.DBUpdate;
import ch.elexis.util.Log;
import ch.rgw.Compress.CompEx;
import ch.rgw.IO.Resource;
import ch.rgw.IO.Settings;
import ch.rgw.IO.SqlSettings;
import ch.rgw.net.NetTool;
import ch.rgw.tools.*;
import ch.rgw.tools.JdbcLink.Stm;


/**
 * Base class for all objects to be stored in the database. A PersistentObject has an unique ID, which is
 * assigned as the object is created. Every object is accessed "lazily" which means that "loading" an object
 * instantiates only a proxy with the ID of the requested object. Members are read only as needed.
 * The class provides static functions to log into the database, and provides methods for reading and writing
 * of fields for derived classes.
 * The get method uses a cache to reduce the number of costly database operations. Repeated read-requests
 * within a configurable life-time (defaults to 15 seconds) are satisfied from the cache.
 * PersistentObject can log every write-access in a trace-table, as desired.
 * get- and set- methods perform necessary coding/decoding of fields as needed. 
 * 
 * Basisklasse für alle Objekte, die in der Datenbank gespeichert werden sollen. Ein PersistentObject
 * hat eine eindeutige ID, welche beim Erstellen des Objekts automatisch vergeben wird. Grundsätzlich wird
 * jedes Objekt "lazy" geladen, indem jede Leseanforderung zunächst nur einen mit der ID des Objekts versehenen
 * Proxy instantiiert und jedes Member-Feld erst auf Anfrage nachlädt.
 * Die Klasse stellt statische Funktionen zur Kontaktaufnahme mit der Datenbank und member-Funktionen
 * zum Lesen und Schreiben von Feldern der Tochterobjekte zur Verfügung.
 * Die get-Methode verwendet einen zeitlich limitierten Cache. um die Zahl teurer Datenbankoperationen
 * zu minimieren: Wiederholte Lesezugriffe innerhalb einer einstellbaren lifetime (Standardmässig
 * 15 Sekunden) werden aus dem cache bedient.
 * PersistentObject kann auch alle Schreibvorgänge in einer speziellen Trace-Tabelle dokumentieren.
 * Die get- und set- Methoden kümmern sich selbst um codierung/decodierung der Felder, wenn nötig.
 * Aufeinanderfolgende und streng zusammengehörende Schreibvorgänge können auch in einer Transaktion
 * zusammengefasst werden, welche nur ganz oder gar nicht ausgeführt wird. (begin()). Es ist aber zu
 * beachten, das nicht alle Datenbanken Transaktionen unterstützen. MySQL beispielsweise nur, wenn es
 * mit InnoDB-Tabellen eingerichtet wurde (welche langsamer sind, als die standardmässig verwendeten
 * MyISAM-Tabellen).
 * @author gerry
 */
public abstract class PersistentObject{
	public static final int CACHE_DEFAULT_LIFETIME = 15;
	public static final int CACHE_MIN_LIFETIME = 5;

	// maximum character length of int fields in tables
	private static int MAX_INT_LENGTH = 10;
	
	protected static JdbcLink j=null;
	protected static Log log=Log.get("PersistentObject");
	//private static final HuffmanTree tree;
    private String id;
    private static Hashtable<String,String> mapping;
    private static SoftCache<String> cache;
    private static Job cacheCleaner;
    private static String username;
    private static String pcname;
    private static String tracetable;
    protected static int default_lifetime;
    
	static{
		mapping=new Hashtable<String,String>();
		default_lifetime=Hub.localCfg.get(PreferenceConstants.ABL_CACHELIFETIME, CACHE_DEFAULT_LIFETIME);
		if(default_lifetime<CACHE_MIN_LIFETIME){
			default_lifetime=CACHE_MIN_LIFETIME;
			Hub.localCfg.set(PreferenceConstants.ABL_CACHELIFETIME, CACHE_MIN_LIFETIME);
		}
		cache=new SoftCache<String>(2000,0.7f);
        
		cacheCleaner=new Job("CacheCleaner"){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				cache.purge();
				schedule(60000L);
				return Status.OK_STATUS;
			}
        	
        };
        cacheCleaner.setUser(false);
        cacheCleaner.setPriority(Job.DECORATE);
        //cacheCleaner.schedule(300000L);
        log.log("Cache setup: default_lifetime "+default_lifetime, Log.INFOS);
    }
	public static enum FieldType{
		TEXT,LIST,JOINT
	};
	  /**
	   * Connect to a database. 
	   * In the first place, the method checks if there is a demoDB in the Elexis base directory. If
	   * found, only this database will be used. If not, connection parameters are taken from the provided
	   * Settings. If there ist no database found, it will be created newly, using the createDB-Script.
	   * After successful connection, the global Settings (Hub.globalCfg) are linked to the database.
	   * @return true on success
	   * 
     * Verbindung mit der Datenbank herstellen. Die Verbindungsparameter werden
     * aus den übergebenen Settings entnommen. 
     * Falls am angegebenen Ort keine Datenbank gefunden wird, wird eine neue erstellt, falls
     * ein create-Script für diesen Datenbanktyp unter rsc gefunden wurde.
     * Wenn die Verbindung hergestell werden konnte, werden die global Settings mit dieser
     * Datenbank verbunden.
     * @return true für ok, false wenn keine Verbindung hergestellt werden konnte.
     */
    public static boolean connect(Settings cfg){
    	File base=new File(Hub.getBasePath());
    	File demo=new File(base.getParentFile().getParent()+"/demoDB");
    	if(demo.exists() && demo.isDirectory()){
    		j=JdbcLink.createInProcHsqlDBLink(demo.getAbsolutePath()+"/db");
    		if(j.connect("sa", "")){
    			return connect(j);
    		}else{
    			MessageDialog.openError(Desk.theDisplay.getActiveShell(), "Fehler mit Demo-Datenbank", "Es wurde zar ein demoDB-Verzeichnis gefunden, aber dort ist keine verwendbare Datenbank");
    			return false;
    		}
    	}
    	
    	IPreferenceStore localstore = new SettingsPreferenceStore(cfg);
        String driver=localstore.getString(PreferenceConstants.DB_CLASS);
        String connectstring=localstore.getString(PreferenceConstants.DB_CONNECT);
        String user=localstore.getString(PreferenceConstants.DB_USERNAME);
        String pwd=localstore.getString(PreferenceConstants.DB_PWD);
        String typ=localstore.getString(PreferenceConstants.DB_TYP);
        /* experimentell
        driver="com.ibm.db2.jcc.DB2Driver";
        connectstring="jdbc:db2:elexis";
        user="testperson";
        pwd="blabla";
        typ="db2";
        */
        if(driver.equals("")){
        	String d=PreferenceInitializer.getDefaultDBPath();
            j=JdbcLink.createInProcHsqlDBLink(d);
            user="sa";
            pwd="";
            typ=j.DBFlavor;
        }else {
            j=new JdbcLink(driver,connectstring,typ);
        }
		if(j.connect(user,pwd)==true){
            log.log("Verbunden mit "+j.dbDriver()+", "+connectstring,Log.SYNCMARK);
            return connect(j);
        }
        return false;
	}
    public static boolean connect(JdbcLink jd){
    	j=jd;
    	  Hub.globalCfg=new SqlSettings(j,"CONFIG");
          // Zugriffskontrolle initialisieren
          Hub.acl.load();
          String created=Hub.globalCfg.get("dbversion",null);
          
          if(created==null){
        	  created=Hub.globalCfg.get("created",null);
          }else{
        	  log.log("Database version "+created, Log.SYNCMARK);
          }
          if(created==null){
        	 log.log("No Version found. Creating new Database", Log.SYNCMARK);
          	java.io.InputStream is=null;
          	Stm stm=null;
              try{
            	  Resource rsc=new Resource("ch.elexis.data");
                  is=rsc.getInputStream("createDB.script");
                  stm=j.getStatement();
                  if(stm.execScript(is,true, true)==true){
                	  Log.setAlertLevel(Log.FATALS);
                  	Hub.globalCfg.undo();
                      Hub.globalCfg.set("created",new TimeTool().toString(TimeTool.FULL_GER));
                      Anwender.init();
                      Mandant.init();
                     	Hub.pin.initializeGrants();
                      Hub.pin.initializeGlobalPreferences();
                      Hub.globalCfg.flush();
                      Hub.localCfg.flush();
                      disconnect();
                      MessageDialog.openInformation(null,"Programmende","Es wurde eine neue Datenbank angelegt. Das Programm muss beendet werden. Bitte starten Sie danach neu.");
                      System.exit(1);
                  }else{
                      log.log("Kein create script für Datenbanktyp "+j.DBFlavor+" gefunden.",Log.ERRORS);
                      return false;
                  }
              }catch(Throwable ex){
                  ExHandler.handle(ex);
                  return false;
              }
              finally{
              	j.releaseStatement(stm);
              	try{
              		is.close();
              	}catch(Exception ex){
              		/* Janusode */
              	}
              }
          }
          VersionInfo vi=new VersionInfo(Hub.globalCfg.get("dbversion","0.0.0"));
          log.log("Verlangte Datenbankversion: "+Hub.DBVersion,Log.INFOS);
          log.log("Gefundene Datenbankversion: "+vi.version(),Log.INFOS);
          if(vi.isOlder(Hub.DBVersion)){
              log.log("ältere Version der Datenbank gefunden ",Log.WARNINGS);
              DBUpdate.doUpdate();
          }
          // Wenn trace global eingeschaltet ist, gilt es für alle
          setTrace(Hub.globalCfg.get(PreferenceConstants.ABL_TRACE,null));
          // wenn trace global nicht eingeschaltet ist, kann es immer noch für diese 
          // Station eingeschaltet sein
          if(tracetable==null){
          	setTrace(Hub.localCfg.get(PreferenceConstants.ABL_TRACE,null));
          }
          return true;
    }
    /**
     * Return the Object containing the connection. This should only in very specific conditions be
     * neccessary, if one needs a direkt access to the database. It is strongly recommended to use this only 
     * very carefully, as callers must ensure for themselves that their code works with different
     * database engines equally.
     *  
     * Das Objekt, das die Connection enthält zurückliefern. Sollte nur in Ausnahmefällen nötig sein,
     * wenn doch mal ein direkter Zugriff auf die Datenbank erforderlich ist.
     * @return den JdbcLink, der die Verbindung zur Datenbank enthält
     */
	public static JdbcLink getConnection(){
		return j;
	}
    /** Die Zuordnung von Membervariablen zu Datenbankfeldern geschieht über statische mappings:
        * Jede abgeleitete Klassen muss ihre mappings in folgender Form deklarieren:
        * addMapping("Tabellenname","Variable=Feld"...); wobei:<ul>
        * <li>"Variable=Feld"    - Einfache Zuordnung, Variable wird zu Feld</li>
        * <li>"Variable=S:x:Feld" - Spezielle Abspeicherung<br>
        *               x=D - Datumsfeld, wird automatisch in Standardformat gebracht<br>
        *               x=C - Feld wird vor Abspeicherung komprimiert</li>
        * <li>"Variable=JOINT:FremdID:EigeneID:Tabelle[:type]" - n:m - Zuordnungen</li>
        * <li>"Variable=LIST:EigeneID:Tabelle:orderby[:type]"  - 1:n - Zuordnungen</li>
        * <li>"Variable=EXT:tabelle"	- Das Feld ist in der genannten externen Tabelle  
        *</ul>
      */  
    static protected void addMapping(String prefix, String... map){
    	for(String s:map){
    		String[] def=s.trim().split("[ \t]*=[ \t]*");
    		if(def.length!=2){
    			mapping.put(prefix+def[0],def[0]);
    		}else{
    			mapping.put(prefix+def[0],def[1]);
    		}
    	}
    }
    /**
     * Trace (protokollieren aller Schreibvorgänge) ein- und ausschalten. Die Trace-Tabelle muss folgende
     * Spalten haben: logtime (long), Workstation (VARCHAR), Username(Varchar), action (Text/Longvarchar)
     * @param Tablename Name der Trace-tabelle oder null: Trace aus.
     */
    public static void setTrace(String Tablename){
    	if((Tablename!=null) && (Tablename.equals("none") || Tablename.equals(""))){
    		Tablename=null;
    	}
        tracetable=Tablename;
        username=JdbcLink.wrap(System.getProperty("user.name"));
        pcname=JdbcLink.wrap(NetTool.hostname);
    }
    /**
     * Exklusiven Zugriff auf eine Ressource verlangen. Die Sperre kann für maximal
     * zwei Sekunden beansprucht werden, dann wird sie gelöst.
     * Dies ist eine sehr teure Methode, die eigentlich nur notwendig ist, weil es
     * keine standardisierte JDBC-Methode für Locks gibt...
     * Die Sperre ist kooperativ: Sie verhindert konkurrierende Zugriffe nicht wirklich,
     * sondern verlässt sich darauf, dass Zugreifende freiwillig zuerst die Sperre abfragen.
     * Sie bezieht sich auch nicht direkt auf eine bestimmte Tabelle, sondern immer nur auf eine
     * willkürliche frei wählbare Bezeichnung. Diese muss für jedes zu schützende Objekt standardisiert
     * werden.
     * @param name Name der gewünschten Sperre
     * @param wait wenn True, warten bis die sperre frei oder abgelaufen ist
     * @return null, wenn die Sperre belegt war, sonst eine id für unlock
     */
    public static synchronized String lock(String name, boolean wait){
    	Stm stm=j.getStatement();
    	String lockname="lock"+name;
    	String lockid=StringTool.unique("lock");
    	while(true){
	    	long timestamp=System.currentTimeMillis();
	    	// Gibt es das angeforderte Lock schon?
	    	String oldlock=stm.queryString("SELECT wert FROM CONFIG WHERE param="+JdbcLink.wrap(lockname));
	    	if(!StringTool.isNothing(oldlock)){
	    		// Ja, wie alt ist es?
	    		String[] def=oldlock.split("#");
	    		long locktime=Long.parseLong(def[1]);
	    		long age=timestamp-locktime;
	    		if(age>2000L){	// Älter als zwei Sekunden -> Löschen
	    			stm.exec("DELETE FROM CONFIG WHERE param="+JdbcLink.wrap(lockname));
	    		}else{
	    			if(wait==false){
	    				return null;
	    			}else{
	    				continue;
	    			}
	    		}
	    	}
	    	// Neues Lock erstellen
	    	String lockstring=lockid+"#"+Long.toString(System.currentTimeMillis());
	    	StringBuilder sb=new StringBuilder();
	    	sb.append("INSERT INTO CONFIG (param,wert) VALUES (")
	    		.append(JdbcLink.wrap(lockname)).append(",")
	    		.append("'").append(lockstring)
	    		.append("')");
	    	stm.exec(sb.toString());
	    	// Prüfen, ob wir es wirklich haben, oder ob doch jemand anders schneller war.
	    	String check=stm.queryString("SELECT wert FROM CONFIG WHERE param="+JdbcLink.wrap(lockname));
	    	if(check.equals(lockstring)){
	    		break;
	    	}
    	}
    	j.releaseStatement(stm);
    	return lockid;
    }
    /** 
     * Exklusivzugriff wieder aufgeben
     * @param name	Name des Locks
     * @param id	bei "lock" erhaltene LockID
     * @return true bei Erfolg
     */
    public static synchronized boolean unlock(String name, String id){
    	String lockname="lock"+name;
    	String lock=j.queryString("SELECT wert from CONFIG WHERE param="+JdbcLink.wrap(lockname));
    	if(StringTool.isNothing(lock)){
    		return false;
    	}
    	String[] res=lock.split("#");
    	if(res[0].equals(id)){
    		j.exec("DELETE FROM CONFIG WHERE param="+JdbcLink.wrap(lockname));
    		return true;
    	}
    	return false;
    }
    /**
     * Einschränkende Bedingungen für Suche nach diesem Objekt definieren
     * @return ein Constraint für eine Select-Abfrage
     */
    protected String getConstraint(){
    	return "";
    }
    
    /**
     * Bedingungen für dieses Objekt setzen
     */
    protected void setConstraint(){
        /* Standardimplementation ist leer */
    }
    
    /** Einen menschenlesbaren Identifikationsstring für dieses Objet liefern */ 
    abstract public String getLabel();
    
	/**
	 * Jede abgeleitete Klasse muss deklarieren, in welcher Tabelle sie gespeichert werden will.
	 * @return Der Name einer bereits existierenden Tabelle der Datenbank
	 */
	abstract protected String getTableName();
	
	/**
	 * Angeben, ob dieses Objekt gültig ist.
	 * @return true wenn die Daten gültig (nicht notwendigerweise korrekt) sind
	 */
	public boolean isValid(){
		if(!exists()){
			return false;
		}
		return true;
	}
	/**
	 * Die eindeutige Identifikation dieses Objektes/Datensatzes liefern. Diese ID wird jeweils automatisch
	 * beim Anlegen eines Objekts dieser oder einer abgeleiteten Klasse erstellt und bleibt dann unveränderlich.
	 * @return die ID.
	 */
	public String getId(){
		return id;
	}
	/**
	 * Die ID in einen datenbankgeeigneten Wrapper verpackt (je nach Datenbank; meist Hochkommata).
	 */
	public String getWrappedId(){
		return JdbcLink.wrap(id);
	}
	
	/** Der Konstruktor erstellt die ID */
	protected PersistentObject()
	{
		id=StringTool.unique("prso");
	}
	/** Konstruktor mit vorgegebener ID (zum Deserialisieren) 
     *  Wird nur von xx::load gebraucht.                    */
	protected PersistentObject(String id){
		this.id=id;
	}
	/**
	 * Objekt in einen String serialisieren. Diese Standardimplementation macht eine "cheap copy":
	 * Es wird eine Textrepräsentation des Objektes erstellt,
	 * mit deren Hilfe das Objekt später wieder aus der Datenbank erstellt werden kann. Dies
	 * funktioniert nur innerhalb derselben Datenbank.
	 * @return der code-String, aus dem mit createFromCode wieder das Objekt erstellt werden 
	 * kann
	 */
	public String storeToString(){
		StringBuilder sb=new StringBuilder();
		sb.append(getClass().getName()).append("::").append(getId());
		return sb.toString();
	}
	/**
     * Feststellen, ob ein PersistentObject bereits in der Datenbank existiert
     * @return true wenn es existiert
     */
    public boolean exists(){
        if(StringTool.isNothing(getId())){
        	return false;
        }
    	String ch=j.queryString("SELECT ID FROM "+getTableName()+" WHERE "+"ID="+getWrappedId());
        return ch==null ? false : true;
    }
    /**
     * Darf dieses Objekt mit Drag&Drop verschoben werden
     * @return true wenn ja.
     */
    public boolean isDragOK(){
        return false;
    }
    /**
     * Aus einem Feldnamen das dazugehörige Datenbankfeld ermitteln
     * @param f Der Feldname
     * @return  Das Datenbankfeld oder **ERROR**, wenn kein mapping für das angegebene Feld existiert.
     */
	 public String map(String f){
         if(f.equals("ID")){
             return f;
         }
		String prefix=getTableName();
		String res=mapping.get(prefix+f);
		if(res==null){
			log.log("Fehler bei der Felddefinition "+f,Log.ERRORS);
			return "**ERROR:"+f+"**";
		}
		return res;
	}
	
	 public FieldType getFieldType(String f){
		 String mapped=map(f);
		 if(mapped.startsWith("LIST:")){
			 return FieldType.LIST;
		 }else if(mapped.startsWith("JOINT:")){
			 return FieldType.JOINT;
		 }else{
			 return FieldType.TEXT;
		 }
	 }
	
	/** 
	 * Ein Feld aus der Datenbank auslesen.
	 * Die Tabelle wird über getTableName() erfragt.
     * Das Feld wird beim ersten Aufruf in jedem Fall aus der Datenbank gelesen.
     * Dann werden weitere Lesezugriffe während der <i>lifetime</i> aus dem
     * cache bedient, um die Zahl der Datenbankzugriffe zu minimieren. Nach Ablauf
     * der lifetime erfolgt wieder ein Zugriff auf die Datenbank, wobei auch der cache wieder
     * erneuert wird.
	 * @param field Name des Felds
	 * @return Der Inhalt des Felds (kann auch null sein), oder **ERROR**, 
	 * wenn versucht werden sollte, ein nicht existierendes Feld auszulesen
	 */
	public String get(String field)
	{
        String key=getKey(field);
        Object ret=cache.get(key);
        if(ret instanceof String){
        	return (String) ret;
        }
        /*
    	if((cf!=null) && (cf.expired()==false) && !(cf.contents instanceof VersionedResource)){
    		return (String)cf.contents;
    	}*/
    	boolean decrypt=false;
		StringBuffer sql=new StringBuffer();
		String mapped=map(field);
		String table=getTableName();
		if(mapped.startsWith("EXT:")){
			int ix=mapped.indexOf(':',5);
			if(ix==-1){
				log.log("Fehlerhaftes Mapping bei "+field,Log.ERRORS);
				return "**ERROR: "+field+"**";
			}
			table=mapped.substring(4,ix);
			mapped=mapped.substring(ix+1);
		}else if(mapped.startsWith("S:")){
			mapped=mapped.substring(4);
			decrypt=true;
		}else if(mapped.startsWith("JOINT:")){
			String[] dwf=mapped.split(":");
			if(dwf.length>4){
				String objdef=dwf[4]+"::";
				StringBuilder sb=new StringBuilder();
				List<String[]> list=getList(field, new String[0]);
				PersistentObjectFactory fac=new PersistentObjectFactory();
				for(String[] s:list){
					PersistentObject po=fac.createFromString(objdef+s[0]);
					sb.append(po.getLabel()).append("\n");
				}
				return sb.toString();
			}
			
		}else if(mapped.startsWith("LIST:")){
			String[] dwf=mapped.split(":");
			if(dwf.length>4){
				String objdef=dwf[4]+"::";
				StringBuilder sb=new StringBuilder();
				List<String> list=getList(field, false);
				PersistentObjectFactory fac=new PersistentObjectFactory();
				for(String s:list){
					PersistentObject po=fac.createFromString(objdef+s);
					sb.append(po.getLabel()).append("\n");
				}
				return sb.toString();
			}
		}else if(mapped.startsWith("**")){	// If the field could not be mapped
			String exi=map("ExtInfo");		// Try to find it in ExtInfo
			if(!exi.startsWith("**")){
				Hashtable ht=getHashtable("ExtInfo");
				Object res=ht.get(field);
				if(res instanceof String){
					return (String)res;
				}
			}
			String method="get"+field;		// or try to find a "getter" Method for the field 
			try {
				Method mx=getClass().getMethod(method, new Class[0]);
				Object ro=mx.invoke(this, new Object[0]);
				if(ro==null){
					return "";
				}else if(ro instanceof String){
					return (String)ro;
				}else if (ro instanceof PersistentObject){
					return ((PersistentObject)ro).getLabel();
				}else{
					return "?invalid field? "+mapped;
				}
			} catch (Exception ex) {
				ExHandler.handle(ex);
				log.log("Fehler in Felddefinition "+field, Log.ERRORS);
				return mapped;
			}
		}
		sql.append("SELECT ").append(mapped).append(" FROM ")
		    	.append(table).append(" WHERE ID='").append(id).append("'");
		Stm stm=j.getStatement();
        ResultSet rs=stm.query(sql.toString());
        String res=null;
        try{
            if((rs!=null) && (rs.next()==true)){
                if(decrypt){
                    res=decode(field,rs);
                }else{
                    res=rs.getString(mapped);
                }
                if(res==null){
                    res="";
                }
               	cache.put(key,res,getCacheTime());
            }
        }catch(Exception ex){
            ExHandler.handle(ex);
        }
        finally{
            j.releaseStatement(stm);
        }
        return res;
	}

		
    protected byte[] getBinary(String field){
        StringBuffer sql=new StringBuffer();
        String mapped=/*map*/(field);
        String table=getTableName();
        sql.append("SELECT ").append(mapped).append(" FROM ")
            .append(table).append(" WHERE ID='").append(id).append("'");
       
       Stm stm=j.getStatement();
       ResultSet res=stm.query(sql.toString());
       try{
           if((res != null) && (res.next()==true)){
               return res.getBytes(mapped);
           }
       }catch(Exception ex){
           ExHandler.handle(ex);
       }finally{
           j.releaseStatement(stm);
       }
       return null;
    }
    
    protected VersionedResource getVersionedResource(String field,boolean flushCache){
        String key=getKey(field);
        if(flushCache==false){
        	Object o=cache.get(key);
        	if(o instanceof VersionedResource){
        		return (VersionedResource)o;
	        }
        }
        byte[] blob=getBinary(field);
        VersionedResource ret=VersionedResource.load(blob);
        cache.put(key,ret,getCacheTime());
        return ret;
    }
    
    /**
     * Eine Hashtable auslesen
     * @param field Feldname der Hashtable
     * @return eine Hashtable (ggf. leer)
     */
	public Hashtable getHashtable(String field){
        String key=getKey(field);
        Object o=cache.get(key);
        if(o instanceof Hashtable){
               return (Hashtable)o;
        }
        byte[] blob=getBinary(field);
        if(blob==null){
            return new Hashtable();
        }
        Hashtable ret=StringTool.fold(blob,StringTool.GUESS,null);
        if(ret==null){
        	return new Hashtable();
        }
        cache.put(key,ret,getCacheTime());
        return ret;
 	}
    /** 
     * Bequemlichkeitsmethode zum lesen eines Integer.
     * @param field
     * @return einen Integer. 0 bei 0 oder unlesbar
     */
	public int getInt(String field){
		return checkZero(get(field));
    }
	
	/**
	 * Eine 1:n Verknüpfung aus der Datenbank auslesen.
	 * @param field das Feld, wie in der mapping-Deklaration angegeben
	 * @param reverse wenn true wird rückwärts sortiert
	 * @return eine Liste mit den IDs (String!) der verknüpften Datensätze.
	 */
	@SuppressWarnings("unchecked")
	public List<String> getList(String field, boolean reverse)
	{
		 StringBuffer sql=new StringBuffer();
		 String mapped=map(field);
		 if(mapped.startsWith("LIST:")){
				String[] m=mapped.split(":");
				if(m.length>2){
					//String order=null;
					
					sql.append("SELECT ID FROM ").append(m[2]).append(" WHERE ")
					.append(m[1]).append("=").append(getWrappedId());
					if(m.length>3){
						sql.append(" ORDER by ").append(m[3]);
						if(reverse){
							sql.append(" DESC");
						}
					}
					Stm stm=j.getStatement();
					List<String> ret=stm.queryList(sql.toString(),new String[]{"ID"});
					j.releaseStatement(stm);
					return ret;
				}
		 }else{
			log.log("Fehlerhaftes Mapping "+mapped,Log.ERRORS);
		 }
		 return null;
	}
    /**
     * Eine n:m - Verknüpfung auslesen
     * @param field Das Feld, für das ein entsprechendes mapping existiert
     * @param extra Extrafelder, die aus der joint-Tabelle ausgelesen werden sollen
     * @return eine Liste aus String-Arrays, welche jeweils die ID des gefundenen Objekts
     * und den Inhalt der Extra-Felder enthalten. Null bei Mapping-Fehler
     */
	public List<String[]> getList(String field, String[] extra)
	{
		if(extra==null){
			extra=new String[0];
		}
		StringBuffer sql=new StringBuffer();
		String mapped=map(field);
		if(mapped.startsWith("JOINT:")){
			String[] abfr=mapped.split(":");
			sql.append("SELECT ").append(abfr[1]);
			for(String ex :extra){
				sql.append(",").append(ex);
			}
			sql.append(" FROM ")
				.append(abfr[3]).append(" WHERE ").append(abfr[2])
				.append("=").append(getWrappedId());
			
			Stm stm=j.getStatement();
			ResultSet rs=stm.query(sql.toString());
			j.releaseStatement(stm);
			LinkedList<String[]> list=new LinkedList<String[]>();
			try{
				while(rs.next()==true){
					String[] line=new String[extra.length+1];
					line[0]=rs.getString(abfr[1]);
					for(int i=1;i<extra.length+1;i++){
						line[i]=rs.getString(extra[i-1]);
					}
					list.add(line);
				}
				return list;

			}catch(Exception ex){
				ExHandler.handle(ex);
				log.log("Fehler beim Lesen der Liste ",Log.ERRORS);
				return null;
			}
		}else{
			log.log("Fehlerhaftes Mapping "+mapped,Log.ERRORS);
		}
		return null;

	}
	
	
	/**
	 * Ein Feld in die Datenbank übertragen. Gleichzeitig Cache-update 
	 * Die Tabelle wird über getTableName() erfragt.
	 * @param field Name des Feldes
	 * @param value Einzusetzender Wert (der vorherige Wert wird überschrieben)
	 * @return 0 bei Fehler
	 */
	public boolean set(String field, String value)
	{
		if(value==null){
			value="";
		}
		String mapped=map(field);
		cache.put(getKey(field),value,getCacheTime());
        StringBuffer sql=new StringBuffer();
        
        sql.append("UPDATE ").append(getTableName()).append(" SET ");
        if(mapped.startsWith("S:")){
            sql.append(mapped.substring(4));
        }else{
           sql.append(mapped);
        }
        sql.append("=? WHERE ID=").append(getWrappedId());
        String cmd=sql.toString();
        PreparedStatement pst=j.prepareStatement(cmd);
        
        encode(1,pst,field,value);
        if(tracetable!=null){
        	StringBuffer params = new StringBuffer();
        	params.append("[");
        	params.append(value);
        	params.append("]");
            doTrace(cmd + " " + params);
        }
        try{
            pst.executeUpdate();
            return true; 
        }catch(Exception ex){
            ExHandler.handle(ex);
            return false;
        }
        
	}
	/**
	 * Eine Hashtable speichern. Diese wird zunächst in ein byte[] geplättet, dann wird
	 * sicherheitshalber geprüft, ob sie wirklich aus diesem Array wiederhergestellt werden kann,
	 * und wenn ja, wird sie gespeichert.
	 * @param field
	 * @param hash
	 * @return 0 bei Fehler
	 */
    public int setHashtable(String field, Hashtable hash){
        if(hash==null){
            return 0;
        }
        try{
        	byte[] bin=StringTool.flatten(hash,StringTool.ZIP,null);
        	Hashtable res=StringTool.fold(bin,StringTool.GUESS,null);
        	if(res==null){
        		String ls=StringTool.flattenStrings(hash);
        		//byte[] bin2=StringTool.flatten(hash,StringTool.ZIP,null);
        		log.log("Hashtable: "+ls,Log.ERRORS);
        		throw new Exception("Hashtable nicht wiederherstellbar");
        	}
        	cache.put(getKey(field),hash,getCacheTime());
        	return setBinary(field,bin);
        }catch(Throwable ex){
        	log.log("Fehler beim Speichern von "+field+" von "+getLabel(),Log.ERRORS);
        	MessageDialog.openError(null,"Interner Fehler","Konnte "+field+" von "+getLabel()+" nicht speichern!");
        	return 0;
        }
        
    }
    /**
     * Eine VersionedResource zurückschreiben. Um Datenverlust durch gleichzeitigen
     * Zugriff zu vermeiden, wird zunächst die aktuelle Version in der Datenbank
     * gelesen und mit der neuen Version überlagert.
     */
    protected int setVersionedResource(String field, String entry){
        String lockid=lock("VersionedResource",true);
        VersionedResource old=getVersionedResource(field,true);
        int ret=1;
        if(old.update(entry,Hub.actUser.getLabel())==true){
        	cache.put(getKey(field),old,getCacheTime());
        	ret= setBinary(field,old.serialize());
        }
        unlock("VersionedResource",lockid);
        return ret;
    }
	protected int setBinary(String field, byte[] value){
		StringBuilder sql=new StringBuilder(1000);
        sql.append("UPDATE ").append(getTableName()).append(" SET ")
        .append(/*map*/(field)).append("=?")
		//.append(JdbcLink.wrap(StringTool.enPrintable(StringTool.flatten(hash,StringTool.NONE,null))))
       .append(" WHERE ID=").append(getWrappedId());
        String cmd=sql.toString();
        if(tracetable!=null){
        	doTrace(cmd);
        }
        PreparedStatement stm=j.prepareStatement(cmd);
        try{
            stm.setBytes(1,value);
            stm.executeUpdate();
            return 1;
        }catch(Exception ex){
            ExHandler.handle(ex);
            log.log("Fehler beim Ausführen der Abfrage "+cmd,Log.ERRORS);
        }
        return 0;
	}
	
	/**
	 * Set a value of type int.
	 * @param field a table field of numeric type
	 * @param value the value to be set
	 * @return true on success, false else
	 */
	public boolean setInt(String field, int value) {
		String stringValue = new Integer(value).toString();
		if (stringValue.length() <= MAX_INT_LENGTH) {
			return set(field, stringValue);
		} else {
			return false;
		}
	}
	
	private void doTrace(String sql)
    {
        StringBuffer tracer=new StringBuffer();
        tracer.append("INSERT INTO ").append(tracetable);
        tracer.append(" (logtime,Workstation,Username,action) VALUES (");
        tracer.append(System.currentTimeMillis()).append(",");
        tracer.append(pcname).append(",");
        tracer.append(username).append(",");
        tracer.append(JdbcLink.wrap(sql.replace('\'','/'))).append(")");
	    j.exec(tracer.toString());
    }
    /**
	 * Eine Element einer n:m Verknüpfung eintragen. Zur Tabellendefinition wird das mapping 
	 * verwendet.
	 * @param field Das n:m Feld, für das ein neuer Eintrag erstellt werden soll.
	 * @param oID ID des Zielobjekts, auf das der Eintrag zeigen soll
	 * @param extra Definition der zusätzlichen Felder der Joint-Tabelle. Jeder Eintrag in der Form Feldname=Wert
	 * @return 0 bei Fehler
	 */
	public int addToList(String field, String oID, String... extra)
	{
		String mapped=map(field);
		if(mapped.startsWith("JOINT:")){
			String[] m=mapped.split(":");// m[1] FremdID, m[2] eigene ID, m[3] Name Joint
			if(m.length>3){
				StringBuffer head=new StringBuffer(100);
				StringBuffer tail=new StringBuffer(100);
				head.append("INSERT INTO ").append(m[3]).append("(ID,").append(m[2])
					.append(",").append(m[1]);
				tail.append(") VALUES (").append(JdbcLink.wrap(StringTool.unique("aij"))).append(",")
					.append(getWrappedId()).append(",").append(JdbcLink.wrap(oID));
				if(extra!=null){
					for(String s : extra){
						String[] def=s.split("=");
						if(def.length!=2){
							log.log("Fehlerhafter Aufruf addToList "+s,Log.ERRORS);
							return 0;
						}
						head.append(",").append(def[0]);
						tail.append(",").append(JdbcLink.wrap(def[1]));
					}
				}
				head.append(tail).append(")");
                if(tracetable!=null){
                    String sql=head.toString();
                    doTrace(sql);
                    return j.exec(sql);
                }
				return j.exec(head.toString());
				//j.exec("INSERT INTO ADRESS_IDENT_JOINT (IdentID,AdressID) VALUES ("+getWrappedId()+","+adr.getWrappedId()+")");
			}
		}
		log.log("Fehlerhaftes Mapping: "+mapped,Log.ERRORS);
		return 0;
	}

	/*
	 * 
	public int addToList(String field, String oID, String... extra)
	{
		String mapped=map(field);
		if(mapped.startsWith("JOINT:")){
			String[] m=mapped.split(":");// m[1] FremdID, m[2] eigene ID, m[3] Name Joint
			if(m.length>3){
				StringBuffer head=new StringBuffer(100);
				StringBuffer tail=new StringBuffer(100);
				head.append("INSERT INTO ").append(m[3]).append("(ID,").append(m[2])
					.append(",").append(m[1]);
				tail.append(") VALUES (").append(JdbcLink.wrap(StringTool.unique("aij"))).append(",")
					.append(getWrappedId()).append(",").append(JdbcLink.wrap(oID));
				if(extra!=null){
					for(String s : extra){
						String[] def=s.split("=");
						if(def.length!=2){
							log.log("Fehlerhafter Aufruf addToList "+s,Log.ERRORS);
							return 0;
						}
						head.append(",").append(def[0]);
						tail.append(",").append(JdbcLink.wrap(def[1]));
					}
				}
				head.append(tail).append(")");
                if(tracetable!=null){
                    String sql=head.toString();
                    doTrace(sql);
                    return j.exec(sql);
                }
				return j.exec(head.toString());
				//j.exec("INSERT INTO ADRESS_IDENT_JOINT (IdentID,AdressID) VALUES ("+getWrappedId()+","+adr.getWrappedId()+")");
			}
		}
		log.log("Fehlerhaftes Mapping: "+mapped,Log.ERRORS);
		return 0;
	}
	*/
	
	/**
	 * Ein neues Objekt erstellen und in die Datenbank eintragen
	 * @param customID Wenn eine ID (muss eindeutig sein!) vorgegeben werden soll. Bei null wird eine generiert.
	 * @return true bei Erfolg
	 */
	protected boolean create(String customID){
		//String pattern=this.getClass().getSimpleName();
		if(customID!=null){
			id=customID;
		}
		StringBuffer sql=new StringBuffer(300);
		sql.append("INSERT INTO ").append(getTableName())
		.append("(ID) VALUES (").append(getWrappedId()).append(")");
		if (j.exec(sql.toString())!=0){
            setConstraint();
            return true;
		}
		return false;
	}
	/**
	 * Ein Objekt aus der Datenbank löschen
	 * @return
	 */
	public boolean delete(){
		StringBuilder sql=new StringBuilder();
		sql.append("DELETE FROM ").append(getTableName())
			.append(" WHERE ID=").append(getWrappedId());
		GlobalEvents.getInstance().fireObjectEvent(this, GlobalEvents.CHANGETYPE.delete);
		GlobalEvents.getInstance().clearSelection(getClass());
		cache.clear();
		return (j.exec(sql.toString())!=0);
	}
	
	/**
	 * Eine zur konkreten Klasse des aufrufenden Objekts passende Query zurückliefern
	 * @return leere Query für die Klasse dieses Objekts.
	 */
	@SuppressWarnings("unchecked")
	public  Query getQuery(){
		return new Query(getClass());
	}
	
	/**
	 * Mehrere Felder auf einmal setzen (Effizienter als einzelnes set)
	 * @param fields die Feldnamen
	 * @param values die Werte
	 * @return false bei Fehler
	 */
	public boolean set(String[] fields, String... values){
		if((fields==null) || (values==null) || (fields.length!=values.length)){
			log.log("Falsche Felddefinition für set",Log.ERRORS);
			return false;
		}
		StringBuffer sql=new StringBuffer(200);
		sql.append("UPDATE ").append(getTableName()).append(" SET ");
		for(int i=0;i<fields.length;i++){
            String mapped=map(fields[i]);
            if(mapped.startsWith("S:")){
                sql.append(mapped.substring(4));
            }else{
                sql.append(mapped);
            }
			sql.append("=?,");
			cache.put(getKey(fields[i]),values[i],getCacheTime());
		}
		sql.delete(sql.length()-1,100000);
		sql.append(" WHERE ID=").append(getWrappedId());
        String cmd=sql.toString();
        PreparedStatement pst=j.prepareStatement(cmd);
        for(int i=0;i<fields.length;i++){
        	encode(i+1,pst,fields[i],values[i]);
        }
        if(tracetable!=null){
        	StringBuffer params = new StringBuffer();
        	params.append("[");
        	params.append(StringTool.join(values, ", "));
        	params.append("]");
            doTrace(cmd + " " + params);
        }
        try{
            pst.executeUpdate();
            return true;
        }catch(Exception ex){
            ExHandler.handle(ex);
            return false;
        }
	}
	
	
	/**
	 * Mehrere Felder auf einmal auslesen
	 * @param fields die Felder
	 * @param values String Array für die gelesenen Werte
	 * @return true ok, values wurden gesetzt
	 */
	public boolean get(String[] fields, String[] values)
	{
		if( (fields==null) || (values==null) || (fields.length!=values.length)){
			log.log("Falscher Aufruf von get(String[],String[]",Log.ERRORS);
			return false;
		}
		StringBuffer sql=new StringBuffer(200);
		sql.append("SELECT ");
		boolean[] decode=new boolean[fields.length];
		for(int i=0;i<fields.length;i++){
			String key=getKey(fields[i]);
			Object ret=cache.get(key);
			if(ret instanceof String){
				values[i]=(String)ret;
			}else{
				String f1=map(fields[i]);
				if(f1.startsWith("S:")){
					sql.append(f1.substring(4));
					decode[i]=true;
				}else{
					sql.append(f1);
				}
				sql.append(",");
			}
		}
		if(sql.length()<8){
			return true;
		}
		sql.delete(sql.length()-1,1000);
		sql.append(" FROM ").append(getTableName()).append(" WHERE ID=")
		.append(getWrappedId());
		Stm stm=j.getStatement();
		ResultSet res=stm.query(sql.toString());
		try{
			if((res!=null) && res.next()){
				for(int i=0;i<values.length;i++){
					if(values[i]==null){
						if(decode[i]==true){
							values[i]=decode(fields[i],res);
						}else{
							values[i]=checkNull(res.getString(map(fields[i])));	
						}
						cache.put(getKey(fields[i]), values[i], getCacheTime());
					}
				}
				
			}
			return true;
		}catch(Exception ex){
			ExHandler.handle(ex);
			return false;
		}finally{
			j.releaseStatement(stm);
		}
		
	}
    private String decode(String field, ResultSet rs){
        
        try{
            String mapped=map(field);
            if(mapped.startsWith("S:")){
                char mode=mapped.charAt(2);
                switch(mode){
                case 'D':
                    String dat=rs.getString(mapped.substring(4));
                    if(dat==null){
                        return "";
                    }
                    TimeTool t=new TimeTool();
                    if(t.set(dat)==true){
                        return t.toString(TimeTool.DATE_GER);
                    }else{
                        return "";
                    }
                
                case 'C':
                        InputStream is=rs.getBinaryStream(mapped.substring(4));
                        if(is==null){
                            return "";
                        }
                        byte[] exp=CompEx.expand(is);
                        return new String(exp,StringTool.default_charset);
                    
                case 'V':
                	byte[] in=rs.getBytes(mapped.substring(4));
                	VersionedResource vr=VersionedResource.load(in);
                	return vr.getHead();
                }     
            }
        }catch(Exception ex){
            ExHandler.handle(ex);
            log.log("Fehler bei decode ",Log.ERRORS);
        }
        return null;
    }

    private String encode(int num, PreparedStatement pst, String field, String value) {
        String mapped=map(field);
         String ret=value;
        try{
            if(mapped.startsWith("S:")){
                String typ=mapped.substring(2,3);
                mapped=mapped.substring(4);
                byte[] enc;
                
                if(typ.startsWith("D")){    // datum
                    TimeTool t=new TimeTool();
                    if((!StringTool.isNothing(value)) && (t.set(value)==true)){
                    	ret=t.toString(TimeTool.DATE_COMPACT);
                        pst.setString(num,ret);
                    }else{
                        ret="";
                        pst.setString(num,"");
                    }
                    
                }else if(typ.startsWith("C")){  // string enocding
                	enc=CompEx.Compress(value,CompEx.ZIP);
                    pst.setBytes(num,enc);
                }else{
                    log.log("Unbekannter encode code "+typ,Log.ERRORS);
                }
            }else{
                pst.setString(num,value);
            }
        }catch(Throwable ex){
            ExHandler.handle(ex);
            log.log("Fehler beim String encoder: "+ex.getMessage(),Log.ERRORS);
        }
        return ret;
    }
    
    public static final int MATCH_EXACT=0;
    public static final int MATCH_LIKE=1;
    public static final int MATCH_REGEXP=2;
    /**
     * Testet ob zwei Objekte bezüglich definierbarer Felder übereinstimmend sind
     * @param other anderes Objekt
     * @param mode gleich, LIKE oder Regexp
     * @param fields die interessierenden Felder
     * @return true wenn this und other vom selben typ sind und alle interessierenden
     * Felder genäss mode übereinstimmen.
     */
    public boolean isMatching(PersistentObject other,int mode, String... fields){
        if(getClass().equals(other.getClass())){
            String[] others=new String[fields.length];
            other.get(fields,others);
            return isMatching(fields,mode,others);
        }
        return false;
    }
    /**
     * testet, ob die angegebenen Felder den angegebenen Werten entsprechen.
     * @param fields die zu testenden Felde
     * @param mode Testmodus (MATCH_EXACT, MATCH_LIKE oder MATCH_REGEXP)
     * @param others die Vergleichswerte
     * @return true bei übereinsteimmung
     */
    public boolean isMatching(String[] fields, int mode, String... others){
        String[] mine=new String[fields.length];
        get(fields,mine);

        for(int i=0;i<fields.length;i++){
        	if(mine[i]==null){
        		if(others[i]==null){
        			return true;
        		}
        		return false;
        	}
        	if(others[i]==null){
        		return false;
        	}
            switch(mode){
            case MATCH_EXACT:
                if(!mine[i].toLowerCase().equals(others[i].toLowerCase())){
                    return false;
                }
                break;
            case MATCH_LIKE:
                if(!mine[i].toLowerCase().startsWith(others[i].toLowerCase())){
                    return false;
                }
                break;
            case MATCH_REGEXP:
                if(!mine[i].matches(others[i])){
                    return false;
                }
            }
            
        }
        return true; 
    }
    /**
     * Eine Transaktion beginnen. schreiboperationen müssen auf das zurückgelieferte Transactions-Objekt erfolgen.
     * (Und können mit Schreiboperationen ausserhalb der Transaktion konkurrieren)
     * @return Ein Transaktionsobjekt, über das Schreiboperationen getätigt werden kann, und das am Ende mit commit()
     * oder rollback() ausgeführt resp. gestoppt werden kann.
     */
    public Transaction begin(){
        return new Transaction(this);
    }
    
    /**
     * Get a unique key for a value, suitable for identifying a key in a cache.
     * The current implementation uses the table name, the id of the PersistentObject
     * and the field name.
     * 
     * @param field the field to get a key for
     * @return a unique key
     */
    private String getKey(String field){
        StringBuffer key=new StringBuffer();
        
        key.append(getTableName());
        key.append(".");
        key.append(getId());
        key.append("#");
        key.append(field);
        
        return key.toString();
    }
    
    /**
     * Verbindung zur Datenbank trennen
     *
     */
	public static void disconnect() {
		if(j!=null){
            if(j.DBFlavor.startsWith("hsqldb")){
                j.exec("SHUTDOWN COMPACT");
            }
            j.disconnect();
    		j=null;
    		log.log("Verbindung zur Datenbank getrennt.",Log.INFOS);
    		cache.stat();
		}
	}
	/*
	private class CacheField implements ICacheable{
	    Object contents;
        long expires;
        CacheField(Object value){
            contents=value;
            expires=System.currentTimeMillis()+lifetime;
        }
        boolean expired(){
        	long act=System.currentTimeMillis();
        	if(expires<act){
        		return true;
        	}
        	return false;
        }
        
    }
	 */

	@Override
	public boolean equals(Object arg0) {
		if(arg0 instanceof PersistentObject){
			return getId().equals(((PersistentObject)arg0).getId());
		}
		return false;
	}
	
	public static String checkNull(String in){
		return in==null ? "" : in;
	}
	 
	public static int checkZero(String in){
		if(StringTool.isNothing(in)){
			return 0;
		}
		try {
			return Integer.parseInt(in.trim());
		} catch (NumberFormatException ex) {
			ExHandler.handle(ex);
			return 0;
		}
	}
	public static double checkZeroDouble(String in){
		if(StringTool.isNothing(in)){
			return 0.0;
		}
		try {
			return Double.parseDouble(in.trim());
		} catch (NumberFormatException ex) {
			ExHandler.handle(ex);
			return 0.0;
		}
	}
	@Override
	public int hashCode() {
		return getId().hashCode();
	}
	public static void clearCache(){
		synchronized(cache){
			cache.clear();
		}
		System.gc();
	}
	/**
	 * Return time-to-live in cache for this object
	 * @return the time in seconds
	 */
	public int getCacheTime(){
		return default_lifetime;
	}

}
