/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.internal.navigator.filters;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.internal.navigator.CommonNavigatorMessages;
import org.eclipse.ui.internal.navigator.NavigatorFilterService;
import org.eclipse.ui.internal.navigator.NavigatorPlugin;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.ICommonFilterDescriptor;
import org.eclipse.ui.navigator.IMementoAware;
import org.eclipse.ui.navigator.INavigatorViewerDescriptor;

/**
 * Creates the last 5 filters that were touched as view menu options.
 *
 * @since 3.2
 *
 */
public class FilterActionGroup extends ActionGroup implements IMementoAware {

	private static final String FILTER_ACTION_GROUP = "filterActionGroup"; //$NON-NLS-1$

	private static final int MAX_FILTER_MENU_ENTRIES = 3;

	private static final String TAG_LRU_FILTERS = "lastRecentlyUsedFilters"; //$NON-NLS-1$
	private static final String TAG_CHILD = "child"; //$NON-NLS-1$
	private static final String TAG_FILTER_ID = "filterId"; //$NON-NLS-1$

	private SelectFiltersAction selectFiltersAction;
	private IMenuManager menuManager;
	private IMenuListener menuListener;
	private IMenuManager filtersMenu;
	private CommonViewer commonViewer;
	private INavigatorViewerDescriptor viewerDescriptor;

	private Deque<ICommonFilterDescriptor> lruFilterDescriptorStack = new ArrayDeque<>();


	/**
	 * @param aCommonViewer The viewer this action group is associated with
	 */
	public FilterActionGroup(CommonViewer aCommonViewer) {
		Assert.isNotNull(aCommonViewer);
		commonViewer = aCommonViewer;
		viewerDescriptor = commonViewer.getNavigatorContentService().getViewerDescriptor();
		makeActions();
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		menuManager = actionBars.getMenuManager();
		menuManager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS,
				new Separator(FILTER_ACTION_GROUP));
		if (selectFiltersAction != null) {
			menuManager.addMenuListener(menuListener);
			menuManager.appendToGroup(FILTER_ACTION_GROUP,
					filtersMenu);
			menuManager.appendToGroup(FILTER_ACTION_GROUP,
					selectFiltersAction);
		}
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (menuManager != null) {
			menuManager.removeMenuListener(menuListener);
		}
		if (filtersMenu != null) {
			filtersMenu.dispose();
		}
	}

	@Override
	public void restoreState(IMemento aMemento) {
		IMemento lruFilters = aMemento.getChild(TAG_LRU_FILTERS);
		lruFilterDescriptorStack.clear();
		if (lruFilters != null) {
			NavigatorFilterService filterService = (NavigatorFilterService) commonViewer.getNavigatorContentService()
					.getFilterService();
			ICommonFilterDescriptor[] visibleFilterDescriptors = filterService.getVisibleFilterDescriptorsForUI();
			for (IMemento child : lruFilters.getChildren(TAG_CHILD)) {
				String id = child.getString(TAG_FILTER_ID);
				if (id != null) {
					for (ICommonFilterDescriptor visibleFilterDescriptor : visibleFilterDescriptors) {
						if (visibleFilterDescriptor.getId().equals(id)) {
							lruFilterDescriptorStack.push(visibleFilterDescriptor);
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public void saveState(IMemento aMemento) {
		IMemento lruFilters = aMemento.createChild(TAG_LRU_FILTERS);
		if (!lruFilterDescriptorStack.isEmpty()) {
			for (ICommonFilterDescriptor filterDescriptor : lruFilterDescriptorStack) {
				IMemento child = lruFilters.createChild(TAG_CHILD);
				child.putString(TAG_FILTER_ID, filterDescriptor.getId());
			}
		}
	}

	/**
	 *
	 */
	private void makeActions() {
		boolean hideAvailableCustomizationsDialog = viewerDescriptor
				.getBooleanConfigProperty(INavigatorViewerDescriptor.PROP_HIDE_AVAILABLE_CUSTOMIZATIONS_DIALOG);
		if (!hideAvailableCustomizationsDialog) {
			selectFiltersAction = new SelectFiltersAction(commonViewer, this);
			ImageDescriptor selectFiltersIcon = NavigatorPlugin.getImageDescriptor("icons/full/elcl16/filter_ps.png"); //$NON-NLS-1$
			selectFiltersAction.setImageDescriptor(selectFiltersIcon);
			selectFiltersAction.setHoverImageDescriptor(selectFiltersIcon);

			filtersMenu = new MenuManager(CommonNavigatorMessages.FilterActionGroup_Filters);
			menuListener = new IMenuListener() {

				@Override
				public void menuAboutToShow(IMenuManager manager) {
					filtersMenu.removeAll();
					addLRUFilterActions(filtersMenu);
				}

			};
		}
	}

	private void addLRUFilterActions(IMenuManager manager) {
		if (lruFilterDescriptorStack.isEmpty()) {
			return;
		}

		NavigatorFilterService filterService = (NavigatorFilterService) commonViewer.getNavigatorContentService()
				.getFilterService();
		ICommonFilterDescriptor[] filterDescriptors = lruFilterDescriptorStack
				.toArray(new ICommonFilterDescriptor[lruFilterDescriptorStack.size()]);
		Arrays.sort(filterDescriptors, new Comparator<ICommonFilterDescriptor>() {

			@Override
			public int compare(ICommonFilterDescriptor o1, ICommonFilterDescriptor o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		for (ICommonFilterDescriptor filterDescriptor : filterDescriptors) {
			manager.add(new ToggleFilterAction(commonViewer, filterService, filterDescriptor));
		}
	}

	protected void updateFilterShortcuts(ICommonFilterDescriptor[] filterDescriptorChangeHistory) {
		Deque<ICommonFilterDescriptor> oldestFirstStack = new ArrayDeque<>();
		int length = Math.min(filterDescriptorChangeHistory.length, MAX_FILTER_MENU_ENTRIES);
		for (int i = 0; i < length; i++) {
			oldestFirstStack.push(filterDescriptorChangeHistory[i]);
		}

		length = Math.min(lruFilterDescriptorStack.size(), MAX_FILTER_MENU_ENTRIES - oldestFirstStack.size());
		for (int i = 0; i < length; i++) {
			ICommonFilterDescriptor filter = lruFilterDescriptorStack.pollFirst();
			if (!oldestFirstStack.contains(filter))
				oldestFirstStack.push(filter);
		}
		lruFilterDescriptorStack = oldestFirstStack;
	}
}
