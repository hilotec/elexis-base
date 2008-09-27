/*******************************************************************************
 * Copyright (c) 2006-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: DecoratedStringChooser.java 4450 2008-09-27 19:49:01Z rgw_ch $
 *******************************************************************************/

package ch.elexis.preferences.inputs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import ch.elexis.Desk;
import ch.elexis.util.DecoratedString;
import ch.elexis.util.SWTHelper;
import ch.rgw.io.Settings;

/**
 * Ein Preference-Element zum EInstellen eines DecoratedStrings (Text mit Farbe
 * und Icon)
 * 
 * @author gerry
 * 
 */
public class DecoratedStringChooser extends Composite {

	public DecoratedStringChooser(Composite parent, final Settings cfg,
			final DecoratedString[] strings) {
		super(parent, SWT.BORDER);

		int num = strings.length;
		int typRows = ((int) Math.sqrt(num));
		int typCols = typRows + (num - (typRows * typRows));
		if (typCols < 4) {
			typCols = 4;
		}
		setLayout(new GridLayout(typCols, true));
		Label expl = new Label(this, SWT.WRAP);
		expl
				.setText("Mit Doppelklick auf eines der Felder können Sie Änderungen vornehmen");
		expl.setLayoutData(SWTHelper.getFillGridData(typCols, false, 1, false));
		for (int i = 0; i < num; i++) {
			Label lab = new Label(this, SWT.NONE);
			lab.setText(strings[i].getText());
			String coldesc = cfg.get(strings[i].getText(), "FFFFFF");
			Color background = Desk.getColorFromRGB(coldesc);
			lab.setBackground(background);
			GridData gd = new GridData(GridData.FILL_BOTH);
			lab.setLayoutData(gd);
			lab.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseDoubleClick(MouseEvent e) {
					ColorDialog cd = new ColorDialog(getShell());
					Label l = (Label) e.getSource();
					RGB selected = cd.open();
					if (selected != null) {
						String symbolic = Desk.createColor(selected);
						l.setBackground(Desk.getColorFromRGB(symbolic));
						cfg.set(l.getText(), symbolic);
					}
				}

			});
		}
	}
}
