/*******************************************************************************
 * Copyright (c) 2016 Remain Software and others,
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wim Jongman - Wizard with cancelable progress monitor
 *******************************************************************************/
package org.eclipse.jface.snippets.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Example Wizard with Progress Monitor and Cancel
 *
 * @author Wim Jongman <wim.jongman@remainsoftware.com>
 */
public class Snippet071WizardWithProgressAndCancel {

	private static final class MyWizard extends Wizard {
		private int fTasks;

		private int fSubTasks;

		private MyPage fPage;

		private final class MyPage extends WizardPage {
			private Button fCloseButton;

			private MyPage(String pPageName, String pTitle, ImageDescriptor pTitleImage) {
				super(pPageName, pTitle, pTitleImage);
			}

			@Override
			public void createControl(Composite pParent) {
				fCloseButton = new Button(pParent, SWT.CHECK);
				fCloseButton.setText("Close wizard after progress is complete");
				setControl(fCloseButton);
			}

			public boolean isCloseWanted() {
				return fCloseButton.getSelection();
			}
		}

		public MyWizard(int pTasks, int pSubTasks) {
			this.fTasks = pTasks;
			this.fSubTasks = pSubTasks;
		}

		@Override
		public boolean performFinish() {
			try {
				getContainer().run(true, true, new IRunnableWithProgress() {

					@Override
					public void run(IProgressMonitor pMonitor) throws InvocationTargetException, InterruptedException {
						SubMonitor monitor = SubMonitor.convert(pMonitor, fTasks * fSubTasks);
						for (int j = 1; j <= fTasks; j++) {
							monitor.worked(1);
							doMainTask(monitor.split(fSubTasks), j);
							if (monitor.isCanceled()) {
								doCancel(monitor);
								break;
							}
						}
					}

					private void doMainTask(SubMonitor pMonitor, int pTaskNum) throws InterruptedException {
						pMonitor.setTaskName("Task " + pTaskNum);
						doSubTask(pMonitor.split(fSubTasks));
					}

					private void doSubTask(SubMonitor pMonitor) throws InterruptedException {
						for (int i = 1; i <= fSubTasks; i++) {
							if (pMonitor.isCanceled()) {
								pMonitor.subTask("Cancel pressed, finishing this task (" + i + ")");
							} else {
								pMonitor.subTask("Performing subtask " + i);
							}
							Thread.sleep(20);
							pMonitor.worked(1);
						}
					}

					private void doCancel(SubMonitor pMonitor) throws InterruptedException {
						pMonitor.subTask("");
						pMonitor.setTaskName("Canceling. Please wait ...");
						Thread.sleep(4000);
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}

			return fPage.isCloseWanted();

		}

		@Override
		public void addPages() {
			fPage = new MyPage("Page1", "First and final page", null);
			addPage(fPage);
		}
	}

	public static void main(String[] args) {
		Display display = new Display();
		final Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		openWizard(shell);
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	private static void openWizard(final Shell pShell) {
		Wizard testWizard = new MyWizard(10, 50);
		testWizard.setNeedsProgressMonitor(true);
		WizardDialog dialog = new WizardDialog(pShell, testWizard);
		dialog.open();
		pShell.dispose();
	}
}
