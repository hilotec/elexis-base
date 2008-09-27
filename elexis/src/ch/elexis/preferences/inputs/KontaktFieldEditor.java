/*******************************************************************************
 * Copyright (c) 2007-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: KontaktFieldEditor.java 4450 2008-09-27 19:49:01Z rgw_ch $
 *******************************************************************************/

package ch.elexis.preferences.inputs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import ch.elexis.Desk;
import ch.elexis.data.Kontakt;
import ch.elexis.dialogs.KontaktSelektor;
import ch.rgw.io.Settings;

public class KontaktFieldEditor extends FieldEditor {
	private Label contactLabel;
	private Settings cfg;
	private String defaultText = "[Bitte auswählen]";
	private Kontakt selected;

	protected KontaktFieldEditor() {
		// no defaults
	}

	public KontaktFieldEditor(Settings cfg, String name, String labelText,
			Composite parent) {
		super(name, labelText, parent);
		this.cfg = cfg;
	}

	@Override
	protected void adjustForNumColumns(int numColumns) {
		((GridData) contactLabel.getLayoutData()).horizontalSpan = numColumns - 1;

	}

	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		Control control = getLabelControl(parent);
		GridData gd = new GridData();
		gd.horizontalSpan = numColumns - 1;
		control.setLayoutData(gd);

		Label cLabel = getChangeControl(parent);
		cLabel.setLayoutData(new GridData());

	}

	@Override
	protected void doLoad() {
		if (contactLabel == null) {
			return;
		}
		selected = Kontakt.load(cfg.get(getPreferenceName(), ""));
		if (selected.isValid()) {
			contactLabel.setText(selected.getLabel());
		} else {
			contactLabel.setText(defaultText);
			selected = null;
		}
	}

	@Override
	protected void doLoadDefault() {
		contactLabel.setText(defaultText);
		selected = null;
	}

	@Override
	protected void doStore() {
		if (selected == null) {
			cfg.remove(getPreferenceName());
		} else {
			cfg.set(getPreferenceName(), selected.getId());
		}

	}

	@Override
	public int getNumberOfControls() {
		return 2;
	}

	protected Label getChangeControl(final Composite parent) {
		if (contactLabel == null) {
			contactLabel = new Label(parent, SWT.NONE);
			contactLabel
					.setForeground(Desk.theColorRegistry.get(Desk.COL_BLUE));
			contactLabel.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseUp(MouseEvent e) {
					KontaktSelektor ksl = new KontaktSelektor(
							parent.getShell(), Kontakt.class, "Kontakt wählen",
							"Bitte wählen Sie den gewünschten Kontakt");
					if (ksl.open() == Dialog.OK) {
						selected = (Kontakt) ksl.getSelection();
						contactLabel.setText(selected.getLabel());
					} else {
						contactLabel.setText(defaultText);
						selected = null;
					}
				}

			});

		} else {
			checkParent(contactLabel, parent);
		}
		return contactLabel;
	}

}
