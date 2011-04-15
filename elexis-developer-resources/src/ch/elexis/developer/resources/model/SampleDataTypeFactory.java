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
 *    $Id: SampleDataTypeFactory.java 6119 2010-02-12 06:16:14Z rgw_ch $
 *******************************************************************************/

package ch.elexis.developer.resources.model;

import java.lang.reflect.Method;

import ch.elexis.data.PersistentObject;
import ch.elexis.data.PersistentObjectFactory;

/**
 * A PersistentObjectFactory is a class that can create instances of a subclass of PersistentObject.
 * It can either retrieve an object from a string representation or create a volatile template. A
 * Factory is necessary, when elexis needs to transfer instances of a class via drag&drop or load
 * instances from the database. Elexis cannot access the object's constructor by itself due to
 * classpath limitations of eclipse. The design of such a factory is quite simple and can almost
 * always just be copied from here.
 * 
 * Note: This class must be declared in the ExtensionPoint ch.elexis.PersistentReference (see
 * plugin.xml)
 */
public class SampleDataTypeFactory extends PersistentObjectFactory {
	public PersistentObject createFromString(String code){
		try {
			String[] ci = code.split("::"); //$NON-NLS-1$
			Class<?> clazz = Class.forName(ci[0]);
			Method load = clazz.getMethod("load", new Class[] { String.class}); //$NON-NLS-1$
			return (PersistentObject) (load.invoke(null, new Object[] {
				ci[1]
			}));
		} catch (Exception ex) {
			// If we can not create the object, we can just return null, so the
			// framwerk will try
			// the following PersistenObjectFactory
			return null;
		}
	}
	
	/**
	 * ¨ create a template of an instance of a given class. A template is an instance that is not
	 * stored in the database.
	 */
	@Override
	public PersistentObject doCreateTemplate(Class typ){
		try {
			return (PersistentObject) typ.newInstance();
		} catch (Exception ex) {
			// ExHandler.handle(ex);
			return null;
		}
	}
}
