/*******************************************************************************
 * Copyright (c) 2005-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 * $Id: SWTHelper.java 4743 2008-12-04 21:37:02Z rgw_ch $
 *******************************************************************************/

package ch.elexis.util;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

import ch.elexis.Desk;
import ch.rgw.tools.StringTool;

/** statische Hilfsfunktionen für SWT-Objekte */
public class SWTHelper {
	/**
	 * Singleton-Variable, die einen FocusListener enthaelt, der in einem Text-Control den Text
	 * selektiert, wenn das Control den Focus erhaelt. Siehe setSelectOnFocus().
	 */
	private static FocusListener selectOnFocusListener = null;
	private static Log log = Log.get("Global: ");
	
	/** Ein Objekt innerhalb des parents zentrieren */
	public static void center(final Shell parent, final Composite child){
		Rectangle par = parent.getBounds();
		Rectangle ch = child.getBounds();
		int xOff = (par.width - ch.width) / 2;
		int yOff = (par.height - ch.height) / 2;
		child.setBounds(par.x + xOff, par.y + yOff, ch.width, ch.height);
	}
	
	/** Ein Objekt innerhalb des parents zentrieren */
	public static void center(final Shell parent, final Shell child){
		Rectangle par = parent.getBounds();
		Rectangle ch = child.getBounds();
		int xOff = (par.width - ch.width) / 2;
		int yOff = (par.height - ch.height) / 2;
		child.setBounds(par.x + xOff, par.y + yOff, ch.width, ch.height);
	}
	
	/** Ein Objekt auf dem Bildschirm zentrieren */
	public static void center(final Shell child){
		Rectangle par = Desk.getDisplay().getBounds();
		Rectangle ch = child.getBounds();
		int xOff = (par.width - ch.width) / 2;
		int yOff = (par.height - ch.height) / 2;
		child.setBounds(par.x + xOff, par.y + yOff, ch.width, ch.height);
	}
	
	/** Einen Text zentriert in ein Rechteck schreiben */
	public static void writeCentered(final GC gc, final String text, final Rectangle bounds){
		int w = gc.getFontMetrics().getAverageCharWidth();
		int h = gc.getFontMetrics().getHeight();
		int woff = (bounds.width - text.length() * w) >> 1;
		int hoff = (bounds.height - h) >> 1;
		gc.drawString(text, bounds.x + woff, bounds.y + hoff);
	}
	
	/** Eine Alertbox anzeigen (synchron) */
	public static void alert(final String title, final String message){
		if (Desk.getDisplay() == null) {
			Desk.theDisplay = PlatformUI.createDisplay();
		}
		Shell shell = Desk.getDisplay().getActiveShell();
		if (shell == null) {
			shell = new Shell(Desk.getDisplay());
		}
		MessageBox msg = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
		msg.setText(title);
		msg.setMessage(message);
		msg.open();
	}
	
	/**
	 * Eine Standard-Fehlermeldung asynchron im UI-Thread zeigen
	 * 
	 * @param title
	 *            Titel
	 * @param message
	 *            Nachricht
	 */
	public static void showError(final String title, final String message){
		Desk.getDisplay().syncExec(new Runnable() {
			
			public void run(){
				Shell shell = Desk.getTopShell();
				MessageDialog.openError(shell, title, message);
			}
		});
	}
	
	/**
	 * Eine Standard-Fehlermeldung asynchron zeigen und loggen
	 * 
	 * @param title
	 *            Titel
	 * @param message
	 *            Nachricht
	 */
	public static void showError(final String logHeader, final String title, final String message){
		log.log(logHeader + ": " + title + "->" + message, Log.ERRORS);
		Desk.getDisplay().syncExec(new Runnable() {
			public void run(){
				Shell shell = Desk.getDisplay().getActiveShell();
				MessageDialog.openError(shell, title, message);
			}
		});
	}
	
	/**
	 * Eine Standard-Infomeldung asynchron zeigen
	 * 
	 * @param title
	 *            Titel
	 * @param message
	 *            Nachricht
	 */
	public static void showInfo(final String title, final String message){
		Desk.getDisplay().syncExec(new Runnable() {
			
			public void run(){
				Shell shell = Desk.getTopShell();
				MessageDialog.openInformation(shell, title, message);
			}
		});
	}
	
	/**
	 * Eine mit Ja oder Nein zu beantwortende Frage im UI-Thread zeigen
	 * 
	 * @param title
	 *            Titel
	 * @param message
	 *            Nachricht
	 * @return true: User hat Ja geklickt
	 */
	public static boolean askYesNo(final String title, final String message){
		InSync rn = new InSync(title, message);
		Desk.getDisplay().syncExec(rn);
		return rn.ret;
	}
	
	private static class InSync implements Runnable {
		boolean ret;
		String title, message;
		
		InSync(final String title, final String message){
			this.title = title;
			this.message = message;
		}
		
		public void run(){
			Shell shell = Desk.getTopShell();
			ret = MessageDialog.openConfirm(shell, title, message);
		}
		
	}
	
	/**
	 * Ein GridData-Objekt erzeugen, das den horizontalen und/oder vertikalen Freiraum ausfüllt.
	 * 
	 * @param horizontal
	 *            true, wenn horizontal gefüllt werden soll
	 * @param vertical
	 *            true, wenn vertikal gefüllt werden soll.
	 * @return ein neu erzeugtes, direkt verwendbares GridData-Objekt
	 */
	public static GridData getFillGridData(final int hSpan, final boolean hFill, final int vSpan,
		final boolean vFill){
		int ld = 0;
		if (hFill) {
			ld = GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL;
		}
		if (vFill) {
			ld |= GridData.FILL_VERTICAL | GridData.GRAB_VERTICAL;
		}
		GridData ret = new GridData(ld);
		ret.horizontalSpan = (hSpan < 1) ? 1 : hSpan;
		ret.verticalSpan = vSpan < 1 ? 1 : vSpan;
		return ret;
	}
	
	public static GridData fillGrid(final Composite parent, final int cols){
		parent.setLayout(new GridLayout(cols, false));
		return getFillGridData(1, true, 1, true);
	}
	
	/**
	 * Set a GridData to the given Control that sets the specified height in lines calculated with
	 * the control's current font.
	 * 
	 * @param control
	 *            the control
	 * @param lines
	 *            reuqested height of the control in lines
	 * @param fillHorizontal
	 *            true if the control should require all horizontal space
	 * @return the GridData (that is already set to the control)
	 */
	public static GridData setGridDataHeight(final Control control, final int lines,
		final boolean fillHorizontal){
		int h = Math.round(control.getFont().getFontData()[0].height);
		GridData gd = getFillGridData(1, fillHorizontal, 1, false);
		gd.heightHint = lines * (h + 2);
		control.setLayoutData(gd);
		return gd;
	}
	
	/**
	 * Constructor wrapper for TableWrapLayout, so that parameters are identical to
	 * GridLayout(numColumns, makeColumnsEqualWidth)
	 */
	public static TableWrapLayout createTableWrapLayout(final int numColumns,
		final boolean makeColumnsEqualWidth){
		TableWrapLayout layout = new TableWrapLayout();
		
		layout.numColumns = numColumns;
		layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		
		return layout;
	}
	
	/**
	 * Ein TableWrapDAta-Objekt erzeugen, das den horizontalen und/oder vertikalen Freiraum
	 * ausfüllt.
	 * 
	 * @param horizontal
	 *            true, wenn horizontal gefüllt werden soll
	 * @param vertical
	 *            true, wenn vertikal gefüllt werden soll.
	 * @return ein neu erzeugtes, direkt verwendbares GridData-Objekt
	 */
	public static TableWrapData getFillTableWrapData(final int hSpan, final boolean hFill,
		final int vSpan, final boolean vFill){
		TableWrapData layoutData = new TableWrapData(TableWrapData.LEFT, TableWrapData.TOP);
		
		if (hFill) {
			layoutData.grabHorizontal = true;
			layoutData.align = TableWrapData.FILL;
		}
		if (vFill) {
			layoutData.grabVertical = true;
			layoutData.valign = TableWrapData.FILL;
		}
		
		layoutData.colspan = (hSpan < 1 ? 1 : hSpan);
		layoutData.rowspan = (vSpan < 1 ? 1 : vSpan);
		
		return layoutData;
	}
	
	/**
	 * Return a color that contrasts optimally to the given color
	 * 
	 * @param col
	 *            an SWT Color
	 * @return black if col was rather bright, white if col was rather dark.
	 */
	public static Color getContrast(final Color col){
		double val = col.getRed() * 0.56 + col.getGreen() * 0.33 + col.getBlue() * 0.11;
		if (val <= 110) {
			return Desk.getDisplay().getSystemColor(SWT.COLOR_WHITE);
		}
		return Desk.getDisplay().getSystemColor(SWT.COLOR_BLACK);
	}
	
	/**
	 * Return a Label that acts as a hyperlink
	 * 
	 * @param parent
	 *            parent control
	 * @param text
	 *            text to display
	 * @param lis
	 *            hyperlink listener that is called on Mouse click
	 * @return a Label
	 */
	public static Label createHyperlink(final Composite parent, final String text,
		final IHyperlinkListener lis){
		final Label ret = new Label(parent, SWT.NONE);
		ret.setText(text);
		ret.setForeground(Desk.getColorRegistry().get(Messages.getString("SWTHelper.blue"))); //$NON-NLS-1$
		ret.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e){
				if (lis != null) {
					lis.linkActivated(new HyperlinkEvent(ret, ret, text, e.stateMask));
				}
			}
			
		});
		return ret;
	}
	
	/**
	 * Create a multiline text widget with a specified height in lines (calculated with the Text's
	 * default font)
	 * 
	 * @param parent
	 *            parent composite
	 * @param lines
	 *            requested height of the text field
	 * @param flags
	 *            creation flags (SWT.MULTI and SWT.WRAP are added automatocally)
	 * @return a Text control
	 */
	public static Text createText(final Composite parent, final int lines, final int flags){
		int lNum = SWT.SINGLE;
		if (lines > 1) {
			lNum = SWT.MULTI | SWT.WRAP;
		}
		Text ret = new Text(parent, SWT.BORDER | flags | lNum);
		GridData gd = getFillGridData(1, true, 1, false);
		int h = Math.round(ret.getFont().getFontData()[0].height);
		gd.minimumHeight = (lines + 1) * (h + 2);
		gd.heightHint = gd.minimumHeight;
		ret.setLayoutData(gd);
		return ret;
	}
	
	public static Text createText(final FormToolkit tk, final Composite parent, final int lines,
		final int flags){
		int lNum = SWT.SINGLE;
		if (lines > 1) {
			lNum = SWT.MULTI | SWT.WRAP;
		}
		Text ret = tk.createText(parent, "", lNum | flags | SWT.BORDER);
		GridData gd = getFillGridData(1, true, 1, true);
		int h = Math.round(ret.getFont().getFontData()[0].height);
		gd.minimumHeight = (lines + 1) * (h + 2);
		gd.heightHint = gd.minimumHeight;
		ret.setLayoutData(gd);
		return ret;
	}
	
	public static LabeledInputField createLabeledField(final Composite parent, final String label,
		final LabeledInputField.Typ typ){
		LabeledInputField ret = new LabeledInputField(parent, label, typ);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		return ret;
	}
	
	/**
	 * Check whether the String is empty and give an error message if so
	 * 
	 * @param test
	 *            the String to test
	 * @param name
	 *            the name for the String
	 * @return false if it was empty
	 */
	public static boolean blameEmptyString(final String test, final String name){
		if (StringTool.isNothing(test)) {
			showError("Falscher Parameter", name + " hat keinen gültigen Inhalt");
			return false;
		}
		return true;
	}
	
	/**
	 * Adds a FocusListener to <code>text</code> so that the text is selected as soon as the
	 * control gets the focus. The selection is cleared when the focus is lost.
	 * 
	 * @param text
	 *            the Text control to add a focus listener to
	 */
	public static void setSelectOnFocus(final Text text){
		if (selectOnFocusListener == null) {
			selectOnFocusListener = new FocusListener() {
				public void focusGained(final FocusEvent e){
					Text t = (Text) e.widget;
					t.selectAll();
				}
				
				public void focusLost(final FocusEvent e){
					Text t = (Text) e.widget;
					if (t.getSelectionCount() > 0) {
						t.clearSelection();
					}
				}
			};
		}
		
		text.addFocusListener(selectOnFocusListener);
	}
	
	public static class SimpleDialog extends Dialog {
		IControlProvider dialogAreaProvider;
		
		public SimpleDialog(final IControlProvider control){
			super(Desk.getTopShell());
			dialogAreaProvider = control;
		}
		
		@Override
		protected Control createDialogArea(final Composite parent){
			return dialogAreaProvider.getControl(parent);
		}
		
		@Override
		protected void okPressed(){
			dialogAreaProvider.beforeClosing();
			super.okPressed();
		}
		
	}
	
	public interface IControlProvider {
		public Control getControl(Composite parent);
		
		public void beforeClosing();
	}
	
	public static java.awt.Font createAWTFontFromSWTFont(final Font swtFont){
		String name = swtFont.getFontData()[0].getName();
		int style = swtFont.getFontData()[0].getStyle();
		int height = swtFont.getFontData()[0].getHeight();
		java.awt.Font awtFont = new java.awt.Font(name, style, height);
		return awtFont;
	}
	
	public static int size(final Rectangle r){
		if (r == null) {
			return 0;
		}
		return (r.width - r.x) * (r.height - r.y);
	}
	
	public static Point getStringBounds(Composite c, String s){
		GC gc = new GC(c);
		return gc.textExtent(s);
	}
}
