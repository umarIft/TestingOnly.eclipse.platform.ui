/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.internal.ide.undo;

import java.net.URI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ui.ide.dialogs.UIResourceFilterDescription;
import org.eclipse.ui.ide.undo.ResourceDescription;

/**
 * ContainerDescription is a lightweight description that describes a container
 * to be created.
 * 
 * This class is not intended to be instantiated or used by clients.
 * 
 * @since 3.3
 * 
 */
public abstract class ContainerDescription extends AbstractResourceDescription {

	String name;

	URI location;

	UIResourceFilterDescription[] filters;
	
	String defaultCharSet;

	AbstractResourceDescription[] members;

	/**
	 * Create a container description from the specified container handle that
	 * can be used to create the container. The returned ContainerDescription
	 * should represent any non-existing parents in addition to the specified
	 * container.
	 * 
	 * @param container
	 *            the handle of the container to be described
	 * @return a container description describing the container and any
	 *         non-existing parents.
	 */

	public static ContainerDescription fromContainer(IContainer container) {
		return fromContainer(container, false);
	}
	
	/**
	 * Create a group container description from the specified container handle that
	 * can be used to create the container. The returned ContainerDescription
	 * should represent any non-existing parents in addition to the specified
	 * container.
	 * 
	 * @param container
	 *            the handle of the container to be described
	 * @return a container description describing the container and any
	 *         non-existing parents.
	 */

	public static ContainerDescription fromVirtualFolderContainer(IContainer container) {
		return fromContainer(container, true);
	}
	
	protected static ContainerDescription fromContainer(IContainer container, boolean usingVirtualFolder) {
		IPath fullPath = container.getFullPath();
		ContainerDescription firstCreatedParent = null;
		ContainerDescription currentContainerDescription = null;

		// Does the container exist already? If so, then the parent exists and
		// we use the normal creation constructor.
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IContainer currentContainer = (IContainer) root.findMember(fullPath);
		if (currentContainer != null) {
			return (ContainerDescription) ResourceDescription
					.fromResource(container);
		}

		// Create container descriptions for any uncreated parents in the given
		// path.
		currentContainer = root;
		for (int i = 0; i < fullPath.segmentCount(); i++) {
			String currentSegment = fullPath.segment(i);
			IResource resource = currentContainer.findMember(currentSegment);
			if (resource != null) {
				// parent already exists, no need to create a description for it
				currentContainer = (IContainer) resource;
			} else {
				if (i == 0) {
					// parent does not exist and it is a project
					firstCreatedParent = new ProjectDescription(root
							.getProject(currentSegment));
					currentContainerDescription = firstCreatedParent;
				} else {
					IFolder folderHandle = currentContainer.getFolder(new Path(
							currentSegment));
					ContainerDescription currentFolder;
					currentFolder = new FolderDescription(folderHandle, usingVirtualFolder);
					currentContainer = folderHandle;
					if (currentContainerDescription != null) {
						currentContainerDescription.addMember(currentFolder);
					}
					currentContainerDescription = currentFolder;
					if (firstCreatedParent == null) {
						firstCreatedParent = currentFolder;
					}
				}
			}
		}
		return firstCreatedParent;
	}

	/**
	 * Create a ContainerDescription with no state.
	 */
	public ContainerDescription() {

	}

	/**
	 * Create a ContainerDescription from the specified container handle.
	 * Typically used when the container handle represents a resource that
	 * actually exists, although it will not fail if the resource is
	 * non-existent.
	 * 
	 * @param container
	 *            the container to be described
	 */
	public ContainerDescription(IContainer container) {
		super(container);
		this.name = container.getName();
		if (container.isLinked()) {
			this.location = container.getLocationURI();
		}
		try {
			if (container.isAccessible()) {
				defaultCharSet = container.getDefaultCharset(false);
				IResource[] resourceMembers = container.members();
				members = new AbstractResourceDescription[resourceMembers.length];
				for (int i = 0; i < resourceMembers.length; i++) {
					members[i] = (AbstractResourceDescription) ResourceDescription
							.fromResource(resourceMembers[i]);
				}
			}
		} catch (CoreException e) {
			// Eat this exception because it only occurs when the resource
			// does not exist and we have already checked this.
			// We do not want to throw exceptions on the simple constructor, as
			// no one has actually tried to do anything yet.
		}
	}

	/**
	 * Create any child resources known by this container description.
	 * 
	 * @param parentHandle
	 *            the handle of the created parent
	 * @param monitor
	 *            the progress monitor to be used
	 * @param ticks
	 *            the number of ticks allocated for creating children
	 * @throws CoreException
	 */
	protected void createChildResources(IContainer parentHandle,
			IProgressMonitor monitor, int ticks) throws CoreException {

		// restore any children
		if (members != null && members.length > 0) {
			for (int i = 0; i < members.length; i++) {
				members[i].parent = parentHandle;
				members[i].createResource(new SubProgressMonitor(monitor, ticks
						/ members.length));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.internal.ide.undo.ResourceDescription#recordStateFromHistory(org.eclipse.core.resources.IResource,
	 *      org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void recordStateFromHistory(IResource resource,
			IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(
				UndoMessages.FolderDescription_SavingUndoInfoProgress, 100);
		if (members != null) {
			for (int i = 0; i < members.length; i++) {
				if (members[i] instanceof FileDescription) {
					IPath path = resource.getFullPath().append(
							((FileDescription) members[i]).name);
					IFile fileHandle = resource.getWorkspace().getRoot().getFile(
							path);
					members[i].recordStateFromHistory(fileHandle,
							new SubProgressMonitor(monitor, 100 / members.length));
				} else if (members[i] instanceof FolderDescription) {
					IPath path = resource.getFullPath().append(
							((FolderDescription) members[i]).name);
					IFolder folderHandle = resource.getWorkspace().getRoot()
							.getFolder(path);
					members[i].recordStateFromHistory(folderHandle,
							new SubProgressMonitor(monitor, 100 / members.length));
				} 
			}
		}
		monitor.done();
	}

	/**
	 * Return the name of the container described by this ContainerDescription.
	 * 
	 * @return the name of the container.
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Return the first folder found that has no child folders.
	 * 
	 * @return the container description for the first child in the receiver
	 *         that is a leaf, or this container if there are no children.
	 */
	public ContainerDescription getFirstLeafFolder() {
		// If there are no members, this is a leaf
		if (members == null || members.length == 0) {
			return this;
		}
		// Traverse the members and find the first potential leaf
		for (int i = 0; i < members.length; i++) {
			if (members[i] instanceof ContainerDescription) {
				return ((ContainerDescription) members[i]).getFirstLeafFolder();
			}
		}
		// No child folders were found, this is a leaf
		return this;
	}

	/**
	 * Add the specified resource description as a member of this resource
	 * description
	 * 
	 * @param member
	 *            the resource description considered a member of this
	 *            container.
	 */
	public void addMember(AbstractResourceDescription member) {
		if (members == null) {
			members = new AbstractResourceDescription[] { member };
		} else {
			AbstractResourceDescription[] expandedMembers = new AbstractResourceDescription[members.length + 1];
			System.arraycopy(members, 0, expandedMembers, 0, members.length);
			expandedMembers[members.length] = member;
			members = expandedMembers;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.internal.ide.undo.ResourceDescription#restoreResourceAttributes(org.eclipse.core.resources.IResource)
	 */
	@Override
	protected void restoreResourceAttributes(IResource resource)
			throws CoreException {
		super.restoreResourceAttributes(resource);
		Assert.isLegal(resource instanceof IContainer);
		IContainer container = (IContainer) resource;
		if (defaultCharSet != null) {
			container.setDefaultCharset(defaultCharSet, null);
		}
	}

	/**
	 * Set the location to which this container is linked.
	 * 
	 * @param location
	 *            the location URI, or <code>null</code> if there is no link
	 */
	public void setLocation(URI location) {
		this.location = location;
	}

	/**
	 * Set the filters to which should be created on this container.
	 * 
	 * @param filters
	 *            the filters
	 */
	public void setFilters(UIResourceFilterDescription[] filters) {
		this.filters = filters;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.internal.ide.undo.ResourceDescription#verifyExistence(boolean)
	 */
	@Override
	public boolean verifyExistence(boolean checkMembers) {
		boolean existence = super.verifyExistence(checkMembers);
		if (existence) {
			if (checkMembers) {
				// restore any children
				if (members != null && members.length > 0) {
					for (int i = 0; i < members.length; i++) {
						if (!members[i].verifyExistence(checkMembers)) {
							return false;
						}
					}
				}
			}
			return true;
		}
		return false;
	}
}
