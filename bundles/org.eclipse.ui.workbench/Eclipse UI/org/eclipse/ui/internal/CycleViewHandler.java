/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@gmail.com> - Bug 440810
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 454143, 461063
 ******************************************************************************/

package org.eclipse.ui.internal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.ui.internal.workbench.PartServiceImpl;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.renderers.swt.SWTPartRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.ICommandService;

/**
 * This handler is used to switch between parts using the keyboard.
 * <p>
 * Replacement for CyclePartAction
 * </p>
 *
 * @since 3.3
 *
 */
public class CycleViewHandler extends CycleBaseHandler {

	@Override
	protected void addItems(Table table, WorkbenchPage page) {

		EPartService partService = page.getWorkbenchWindow().getService(EPartService.class);
		EModelService modelService = page.getWorkbenchWindow().getService(EModelService.class);
		MPerspective currentPerspective = page.getCurrentPerspective();

		boolean includeEditor = true;

		List<MPart> partsOfActivePerspective = modelService.findElements(currentPerspective, null, MPart.class, null);

		Collection<MPart> sortedParts = getPartListSortedByActivation(partService, partsOfActivePerspective);

		for (MPart part : sortedParts) {
			if (!partService.isPartOrPlaceholderInPerspective(part.getElementId(), currentPerspective)) {
				continue;
			}

			if (part.getTags().contains("Editor")) { //$NON-NLS-1$
				if (includeEditor) {
					IEditorPart activeEditor = page.getActiveEditor();
					TableItem item = new TableItem(table, SWT.NONE);
					item.setText(WorkbenchMessages.CyclePartAction_editor);
					item.setImage(activeEditor.getTitleImage());
					if (activeEditor.getSite() instanceof PartSite) {
						item.setData(((PartSite) activeEditor.getSite()).getPartReference());
					} else {
						item.setData(part);
					}
					includeEditor = false;
				}
			} else {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(part.getLabel());
				IWorkbenchWindow iwbw = page.getWorkbenchWindow();
				if (iwbw instanceof WorkbenchWindow) {
					WorkbenchWindow wbw = (WorkbenchWindow) iwbw;
					if (part != null && wbw.getModel().getRenderer() instanceof SWTPartRenderer) {
						SWTPartRenderer r = (SWTPartRenderer) wbw.getModel().getRenderer();
						item.setImage(r.getImage(part));
					}
				}
				item.setData(part);
			}
		}
	}

	private Collection<MPart> getPartListSortedByActivation(EPartService partService, List<MPart> parts) {
		if (partService instanceof PartServiceImpl) {
			PartServiceImpl partServiceImpl = (PartServiceImpl) partService;

			List<MPart> activationList = partServiceImpl.getActivationList();
			if (activationList.isEmpty()) {
				return parts;
			}
			Set<MPart> partList = new LinkedHashSet<>(activationList);

			// remove all parts, which are not in the part list of the current
			// perspective
			partList.retainAll(parts);

			// add all remaining parts of the part list
			partList.addAll(parts);
			return partList;
		}

		return parts;
	}

	@Override
	protected ParameterizedCommand getBackwardCommand() {
		// TODO Auto-generated method stub
		final ICommandService commandService = window.getWorkbench().getService(ICommandService.class);
		final Command command = commandService.getCommand(IWorkbenchCommandConstants.WINDOW_PREVIOUS_VIEW);
		ParameterizedCommand commandBack = new ParameterizedCommand(command, null);
		return commandBack;
	}

	@Override
	protected ParameterizedCommand getForwardCommand() {
		// TODO Auto-generated method stub
		final ICommandService commandService = window.getWorkbench().getService(ICommandService.class);
		final Command command = commandService.getCommand(IWorkbenchCommandConstants.WINDOW_NEXT_VIEW);
		ParameterizedCommand commandF = new ParameterizedCommand(command, null);
		return commandF;
	}

	@Override
	protected String getTableHeader(IWorkbenchPart activePart) {
		// TODO Auto-generated method stub
		return WorkbenchMessages.CyclePartAction_header;
	}

}
