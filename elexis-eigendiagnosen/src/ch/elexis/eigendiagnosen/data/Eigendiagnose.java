/*******************************************************************************
 * Copyright (c) 2007, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *    $Id: Eigendiagnose.java 2881 2007-07-23 19:10:44Z rgw_ch $
 *******************************************************************************/
package ch.elexis.eigendiagnosen.data;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.jface.dialogs.MessageDialog;

import ch.elexis.data.PersistentObject;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.VersionInfo;

public class Eigendiagnose extends PersistentObject {
	static final String VERSION="0.1.0";
	static final String TABLENAME="CH_ELEXIS_EIGENDIAGNOSEN";
	private static final String createDB="CREATE TABLE "+TABLENAME+"("
		+"ID			VARCHAR(25) primary key,"	// must always be present
		+"deleted		CHAR(1) default '0',"		// must always be present
		+"parent		VARCHAR(20),"
		+"code			VARCHAR(20),"
		+"title			VARCHAR(80),"
		+"comment		TEXT,"
		+"ExtInfo		BLOB);"
		+"CREATE INDEX "+TABLENAME+"_idx1 on "+TABLENAME+"(parent,code);"
		+"INSERT INTO "+TABLENAME+" (ID,title) VALUES ('VERSION','"+VERSION+"');";
	
	/** 
	 * Here we define the mapping between internal fieldnames and database fieldnames. (@see PersistentObject)
	 * then we try to load a version element. If this does not exist, we create the table. If it exists, we
	 * check the version
	 */
	static{
		addMapping(TABLENAME, "parent", "Text=title","Kuerzel=code","Kommentar=comment","ExtInfo");
		Eigendiagnose check=load("VERSION");
		if(check.state()<PersistentObject.DELETED){		// Object never existed, so we have to create the database
			createTable();
		}else{	// found existing table, check version
			VersionInfo v=new VersionInfo(check.get("Text"));
			if(v.isOlder(VERSION)){
				SWTHelper.showError("Eigendiagnise: Falsche Version", "Die Datenbank hat eine zu alte Version dieser Tabelle");
				
			}
		}
		
	}
	
	static void createTable(){
		ByteArrayInputStream bais;
		try {
			bais = new ByteArrayInputStream(createDB.getBytes("UTF-8"));
			if(j.execScript(bais,true,false)==false){
				MessageDialog.openError(null,"Datenbank-Fehler","Konnte Tabelle nicht erstellen");
			}
		} catch (UnsupportedEncodingException e) {
			// should really never happen
			e.printStackTrace();
		}
	}
	@Override
	public String getLabel() {
		return get("code")+" "+get("title");
	}

	@Override
	protected String getTableName() {
		return TABLENAME;
	}
	
	public static Eigendiagnose load(String id){
		return new Eigendiagnose(id);
	}
	protected Eigendiagnose(String id){
		super(id);
	}
	protected Eigendiagnose(){}
}
