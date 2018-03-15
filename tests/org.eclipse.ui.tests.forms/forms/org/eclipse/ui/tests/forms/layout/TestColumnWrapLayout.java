/*******************************************************************************
 * Copyright (c) 2011,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ralf M Petter<ralf.petter@gmail.com> - Bug 510241
 *******************************************************************************/

package org.eclipse.ui.tests.forms.layout;

import static org.junit.Assert.assertEquals;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.eclipse.ui.internal.forms.widgets.ColumnLayoutUtils;
import org.junit.Test;

public class TestColumnWrapLayout {

	private final Point p20 = new Point(100, 20);
	private final Point p30 = new Point(100, 30);
	private final Point p50 = new Point(100, 50);
	private final Point p100 = new Point(100, 100);
	private final Point p200 = new Point(100, 200);

	@Test
	public void testEqualSizeColumns() {
		Point[] sizes = { p20, p30, p30, p20, p20, p30 };
		assertEquals(50, ColumnLayoutUtils.computeColumnHeight(3, sizes, 237, 0));
	}

	@Test
	public void testEqualSizeColumnsWithMargins() {
		Point[] sizes = { p20, p30, p30, p20, p20, p30 };
		assertEquals(60, ColumnLayoutUtils.computeColumnHeight(3, sizes, 200, 10));
	}

	@Test
	public void testVariedSizeColumns() {
		Point[] sizes = { p200, p200, p30 };
		assertEquals(230, ColumnLayoutUtils.computeColumnHeight(2, sizes, 100, 0));
	}

	@Test
	public void testLastEntryLargest() {
		Point[] sizes = { p50, p30, p30, p30, p50, p50, p100 };
		assertEquals(100, ColumnLayoutUtils.computeColumnHeight(4, sizes, 100, 0));
	}

	@Test
	public void testLargeMargins() {
		Point[] sizes = { p20, p20, p20, p20, p20, p50, p50};
		assertEquals(260, ColumnLayoutUtils.computeColumnHeight(3, sizes, 100, 100));
	}

	/**
	 * Test that labels with the WRAP property set do indeed wrap.
	 */
	@Test
	public void testColumnLayoutInShell() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		Shell shell = new Shell(display);
		shell.setSize(100, 300);
		GridLayoutFactory.fillDefaults().applyTo(shell);
		Composite inner = new Composite(shell, SWT.NULL);
		ColumnLayout layout = new ColumnLayout();
		layout.verticalSpacing = 5;
		layout.minNumColumns = 2;
		layout.maxNumColumns = 2;
		layout.topMargin=2;
		layout.bottomMargin=3;
		inner.setLayout(layout);
		ControlFactory.create(inner, 20, 20, 30);
		ControlFactory.create(inner, 20, 20, 40);
		ControlFactory.create(inner, 20, 20, 20);
		shell.layout(true);
		assertEquals(70, inner.getSize().y);
		shell.dispose();
	}

}
