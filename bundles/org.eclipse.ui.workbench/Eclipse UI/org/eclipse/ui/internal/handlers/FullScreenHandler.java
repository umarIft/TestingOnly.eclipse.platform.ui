/*******************************************************************************
 * Copyright (c) 2016 vogella GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Simon Scholz <simon.scholz@vogella.com> - initial API and
 * implementation
 ******************************************************************************/

package org.eclipse.ui.internal.handlers;

import java.util.Arrays;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.ui.bindings.internal.BindingTableManager;
import org.eclipse.e4.ui.bindings.internal.ContextSet;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.keys.IBindingService;

/**
 * Handler, which enables a full screen mode.
 *
 * @since 3.5
 *
 */
public class FullScreenHandler extends AbstractHandler {

	private static final String QUICK_ACCESS_COMMAND_ID = "org.eclipse.ui.window.quickAccess"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) {
		Shell shell = HandlerUtil.getActiveShell(event);
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IBindingService bindingService = window.getService(IBindingService.class);
		ECommandService commandService = window.getService(ECommandService.class);
		BindingTableManager bindingTableManager = window.getService(BindingTableManager.class);
		IContextService bindingContextService = window.getService(IContextService.class);

		TriggerSequence sequence = getKeybindingSequence(bindingService, commandService, bindingTableManager,
				bindingContextService);

		String keybinding = ""; //$NON-NLS-1$
		if (sequence != null) {
			keybinding = sequence.format();
		}

		shell.setFullScreen(!shell.getFullScreen());

		if (shell.getFullScreen()) {
			FullScreenInfoPopup fullScreenInfoPopup = new FullScreenInfoPopup(shell, PopupDialog.HOVER_SHELLSTYLE, true,
					false, false, false, false, null, null, keybinding);
			fullScreenInfoPopup.open();
		}
		return Status.OK_STATUS;
	}

	private static class FullScreenInfoPopup extends PopupDialog {

		private String keybinding;

		public FullScreenInfoPopup(Shell parent, int shellStyle, boolean takeFocusOnOpen, boolean persistSize,
				boolean persistLocation, boolean showDialogMenu, boolean showPersistActions, String titleText,
				String infoText, String keybinding) {
			super(parent, shellStyle, takeFocusOnOpen, persistSize, persistLocation, showDialogMenu, showPersistActions,
					titleText, infoText);
			this.keybinding = keybinding;
		}

		@Override
		protected Point getInitialLocation(Point initialSize) {
			return super.getInitialLocation(initialSize);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite composite = (Composite) super.createDialogArea(parent);

			Link link = new Link(composite, SWT.BORDER);

			if (keybinding.isEmpty()) {
				link.setText(WorkbenchMessages.ToggleFullScreenMode_ActivationPopup_Description_NoKeybinding);
			} else {
				link.setText(NLS.bind(WorkbenchMessages.ToggleFullScreenMode_ActivationPopup_Description, keybinding));
			}

			link.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					close();
				}
			});
			GridData gd = new GridData(GridData.BEGINNING | GridData.FILL_BOTH);
			gd.horizontalIndent = PopupDialog.POPUP_HORIZONTALSPACING;
			gd.verticalIndent = PopupDialog.POPUP_VERTICALSPACING;
			link.setLayoutData(gd);

			return composite;
		}

	}

	protected TriggerSequence getKeybindingSequence(IBindingService bindingService, ECommandService eCommandService,
			BindingTableManager bindingTableManager, IContextService contextService) {
		TriggerSequence triggerSequence = bindingService.getBestActiveBindingFor(QUICK_ACCESS_COMMAND_ID);
		// FIXME Bug 491701 - [KeyBinding] get best active binding is not
		// working
		if (triggerSequence == null) {
			ParameterizedCommand cmd = eCommandService.createCommand(QUICK_ACCESS_COMMAND_ID, null);
			ContextSet contextSet = bindingTableManager
					.createContextSet(Arrays.asList(contextService.getDefinedContexts()));
			Binding binding = bindingTableManager.getBestSequenceFor(contextSet, cmd);
			if (binding != null) {
				triggerSequence = binding.getTriggerSequence();
			}
		}
		return triggerSequence;
	}

}
