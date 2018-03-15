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
package org.eclipse.e4.core.macros.internal;

import java.util.Map;
import org.eclipse.e4.core.macros.IMacroCommandFactory;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;

/**
 * Basic interface for a macro (private API, only meant to be used inside the
 * org.eclipse.e4.core.macros plugin).
 *
 * Note that the actual macro implementation could be created from a list of
 * commands in memory, a javascript file to be run, a json/xml describing
 * commands, etc.
 */
/* default */ interface IMacro {

	/**
	 * Used to playback a macro in the given context.
	 *
	 * @param macroPlaybackContext
	 *            the context to playback the macro.
	 * @throws Exception
	 *             if there was some error running the macro.
	 */
	void playback(IMacroPlaybackContext macroPlaybackContext) throws Exception;

	/**
	 * Sets a map which maps a command id to an implementation which is able to
	 * create a macro from its persisted state.
	 *
	 * @param macroCommandIdToFactory
	 *            maps a macro command id to an implementation which is able to
	 *            create a macro from its persisted state.
	 */
	void setMacroCommandIdToFactory(Map<String, IMacroCommandFactory> macroCommandIdToFactory);

}