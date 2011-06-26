/**
 * (c) 2007-2010 by G. Weirich
 * All rights reserved
 * 
 * Adapted from Viollier to Bioanalytica by Daniel Lutz <danlutz@watz.ch>
 * Important changes:
 * - OpenMedical Library configurable
 * - Easier handling of direct import
 * - Non-unique patients can be assigned to existing patients by user
 *   (instead of creating new patients)
 *   
 * Adapted to Risch by Gerry Weirich
 * Changes:
 * -  Improved detection of Patient ID by evaluation the fields PATIENT_ID and PLACER_ORDER_NUMBER
 * -  Improved matching of Names to the database
 * 
 * 11.1.2008, Gerry Weirich
 * - Re-Adapted to Viollier
 * - Added -allInOne option from OpenMedical
 * 
 * $Id: Importer.java 6046 2010-02-01 18:23:57Z rgw_ch $
 */

package ch.elexis.laborimport.viollier;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ch.elexis.Hub;
import ch.elexis.importers.HL7Parser;
import ch.elexis.util.ImporterPage;
import ch.elexis.util.Messages;
import ch.elexis.util.ResultAdapter;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.Result;
import ch.rgw.tools.Result.SEVERITY;

public class Importer extends ImporterPage {
	public static final String MY_LAB = "Viollier";
	public static final String PLUGIN_ID = "ch.elexis.laborimport_viollier";
	
	private static final String OPENMEDICAL_MAINCLASS = "ch.openmedical.JMedTransfer.JMedTransfer";
	
	private HL7Parser hlp = new HL7Parser(MY_LAB);
	
	// importer type
	private static final int FILE = 1;
	private static final int DIRECT = 2;
	
	private Object openmedicalObject = null;
	private Method openmedicalDownloadMethod = null;
	
	public Importer(){}
	
	private static URLClassLoader getURLClassLoader(final URL jarURL){
		return new URLClassLoader(new URL[] {
			jarURL
		});
	}
	
	@Override
	public Composite createPage(final Composite parent){
		// try to dynamically load the openmedical JAR file
		String jarPath = Hub.localCfg.get(PreferencePage.JAR_PATH, null);
		if (jarPath != null) {
			File jar = new File(jarPath);
			if (jar.canRead()) {
				try {
					URLClassLoader urlLoader =
						getURLClassLoader(new URL("file", null, jar.getAbsolutePath()));
					
					Class<?> openmedicalClass = urlLoader.loadClass(OPENMEDICAL_MAINCLASS);
					
					// try to get the download method
					Method meth;
					try {
						meth = openmedicalClass.getMethod("download", String[].class);
					} catch (Throwable e) {
						throw e;
					}
					
					// try to get an instance
					Object obj = openmedicalClass.newInstance();
					
					// success (no exception); set the global variables
					openmedicalObject = obj;
					openmedicalDownloadMethod = meth;
				} catch (Throwable e) {
					// loading the class failed; do nothing
				}
			}
		}
		
		// parentShell=parent.getShell();
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayout(new GridLayout());
		LabImporter labImporter = new LabImporter(ret, this);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		labImporter.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		return ret;
	}
	
	private Result<?> importDirect(){
		if (openmedicalObject == null) {
			return new Result<String>(SEVERITY.ERROR, 1, MY_LAB, "Fehlerhafte Konfiguration", true);
		}
		Result<String> result = new Result<String>("OK");
		
		String downloadDirPath = Hub.localCfg.get(PreferencePage.DL_DIR, Hub.getTempDir().toString());
		String iniPath = Hub.localCfg.get(PreferencePage.INI_PATH, null);
		
		int res = -1;
		if (iniPath != null) {
			try {
				Object omResult =
					openmedicalDownloadMethod.invoke(openmedicalObject, new Object[] {
						new String[] {
							"--download", downloadDirPath, "--logPath", downloadDirPath, "--ini",
							iniPath, "--verbose", "INF", "-#OpenMedicalKey#", "-allInOne"
						}
					});
				if (omResult instanceof Integer) {
					res = ((Integer) omResult).intValue();
					System.out.println(res + " files downoladed");
					if (res < 1) {
						SWTHelper.showInfo("Verbindung mit Labor " + MY_LAB + " erfolgreich",
							"Es sind keine Resultate zum Abholen vorhanden");
					}
				}
			} catch (Throwable e) {
				// method call failed; do nothing
			}
		}
		// if (res > 0) {
		File downloadDir = new File(downloadDirPath);
		if (downloadDir.isDirectory()) {
			File archiveDir = new File(downloadDir, "archive");
			if (!archiveDir.exists()) {
				archiveDir.mkdir();
			}
			
			String[] files = downloadDir.list(new FilenameFilter() {
				
				public boolean accept(File path, String name){
					if (name.toLowerCase().endsWith(".hl7")) {
						return true;
					}
					return false;
				}
			});
			for (String file : files) {
				File f = new File(downloadDir, file);
				Result<?> rs = hlp.importFile(f, archiveDir, false);
				if (!rs.isOK()) {
					// importFile already shows error
					// rs.display("Fehler beim Import");
				}
			}
			SWTHelper.showInfo("Verbindung mit Labor " + MY_LAB + " erfolgreich", "Es wurden "
				+ Integer.toString(res) + " Dateien verarbeitet");
		} else {
			SWTHelper.showError("Falsches Verzeichnis",
				"Bitte kontrollieren Sie die Einstellungen für das Download-Verzeichnis");
			result =
				new Result<String>(SEVERITY.ERROR, 1, MY_LAB, "Fehlerhafte Konfiguration", true);
		}
		// }
		
		return result;
	}
	
	@Override
	public IStatus doImport(final IProgressMonitor monitor) throws Exception{
		int type;
		try {
			String sType = results[0];
			type = Integer.parseInt(sType);
		} catch (NumberFormatException ex) {
			type = FILE;
		}
		
		if ((type != FILE) && (type != DIRECT)) {
			type = FILE;
		}
		
		if (type == FILE) {
			String filename = results[1];
			return ResultAdapter.getResultAsStatus(hlp.importFile(filename, false));
		} else {
			return ResultAdapter.getResultAsStatus(importDirect());
		}
	}
	
	@Override
	public String getDescription(){
		return "Bitte wählen Sie eine Datei im HL7-Format oder die Direktübertragung zum Import aus";
	}
	
	@Override
	public String getTitle(){
		return "Labor " + MY_LAB;
	}
	
	String getBasePath(){
		try {
			URL url = Platform.getBundle(PLUGIN_ID).getEntry("/");
			url = FileLocator.toFileURL(url);
			String bundleLocation = url.getPath();
			File file = new File(bundleLocation);
			bundleLocation = file.getAbsolutePath();
			return bundleLocation;
		} catch (Throwable throwable) {
			return null;
		}
	}
	
	/**
	 * An importer that lets the user select a file to import or directly import the data from the
	 * lab. The chosen type (file or direct import) is stored in results[0] (FILE or DIRECT). If
	 * FILE is chosen, the file path is stored in results[1].
	 * 
	 * @author gerry, danlutz
	 * 
	 */
	private class LabImporter extends Composite {
		private final Button bFile;
		private final Button bDirect;
		
		private final Text tFilename;
		
		public LabImporter(final Composite parent, final ImporterPage home){
			super(parent, SWT.BORDER);
			setLayout(new GridLayout(3, false));
			
			bFile = new Button(this, SWT.RADIO);
			bFile.setText("Import aus Datei (HL7)");
			bFile.setLayoutData(SWTHelper.getFillGridData(3, true, 1, false));
			
			Label lFile = new Label(this, SWT.NONE);
			lFile.setText("    " + Messages.getString("ImporterPage.file")); //$NON-NLS-1$ //$NON-NLS-2$
			GridData gd = SWTHelper.getFillGridData(1, false, 1, false);
			gd.horizontalAlignment = GridData.END;
			gd.widthHint = lFile.getSize().x + 20;
			
			tFilename = new Text(this, SWT.BORDER);
			tFilename.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
			
			Button bBrowse = new Button(this, SWT.PUSH);
			bBrowse.setText(Messages.getString("ImporterPage.browse")); //$NON-NLS-1$
			
			bDirect = new Button(this, SWT.RADIO);
			bDirect.setText("Direkter Import");
			bDirect.setLayoutData(SWTHelper.getFillGridData(3, true, 1, false));
			
			int type = Hub.localCfg.get("ImporterPage/" + home.getTitle() + "/type", FILE); //$NON-NLS-1$ //$NON-NLS-2$
			if (openmedicalObject == null) {
				type = FILE;
			}
			
			home.results = new String[2];
			
			if (type == FILE) {
				bFile.setSelection(true);
				bDirect.setSelection(false);
				
				String filename =
					Hub.localCfg.get("ImporterPage/" + home.getTitle() + "/filename", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				tFilename.setText(filename);
				
				home.results[0] = new Integer(FILE).toString();
				home.results[1] = filename;
			} else {
				bFile.setSelection(false);
				bDirect.setSelection(true);
				
				tFilename.setText("");
				
				home.results[0] = new Integer(DIRECT).toString();
				home.results[1] = "";
			}
			
			if (openmedicalObject == null) {
				bDirect.setEnabled(false);
			}
			
			SelectionAdapter sa = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e){
					Button button = (Button) e.getSource();
					
					// only handle selection == true
					if (!button.getSelection()) {
						return;
					}
					
					int type = FILE;
					
					if (button == bFile) {
						type = FILE;
					} else if (button == bDirect) {
						type = DIRECT;
					}
					
					if (type == FILE) {
						bFile.setSelection(true);
						bDirect.setSelection(false);
						
						String filename = tFilename.getText();
						
						home.results[0] = new Integer(FILE).toString();
						home.results[1] = filename;
						
						Hub.localCfg.set("ImporterPage/" + home.getTitle() + "/type", FILE); //$NON-NLS-1$ //$NON-NLS-2$
						Hub.localCfg.set("ImporterPage/" + home.getTitle() + "/filename", filename); //$NON-NLS-1$ //$NON-NLS-2$
					} else {
						bFile.setSelection(false);
						bDirect.setSelection(true);
						
						tFilename.setText("");
						
						home.results[0] = new Integer(DIRECT).toString();
						home.results[1] = "";
						
						Hub.localCfg.set("ImporterPage/" + home.getTitle() + "/type", DIRECT); //$NON-NLS-1$ //$NON-NLS-2$
						Hub.localCfg.set("ImporterPage/" + home.getTitle() + "/filename", ""); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			};
			
			bFile.addSelectionListener(sa);
			bDirect.addSelectionListener(sa);
			
			bBrowse.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e){
					bFile.setSelection(true);
					bDirect.setSelection(false);
					
					FileDialog fdl = new FileDialog(parent.getShell(), SWT.OPEN);
					fdl.setFilterExtensions(new String[] {
						"*"}); //$NON-NLS-1$
					fdl.setFilterNames(new String[] {
						Messages.getString("ImporterPage.allFiles")}); //$NON-NLS-1$
					String filename = fdl.open();
					if (filename == null) {
						filename = "";
					}
					
					tFilename.setText(filename);
					home.results[0] = new Integer(FILE).toString();
					home.results[1] = filename;
					
					Hub.localCfg.set("ImporterPage/" + home.getTitle() + "/type", FILE); //$NON-NLS-1$ //$NON-NLS-2$
					Hub.localCfg.set("ImporterPage/" + home.getTitle() + "/filename", filename); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
			});
		}
	}
}
