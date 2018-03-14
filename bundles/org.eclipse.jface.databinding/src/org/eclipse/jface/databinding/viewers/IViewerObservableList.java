/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jface.databinding.viewers;

import org.eclipse.core.databinding.observable.list.IObservableList;

/**
 * {@link IObservableList} observing a JFace Viewer.
 *
 * @since 1.2
 *
 */
public interface IViewerObservableList extends IObservableList,
		IViewerObservable {
}
