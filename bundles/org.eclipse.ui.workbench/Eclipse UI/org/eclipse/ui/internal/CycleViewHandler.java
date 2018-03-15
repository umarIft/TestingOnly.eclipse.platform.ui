/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@gmail.com> - Bug 440810
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 454143, 461063
 *     Friederike Schertel <friederike@schertel.org> - Bug 478336
 ******************************************************************************/

package org.eclipse.ui.internal;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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

		List<MPart> parts = modelService.findElements(currentPerspective, null, MPart.class, null,
				EModelService.PRESENTATION);

		AtomicBoolean includeEditor = new AtomicBoolean(true);

		parts.stream().sorted((firstPart, secondPart) -> {
			Long firstPartActivationTime = (Long) firstPart.getTransientData()
					.getOrDefault(PartServiceImpl.PART_ACTIVATION_TIME, Long.MIN_VALUE);
			Long secondPartActivationTime = (Long) secondPart.getTransientData()
					.getOrDefault(PartServiceImpl.PART_ACTIVATION_TIME, Long.MIN_VALUE);
			// use decreasing order by inverting the result using "-" at the beginning
			return -(firstPartActivationTime.compareTo(secondPartActivationTime));
		}).forEach(part -> {
			if (!partService.isPartOrPlaceholderInPerspective(part.getElementId(), currentPerspective)) {
				return;
			}

			if (part.getTags().contains("Editor")) { //$NON-NLS-1$
				if (includeEditor.getAndSet(false)) {
					IEditorPart activeEditor = page.getActiveEditor();
					TableItem item = new TableItem(table, SWT.NONE);
					item.setText(WorkbenchMessages.CyclePartAction_editor);
					item.setImage(activeEditor.getTitleImage());
					if (activeEditor.getSite() instanceof PartSite) {
						item.setData(((PartSite) activeEditor.getSite()).getPartReference());
					} else {
						item.setData(part);
					}
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
		});

	}

	@Override
	protected ParameterizedCommand getBackwardCommand() {
		return getParametrizedCommand(IWorkbenchCommandConstants.WINDOW_PREVIOUS_VIEW);
	}

	@Override
	protected ParameterizedCommand getForwardCommand() {
		return getParametrizedCommand(IWorkbenchCommandConstants.WINDOW_NEXT_VIEW);
	}

  private ParameterizedCommand getParametrizedCommand(IWorkbenchCommandConstants workbenchCommand)
	{
		final ICommandService commandService = window.getWorkbench().getService(ICommandService.class);
		final Command command = commandService.getCommand(workbenchCommand);
		ParameterizedCommand parameterizedCommand = new ParameterizedCommand(command, null);
		return parameterizedCommand;
	}

	@Override
	protected String getTableHeader(IWorkbenchPart activePart) {
		return WorkbenchMessages.CyclePartAction_header;
	}

}
