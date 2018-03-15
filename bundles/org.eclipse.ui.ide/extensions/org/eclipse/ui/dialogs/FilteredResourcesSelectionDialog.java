/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     James Blackburn (Broadcom Corp.) Bug 86973 Allow path pattern matching
 *     Anton Leherbauer (Wind River Systems, Inc.) - Bug 415099 Terminating with "<" or " " (space) does not work for extensions
 *     Mickael Istria (Red Hat Inc.) - Bug 460749: filter resources with same location
 *     Patrik Suzzi <psuzzi@gmail.com> - Bug 491025
 *******************************************************************************/
package org.eclipse.ui.dialogs;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ResourceWorkingSetFilter;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.actions.WorkingSetFilterActionGroup;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.IIDEHelpContextIds;
import org.eclipse.ui.internal.ide.model.ResourceFactory;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.statushandlers.StatusManager;

import com.ibm.icu.text.Collator;

/**
 * Shows a list of resources to the user with a text entry field for a string
 * pattern used to filter the list of resources.
 *
 * @since 3.3
 */
public class FilteredResourcesSelectionDialog extends
		FilteredItemsSelectionDialog {

	private static final String DIALOG_SETTINGS = "org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog"; //$NON-NLS-1$

	private static final String WORKINGS_SET_SETTINGS = "WorkingSet"; //$NON-NLS-1$

	private static final String SHOW_DERIVED = "ShowDerived"; //$NON-NLS-1$
	private static final String FILTER_BY_LOCATION = "FilterByLocation"; //$NON-NLS-1$

	private ShowDerivedResourcesAction showDerivedResourcesAction;

	private ResourceItemLabelProvider resourceItemLabelProvider;

	private ResourceItemDetailsLabelProvider resourceItemDetailsLabelProvider;

	private WorkingSetFilterActionGroup workingSetFilterActionGroup;

	private CustomWorkingSetFilter workingSetFilter = new CustomWorkingSetFilter();

	private FilterResourcesByLocation filterResourceByLocation = new FilterResourcesByLocation();
	private GroupResourcesByLocationAction groupResourcesByLocationAction;

	private String title;

	/**
	 * The base outer-container which will be used to search for resources. This
	 * is the root of the tree that spans the search space. Often, this is the
	 * workspace root.
	 */
	private IContainer container;

	/**
	 * The container to use as starting point for relative search, or
	 * <code>null</code> if none.
	 * @since 3.6
	 */
	private IContainer searchContainer;

	private int typeMask;

	private boolean isDerived;

	/**
	 * Creates a new instance of the class
	 *
	 * @param shell
	 *            the parent shell
	 * @param multi
	 *            the multi selection flag
	 * @param container
	 *            the container to select resources from, e.g. the workspace root
	 * @param typesMask
	 *            a mask specifying which resource types should be shown in the dialog.
	 *            The mask should contain one or more of the resource type bit masks
	 *            defined in {@link IResource#getType()}
	 */
	public FilteredResourcesSelectionDialog(Shell shell, boolean multi,
			IContainer container, int typesMask) {
		super(shell, multi);

		setSelectionHistory(new ResourceSelectionHistory());

		setTitle(IDEWorkbenchMessages.OpenResourceDialog_title);

		/*
		 * Allow location of paths relative to a searchContainer, which is
		 * initialized from the active editor or the selected element.
		 */
		IWorkbenchWindow ww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (ww != null) {
			IWorkbenchPage activePage = ww.getActivePage();
			if (activePage != null) {
				IResource resource = null;
				IEditorPart activeEditor = activePage.getActiveEditor();
				if (activeEditor != null && activeEditor == activePage.getActivePart()) {
					IEditorInput editorInput = activeEditor.getEditorInput();
					resource = ResourceUtil.getResource(editorInput);
				} else {
					ISelection selection = ww.getSelectionService().getSelection();
					if (selection instanceof IStructuredSelection) {
						IStructuredSelection structuredSelection = (IStructuredSelection) selection;
						if (structuredSelection.size() == 1) {
							resource = ResourceUtil.getResource(structuredSelection.getFirstElement());
						}
					}
				}
				if (resource != null) {
					if (!(resource instanceof IContainer)) {
						resource = resource.getParent();
					}
					searchContainer = (IContainer) resource;
				}
			}
		}

		this.container = container;
		this.typeMask = typesMask;

		resourceItemLabelProvider = new ResourceItemLabelProvider();

		resourceItemDetailsLabelProvider = new ResourceItemDetailsLabelProvider();

		setListLabelProvider(resourceItemLabelProvider);
		setDetailsLabelProvider(resourceItemDetailsLabelProvider);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IIDEHelpContextIds.OPEN_RESOURCE_DIALOG);
	}

	@Override
	public void setTitle(String title) {
		super.setTitle(title);
		this.title = title;
	}

	/**
	 * Adds or replaces subtitle of the dialog
	 *
	 * @param text
	 *            the new subtitle
	 */
	private void setSubtitle(String text) {
		if (text == null || text.length() == 0) {
			getShell().setText(title);
		} else {
			getShell().setText(title + " - " + text); //$NON-NLS-1$
		}
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = IDEWorkbenchPlugin.getDefault()
				.getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings = IDEWorkbenchPlugin.getDefault().getDialogSettings()
					.addNewSection(DIALOG_SETTINGS);
		}

		return settings;
	}

	@Override
	protected void storeDialog(IDialogSettings settings) {
		super.storeDialog(settings);

		settings.put(SHOW_DERIVED, showDerivedResourcesAction.isChecked());
		settings.put(FILTER_BY_LOCATION, this.groupResourcesByLocationAction.isChecked());

		XMLMemento memento = XMLMemento.createWriteRoot("workingSet"); //$NON-NLS-1$
		workingSetFilterActionGroup.saveState(memento);
		workingSetFilterActionGroup.dispose();
		StringWriter writer = new StringWriter();
		try {
			memento.save(writer);
			settings.put(WORKINGS_SET_SETTINGS, writer.getBuffer().toString());
		} catch (IOException e) {
			StatusManager.getManager().handle(
					new Status(IStatus.ERROR, WorkbenchPlugin.PI_WORKBENCH,
							IStatus.ERROR, "", e)); //$NON-NLS-1$
			// don't do anything. Simply don't store the settings
		}
	}

	@Override
	protected void restoreDialog(IDialogSettings settings) {
		super.restoreDialog(settings);

		boolean showDerived = false;
		if (settings.get(SHOW_DERIVED) != null) {
			showDerived = settings.getBoolean(SHOW_DERIVED);
		}
		showDerivedResourcesAction.setChecked(showDerived);
		this.isDerived = showDerived;

		boolean groupByLocation = true;
		if (settings.get(FILTER_BY_LOCATION) != null) {
			groupByLocation = settings.getBoolean(FILTER_BY_LOCATION);
		}
		this.groupResourcesByLocationAction.setChecked(groupByLocation);
		this.filterResourceByLocation.setEnabled(groupByLocation);
		addListFilter(this.filterResourceByLocation);

		String setting = settings.get(WORKINGS_SET_SETTINGS);
		if (setting != null) {
			try {
				IMemento memento = XMLMemento.createReadRoot(new StringReader(
						setting));
				workingSetFilterActionGroup.restoreState(memento);
			} catch (WorkbenchException e) {
				StatusManager.getManager().handle(
						new Status(IStatus.ERROR, WorkbenchPlugin.PI_WORKBENCH,
								IStatus.ERROR, "", e)); //$NON-NLS-1$
				// don't do anything. Simply don't restore the settings
			}
		}

		addListFilter(workingSetFilter);

		applyFilter();
	}

	@Override
	protected void fillViewMenu(IMenuManager menuManager) {
		super.fillViewMenu(menuManager);

		showDerivedResourcesAction = new ShowDerivedResourcesAction();
		menuManager.add(showDerivedResourcesAction);
		this.groupResourcesByLocationAction = new GroupResourcesByLocationAction();
		menuManager.add(this.groupResourcesByLocationAction);

		workingSetFilterActionGroup = new WorkingSetFilterActionGroup(
				getShell(), event -> {
					String property = event.getProperty();

					if (WorkingSetFilterActionGroup.CHANGE_WORKING_SET
							.equals(property)) {

						IWorkingSet workingSet = (IWorkingSet) event
								.getNewValue();

						if (workingSet != null
								&& !(workingSet.isAggregateWorkingSet() && workingSet
										.isEmpty())) {
							workingSetFilter.setWorkingSet(workingSet);
							setSubtitle(workingSet.getLabel());
						} else {
							IWorkbenchWindow window = PlatformUI
									.getWorkbench()
									.getActiveWorkbenchWindow();

							if (window != null) {
								IWorkbenchPage page = window
										.getActivePage();
								workingSet = page.getAggregateWorkingSet();

								if (workingSet.isAggregateWorkingSet()
										&& workingSet.isEmpty()) {
									workingSet = null;
								}
							}

							workingSetFilter.setWorkingSet(workingSet);
							setSubtitle(null);
						}

						scheduleRefresh();
					}
				});

		menuManager.add(new Separator());
		workingSetFilterActionGroup.fillContextMenu(menuManager);
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	@Override
	public Object[] getResult() {
		Object[] result = super.getResult();

		if (result == null)
			return null;

		List resultToReturn = new ArrayList();

		for (int i = 0; i < result.length; i++) {
			if (result[i] instanceof IResource) {
				resultToReturn.add((result[i]));
			}
		}

		return resultToReturn.toArray();
	}

	@Override
	public int open() {
		if (getInitialPattern() == null) {
			IWorkbenchWindow window = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			if (window != null) {
				ISelection selection = window.getSelectionService()
						.getSelection();
				if (selection instanceof ITextSelection) {
					String text = ((ITextSelection) selection).getText();
					if (text != null) {
						text = text.trim();
						if (text.length() > 0) {
							IWorkspace workspace = ResourcesPlugin
									.getWorkspace();
							IStatus result = workspace.validateName(text,
									IResource.FILE);
							if (result.isOK()) {
								setInitialPattern(text);
							}
						}
					}
				}
			}
		}
		return super.open();
	}

	@Override
	public String getElementName(Object item) {
		IResource resource = (IResource) item;
		return resource.getName();
	}

	@Override
	protected IStatus validateItem(Object item) {
		return new Status(IStatus.OK, WorkbenchPlugin.PI_WORKBENCH, 0, "", null); //$NON-NLS-1$
	}

	@Override
	protected ItemsFilter createFilter() {
		return new ResourceFilter(container, searchContainer, isDerived, typeMask);
	}

	@Override
	protected void applyFilter() {
		super.applyFilter();
	}

	@Override
	protected Comparator getItemsComparator() {
		return (o1, o2) -> {
			Collator collator = Collator.getInstance();
			IResource resource1 = (IResource) o1;
			IResource resource2 = (IResource) o2;
			String s1 = resource1.getName();
			String s2 = resource2.getName();

			// Compare names without extension first
			int s1Dot = s1.lastIndexOf('.');
			int s2Dot = s2.lastIndexOf('.');
			String n1 = s1Dot == -1 ? s1 : s1.substring(0, s1Dot);
			String n2 = s2Dot == -1 ? s2 : s2.substring(0, s2Dot);
			int comparability = collator.compare(n1, n2);
			if (comparability != 0)
				return comparability;

			// Compare full names
			if (s1Dot != -1 || s2Dot != -1) {
				comparability = collator.compare(s1, s2);
				if (comparability != 0)
					return comparability;
			}

			// Search for resource relative paths
			if (searchContainer != null) {
				IContainer c11 = resource1.getParent();
				IContainer c21 = resource2.getParent();

				// Return paths 'closer' to the searchContainer first
				comparability = pathDistance(c11) - pathDistance(c21);
				if (comparability != 0)
					return comparability;
			}

			// Finally compare full path segments
			IPath p1 = resource1.getFullPath();
			IPath p2 = resource2.getFullPath();
			// Don't compare file names again, so subtract 1
			int c12 = p1.segmentCount() - 1;
			int c22 = p2.segmentCount() - 1;
			for (int i= 0; i < c12 && i < c22; i++) {
				comparability = collator.compare(p1.segment(i), p2.segment(i));
				if (comparability != 0)
					return comparability;
			}
			comparability = c12 - c22;

			return comparability;
		};
	}

	/**
	 * Return the "distance" of the item from the root of the relative search
	 * container. Distances can be compared (smaller numbers are better).
	 * <br>
	 * - Closest distance is if the item is the same folder as the search container.<br>
	 * - Next are folders inside the search container.<br>
	 * - After all those, distance increases with decreasing matching prefix folder count.<br>
	 *
	 * @param item
	 *            parent of the resource being examined
	 * @return the "distance" of the passed in IResource from the search
	 *         container
	 * @since 3.6
	 */
	private int pathDistance(IContainer item) {
		// Container search path: e.g. /a/b/c
		IPath containerPath = searchContainer.getFullPath();
		// itemPath:          distance:
		// /a/b/c         ==> 0
		// /a/b/c/d/e     ==> 2
		// /a/b           ==> Integer.MAX_VALUE/4 + 1
		// /a/x/e/f       ==> Integer.MAX_VALUE/4 + 2
		// /g/h           ==> Integer.MAX_VALUE/2
		IPath itemPath = item.getFullPath();
		if (itemPath.equals(containerPath))
			return 0;

		int matching = containerPath.matchingFirstSegments(itemPath);
		if (matching == 0)
			return Integer.MAX_VALUE / 2;

		int containerSegmentCount = containerPath.segmentCount();
		if (matching == containerSegmentCount) {
			// inside searchContainer:
			return itemPath.segmentCount() - matching;
		}

		//outside searchContainer:
		return Integer.MAX_VALUE / 4 + containerSegmentCount - matching;
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider,
			ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
			throws CoreException {
		if (itemsFilter instanceof ResourceFilter) {
			IResource[] members = container.members();
			progressMonitor
					.beginTask(
							WorkbenchMessages.FilteredItemsSelectionDialog_searchJob_taskName,
							members.length);

			ResourceProxyVisitor visitor = new ResourceProxyVisitor(
					contentProvider, (ResourceFilter) itemsFilter,
					progressMonitor);

			if (visitor.visit(container.createProxy())) {
				for (int i= 0; i < members.length; i++) {
					IResource member = members[i];
					if (member.isAccessible())
						member.accept(visitor, IResource.NONE);
					progressMonitor.worked(1);
					if (progressMonitor.isCanceled())
						break;
				}
			}

		}
		progressMonitor.done();
	}

	/**
	 * Sets the derived flag on the ResourceFilter instance
	 */
	private class ShowDerivedResourcesAction extends Action {

		/**
		 * Creates a new instance of the action.
		 */
		public ShowDerivedResourcesAction() {
			super(
					IDEWorkbenchMessages.FilteredResourcesSelectionDialog_showDerivedResourcesAction,
					IAction.AS_CHECK_BOX);
		}

		@Override
		public void run() {
			FilteredResourcesSelectionDialog.this.isDerived = isChecked();
			applyFilter();
		}
	}

	/**
	 * Sets the groupByLocation flag on the FilterResourceByLocation instance
	 */
	private class GroupResourcesByLocationAction extends Action {

		/**
		 * Creates a new instance of the action.
		 */
		public GroupResourcesByLocationAction() {
			super(IDEWorkbenchMessages.FilteredResourcesSelectionDialog_groupResourcesWithSameUndelyingLocation,
					IAction.AS_CHECK_BOX);
		}

		@Override
		public void run() {
			FilteredResourcesSelectionDialog.this.filterResourceByLocation.setEnabled(isChecked());
			scheduleRefresh();
			applyFilter();
		}
	}

	/**
	 * A label provider for ResourceDecorator objects. It creates labels with a
	 * resource full path for duplicates. It uses the Platform UI label
	 * decorator for providing extra resource info.
	 */
	private class ResourceItemLabelProvider extends LabelProvider implements
			ILabelProviderListener, IStyledLabelProvider {

		// Need to keep our own list of listeners
		private ListenerList listeners = new ListenerList();

		WorkbenchLabelProvider provider = new WorkbenchLabelProvider();

		/**
		 * Creates a new instance of the class
		 */
		public ResourceItemLabelProvider() {
			super();
			provider.addListener(this);
		}

		@Override
		public Image getImage(Object element) {
			if (!(element instanceof IResource)) {
				return super.getImage(element);
			}

			IResource res = (IResource) element;

			return provider.getImage(res);
		}

		@Override
		public String getText(Object element) {
			if (!(element instanceof IResource)) {
				return super.getText(element);
			}

			IResource res = (IResource) element;

			String str = res.getName();

			// extra info for duplicates
			if (isDuplicateElement(element))
				str = str
						+ " - " + res.getParent().getFullPath().makeRelative().toString(); //$NON-NLS-1$

			return str;
		}

		@Override
		public StyledString getStyledText(Object element) {
			if (!(element instanceof IResource)) {
				return new StyledString(super.getText(element));
			}

			IResource res = (IResource) element;

			StyledString str = new StyledString(res.getName());

			// extra info for duplicates
			if (isDuplicateElement(element)) {
				str.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				str.append(res.getParent().getFullPath().makeRelative().toString(), StyledString.QUALIFIER_STYLER);
			}

//Debugging:
//			int pathDistance = pathDistance(res.getParent());
//			if (pathDistance != Integer.MAX_VALUE / 2) {
//				if (pathDistance > Integer.MAX_VALUE / 4)
//					str.append(" (" + (pathDistance - Integer.MAX_VALUE / 4) + " folders up from current selection)", StyledString.QUALIFIER_STYLER);
//				else
//					str.append(" (" + pathDistance + " folders down from current selection)", StyledString.QUALIFIER_STYLER);
//			}

			return str;
		}

		@Override
		public void dispose() {
			provider.removeListener(this);
			provider.dispose();

			super.dispose();
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
			listeners.add(listener);
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			listeners.remove(listener);
		}

		@Override
		public void labelProviderChanged(LabelProviderChangedEvent event) {
			Object[] l = listeners.getListeners();
			for (int i = 0; i < listeners.size(); i++) {
				((ILabelProviderListener) l[i]).labelProviderChanged(event);
			}
		}

	}

	/**
	 * A label provider for details of ResourceItem objects.
	 */
	private class ResourceItemDetailsLabelProvider extends
			ResourceItemLabelProvider {
		@Override
		public Image getImage(Object element) {
			if (!(element instanceof IResource)) {
				return super.getImage(element);
			}

			IResource parent = ((IResource) element).getParent();
			return provider.getImage(parent);
		}

		@Override
		public String getText(Object element) {
			if (!(element instanceof IResource)) {
				return super.getText(element);
			}

			IResource parent = ((IResource) element).getParent();

			if (parent.getType() == IResource.ROOT) {
				// Get readable name for workspace root ("Workspace"), without
				// duplicating language-specific string here.
				return null;
			}

			return parent.getFullPath()	.makeRelative().toString();
		}

		@Override
		public void labelProviderChanged(LabelProviderChangedEvent event) {
			Object[] l = super.listeners.getListeners();
			for (int i = 0; i < super.listeners.size(); i++) {
				((ILabelProviderListener) l[i]).labelProviderChanged(event);
			}
		}
	}

	/**
	 * Viewer filter which filters resources due to current working set
	 */
	private class CustomWorkingSetFilter extends ViewerFilter {
		private ResourceWorkingSetFilter resourceWorkingSetFilter = new ResourceWorkingSetFilter();

		/**
		 * Sets the active working set.
		 *
		 * @param workingSet
		 *            the working set the filter should work with
		 */
		public void setWorkingSet(IWorkingSet workingSet) {
			resourceWorkingSetFilter.setWorkingSet(workingSet);
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			return resourceWorkingSetFilter.select(viewer, parentElement,
						element);
		}
	}

	/**
	 * ResourceProxyVisitor to visit resource tree and get matched resources.
	 * During visit resources it updates progress monitor and adds matched
	 * resources to ContentProvider instance.
	 */
	private class ResourceProxyVisitor implements IResourceProxyVisitor {

		private AbstractContentProvider proxyContentProvider;

		private ResourceFilter resourceFilter;

		private IProgressMonitor progressMonitor;

		/**
		 * Creates new ResourceProxyVisitor instance.
		 *
		 * @param contentProvider
		 * @param resourceFilter
		 * @param progressMonitor
		 * @throws CoreException
		 */
		public ResourceProxyVisitor(AbstractContentProvider contentProvider,
				ResourceFilter resourceFilter, IProgressMonitor progressMonitor)
				throws CoreException {
			super();
			this.proxyContentProvider = contentProvider;
			this.resourceFilter = resourceFilter;
			this.progressMonitor = progressMonitor;
		}

		@Override
		public boolean visit(IResourceProxy proxy) {

			if (progressMonitor.isCanceled())
				return false;

			IResource resource = proxy.requestResource();

			proxyContentProvider.add(resource, resourceFilter);

			if (resource.getType() == IResource.FOLDER && resource.isDerived()
					&& !resourceFilter.isShowDerived()) {

				return false;
			}

			if (resource.getType() == IResource.FILE) {
				return false;
			}

			return true;
		}
	}

	/**
	 * Filters resources using pattern and showDerived flag. It overrides
	 * ItemsFilter.
	 */
	protected class ResourceFilter extends ItemsFilter {

		private boolean showDerived = false;

		private IContainer filterContainer;

		/**
		 * Container path pattern. Is <code>null</code> when only a file name pattern is used.
		 * @since 3.6
		 */
		private SearchPattern containerPattern;
		/**
		 * Container path pattern, relative to the current searchContainer. Is <code>null</code> if there's no search container.
		 * @since 3.6
		 */
		private SearchPattern relativeContainerPattern;

		/**
		 * Camel case pattern for the name part of the file name (without extension). Is <code>null</code> if there's no extension.
		 * @since 3.6
		 */
		SearchPattern namePattern;
		/**
		 * Camel case pattern for the file extension. Is <code>null</code> if there's no extension.
		 * @since 3.6
		 */
		SearchPattern extensionPattern;

		private int filterTypeMask;

		/**
		 * Creates new ResourceFilter instance
		 *
		 * @param container
		 * @param showDerived
		 *            flag which determine showing derived elements
		 * @param typeMask
		 */
		public ResourceFilter(IContainer container, boolean showDerived,
				int typeMask) {
			super();
			this.filterContainer = container;
			this.showDerived = showDerived;
			this.filterTypeMask = typeMask;
		}

		/**
		 * Creates new ResourceFilter instance
		 *
		 * @param container
		 * @param searchContainer
		 *            IContainer to use for performing relative search
		 * @param showDerived
		 *            flag which determine showing derived elements
		 * @param typeMask
		 * @since 3.6
		 */
		private ResourceFilter(IContainer container, IContainer searchContainer, boolean showDerived, int typeMask) {
			this(container, showDerived, typeMask);

			String stringPattern = getPattern();
			int matchRule = getMatchRule();
			String filenamePattern;

			int sep = stringPattern.lastIndexOf(IPath.SEPARATOR);
			if (sep != -1) {
				filenamePattern = stringPattern.substring(sep + 1, stringPattern.length());
				if ("*".equals(filenamePattern)) //$NON-NLS-1$
					filenamePattern= "**"; //$NON-NLS-1$

				if (sep > 0) {
					if (filenamePattern.length() == 0) // relative patterns don't need a file name
						filenamePattern= "**"; //$NON-NLS-1$

					String containerPattern = stringPattern.substring(0, sep);

					if (searchContainer != null) {
						relativeContainerPattern = new SearchPattern(SearchPattern.RULE_EXACT_MATCH | SearchPattern.RULE_PATTERN_MATCH);
						relativeContainerPattern.setPattern(searchContainer.getFullPath().append(containerPattern).toString());
					}

					if (!containerPattern.startsWith("" + IPath.SEPARATOR)) //$NON-NLS-1$
						containerPattern = IPath.SEPARATOR + containerPattern;
					this.containerPattern= new SearchPattern(SearchPattern.RULE_EXACT_MATCH | SearchPattern.RULE_PREFIX_MATCH | SearchPattern.RULE_PATTERN_MATCH);
					this.containerPattern.setPattern(containerPattern);
				}
				boolean isPrefixPattern = matchRule == SearchPattern.RULE_PREFIX_MATCH
						|| (matchRule == SearchPattern.RULE_PATTERN_MATCH && filenamePattern.endsWith("*")); //$NON-NLS-1$
				if (!isPrefixPattern)
					// Add '<' again as it was removed by SearchPattern
					filenamePattern += '<';
				else if (filenamePattern.endsWith("*") && !filenamePattern.equals("**")) //$NON-NLS-1$ //$NON-NLS-2$
					// Remove added '*' as the filename pattern might be a camel case pattern
					filenamePattern = filenamePattern.substring(0, filenamePattern.length() - 1);
				patternMatcher.setPattern(filenamePattern);
				// Update filenamePattern and matchRule as they might have changed
				filenamePattern = getPattern();
				matchRule = getMatchRule();
			} else {
				filenamePattern= stringPattern;
			}

			int lastPatternDot = filenamePattern.lastIndexOf('.');
			if (lastPatternDot != -1) {
				if (matchRule != SearchPattern.RULE_EXACT_MATCH) {
					namePattern = new SearchPattern();
					namePattern.setPattern(filenamePattern.substring(0, lastPatternDot));
					String extensionPatternStr = filenamePattern.substring(lastPatternDot + 1);
					// Add a '<' except this is a camel case pattern or a prefix pattern
					if (matchRule != SearchPattern.RULE_CAMELCASE_MATCH
							&& matchRule != SearchPattern.RULE_PREFIX_MATCH
							&& !extensionPatternStr.endsWith("*")) //$NON-NLS-1$
						extensionPatternStr += '<';
					extensionPattern = new SearchPattern();
					extensionPattern.setPattern(extensionPatternStr);
				}
			}

		}

		/**
		 * Creates new ResourceFilter instance
		 */
		public ResourceFilter() {
			this(container, searchContainer, isDerived, typeMask);
		}

		/**
		 * @param item
		 *            Must be instance of IResource, otherwise
		 *            <code>false</code> will be returned.
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#isConsistentItem(java.lang.Object)
		 */
		@Override
		public boolean isConsistentItem(Object item) {
			if (!(item instanceof IResource)) {
				return false;
			}
			IResource resource = (IResource) item;
			if (this.filterContainer.findMember(resource.getFullPath()) != null)
				return true;
			return false;
		}

		/**
		 * @param item
		 *            Must be instance of IResource, otherwise
		 *            <code>false</code> will be returned.
		 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter#matchItem(java.lang.Object)
		 */
		@Override
		public boolean matchItem(Object item) {
			if (!(item instanceof IResource)) {
				return false;
			}
			IResource resource = (IResource) item;
			return (this.filterTypeMask & resource.getType()) != 0
					&& matchName(resource)
					&& (this.showDerived || !resource.isDerived());
		}

		private boolean matchName(IResource resource) {
			String name = resource.getName();
			if (nameMatches(name)) {
				if (containerPattern != null) {
					// match full container path:
					String containerPath = resource.getParent().getFullPath().toString();
					if (containerPattern.matches(containerPath))
						return true;
					// match path relative to current selection:
					if (relativeContainerPattern != null)
						return relativeContainerPattern.matches(containerPath);
					return false;
				}
				return true;
			}

			return false;
		}

		private boolean nameMatches(String name) {
			if (namePattern != null) {
				// fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=212565
				int lastDot = name.lastIndexOf('.');
				if (lastDot != -1
						&& namePattern.matches(name.substring(0, lastDot))
						&& extensionPattern.matches(name.substring(lastDot + 1))) {
					return true;
				}
			}
			return matches(name);
		}

		@Override
		public boolean isSubFilter(ItemsFilter filter) {
			if (!super.isSubFilter(filter))
				return false;
			if (filter instanceof ResourceFilter) {
				ResourceFilter resourceFilter = (ResourceFilter) filter;
				if (this.showDerived == resourceFilter.showDerived) {
					if (containerPattern == null) {
						return resourceFilter.containerPattern == null;
					} else if (resourceFilter.containerPattern == null) {
						return false;
					} else {
						return containerPattern.equals(resourceFilter.containerPattern);
					}
				}
			}
			return false;
		}

		@Override
		public boolean equalsFilter(ItemsFilter iFilter) {
			if (!super.equalsFilter(iFilter))
				return false;
			if (iFilter instanceof ResourceFilter) {
				ResourceFilter resourceFilter = (ResourceFilter) iFilter;
				if (this.showDerived == resourceFilter.showDerived) {
					if (containerPattern == null) {
						return resourceFilter.containerPattern == null;
					} else if (resourceFilter.containerPattern == null) {
						return false;
					} else {
						return containerPattern.equals(resourceFilter.containerPattern);
					}
				}
			}
			return false;
		}

		/**
		 * Check show derived flag for a filter
		 *
		 * @return true if filter allow derived resources false if not
		 */
		public boolean isShowDerived() {
			return showDerived;
		}

	}

	private class FilterResourcesByLocation extends ViewerFilter {

		private boolean enabled;

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
			if (!this.enabled) {
				return elements;
			}
			Map<IPath, IResource> bestResourceForPath = new LinkedHashMap<>();
			for (Object item : elements) {
				if (item instanceof IResource) {
					IResource currentResource = (IResource) item;
					IResource otherResource = bestResourceForPath.get(currentResource.getLocation());
					if (otherResource == null || otherResource.getFullPath().segmentCount() > currentResource
							.getFullPath().segmentCount()) {
						bestResourceForPath.put(currentResource.getLocation(), currentResource);
					}
				}
			}
			return bestResourceForPath.values().toArray(new IResource[bestResourceForPath.size()]);
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			// shouldn't be called, but err on the side of caution
			return true;
		}

	}

	/**
	 * <code>ResourceSelectionHistory</code> provides behavior specific to
	 * resources - storing and restoring <code>IResource</code>s state
	 * to/from XML (memento).
	 */
	private class ResourceSelectionHistory extends SelectionHistory {

		@Override
		protected Object restoreItemFromMemento(IMemento element) {
			ResourceFactory resourceFactory = new ResourceFactory();
			IResource resource = (IResource) resourceFactory
					.createElement(element);
			return resource;
		}

		@Override
		protected void storeItemToMemento(Object item, IMemento element) {
			IResource resource = (IResource) item;
			ResourceFactory resourceFactory = new ResourceFactory(resource);
			resourceFactory.saveState(element);
		}

	}

}
