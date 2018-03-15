/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.about;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class InstallationHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) {
		IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
		if (workbenchWindow == null)
			workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		InstallationDialog dialog = new InstallationDialog(HandlerUtil.getActiveShell(event), workbenchWindow);
		InstallationDialog.lastSelectedTabId = null;
		dialog.open();
		return null;
	}

}
