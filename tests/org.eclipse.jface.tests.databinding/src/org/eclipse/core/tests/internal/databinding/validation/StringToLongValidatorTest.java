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

package org.eclipse.core.tests.internal.databinding.validation;

import org.eclipse.core.databinding.conversion.StringToNumberConverter;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.internal.databinding.validation.NumberFormatConverter;
import org.eclipse.core.internal.databinding.validation.StringToLongValidator;

import com.ibm.icu.text.NumberFormat;

/**
 * @since 1.1
 */
public class StringToLongValidatorTest extends StringToNumberValidatorTestHarness {
	@Override
	protected Number getInRangeNumber() {
		return new Long(1);
	}

	@Override
	protected String getInvalidString() {
		return "1.1";
	}

	@Override
	protected Number getOutOfRangeNumber() {
		return Double.valueOf(Double.MAX_VALUE);
	}

	@Override
	protected NumberFormat setupNumberFormat() {
		return NumberFormat.getIntegerInstance();
	}

	@Override
	protected IValidator setupValidator(NumberFormat numberFormat) {
		NumberFormatConverter converter = StringToNumberConverter.toInteger(numberFormat, false);
		return new StringToLongValidator(converter);
	}
}
