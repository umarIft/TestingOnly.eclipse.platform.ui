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

package org.eclipse.core.internal.databinding.validation;

import org.eclipse.core.internal.databinding.conversion.StringToNumberParser;

/**
 * @since 1.0
 */
public class StringToShortValidator extends AbstractStringToNumberValidator {
	private static final Short MIN = new Short(Short.MIN_VALUE);
	private static final Short MAX = new Short(Short.MAX_VALUE);
	
	/**
	 * @param converter
	 */
	public StringToShortValidator(NumberFormatConverter converter) {
		super(converter, MIN, MAX);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.internal.databinding.validation.AbstractStringToNumberValidator#inRange(java.lang.Number)
	 */
	@Override
	protected boolean isInRange(Number number) {
		return StringToNumberParser.inShortRange(number);
	}
}
