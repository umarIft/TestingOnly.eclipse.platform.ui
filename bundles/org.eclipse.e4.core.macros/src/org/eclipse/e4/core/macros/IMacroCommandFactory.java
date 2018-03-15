/*******************************************************************************
 * Copyright (c) 2017 Fabio Zadrozny and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabio Zadrozny - initial API and implementation - http://eclip.se/8519
 *******************************************************************************/
package org.eclipse.e4.core.macros;

import java.util.Map;

/**
 * Factory for macro commands which had the contents of
 * {@link IMacroCommand#toMap()} persisted.
 *
 * Should be registered through the
 * org.eclipse.e4.core.macros.macroCommandsFactory extension point (with a match
 * through {@link org.eclipse.e4.core.macros.IMacroCommand#getId()}).
 */
public interface IMacroCommandFactory {

	/**
	 * @param stringMap
	 *            a map which was created from {@link IMacroCommand#toMap()}
	 * @return the IMacroCommand created from the given stringMap.
	 * @throws Exception
	 *             if some error happened recreating the macro command.
	 */
	IMacroCommand create(Map<String, String> stringMap) throws Exception;

}
