/*******************************************************************************
 * Copyright (c) 2007-2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 *  $Id: ACLContributor.java 5905 2009-12-26 17:29:29Z rgw_ch $
 *******************************************************************************/
package ch.elexis.admin;

import static ch.elexis.admin.AccessControlDefaults.ACCOUNTING_BILLCREATE;
import static ch.elexis.admin.AccessControlDefaults.ACCOUNTING_BILLMODIFY;
import static ch.elexis.admin.AccessControlDefaults.ACCOUNTING_GLOBAL;
import static ch.elexis.admin.AccessControlDefaults.ACCOUNTING_READ;
import static ch.elexis.admin.AccessControlDefaults.ACCOUNTING_STATS;
import static ch.elexis.admin.AccessControlDefaults.ACL_USERS;
import static ch.elexis.admin.AccessControlDefaults.AC_ABOUT;
import static ch.elexis.admin.AccessControlDefaults.AC_CHANGEMANDANT;
import static ch.elexis.admin.AccessControlDefaults.AC_CONNECT;
import static ch.elexis.admin.AccessControlDefaults.AC_EXIT;
import static ch.elexis.admin.AccessControlDefaults.AC_HELP;
import static ch.elexis.admin.AccessControlDefaults.AC_IMORT;
import static ch.elexis.admin.AccessControlDefaults.AC_LOGIN;
import static ch.elexis.admin.AccessControlDefaults.AC_PREFS;
import static ch.elexis.admin.AccessControlDefaults.AC_PURGE;
import static ch.elexis.admin.AccessControlDefaults.AC_SHOWPERSPECTIVE;
import static ch.elexis.admin.AccessControlDefaults.AC_SHOWVIEW;
import static ch.elexis.admin.AccessControlDefaults.ADMIN_ACE;
import static ch.elexis.admin.AccessControlDefaults.ADMIN_CHANGE_BILLSTATUS_MANUALLY;
import static ch.elexis.admin.AccessControlDefaults.ADMIN_KONS_EDIT_IF_BILLED;
import static ch.elexis.admin.AccessControlDefaults.ADMIN_VIEW_ALL_REMINDERS;
import static ch.elexis.admin.AccessControlDefaults.CASE_MODIFY;
import static ch.elexis.admin.AccessControlDefaults.DATA;
import static ch.elexis.admin.AccessControlDefaults.DELETE;
import static ch.elexis.admin.AccessControlDefaults.DELETE_FORCED;
import static ch.elexis.admin.AccessControlDefaults.DELETE_LABITEMS;
import static ch.elexis.admin.AccessControlDefaults.DELETE_MEDICATION;
import static ch.elexis.admin.AccessControlDefaults.DOCUMENT;
import static ch.elexis.admin.AccessControlDefaults.DOCUMENT_CREATE;
import static ch.elexis.admin.AccessControlDefaults.DOCUMENT_SYSTEMPLATE;
import static ch.elexis.admin.AccessControlDefaults.DOCUMENT_TEMPLATE;
import static ch.elexis.admin.AccessControlDefaults.KONS_CREATE;
import static ch.elexis.admin.AccessControlDefaults.KONS_DELETE;
import static ch.elexis.admin.AccessControlDefaults.KONS_EDIT;
import static ch.elexis.admin.AccessControlDefaults.KONS_REASSIGN;
import static ch.elexis.admin.AccessControlDefaults.KONTAKT;
import static ch.elexis.admin.AccessControlDefaults.KONTAKT_DELETE;
import static ch.elexis.admin.AccessControlDefaults.KONTAKT_DISPLAY;
import static ch.elexis.admin.AccessControlDefaults.KONTAKT_ETIKETTE;
import static ch.elexis.admin.AccessControlDefaults.KONTAKT_EXPORT;
import static ch.elexis.admin.AccessControlDefaults.KONTAKT_INSERT;
import static ch.elexis.admin.AccessControlDefaults.KONTAKT_MODIFY;
import static ch.elexis.admin.AccessControlDefaults.LAB_SEEN;
import static ch.elexis.admin.AccessControlDefaults.LSTG_VERRECHNEN;
import static ch.elexis.admin.AccessControlDefaults.MEDICATION_MODIFY;
import static ch.elexis.admin.AccessControlDefaults.PATIENT;
import static ch.elexis.admin.AccessControlDefaults.PATIENT_DISPLAY;
import static ch.elexis.admin.AccessControlDefaults.PATIENT_INSERT;
import static ch.elexis.admin.AccessControlDefaults.PATIENT_MODIFY;
import static ch.elexis.admin.AccessControlDefaults.SCRIPT_EDIT;
import static ch.elexis.admin.AccessControlDefaults.SCRIPT_EXECUTE;

/**
 * Contribution of the basic system's ACLs
 * 
 * @author gerry
 * 
 */
public class ACLContributor implements IACLContributor {
	private final ACE[] acls =
		new ACE[] {
		ACCOUNTING_GLOBAL, ADMIN_ACE, ACCOUNTING_BILLCREATE, ACCOUNTING_BILLMODIFY,
		ACCOUNTING_READ, ACCOUNTING_STATS, ACL_USERS, DATA, KONTAKT, PATIENT, KONTAKT_DELETE,
		DELETE, DELETE_FORCED, KONTAKT_DISPLAY, KONTAKT_INSERT, KONTAKT_MODIFY, KONTAKT_EXPORT,
		KONTAKT_ETIKETTE, PATIENT_DISPLAY, PATIENT_INSERT, PATIENT_MODIFY, LAB_SEEN,
		LSTG_VERRECHNEN, KONS_CREATE, KONS_DELETE, KONS_EDIT, KONS_REASSIGN, AC_ABOUT,
		AC_CHANGEMANDANT, AC_CONNECT, AC_EXIT, AC_HELP, AC_IMORT, AC_LOGIN, AC_PREFS, AC_PURGE,
		AC_SHOWPERSPECTIVE, AC_SHOWVIEW, DOCUMENT, DOCUMENT_CREATE, DOCUMENT_SYSTEMPLATE,
		DOCUMENT_TEMPLATE, ADMIN_CHANGE_BILLSTATUS_MANUALLY, ADMIN_KONS_EDIT_IF_BILLED,
		ADMIN_VIEW_ALL_REMINDERS, MEDICATION_MODIFY, DELETE_MEDICATION, DELETE_LABITEMS,
		CASE_MODIFY, SCRIPT_EXECUTE, SCRIPT_EDIT
		
	};
	
	public ACE[] getACL(){
		return acls;
	}
	
	public ACE[] reject(final ACE[] acl){
		// TODO Management of collisions
		return null;
	}
	
}
