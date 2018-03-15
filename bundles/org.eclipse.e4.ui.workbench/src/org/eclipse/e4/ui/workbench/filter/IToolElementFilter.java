/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andreas Schwarz - initial API and implementation
 ******************************************************************************/

package org.eclipse.e4.ui.workbench.filter;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarElement;

/**
 * Filter that allows to suppress the rendering of a menu element.
 *
 * @since 1.5
 * @implement This filter is intended to be implemented by clients
 */
public interface IToolElementFilter {

	/**
	 * Checks if the given tool element should be displayed.
	 *
	 * @param toolBarElement
	 *            the element that should be checked
	 * @param context
	 *            The current eclipse context
	 * @return <b>true</b> if the toolbar element should not be shown otherwise
	 *         <b>false</b>.
	 */
	public boolean filterElement(MToolBarElement toolBarElement, IEclipseContext context);

}
