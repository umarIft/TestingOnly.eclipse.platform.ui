/*******************************************************************************
 * Copyright (c) 2009, 2015 Fair Isaac Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fair Isaac Corporation - initial API and implementation
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 460405
 ******************************************************************************/

package org.eclipse.ui.tests.navigator.m12;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.tests.navigator.m12.model.M1Project;

public class M1AdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof M1Project
				&& IProject.class.isAssignableFrom(adapterType)) {
			IResource res = ((M1Project) adaptableObject).getResource();
			if (res instanceof IProject) {
				@SuppressWarnings("unchecked")
				T resource = (T) res;
				return resource;
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { IProject.class };
	}

}

