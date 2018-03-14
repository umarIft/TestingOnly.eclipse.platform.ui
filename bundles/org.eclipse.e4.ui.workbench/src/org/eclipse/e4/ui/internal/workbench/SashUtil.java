/*******************************************************************************
 * Copyright (c) 2014 TwelveTone LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Steven Spungin <steven@spungin.tv> - initial API and implementation
 *******************************************************************************/

package org.eclipse.e4.ui.internal.workbench;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import org.eclipse.e4.ui.internal.workbench.PartSizeInfo.PartResizeMode;
import org.eclipse.e4.ui.model.application.ui.MGenericTile;
import org.eclipse.e4.ui.model.application.ui.MUIElement;

/**
 * Methods to manage sash container size information.
 */
public class SashUtil {

	/**
	 * @param sashContainer
	 * @return The number of visible children in the container.
	 */
	static public List<MUIElement> getVisibleChildren(MGenericTile<?> sashContainer) {
		List<MUIElement> visKids = new ArrayList<MUIElement>();
		for (MUIElement child : sashContainer.getChildren()) {
			if (child.isToBeRendered() && child.isVisible())
				visKids.add(child);
		}
		return visKids;
	}

	/**
	 * If a container does not contain any relative resizing mode children, the last item is
	 * converted to relative resizing mode and set to the default weight. <br/ >
	 * <br />
	 * At least 1 relative weight container must NOT contain a max value. If all contain a max
	 * value, the last one will have the max value removed. <br />
	 * Relative resizing mode must contain weights as the size specifier. Fixed resized mode must
	 * contain absolute sizes.
	 * 
	 * @param sashContainer
	 * @param size
	 *            The size of the container in pixels
	 * @param sashSize
	 */
	static public void validatePartSize(MGenericTile<?> sashContainer, int size, double sashSize) {
		// boolean foundNoMax = false;
		MUIElement lastRelative = null;
		List<MUIElement> visibleChildren = getVisibleChildren(sashContainer);

		// make sure all weights resize containers have relative values and all
		// fixed resize containers have absolute values
		if (size > 0) {
			double totalWeight = getTotalWeightForAllModes(visibleChildren);
			for (MUIElement ele : visibleChildren) {
				PartSizeInfo sizeInfo = PartSizeInfo.get(ele);
				if (sizeInfo.getResizeMode() == PartResizeMode.FIXED) {
					if (sizeInfo.isDefaultAbsolute() == false) {
						sizeInfo.setDefaultAbsolute(true);
						double pixels = sizeInfo.getDefaultValue()
								/ totalWeight
								* getAvailableRelative(sashContainer.isHorizontal(), size, 3,
										visibleChildren);
						if (pixels < 10) {
							pixels = 0;
						}
						sizeInfo.setDefaultValue(pixels);
						sizeInfo.notifyChanged();
					}
				}
			}

			Hashtable<MUIElement, Double> map = new Hashtable<MUIElement, Double>();
			for (MUIElement ele : visibleChildren) {
				PartSizeInfo sizeInfo = PartSizeInfo.get(ele);
				if (sizeInfo.getResizeMode() == PartResizeMode.WEIGHTED) {
					if (sizeInfo.isDefaultAbsolute()) {
						// TODO convert requested pixels to weight
						double pixelsRequested = sizeInfo.getDefaultValue();

						// the total weight of siblings
						double totalRelative = getTotalWeight(visibleChildren);
						if (totalRelative == 0) {
							totalRelative = 10000;
						}
						double totalFixupPixels = getTotalFixedPixelsForWeightModeElements(visibleChildren);

						double availablePixels = size - getTotalFixedPixels(visibleChildren)
								- ((visibleChildren.size() - 1) * sashSize);

						// this is the total weight to divide for all fixed-up
						// items
						double weightForAbsoluteSpecifiedRelativeContainers = (totalFixupPixels * totalRelative)
								/ (availablePixels - totalFixupPixels);

						double ratio = pixelsRequested / totalFixupPixels;
						double newWeight = ratio * weightForAbsoluteSpecifiedRelativeContainers;
						map.put(ele, newWeight);
					}
				}
			}
			for (MUIElement element : map.keySet()) {
				PartSizeInfo sizeInfo = PartSizeInfo.get(element);
				sizeInfo.setDefaultAbsolute(false);
				sizeInfo.setDefaultValue(map.get(element));
				sizeInfo.notifyChanged();
			}
		}

		for (MUIElement ele : visibleChildren) {
			PartSizeInfo sizeInfo = PartSizeInfo.get(ele);
			if (sizeInfo.getResizeMode() == PartResizeMode.WEIGHTED) {
				lastRelative = ele;
			}
		}
		if (lastRelative == null && visibleChildren.isEmpty() == false) {
			lastRelative = visibleChildren.get(visibleChildren.size() - 1);
			PartSizeInfo info = PartSizeInfo.get(lastRelative);
			info.setResizeMode(PartResizeMode.WEIGHTED);
			info.setDefaultAbsolute(false);
			info.setDefaultValue(5000);
			info.notifyChanged();
		}
	}

	/**
	 * @param visibleChildren
	 * @return The total relative weight of all weighted items
	 */
	static public double getTotalWeight(List<MUIElement> visibleChildren) {
		double total = 0;
		for (MUIElement item : visibleChildren) {
			PartSizeInfo info = PartSizeInfo.get(item);
			if (info.getResizeMode() == PartResizeMode.WEIGHTED) {
				if (info.isDefaultAbsolute()) {
					// total += info.getDefaultValue();
				} else {
					total += info.getDefaultValue();
				}
			}
		}
		return total;
	}

	/**
	 * @param visibleChildren
	 * @return The total relative weight of all relative items
	 */
	static public double getTotalWeightForAllModes(List<MUIElement> visibleChildren) {
		double total = 0;
		for (MUIElement item : visibleChildren) {
			PartSizeInfo info = PartSizeInfo.get(item);
			if (!info.isDefaultAbsolute()) {
				total += info.getDefaultValue();
			}
		}
		return total;
	}

	/**
	 * @param visibleChildren
	 * @return The total declared fixed weight of all resize-mode-weighted elements
	 */
	static public int getTotalFixedPixelsForWeightModeElements(List<MUIElement> visibleChildren) {
		int totalFixed = 0;
		for (MUIElement item : visibleChildren) {
			PartSizeInfo info = PartSizeInfo.get(item);
			if (info.getResizeMode() == PartResizeMode.WEIGHTED) {
				if (info.isDefaultAbsolute() == true) {
					totalFixed += info.getDefaultValue();
				}
			}
		}
		return totalFixed;
	}

	/**
	 * @param visibleChildren
	 * @return The total fixed weight of all resize-mode-fixed elements
	 */
	static public int getTotalFixedPixels(List<MUIElement> visibleChildren) {
		int totalFixed = 0;
		for (MUIElement item : visibleChildren) {
			PartSizeInfo info = PartSizeInfo.get(item);
			if (info.getResizeMode() == PartResizeMode.FIXED) {
				if (info.isDefaultAbsolute() == true) {
					totalFixed += info.getDefaultValue();
				}
			}
		}
		return totalFixed;
	}

	/**
	 * 
	 * @param isHorizontal
	 * @param totalSize
	 * @param sashWidth
	 * @param visibleChildren
	 * @return The available pixels for relative containers. This is the total width minus the
	 *         absolute widths and sash widths.
	 */
	static public int getAvailableRelative(boolean isHorizontal, int totalSize, int sashWidth,
			List<MUIElement> visibleChildren) {
		int availableRelative;
		int totalFixed = 0;
		for (MUIElement item : visibleChildren) {
			PartSizeInfo info = PartSizeInfo.get(item);
			if (info.getResizeMode() == PartResizeMode.FIXED) {
				if (info.isDefaultAbsolute() == true) {
					totalFixed += info.getDefaultValue();
				}
			}
		}
		int sashSpace = (visibleChildren.size() - 1) * sashWidth;
		if (isHorizontal) {
			availableRelative = totalSize - totalFixed - sashSpace;
		} else {
			availableRelative = totalSize - totalFixed - sashSpace;
		}
		return availableRelative;
	}

	/**
	 * @param pixelsRequested
	 * @param visibleChildren
	 * @param size
	 * @param sashSize
	 * @return The weighted value equivalent to the requested pixels
	 */
	static public double fixed2Weighted(Double pixelsRequested, List<MUIElement> visibleChildren,
			Double size, Double sashSize) {
		// the total weight of siblings
		double totalRelative = getTotalWeight(visibleChildren);
		if (totalRelative == 0) {
			// TODO use constant
			totalRelative = 10000;
		}

		double availablePixels = size - (getTotalFixedPixels(visibleChildren) - pixelsRequested)
				- ((visibleChildren.size() - 1) * sashSize);

		double weightForAbsoluteSpecifiedRelativeContainers = (pixelsRequested * totalRelative)
				/ (availablePixels - pixelsRequested);

		double newWeight = weightForAbsoluteSpecifiedRelativeContainers;
		return newWeight;
	}

}
