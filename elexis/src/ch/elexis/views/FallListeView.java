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
 *  $Id: FallListeView.java 5970 2010-01-27 16:43:04Z rgw_ch $
 *******************************************************************************/

package ch.elexis.views;

import static ch.elexis.actions.GlobalActions.delFallAction;
import static ch.elexis.actions.GlobalActions.delKonsAction;
import static ch.elexis.actions.GlobalActions.makeBillAction;
import static ch.elexis.actions.GlobalActions.moveBehandlungAction;
import static ch.elexis.actions.GlobalActions.openFallaction;
import static ch.elexis.actions.GlobalActions.redateAction;
import static ch.elexis.actions.GlobalActions.reopenFallAction;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListenerImpl;
import ch.elexis.actions.GlobalActions;
import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.actions.GlobalEventDispatcher.IActivationListener;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.viewers.CommonViewer;
import ch.elexis.util.viewers.DefaultContentProvider;
import ch.elexis.util.viewers.DefaultControlFieldProvider;
import ch.elexis.util.viewers.DefaultLabelProvider;
import ch.elexis.util.viewers.SimpleWidgetProvider;
import ch.elexis.util.viewers.ViewerConfigurer;
import ch.elexis.util.viewers.ViewerConfigurer.ButtonProvider;
import ch.rgw.tools.IFilter;

/**
 * Eine View, die untereinander Fälle und zugehörigende Behandlungen des aktuell
 * ausgewählten Patienten anzeigt.
 * 
 * @author gerry
 * 
 */
public class FallListeView extends ViewPart implements IActivationListener,
		ISaveablePart2 {
	public static final String ID = "ch.elexis.FallListeView"; //$NON-NLS-1$
	CommonViewer fallViewer;
	CommonViewer behandlViewer;
	private ViewerConfigurer fallCf, behandlCf;
	private FormToolkit tk;
	private Form form;
	private Patient actPatient;
	private Fall actFall;
	private Konsultation actBehandlung;
	// private Filter behandlungsFilter;
	private Image iClosed;
	private ElexisEventListenerImpl eeli_pat = new ElexisEventListenerImpl(
			Patient.class) {

		public void runInUi(final ElexisEvent ev) {
			actPatient = (Patient) ev.getObject();
			form.setText(actPatient.getPersonalia());
			fallViewer.getViewerWidget().refresh(false);
		}
	};
	private ElexisEventListenerImpl eeli_fall = new ElexisEventListenerImpl(
			Fall.class) {

		public void runInUi(final ElexisEvent ev) {
			Fall f = (Fall) ev.getObject();
			setFall(f, null);
		}
	};

	public FallListeView() {
		super();
	}

	@Override
	public void createPartControl(Composite parent) {
		iClosed = Desk.getImage(Desk.IMG_LOCK_CLOSED);
		tk = Desk.getToolkit();
		form = tk.createForm(parent);
		form.getBody().setLayout(new GridLayout());
		SashForm sash = new SashForm(form.getBody(), SWT.VERTICAL);
		form.setText(Messages.getString("FallListeView.NoPatientSelected")); //$NON-NLS-1$
		sash.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		ButtonProvider fallButton = new ButtonProvider() {

			public Button createButton(Composite parent1) {
				Button ret = tk.createButton(parent1, Messages
						.getString("FallListeView.NewCase"), SWT.PUSH); //$NON-NLS-1$
				ret.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						String bez = fallCf.getControlFieldProvider()
								.getValues()[0];
						Fall fall = actPatient.neuerFall(bez, Messages
								.getString("FallListeView.Illness"), "KVG"); //$NON-NLS-1$ //$NON-NLS-2$
						Konsultation b = fall.neueKonsultation();
						b.setMandant(Hub.actMandant);
						fallCf.getControlFieldProvider().clearValues();
						fallViewer.getViewerWidget().refresh();
						fallViewer.setSelection(fall, true);

					}

				});
				return ret;
			}

			public boolean isAlwaysEnabled() {
				return false;
			}
		};
		fallViewer = new CommonViewer();
		fallCf = new ViewerConfigurer(new DefaultContentProvider(fallViewer,
				Fall.class) {
			@Override
			public Object[] getElements(Object inputElement) {

				if (actPatient != null) {
					if (fallCf.getControlFieldProvider().isEmpty()) {
						return actPatient.getFaelle();
					} else {
						IFilter filter = fallCf.getControlFieldProvider()
								.createFilter();
						List<String> list = actPatient.getList(Messages
								.getString("FallListeView.Cases"), true); //$NON-NLS-1$
						ArrayList<Fall> arr = new ArrayList<Fall>();
						for (String s : list) {
							Fall f = Fall.load(s);
							if (filter.select(f)) {
								arr.add(f);
							}
						}
						return arr.toArray();
					}
				}
				return new Object[0];
			}
		}, new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				if (element instanceof Fall) {
					if (((Fall) element).isOpen()) {
						return null;
					} else {
						return iClosed;
					}
				}
				return super.getImage(element);
			}

			@Override
			public String getText(Object element) {
				return (((Fall) element).getLabel());
			}

		}, new DefaultControlFieldProvider(fallViewer, new String[] { Messages
				.getString("FallListeView.Label") //$NON-NLS-1$
				}), fallButton, new SimpleWidgetProvider(
				SimpleWidgetProvider.TYPE_TABLE, SWT.SINGLE, fallViewer));
		fallViewer.create(fallCf, sash, SWT.NONE, getViewSite());
		fallViewer.getViewerWidget().addSelectionChangedListener(
				GlobalEventDispatcher.getInstance().getDefaultListener());
		behandlViewer = new CommonViewer();
		ButtonProvider behandlButton = new ButtonProvider() {
			public Button createButton(Composite parent1) {
				Button ret = tk.createButton(parent1, Messages
						.getString("FallListeView.NewKons"), SWT.PUSH); //$NON-NLS-1$
				ret.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						Konsultation b = actFall.neueKonsultation();
						if (b != null) {
							b.setMandant(Hub.actMandant);
							behandlCf.getControlFieldProvider().clearValues();
							behandlViewer.getViewerWidget().refresh();
							// behandlViewer.setSelection(b);
							setFall(actFall, b);
						}

					}

				});
				return ret;
			}

			public boolean isAlwaysEnabled() {
				return true;
			}

		};
		behandlCf = new ViewerConfigurer(
				new ViewerConfigurer.ContentProviderAdapter() {
					@Override
					public Object[] getElements(Object inputElement) {
						if (actFall != null) {
							Konsultation[] alle = actFall.getBehandlungen(true);
							/*
							 * if(behandlungsFilter!=null){
							 * ArrayList<Konsultation> al=new
							 * ArrayList<Konsultation>(alle.length); for(int
							 * i=0;i<alle.length;i++){
							 * if(behandlungsFilter.select(alle[i])==true){
							 * al.add(alle[i]); } } return al.toArray(); }
							 */
							return actFall.getBehandlungen(true);
						}
						return new Object[0];
					}
				}, new DefaultLabelProvider(), new DefaultControlFieldProvider(
						behandlViewer, new String[] { Messages
								.getString("FallListeView.Date") //$NON-NLS-1$
						}), behandlButton, new SimpleWidgetProvider(
						SimpleWidgetProvider.TYPE_LIST, SWT.SINGLE
								| SWT.V_SCROLL, behandlViewer));
		Composite cf = new Composite(sash, SWT.BORDER);
		cf.setLayout(new GridLayout());
		behandlViewer.create(behandlCf, cf, SWT.NONE, getViewSite());
		behandlViewer.getViewerWidget().addSelectionChangedListener(
				GlobalEventDispatcher.getInstance().getDefaultListener());
		tk.adapt(sash, false, false);
		GlobalEventDispatcher.addActivationListener(this, this);
		sash.setWeights(new int[] { 50, 50 });
		// behandlungsFilter=null;
		createMenuAndToolbar();
		createContextMenu();
		((DefaultContentProvider) fallCf.getContentProvider()).startListening();

	}

	private void createContextMenu() {
		MenuManager fallMenuMgr = new MenuManager();
		fallMenuMgr.setRemoveAllWhenShown(true);
		fallMenuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				manager.add(openFallaction);
				manager.add(reopenFallAction);
				manager.add(new GroupMarker(
						IWorkbenchActionConstants.MB_ADDITIONS));
				manager.add(delFallAction);
				manager.add(new Separator());
				manager.add(makeBillAction);
			}
		});

		Menu fallMenu = fallMenuMgr.createContextMenu(fallViewer
				.getViewerWidget().getControl());
		fallViewer.getViewerWidget().getControl().setMenu(fallMenu);
		getSite().registerContextMenu("ch.elexis.FallListeMenu", fallMenuMgr, //$NON-NLS-1$
				fallViewer.getViewerWidget());

		MenuManager behdlMenuMgr = new MenuManager();
		behdlMenuMgr.setRemoveAllWhenShown(true);
		behdlMenuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				manager.add(new GroupMarker(
						IWorkbenchActionConstants.MB_ADDITIONS));
				manager.add(delKonsAction);
				manager.add(moveBehandlungAction);
				manager.add(redateAction);
			}
		});
		Menu behdlMenu = behdlMenuMgr.createContextMenu(behandlViewer
				.getViewerWidget().getControl());
		behandlViewer.getViewerWidget().getControl().setMenu(behdlMenu);
		getSite().registerContextMenu(
				"ch.elexis.BehandlungsListeMenu", behdlMenuMgr, //$NON-NLS-1$
				behandlViewer.getViewerWidget());
	}

	private void createMenuAndToolbar() {
		IMenuManager mgr = getViewSite().getActionBars().getMenuManager();
		mgr.add(delFallAction);
		mgr.add(delKonsAction);
		mgr.add(new Separator());
		// mgr.add(filterAction);
		IToolBarManager tmg = getViewSite().getActionBars().getToolBarManager();
		tmg.add(GlobalActions.helpAction);
		// tmg.add(filterAction);
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	public void setFall(Fall f, Konsultation b) {
		actFall = f;
		if (f != null) {
			actPatient = f.getPatient();
			// System.out.println(actPatient.getLabel());
			form.setText(actPatient.getLabel());
			fallViewer.notify(CommonViewer.Message.update);
			fallViewer.setSelection(f, false);
			if (b == null) {
				b = f.getLetzteBehandlung();
			}
			if (b != null) {
				behandlViewer.setSelection(b, false);
			}
			// Hub.actBehandlung=b;
			actBehandlung = b;
			reopenFallAction.setEnabled(!f.isOpen());
			behandlViewer.getViewerWidget().refresh(true);

		} else {
			ElexisEventDispatcher.clearSelection(Konsultation.class);
			ElexisEventDispatcher.clearSelection(Fall.class);
			if (actPatient == null) {
				form.setText(Messages
						.getString("FallListeView.NoPatientSelected")); //$NON-NLS-1$
			} else {
				form.setText(actPatient.getLabel());
			}
			fallViewer.notify(CommonViewer.Message.update);
			reopenFallAction.setEnabled(false);
			behandlViewer.getViewerWidget().refresh(true);
		}

	}

	public void selectionEvent(PersistentObject obj) {
		if (obj instanceof Patient) {
		} else if (obj instanceof Fall) {
		}
	}

	@Override
	public void dispose() {
		((DefaultContentProvider) fallCf.getContentProvider()).stopListening();
		GlobalEventDispatcher.removeActivationListener(this, this);
		super.dispose();
	}

	public void activation(boolean mode) {
		// TODO Auto-generated method stub

	}

	public void visible(boolean mode) {
		if (mode == true) {
			ElexisEventDispatcher.getInstance().addListeners(eeli_fall,
					eeli_pat);
			actPatient = (Patient) ElexisEventDispatcher
					.getSelected(Patient.class);
			actFall = (Fall) ElexisEventDispatcher.getSelected(Fall.class);
			// System.out.println(actPatient.getLabel());
			if (actPatient != null) {
				if (actFall == null) {
					actBehandlung = actPatient.getLetzteKons(false);
					if (actBehandlung == null) {
						actFall = null;
					} else {
						actFall = actBehandlung.getFall();
					}
				} else {
					// System.out.println(actFall.getPatient().getLabel());
					if (actFall.getPatient().getId().equals(actPatient.getId())) {
						if (actBehandlung != null) {
							if ((actBehandlung.getFall() == null)
									|| (!actBehandlung.getFall().getId()
											.equals(actFall.getId()))) {
								actBehandlung = actPatient.getLetzteKons(false);
							}
						}
					} else {
						actBehandlung = actPatient.getLetzteKons(false);
						if (actBehandlung == null) {
							actFall = null;
						} else {
							actFall = actBehandlung.getFall();
						}
					}
				}
			} else {
				actFall = null;
				actBehandlung = null;
			}
			setFall(actFall, actBehandlung);

		} else {
			ElexisEventDispatcher.getInstance().removeListeners(eeli_fall,
					eeli_pat);
		}

	}

	/***********************************************************************************************
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2 Wir
	 * benötigen das Interface nur, um das Schliessen einer View zu verhindern,
	 * wenn die Perspektive fixiert ist. Gibt es da keine einfachere Methode?
	 */
	public int promptToSaveOnClose() {
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL
				: ISaveablePart2.NO;
	}

	public void doSave(IProgressMonitor monitor) { /* leer */
	}

	public void doSaveAs() { /* leer */
	}

	public boolean isDirty() {
		return true;
	}

	public boolean isSaveAsAllowed() {
		return false;
	}

	public boolean isSaveOnCloseNeeded() {
		return true;
	}
}
