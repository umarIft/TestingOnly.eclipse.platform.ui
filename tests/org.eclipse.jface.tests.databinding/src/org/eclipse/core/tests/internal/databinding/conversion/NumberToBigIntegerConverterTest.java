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

package org.eclipse.core.tests.internal.databinding.conversion;

import java.math.BigInteger;

import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.internal.databinding.conversion.NumberToBigIntegerConverter;

import com.ibm.icu.text.NumberFormat;

/**
 * @since 1.1
 */
public class NumberToBigIntegerConverterTest extends NumberToNumberTestHarness {
	private NumberFormat numberFormat;
	
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	protected Number doGetOutOfRangeNumber() {
		return null;
	}

	protected IConverter doGetToBoxedTypeValidator(Class fromType) {
		return new NumberToBigIntegerConverter(numberFormat, fromType);
	}

	protected IConverter doGetToPrimitiveValidator(Class fromType) {
		return null;  //no such thing
	}

	protected Class doGetToType(boolean primitive) {
		return (primitive) ? null : BigInteger.class;
	}
}
