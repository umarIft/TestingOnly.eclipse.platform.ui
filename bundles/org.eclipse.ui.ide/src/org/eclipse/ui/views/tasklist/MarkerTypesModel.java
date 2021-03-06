/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.views.tasklist;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

/**
 * Maintains a model of all known marker types.
 */
class MarkerTypesModel {
    /**
     * Maps from marker type id to MarkerType.
     */
	private HashMap<String, MarkerType> types;

    /**
     * Creates a new marker types model.
     */
    public MarkerTypesModel() {
        types = readTypes();
    }

    /**
     * Returns the marker type with the given id, or <code>null</code> if there is no such marker type.
     */
    public MarkerType getType(String id) {
        return types.get(id);
    }

    /**
     * Returns all known marker types.
     */
    public MarkerType[] getTypes() {
        MarkerType[] result = new MarkerType[types.size()];
        types.values().toArray(result);
        return result;
    }

    /**
     * Returns the label for the given marker type.
     * Workaround until we have labels in XML.
     */
    private String getWellKnownLabel(String type) {
        if (type.equals(IMarker.PROBLEM)) {
			return "Problem";//$NON-NLS-1$
		}
        if (type.equals(IMarker.TASK)) {
			return "Task";//$NON-NLS-1$
		}
        if (type.equals("org.eclipse.jdt.core.problem")) { //$NON-NLS-1$
			return "Java Problem";//$NON-NLS-1$
		}
        return type;
    }

    /**
     * Reads the marker types from the registry.
     */
	private HashMap<String, MarkerType> readTypes() {
		HashMap<String, MarkerType> types = new HashMap<>();
        IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(
                ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_MARKERS);
        if (point != null) {
            // Gather all registered marker types.
			for (IExtension extension : point.getExtensions()) {
				String id = extension.getUniqueIdentifier();
				String label = extension.getLabel();
                if (label.equals("")) {//$NON-NLS-1$
                    label = getWellKnownLabel(id);
                }
				ArrayList<String> supersList = new ArrayList<>();
				for (IConfigurationElement configElement : extension.getConfigurationElements()) {
                    if (configElement.getName().equalsIgnoreCase("super")) {//$NON-NLS-1$
                        String sup = configElement.getAttribute("type");//$NON-NLS-1$
                        if (sup != null) {
                            supersList.add(sup);
                        }
                    }
                }
                String[] superTypes = new String[supersList.size()];
                supersList.toArray(superTypes);
                MarkerType type = new MarkerType(this, id, label, superTypes);
                types.put(id, type);
            }
        }
        return types;
    }
}
