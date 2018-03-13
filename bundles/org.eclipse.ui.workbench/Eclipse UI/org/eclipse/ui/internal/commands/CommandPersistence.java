/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.internal.commands;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.commands.AbstractParameterValueConverter;
import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.ParameterType;
import org.eclipse.core.commands.State;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.commands.internal.HandlerServiceImpl;
import org.eclipse.e4.ui.internal.workbench.Parameter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.registry.IWorkbenchRegistryConstants;
import org.eclipse.ui.internal.services.RegistryPersistence;
import org.eclipse.ui.internal.util.PrefUtil;

/**
 * <p>
 * A static class for accessing the registry and the preference store.
 * </p>
 * 
 * @since 3.1
 */
public final class CommandPersistence extends RegistryPersistence {

	/**
	 * The index of the category elements in the indexed array.
	 * 
	 * @see CommandPersistence#read()
	 */
	private static final int INDEX_CATEGORY_DEFINITIONS = 0;

	/**
	 * The index of the command elements in the indexed array.
	 * 
	 * @see CommandPersistence#read()
	 */
	private static final int INDEX_COMMAND_DEFINITIONS = 1;

	/**
	 * The index of the commandParameterType elements in the indexed array.
	 * 
	 * @see CommandPersistence#read()
	 * @since 3.2
	 */
	private static final int INDEX_PARAMETER_TYPE_DEFINITIONS = 2;

	/**
	 * Reads all of the category definitions from the commands extension point.
	 * 
	 * @param configurationElements
	 *            The configuration elements in the commands extension point;
	 *            must not be <code>null</code>, but may be empty.
	 * @param configurationElementCount
	 *            The number of configuration elements that are really in the
	 *            array.
	 * @param commandManager
	 *            The command service to which the categories should be added;
	 *            must not be <code>null</code>.
	 */
	private static final void readCategoriesFromRegistry(
			final IConfigurationElement[] configurationElements,
			final int configurationElementCount, final CommandManager commandManager) {

		Category undefCat = commandManager.getCategory(null);
		if (!undefCat.isDefined()) {
			// Define the uncategorized category.
			commandManager.defineUncategorizedCategory(
					WorkbenchMessages.CommandService_AutogeneratedCategoryName,
					WorkbenchMessages.CommandService_AutogeneratedCategoryDescription);
		}

		final List warningsToLog = new ArrayList(1);

		for (int i = 0; i < configurationElementCount; i++) {
			final IConfigurationElement configurationElement = configurationElements[i];

			// Read out the category identifier.
			final String categoryId = readRequired(configurationElement,
					ATT_ID, warningsToLog, "Categories need an id"); //$NON-NLS-1$
			if (categoryId == null) {
				continue;
			}

			// Read out the name.
			final String name = readRequired(configurationElement, ATT_NAME,
					warningsToLog, "Categories need a name", //$NON-NLS-1$
					categoryId);
			if (name == null) {
				continue;
			}

			// Read out the description.
			final String description = readOptional(configurationElement,
					ATT_DESCRIPTION);

			final Category category = commandManager.getCategory(categoryId);
			if (!category.isDefined()) {
				category.define(name, description);
			}
		}

		// If there were any warnings, then log them now.
		logWarnings(
				warningsToLog,
				"Warnings while parsing the commands from the 'org.eclipse.ui.commands' and 'org.eclipse.ui.actionDefinitions' extension points."); //$NON-NLS-1$
	}

	/**
	 * Reads all of the command definitions from the commands extension point.
	 * 
	 * @param configurationElements
	 *            The configuration elements in the commands extension point;
	 *            must not be <code>null</code>, but may be empty.
	 * @param configurationElementCount
	 *            The number of configuration elements that are really in the
	 *            array.
	 * @param commandManager
	 *            The command service to which the commands should be added;
	 *            must not be <code>null</code>.
	 */
	private static final void readCommandsFromRegistry(
			final IConfigurationElement[] configurationElements,
			final int configurationElementCount, final CommandManager commandManager) {
		final List warningsToLog = new ArrayList(1);

		for (int i = 0; i < configurationElementCount; i++) {
			final IConfigurationElement configurationElement = configurationElements[i];

			// Read out the command identifier.
			final String commandId = readRequired(configurationElement, ATT_ID,
					warningsToLog, "Commands need an id"); //$NON-NLS-1$
			if (commandId == null) {
				continue;
			}

			// Read out the name.
			final String name = readRequired(configurationElement, ATT_NAME,
					warningsToLog, "Commands need a name"); //$NON-NLS-1$
			if (name == null) {
				continue;
			}

			// Read out the description.
			final String description = readOptional(configurationElement,
					ATT_DESCRIPTION);

			// Read out the category id.
			String categoryId = configurationElement
					.getAttribute(ATT_CATEGORY_ID);
			if ((categoryId == null) || (categoryId.length() == 0)) {
				categoryId = configurationElement.getAttribute(ATT_CATEGORY);
				if ((categoryId != null) && (categoryId.length() == 0)) {
					categoryId = null;
				}
			}

			// Read out the parameters.
			final Parameter[] parameters = readParameters(configurationElement,
					warningsToLog, commandManager);

			// Read out the returnTypeId.
			final String returnTypeId = readOptional(configurationElement,
					ATT_RETURN_TYPE_ID);

			// Read out the help context identifier.
			final String helpContextId = readOptional(configurationElement,
					ATT_HELP_CONTEXT_ID);

			final Command command = commandManager.getCommand(commandId);
			final Category category = commandManager.getCategory(categoryId);
			if (!category.isDefined()) {
				addWarning(
						warningsToLog,
						"Commands should really have a category", //$NON-NLS-1$
						configurationElement, commandId,
						"categoryId", categoryId); //$NON-NLS-1$
			}

			final ParameterType returnType;
			if (returnTypeId == null) {
				returnType = null;
			} else {
				returnType = commandManager.getParameterType(returnTypeId);
			}

			if (parameters != null && parameters.length > 0) {
				command.undefine();
			}
			if (!command.isDefined()) {
				command.define(name, description, category, parameters, returnType, helpContextId);
				command.setHandler(HandlerServiceImpl.getHandler(commandId));
			}
			readState(configurationElement, warningsToLog, command);
		}

		// If there were any warnings, then log them now.
		logWarnings(
				warningsToLog,
				"Warnings while parsing the commands from the 'org.eclipse.ui.commands' and 'org.eclipse.ui.actionDefinitions' extension points."); //$NON-NLS-1$
	}

	/**
	 * Reads the parameters from a parent configuration element. This is used to
	 * read the parameter sub-elements from a command element. Each parameter is
	 * guaranteed to be valid. If invalid parameters are found, then a warning
	 * status will be appended to the <code>warningsToLog</code> list.
	 * 
	 * @param configurationElement
	 *            The configuration element from which the parameters should be
	 *            read; must not be <code>null</code>.
	 * @param warningsToLog
	 *            The list of warnings found during parsing. Warnings found
	 *            while parsing the parameters will be appended to this list.
	 *            This value must not be <code>null</code>.
	 * @param commandManager
	 *            The command service from which the parameter can get parameter
	 *            types; must not be <code>null</code>.
	 * @return The array of parameters found for this configuration element;
	 *         <code>null</code> if none can be found.
	 */
	private static final Parameter[] readParameters(
			final IConfigurationElement configurationElement, final List warningsToLog,
			final CommandManager commandManager) {
		final IConfigurationElement[] parameterElements = configurationElement
				.getChildren(TAG_COMMAND_PARAMETER);
		if ((parameterElements == null) || (parameterElements.length == 0)) {
			return null;
		}

		int insertionIndex = 0;
		Parameter[] parameters = new Parameter[parameterElements.length];
		for (int i = 0; i < parameterElements.length; i++) {
			final IConfigurationElement parameterElement = parameterElements[i];
			// Read out the id
			final String id = readRequired(parameterElement, ATT_ID,
					warningsToLog, "Parameters need an id"); //$NON-NLS-1$
			if (id == null) {
				continue;
			}

			// Read out the name.
			final String name = readRequired(parameterElement, ATT_NAME,
					warningsToLog, "Parameters need a name"); //$NON-NLS-1$
			if (name == null) {
				continue;
			}

			/*
			 * The IParameterValues will be initialized lazily as an
			 * IExecutableExtension.
			 */

			// Read out the typeId attribute, if present.
			final String typeId = readOptional(parameterElement, ATT_TYPE_ID);

			// Read out the optional attribute, if present.
			final boolean optional = readBoolean(parameterElement,
					ATT_OPTIONAL, true);

			final ParameterType type;
			if (typeId == null) {
				type = null;
			} else {
				type = commandManager.getParameterType(typeId);
			}

			final Parameter parameter = new Parameter(id, name,
					parameterElement, type, optional);
			parameters[insertionIndex++] = parameter;
		}

		if (insertionIndex != parameters.length) {
			final Parameter[] compactedParameters = new Parameter[insertionIndex];
			System.arraycopy(parameters, 0, compactedParameters, 0,
					insertionIndex);
			parameters = compactedParameters;
		}

		return parameters;
	}

	/**
	 * Reads all of the commandParameterType definitions from the commands
	 * extension point.
	 * 
	 * @param configurationElements
	 *            The configuration elements in the commands extension point;
	 *            must not be <code>null</code>, but may be empty.
	 * @param configurationElementCount
	 *            The number of configuration elements that are really in the
	 *            array.
	 * @param commandManager
	 *            The command service to which the commands should be added;
	 *            must not be <code>null</code>.
	 * @since 3.2
	 */
	private static final void readParameterTypesFromRegistry(
			final IConfigurationElement[] configurationElements,
			final int configurationElementCount, final CommandManager commandManager) {

		final List warningsToLog = new ArrayList(1);

		for (int i = 0; i < configurationElementCount; i++) {
			final IConfigurationElement configurationElement = configurationElements[i];

			// Read out the commandParameterType identifier.
			final String parameterTypeId = readRequired(configurationElement,
					ATT_ID, warningsToLog, "Command parameter types need an id"); //$NON-NLS-1$
			if (parameterTypeId == null) {
				continue;
			}

			// Read out the type.
			final String type = readOptional(configurationElement, ATT_TYPE);

			// Read out the converter.
			final String converter = readOptional(configurationElement,
					ATT_CONVERTER);

			/*
			 * if the converter attribute was given, create a proxy
			 * AbstractParameterValueConverter for the ParameterType, otherwise
			 * null indicates there is no converter
			 */
			final AbstractParameterValueConverter parameterValueConverter = (converter == null) ? null
					: new ParameterValueConverterProxy(configurationElement);

			final ParameterType parameterType = commandManager
					.getParameterType(parameterTypeId);
			if (!parameterType.isDefined()) {
				parameterType.define(type, parameterValueConverter);
			}
		}

		// If there were any warnings, then log them now.
		logWarnings(
				warningsToLog,
				"Warnings while parsing the commandParameterTypes from the 'org.eclipse.ui.commands' extension point."); //$NON-NLS-1$

	}

	/**
	 * Reads the states from a parent configuration element. This is used to
	 * read the state sub-elements from a command element. Each state is
	 * guaranteed to be valid. If invalid states are found, then a warning
	 * status will be appended to the <code>warningsToLog</code> list.
	 * 
	 * @param configurationElement
	 *            The configuration element from which the states should be
	 *            read; must not be <code>null</code>.
	 * @param warningsToLog
	 *            The list of warnings found during parsing. Warnings found
	 *            while parsing the parameters will be appended to this list.
	 *            This value must not be <code>null</code>.
	 * @param command
	 *            The command for which the state is being read; may be
	 *            <code>null</code>.
	 */
	private static final void readState(
			final IConfigurationElement configurationElement,
			final List warningsToLog, final Command command) {
		final IConfigurationElement[] stateElements = configurationElement
				.getChildren(TAG_STATE);
		if ((stateElements == null) || (stateElements.length == 0)) {
			return;
		}

		for (int i = 0; i < stateElements.length; i++) {
			final IConfigurationElement stateElement = stateElements[i];

			final String id = readRequired(stateElement, ATT_ID, warningsToLog, "State needs an id"); //$NON-NLS-1$
			if (id == null) {
				continue;
			}

			if (checkClass(stateElement, warningsToLog, "State must have an associated class", id)) { //$NON-NLS-1$
				if (command.getState(id) == null) {
					final State state = new CommandStateProxy(stateElement, ATT_CLASS,
							PrefUtil.getInternalPreferenceStore(),
							CommandService.createPreferenceKey(command, id));
					command.addState(id, state);
				}
			}
		}
	}

	/**
	 * The command service with which this persistence class is associated;
	 * never <code>null</code>.
	 */
	private final CommandManager commandManager;

	/**
	 * Constructs a new instance of <code>CommandPersistence</code>.
	 * 
	 * @param commandService
	 *            The command service which should be populated with the values
	 *            from the registry; must not be <code>null</code>.
	 */
	public CommandPersistence(final CommandManager commandService) {
		if (commandService == null) {
			throw new NullPointerException("The command service cannot be null"); //$NON-NLS-1$
		}
		this.commandManager = commandService;
	}

	protected final boolean isChangeImportant(final IRegistryChangeEvent event) {
		return false;
	}

	public boolean commandsNeedUpdating(final IRegistryChangeEvent event) {
		final IExtensionDelta[] commandDeltas = event.getExtensionDeltas(
				PlatformUI.PLUGIN_ID, IWorkbenchRegistryConstants.PL_COMMANDS);
		if (commandDeltas.length == 0) {
			final IExtensionDelta[] actionDefinitionDeltas = event
					.getExtensionDeltas(PlatformUI.PLUGIN_ID,
							IWorkbenchRegistryConstants.PL_ACTION_DEFINITIONS);
			if (actionDefinitionDeltas.length == 0) {
				return false;
			}
		}

		return true;
	}
	
	/**
	 * Reads all of the commands and categories from the registry,
	 * 
	 * @param commandManager
	 *            The command service which should be populated with the values
	 *            from the registry; must not be <code>null</code>.
	 */
	protected final void read() {
		super.read();
		reRead();
	}
	
	public void reRead() {
		// Create the extension registry mementos.
		final IExtensionRegistry registry = Platform.getExtensionRegistry();
		int commandDefinitionCount = 0;
		int categoryDefinitionCount = 0;
		int parameterTypeDefinitionCount = 0;
		final IConfigurationElement[][] indexedConfigurationElements = new IConfigurationElement[3][];

		// Sort the commands extension point based on element name.
		final IConfigurationElement[] commandsExtensionPoint = registry
				.getConfigurationElementsFor(EXTENSION_COMMANDS);
		for (int i = 0; i < commandsExtensionPoint.length; i++) {
			final IConfigurationElement configurationElement = commandsExtensionPoint[i];
			final String name = configurationElement.getName();

			// Check if it is a binding definition.
			if (TAG_COMMAND.equals(name)) {
				addElementToIndexedArray(configurationElement,
						indexedConfigurationElements,
						INDEX_COMMAND_DEFINITIONS, commandDefinitionCount++);
			} else if (TAG_CATEGORY.equals(name)) {
				addElementToIndexedArray(configurationElement,
						indexedConfigurationElements,
						INDEX_CATEGORY_DEFINITIONS, categoryDefinitionCount++);
			} else if (TAG_COMMAND_PARAMETER_TYPE.equals(name)) {
				addElementToIndexedArray(configurationElement,
						indexedConfigurationElements,
						INDEX_PARAMETER_TYPE_DEFINITIONS,
						parameterTypeDefinitionCount++);
			}
		}

		final IConfigurationElement[] actionDefinitionsExtensionPoint = registry
				.getConfigurationElementsFor(EXTENSION_ACTION_DEFINITIONS);
		for (int i = 0; i < actionDefinitionsExtensionPoint.length; i++) {
			final IConfigurationElement configurationElement = actionDefinitionsExtensionPoint[i];
			final String name = configurationElement.getName();

			if (TAG_ACTION_DEFINITION.equals(name)) {
				addElementToIndexedArray(configurationElement,
						indexedConfigurationElements,
						INDEX_COMMAND_DEFINITIONS, commandDefinitionCount++);
			}
		}

		readCategoriesFromRegistry(
				indexedConfigurationElements[INDEX_CATEGORY_DEFINITIONS],
				categoryDefinitionCount, commandManager);
		readCommandsFromRegistry(
				indexedConfigurationElements[INDEX_COMMAND_DEFINITIONS],
				commandDefinitionCount, commandManager);
		readParameterTypesFromRegistry(
				indexedConfigurationElements[INDEX_PARAMETER_TYPE_DEFINITIONS],
				parameterTypeDefinitionCount, commandManager);
	}
}
