package elexis_db_shaker.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import ch.elexis.data.Konsultation;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Person;
import ch.elexis.data.Query;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.VersionedResource;

/**
 * Our sample action implements workbench action delegate. The action proxy will
 * be created by the workbench and shown in the UI. When the user tries to use
 * the action, this delegate will be created and execution will be delegated to
 * it.
 * 
 * @see IWorkbenchWindowActionDelegate
 */
public class Shake implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	boolean zufallsnamen;
	int TOTAL=Integer.MAX_VALUE;
	
	/**
	 * The constructor.
	 */
	public Shake() {
	}

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * 
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		final SettingsDialog sd = new SettingsDialog(window.getShell());
		if (sd.open() == Dialog.OK) {
			if (SWTHelper
					.askYesNo(
							"Wirklich Datenbank anonymisieren",
							"Achtung! Diese Aktion macht die Datenbank unwiderruflich unbrauchbar! Wirklich anonymisieren?")) {
				zufallsnamen = sd.replaceNames;
				IWorkbench wb = PlatformUI.getWorkbench();
				IProgressService ps = wb.getProgressService();
				try {
					ps.busyCursorWhile(new IRunnableWithProgress() {
						public void run(IProgressMonitor pm) {
							pm.beginTask("Anonymisiere Datenbank", TOTAL);
							int jobs=1;
							if(sd.replaceKons){
								jobs=2;
							}
							doShakeNames(pm,TOTAL/jobs);
							if(sd.replaceKons){
								doShakeKons(pm,TOTAL/jobs);
							}
						}
					});
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void doShakeKons(IProgressMonitor monitor, int workUnits){
		Query<Konsultation> qbe=new Query<Konsultation>(Konsultation.class);
		List<Konsultation> list=qbe.execute();
		int workPerKons=(Math.round(workUnits*.8f)/list.size());
		Lipsum lipsum=new Lipsum();
		monitor.worked(Math.round(workUnits*.2f));
		for(Konsultation k:list){
			VersionedResource vr=k.getEintrag();
			StringBuilder par=new StringBuilder();
			int numPars=(int) Math.round(3*Math.random()+1);
			while(numPars-->0){
				par.append(lipsum.getParagraph());
			}
			vr.update(par.toString(), "random contents");
			k.setEintrag(vr, true);
			k.purgeEintrag();
			monitor.worked(workPerKons);
		}
	}
	private void doShakeNames(IProgressMonitor monitor, int workUnits) {
		Query<Kontakt> qbe = new Query<Kontakt>(Kontakt.class);
		List<Kontakt> list = qbe.execute();
		int workPerName=(Math.round(workUnits*.8f)/list.size());
		Namen n = null;
		if (zufallsnamen) {
			n = new Namen();
		}
		monitor.worked(Math.round(workUnits*.2f));
		for (Kontakt k : list) {
			String vorname = "";
			// Mandanten behalten
			// if(k.get(Kontakt.FLD_IS_MANDATOR).equalsIgnoreCase(StringConstants.ONE))
			// continue;

			if (zufallsnamen) {
				k.set("Bezeichnung1", n.getRandomNachname());
			} else {
				k.set("Bezeichnung1", getWord());
			}

			if (zufallsnamen) {
				vorname = n.getRandomVorname();
			} else {
				vorname = getWord();
			}
			k.set("Bezeichnung2", vorname);

			if (k.istPerson()) {
				Person p = Person.load(k.getId());
				p.set(Person.SEX, StringTool.isFemale(vorname) ? Person.FEMALE
						: Person.MALE);
			}
			k.set(Kontakt.FLD_ANSCHRIFT, "");
			k.set(Kontakt.FLD_PHONE1, getPhone());
			k.set(Kontakt.FLD_PHONE2, Math.random() > 0.6 ? getPhone() : "");
			k.set(Kontakt.FLD_MOBILEPHONE, Math.random() > 0.5 ? getPhone()
					: "");
			k.set(Kontakt.FLD_E_MAIL, "");
			k.set(Kontakt.FLD_PLACE, "");
			k.set(Kontakt.FLD_STREET, "");
			k.set(Kontakt.FLD_ZIP, "");
			k.set(Kontakt.FLD_FAX, Math.random() > 0.8 ? getPhone() : "");
			monitor.worked(workPerName);
		}
	}

	private String getPhone() {
		StringBuilder ret = new StringBuilder();
		ret.append("555-");
		for (int i = 0; i < 7; i++) {
			ret.append((char) Math.round(Math.random() * ('9' - '0') + '0'));
		}
		return ret.toString();
	}

	private String getWord() {
		int l = (int) Math.round(Math.random() * 5 + 5);
		StringBuilder ret = new StringBuilder();
		ret.append(Character.toUpperCase(getLetter()));
		for (int i = 0; i < l; i++) {
			ret.append(getLetter());
		}
		return ret.toString();
	}

	private char getLetter() {
		return (char) Math.round(Math.random() * ('z' - 'a') + 'a');
	}

	/**
	 * Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 * 
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 * 
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 * 
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}