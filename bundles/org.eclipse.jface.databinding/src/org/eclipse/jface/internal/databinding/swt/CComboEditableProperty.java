/*******************************************************************************
 * Copyright (c) 2011 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 306203)
 ******************************************************************************/

package org.eclipse.jface.internal.databinding.swt;

import org.eclipse.swt.custom.CCombo;

/**
 * @since 3.3
 *
 */
public class CComboEditableProperty extends WidgetBooleanValueProperty {
	@Override
	boolean doGetBooleanValue(Object source) {
		return ((CCombo) source).getEditable();
	}

	@Override
	void doSetBooleanValue(Object source, boolean value) {
		((CCombo) source).setEditable(value);
	}

	@Override
	public String toString() {
		return "CCombo.editable <boolean>"; //$NON-NLS-1$
	}
}
