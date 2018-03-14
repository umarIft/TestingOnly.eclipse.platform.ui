/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.dynamichelpers.IExtensionTracker;
import org.eclipse.ui.SubActionBars;
import org.eclipse.ui.internal.provisional.application.IActionBarConfigurer2;
import org.eclipse.ui.internal.registry.IActionSet;
import org.eclipse.ui.internal.registry.IActionSetDescriptor;

/**
 * Manage the configurable actions for one window.
 */
public class ActionPresentation {
    private WorkbenchWindow window;

	private HashMap<IActionSetDescriptor, SetRec> mapDescToRec = new HashMap<IActionSetDescriptor, SetRec>(
			3);

	private HashMap<IActionSetDescriptor, SetRec> invisibleBars = new HashMap<IActionSetDescriptor, SetRec>(
			3);

    private class SetRec {
        public SetRec(IActionSet set,
                SubActionBars bars) {
            this.set = set;
            this.bars = bars;
        }

        public IActionSet set;

        public SubActionBars bars;
    }

    /**
	 * ActionPresentation constructor comment.
	 * 
	 * @param window
	 */
    public ActionPresentation(WorkbenchWindow window) {
        super();
        this.window = window;
    }

    /**
     * Remove all action sets.
     */
    public void clearActionSets() {
        // Get all of the action sets -- both visible and invisible.
		final List<IActionSetDescriptor> oldList = new ArrayList<IActionSetDescriptor>();
        oldList.addAll(mapDescToRec.keySet());
        oldList.addAll(invisibleBars.keySet());

		Iterator<IActionSetDescriptor> iter = oldList.iterator();
        while (iter.hasNext()) {
			IActionSetDescriptor desc = iter.next();
            removeActionSet(desc);
        }
    }

    /**
	 * Destroy an action set.
	 * 
	 * @param desc
	 */
    public void removeActionSet(IActionSetDescriptor desc) {
		SetRec rec = mapDescToRec.remove(desc);
        if (rec == null) {
			rec = invisibleBars.remove(desc);
        }
        if (rec != null) {
            IActionSet set = rec.set;
            SubActionBars bars = rec.bars;
            if (bars != null) {
                bars.dispose();
            }
            if (set != null) {
                set.dispose();
            }
        }
    }

    /**
	 * Sets the list of visible action set.
	 * 
	 * @param newArray
	 *            of IActionSetDescriptor
	 */
    public void setActionSets(IActionSetDescriptor[] newArray) {
        // Convert array to list.
		HashSet<IActionSetDescriptor> newList = new HashSet<IActionSetDescriptor>();
        
        for (int i = 0; i < newArray.length; i++) {
            IActionSetDescriptor descriptor = newArray[i];
            
            newList.add(descriptor);
        }
		List<IActionSetDescriptor> oldList = new ArrayList<IActionSetDescriptor>(
				mapDescToRec.keySet());

        // Remove obsolete actions.
		Iterator<IActionSetDescriptor> iter = oldList.iterator();
        while (iter.hasNext()) {
			IActionSetDescriptor desc = iter.next();
            if (!newList.contains(desc)) {
				SetRec rec = mapDescToRec.get(desc);
                if (rec != null) {
                    mapDescToRec.remove(desc);
                    IActionSet set = rec.set;
                    SubActionBars bars = rec.bars;
                    if (bars != null) {
                        SetRec invisibleRec = new SetRec(set, bars);
                        invisibleBars.put(desc, invisibleRec);
                        bars.deactivate();
                    }
                }
            }
        }

        // Add new actions.
		ArrayList<IActionSet> sets = new ArrayList<IActionSet>();
        
        for (int i = 0; i < newArray.length; i++) {
            IActionSetDescriptor desc = newArray[i];

            if (!mapDescToRec.containsKey(desc)) {
                try {
                    SetRec rec;
                    // If the action bars and sets have already been created
                    // then
                    // reuse those action sets
                    if (invisibleBars.containsKey(desc)) {
						rec = invisibleBars.get(desc);
                        if (rec.bars != null) {
                            rec.bars.activate();
                        }
                        invisibleBars.remove(desc);
                    } else {
                        IActionSet set = desc.createActionSet();
                        SubActionBars bars = new ActionSetActionBars(window
								.getActionBars(), window,
								(IActionBarConfigurer2) window.getWindowConfigurer()
										.getActionBarConfigurer(), desc.getId());
                        rec = new SetRec(set, bars);
                        set.init(window, bars);
                        sets.add(set);

                        // only register against the tracker once - check for
                        // other registrations against the provided extension
                        Object[] existingRegistrations = window
                                .getExtensionTracker().getObjects(
                                        desc.getConfigurationElement()
                                                .getDeclaringExtension());
                        if (existingRegistrations.length == 0
                                || !containsRegistration(existingRegistrations,
                                        desc)) {
                            //register the set with the page tracker
                            //this will be cleaned up by WorkbenchWindow listener
                            window.getExtensionTracker().registerObject(
                                    desc.getConfigurationElement().getDeclaringExtension(),
                                    desc, IExtensionTracker.REF_WEAK);
                        }
                    }
                    mapDescToRec.put(desc, rec);
                } catch (CoreException e) {
                    WorkbenchPlugin
                            .log("Unable to create ActionSet: " + desc.getId(), e);//$NON-NLS-1$
                }
            }
        }
        // We process action sets in two passes for coolbar purposes. First we
        // process base contributions
        // (i.e., actions that the action set contributes to its toolbar), then
        // we process adjunct contributions
        // (i.e., actions that the action set contributes to other toolbars).
        // This type of processing is
        // necessary in order to maintain group order within a coolitem.
        PluginActionSetBuilder.processActionSets(sets, window);

		Iterator<IActionSet> iter2 = sets.iterator();
		while (iter2.hasNext()) {
			PluginActionSet set = (PluginActionSet) iter2.next();
            set.getBars().activate();
        }
    }

    /**
     * Return whether the array contains the given action set.
     * 
     * @param existingRegistrations the array to check
     * @param set the set to look for
     * @return whether the set is in the array
     * @since 3.1
     */
    private boolean containsRegistration(Object[] existingRegistrations, IActionSetDescriptor set) {
        for (int i = 0; i < existingRegistrations.length; i++) {
            if (existingRegistrations[i] == set) {
				return true;
			}
        }
        return false;
    }

    /**
	 * @return IActionSet[]
	 */
    public IActionSet[] getActionSets() {
		Collection<SetRec> setRecCollection = mapDescToRec.values();
        IActionSet result[] = new IActionSet[setRecCollection.size()];
        int i = 0;
		for (Iterator<SetRec> iterator = setRecCollection.iterator(); iterator
                .hasNext(); i++) {
			result[i] = iterator.next().set;
		}
        return result;
    }
}
