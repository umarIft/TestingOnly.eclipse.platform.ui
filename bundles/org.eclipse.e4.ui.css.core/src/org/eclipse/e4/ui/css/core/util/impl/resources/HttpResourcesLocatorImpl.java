/*******************************************************************************
 * Copyright (c) 2008 Angelo Zerr and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.ui.css.core.util.impl.resources;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.eclipse.e4.ui.css.core.util.resources.IResourceLocator;

/**
 * Http resources locator implementation.
 */
public class HttpResourcesLocatorImpl implements IResourceLocator {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.e4.ui.css.core.util.resources.IURIResolver#resolve(java.lang.String)
	 */
	public String resolve(String uri) {
		if (uri.startsWith("http"))
			return uri;
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.e4.ui.css.core.util.resources.IResourceLocator#getInputStream(java.lang.String)
	 */
	public InputStream getInputStream(String uri) throws Exception {
		URL url = new java.net.URL((new File("./")).toURL(), uri);
		return url.openStream();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.e4.ui.css.core.util.resources.IResourceLocator#getReader(java.lang.String)
	 */
	public Reader getReader(String uri) throws Exception {
		// TODO
		return null;// new FileReader(new File(uri));
	}

}
