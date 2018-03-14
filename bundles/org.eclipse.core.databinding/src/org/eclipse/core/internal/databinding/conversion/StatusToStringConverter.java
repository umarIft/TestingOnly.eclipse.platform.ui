/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.core.internal.databinding.conversion;

import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.runtime.IStatus;

/**
 * Converts an IStatus into a String. The message of the status is the returned
 * value.
 *
 * @since 1.0
 */
public class StatusToStringConverter extends Converter<IStatus, String> {
	/**
	 * Constructs a new instance.
	 */
	public StatusToStringConverter() {
		super(IStatus.class, String.class);
	}

	@Override
	public String convert(IStatus fromObject) {
		if (fromObject == null) {
			throw new IllegalArgumentException(
					"Parameter 'fromObject' was null."); //$NON-NLS-1$
		}

		IStatus status = fromObject;
		return status.getMessage();
	}
}
