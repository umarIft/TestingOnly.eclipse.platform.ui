/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation, Bug 182443
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 402445
 *******************************************************************************/

package org.eclipse.jface.viewers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * The CellLabelProvider is an abstract implementation of a label provider for
 * structured viewers.
 *
 * <p>
 * <b>This class is intended to be subclassed</b>
 * </p>
 *
 * @param <E>
 *            Type of an element of the model
 *
 * @since 3.3
 * @see ColumnLabelProvider as a concrete implementation
 */
public abstract class CellLabelProvider<E> extends BaseLabelProvider<E> implements IToolTipProvider<E> {

	/**
	 * Create a ViewerLabelProvider for the column at index
	 *
	 * @param labelProvider
	 *            The labelProvider to convert
	 * @return ViewerLabelProvider
	 */
	/* package */static <E> CellLabelProvider<E> createViewerLabelProvider(
			ColumnViewer<E, ?> viewer, IBaseLabelProvider<E> labelProvider) {

		boolean noColumnTreeViewer = viewer instanceof AbstractTreeViewer && viewer
				.doGetColumnCount() == 0;

		if (!noColumnTreeViewer
				&& (labelProvider instanceof ITableLabelProvider
						|| labelProvider instanceof ITableColorProvider || labelProvider instanceof ITableFontProvider))
			return new TableColumnViewerLabelProvider<>(labelProvider);
		if (labelProvider instanceof CellLabelProvider) {
			@SuppressWarnings("unchecked")
			CellLabelProvider<E> cellLabelProvider = (CellLabelProvider<E>) labelProvider;
			return cellLabelProvider;
		}
		return new WrappedViewerLabelProvider<>(labelProvider);

	}

	/**
	 * Get the image displayed in the tool tip for object.
	 *
	 * <p>
	 * <b>If {@link #getToolTipText(Object)} and
	 * {@link #getToolTipImage(Object)} both return <code>null</code> the
	 * control is set back to standard behavior</b>
	 * </p>
	 *
	 * @param object
	 *            the element for which the tool tip is shown
	 * @return {@link Image} or <code>null</code> if there is not image.
	 */

	public Image getToolTipImage(E object) {
		return null;
	}

	/**
	 * Get the text displayed in the tool tip for object.
	 *
	 * <p>
	 * <b>If {@link #getToolTipText(Object)} and
	 * {@link #getToolTipImage(Object)} both return <code>null</code> the
	 * control is set back to standard behavior</b>
	 * </p>
	 *
	 * @param element
	 *            the element for which the tool tip is shown
	 * @return the {@link String} or <code>null</code> if there is not text to
	 *         display
	 */
	@Override
	public String getToolTipText(E element) {
		return null;
	}

	/**
	 * Return the background color used for the tool tip
	 *
	 * @param object
	 *            the {@link Object} for which the tool tip is shown
	 *
	 * @return the {@link Color} used or <code>null</code> if you want to use
	 *         the default color {@link SWT#COLOR_INFO_BACKGROUND}
	 * @see SWT#COLOR_INFO_BACKGROUND
	 */
	public Color getToolTipBackgroundColor(E object) {
		return null;
	}

	/**
	 * The foreground color used to display the the text in the tool tip
	 *
	 * @param object
	 *            the {@link Object} for which the tool tip is shown
	 * @return the {@link Color} used or <code>null</code> if you want to use
	 *         the default color {@link SWT#COLOR_INFO_FOREGROUND}
	 * @see SWT#COLOR_INFO_FOREGROUND
	 */
	public Color getToolTipForegroundColor(E object) {
		return null;
	}

	/**
	 * Get the {@link Font} used to display the tool tip
	 *
	 * @param object
	 *            the element for which the tool tip is shown
	 * @return {@link Font} or <code>null</code> if the default font is to be
	 *         used.
	 */
	public Font getToolTipFont(E object) {
		return null;
	}

	/**
	 * Return the amount of pixels in x and y direction you want the tool tip to
	 * pop up from the mouse pointer. The default shift is 10px right and 0px
	 * below your mouse cursor. Be aware of the fact that you should at least
	 * position the tool tip 1px right to your mouse cursor else click events
	 * may not get propagated properly.
	 *
	 * @param object
	 *            the element for which the tool tip is shown
	 * @return {@link Point} to shift of the tool tip or <code>null</code> if the
	 *         default shift should be used.
	 */
	public Point getToolTipShift(E object) {
		return null;
	}

	/**
	 * Return whether or not to use the native tool tip. If you switch to native
	 * tool tips only the value from {@link #getToolTipText(Object)} is used all
	 * other features from custom tool tips are not supported.
	 *
	 * <p>
	 * To reset the control to native behavior you should return
	 * <code>true</code> from this method and <code>null</code> from
	 * {@link #getToolTipText(Object)} or <code>null</code> from
	 * {@link #getToolTipText(Object)} and {@link #getToolTipImage(Object)} at
	 * the same time
	 * </p>
	 *
	 * @param object
	 *            the {@link Object} for which the tool tip is shown
	 * @return <code>true</code> if native tool tips should be used
	 */
	public boolean useNativeToolTip(E object) {
		return false;
	}

	/**
	 * The time in milliseconds the tool tip is shown for.
	 *
	 * @param object
	 *            the {@link Object} for which the tool tip is shown
	 * @return time in milliseconds the tool tip is shown for
	 */
	public int getToolTipTimeDisplayed(E object) {
		return 0;
	}

	/**
	 * The time in milliseconds until the tool tip is displayed.
	 *
	 * @param object
	 *            the {@link Object} for which the tool tip is shown
	 * @return time in milliseconds until the tool tip is displayed
	 */
	public int getToolTipDisplayDelayTime(E object) {
		return 0;
	}

	/**
	 * The {@link SWT} style used to create the {@link CLabel} (see there for
	 * supported styles). By default {@link SWT#SHADOW_NONE} is used.
	 *
	 * @param object
	 *            the element for which the tool tip is shown
	 * @return the style used to create the label
	 * @see CLabel
	 */
	public int getToolTipStyle(E object) {
		return SWT.SHADOW_NONE;
	}

	/**
	 * Update the label for cell.
	 *
	 * @param cell
	 *            {@link ViewerCell}
	 */
	public abstract void update(ViewerCell<E> cell);

	/**
	 * Initialize this label provider for use with the given column viewer for
	 * the given column. Subclasses may extend but should call the super
	 * implementation (which at this time is empty but may be changed in the
	 * future).
	 *
	 * @param viewer
	 *            the viewer
	 * @param column
	 *            the column, or <code>null</code> if a column is not
	 *            available.
	 *
	 * @since 3.4
	 */
	protected void initialize(ColumnViewer<E,?> viewer, ViewerColumn<E,?> column) {
	}

	/**
	 * Dispose of this label provider which was used with the given column
	 * viewer and column. Subclasses may extend but should call the super
	 * implementation (which calls {@link #dispose()}).
	 *
	 * @param viewer
	 *            the viewer
	 * @param column
	 *            the column, or <code>null</code> if a column is not
	 *            available.
	 *
	 * @since 3.4
	 */
	public void dispose(ColumnViewer<E,?> viewer, ViewerColumn<E,?> column) {
		dispose();
	}

}
