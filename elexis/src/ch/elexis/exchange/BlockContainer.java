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
 *  $Id: BlockContainer.java 5875 2009-12-18 06:26:22Z rgw_ch $
 *******************************************************************************/
package ch.elexis.exchange;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Leistungsblock;
import ch.elexis.data.PersistentObject;
import ch.elexis.exchange.elements.ServiceBlockElement;
import ch.elexis.exchange.elements.ServiceBlocksElement;
import ch.elexis.exchange.elements.XChangeElement;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.Result;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;
import ch.rgw.tools.XMLTool;
import ch.rgw.tools.Result.SEVERITY;

public class BlockContainer extends XChangeContainer {
	Document doc;
	Element eRoot;
	ServiceBlocksElement lbs;
	
	public BlockContainer(){
		doc = new Document();
		eRoot = new Element(ROOT_ELEMENT, ns);
		eRoot.addNamespaceDeclaration(nsxsi);
		eRoot.addNamespaceDeclaration(nsschema);
		eRoot.setAttribute("timestamp", new TimeTool().toString(TimeTool.DATETIME_XML)); //$NON-NLS-1$
		eRoot.setAttribute("id", XMLTool.idToXMLID(StringTool.unique("xChange"))); //$NON-NLS-1$ //$NON-NLS-2$
		eRoot.setAttribute("origin", XMLTool.idToXMLID(Hub.actMandant.getId())); //$NON-NLS-1$
		eRoot.setAttribute("destination", "undefined"); //$NON-NLS-1$ //$NON-NLS-2$
		eRoot.setAttribute("responsible", XMLTool.idToXMLID(Hub.actMandant.getId())); //$NON-NLS-1$
		doc.setRootElement(eRoot);
		lbs = new ServiceBlocksElement(this, null);
		eRoot.addContent(lbs.getElement());
	}
	
	public BlockContainer(InputStream is){
		SAXBuilder builder = new SAXBuilder();
		try {
			doc = builder.build(is);
			eRoot = doc.getRootElement();
		} catch (Exception e) {
			ExHandler.handle(e);
		}
		
	}
	
	
	
	public boolean canHandle(Class<? extends PersistentObject> clazz){
		if (clazz.equals(Leistungsblock.class)) {
			return true;
		}
		return false;
	}
	
	public void finalizeExport() throws XChangeException{
		FileDialog fd = new FileDialog(Desk.getTopShell(), SWT.SAVE);
		fd.setText(Messages.BlockContainer_Blockbeschreibung);
		fd.setFilterExtensions(new String[] {
			"*.xchange" //$NON-NLS-1$
		});
		fd.setFilterNames(new String[] {
			Messages.BlockContainer_xchangefiles
		});
		String filename = fd.open();
		if (filename != null) {
			Format format = Format.getPrettyFormat();
			format.setEncoding("utf-8"); //$NON-NLS-1$
			XMLOutputter xmlo = new XMLOutputter(format);
			String xmlAspect = xmlo.outputString(doc);
			try {
				FileOutputStream fos = new FileOutputStream(filename);
				fos.write(xmlAspect.getBytes());
				fos.close();
			} catch (Exception ex) {
				ExHandler.handle(ex);
				throw new XChangeException("Output failed "+ex.getMessage());
			}
		}
	
	}
	
	public XChangeElement store(Object output) throws XChangeException{
		if (output instanceof Leistungsblock) {
			ServiceBlockElement sbe = new ServiceBlockElement(this, (Leistungsblock) output);
			lbs.add(sbe);
			return sbe;
		}
		throw new XChangeException("Can't handle object type "+output.getClass().getName());
	}
	
	public Result<?> finalizeImport(){
		if (eRoot != null) {
			ServiceBlocksElement eBlocks =
				new ServiceBlocksElement(this, eRoot.getChild(ServiceBlockElement.ENCLOSING,
					XChangeContainer.ns));
			if (eBlocks != null) {
				List<ServiceBlockElement> lBlocks =
					(List<ServiceBlockElement>) eBlocks.getChildren(ServiceBlockElement.XMLNAME,
						ServiceBlockElement.class);
				for (ServiceBlockElement eBlock : lBlocks) {
					eBlock.doImport();
				}
			}
		}
		return new Result<String>("OK"); //$NON-NLS-1$
	}
	
	public Result<Object> load(Element input, Object context){
		// TODO Auto-generated method stub
		return null;
	}
	
}