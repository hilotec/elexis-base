/*******************************************************************************
 * Copyright (c) 2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 *    $Id: SampleDataType.java 6076 2010-02-04 20:46:19Z rgw_ch $
 *******************************************************************************/

package ch.elexis.developer.resources;

import ch.elexis.data.PersistentObject;
import ch.rgw.tools.VersionInfo;

/**
 * This is an example on how to derive your own type from PersistentObject and make it persisten
 * @author gerry
 *
 */
public class SampleDataType extends PersistentObject {
	static final String VERSION="1.0.0";
	/** 
	 * The Name of the Table objects of this class will reside in. If a plugin creates its
	 * own table, the name MUST begin with the plugin ID to avoid name clashes. Note that dots
	 * must be replaced by underscores due to naming restrictions of the database engines.
	 */
	static final String TABLENAME="ch_elexis_developer_resources_sampletable";
	
	/** Definition of the database table */
	static final String createDB="CREATE TABLE "+TABLENAME+"("+
	"ID				VARCHAR(25) primary key,"+		// This field must always be present
	"lastupdate		BIGINT,"+						// This field must always be present
	"deleted		CHAR(1) default '0',"+			// This field must always be present
	"Title          VARCHAR(50),"+
	"FunFactor		VARCHAR(6),"+					// No numeric fields
	"BoreFactor		VARCHAR(6),"+
	"Date			CHAR(8),"+						// use always this for dates
	"Remarks		TEXT,"+
	"FunnyStuff		BLOB);"+
	"CREATE INDEX "+TABLENAME+"idx1 on "+TABLENAME+" (FunFactor);"+
	"insert into "+TABLENAME+" (ID,Title) VALUES (VERSION,"+VERSION+");";
	
	/**
	 * In the static initializer we construct the table mappings and create or update the table
	 */
	static{
		addMapping(TABLENAME, "Title","Fun=FunFactor","Bore=BoreFactor","Date=S:D:Date","Remarks","FunnyStuff");
		SampleDataType version=load("VERSION");
		VersionInfo vi=new VersionInfo(version.get("Title"));
		if(vi.isOlder(VERSION)){
			// we should update
			// And then set the new version 
			version.set("Title", VERSION);
		}
	}
	/**
	 * This should return a human readable short description of this object
	 */
	@Override
	public String getLabel() {
		StringBuilder sb=new StringBuilder();
		synchronized(sb){
			sb.append(get("Title")).append(" has fun:").append(get("Fun"))
			.append(" and is bored with factor ").append(get("Bore"));
		}
		return sb.toString();
	}

	/**
	 * This static method should always be defined. We need this to retrieve PersistentObjects from the Database
	 * @param id
	 * @return
	 */
	public static SampleDataType load(String id){
		return new SampleDataType(id);
	}
	/**
	 * This must return the name of the Table this class will reside in. This may be an existent table
	 * or one specificallym created by this plugin.
	 */
	@Override
	protected String getTableName() {
		return TABLENAME;
	}

	/**
	 * The constructor with a String parameter must be present
	 * @param id
	 */
	protected SampleDataType(String id){
		super(id);
	}
	/**
	 * The default constructor must be present
	 */
	public SampleDataType() {
	}
}
