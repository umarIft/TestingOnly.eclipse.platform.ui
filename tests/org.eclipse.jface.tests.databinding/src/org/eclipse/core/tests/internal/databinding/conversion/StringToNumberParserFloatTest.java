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

import org.eclipse.core.internal.databinding.conversion.StringToNumberParser;

/**
 * @since 1.1
 *
 */
public class StringToNumberParserFloatTest extends
		StringToNumberParserTestHarness {

	protected boolean assertValid(Number number) {
		return StringToNumberParser.inFloatRange(number);
	}

	protected Number getValidMax() {
		return new Float(Float.MAX_VALUE);
	}

	protected Number getValidMin() {
		return new Float(-Float.MAX_VALUE);
	}

}
