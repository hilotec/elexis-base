/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: ArtikelDetailDialog.java 5970 2010-01-27 16:43:04Z rgw_ch $
 *******************************************************************************/

package ch.elexis.dialogs;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.data.Artikel;
import ch.elexis.data.PersistentObject;
import ch.elexis.util.LabeledInputField;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.LabeledInputField.AutoForm;
import ch.elexis.views.artikel.Artikeldetail;

public class ArtikelDetailDialog extends TitleAreaDialog {
	Artikel art;
	
	public ArtikelDetailDialog(Shell shell, PersistentObject o){
		super(shell);
		art = (Artikel) o;
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		ScrolledComposite ret = new ScrolledComposite(parent, SWT.V_SCROLL);
		Composite cnt = new Composite(ret, SWT.NONE);
		ret.setContent(cnt);
		ret.setExpandHorizontal(true);
		ret.setExpandVertical(true);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		cnt.setLayout(new FillLayout());
		AutoForm tblArtikel =
			new LabeledInputField.AutoForm(cnt, Artikeldetail.getFieldDefs(parent.getShell()));
		tblArtikel.reload(art);
		ret.setMinSize(cnt.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return ret;
	}
	
	@Override
	protected Point getInitialSize(){
		Point orig = super.getInitialSize();
		orig.y += orig.y >> 2;
		return orig;
	}
	
	@Override
	public void create(){
		setShellStyle(getShellStyle() | SWT.RESIZE);
		super.create();
		getShell().setText(Messages.getString("ArtikelDetailDialog.articleDetail")); //$NON-NLS-1$
		setTitle(art.getLabel());
		setMessage(Messages.getString("ArtikelDetailDialog.enterArticleDetails")); //$NON-NLS-1$
	}
	
	@Override
	protected void okPressed(){
		ElexisEventDispatcher.reload(Artikel.class);
		super.okPressed();
	}
	
}