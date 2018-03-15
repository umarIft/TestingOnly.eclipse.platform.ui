/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.ui.internal.navigator;

import java.util.Iterator;
import java.util.LinkedHashSet;

import org.eclipse.ui.navigator.INavigatorContentDescriptor;

/**
 * Used to associate the NavigatorContentDescriptor (NCD) with an object that it contributes.
 *
 * The NCD/object association is tracked using the NavigatorContentService.rememberContribution().
 *
 * @since 3.2
 *
 */
public class ContributorTrackingSet extends LinkedHashSet {


	private static final long serialVersionUID = 2516241537206281972L;

	private INavigatorContentDescriptor contributor;
	private INavigatorContentDescriptor firstClassContributor;
	private NavigatorContentService contentService;

	/**
	 * Construct a tracking set.
	 *
	 * @param aContentService
	 */
	public ContributorTrackingSet(NavigatorContentService aContentService) {
		contentService = aContentService;
	}

	/**
	 * Construct a tracking set.
	 *
	 * @param aContentService
	 * @param elements
	 */
	public ContributorTrackingSet(NavigatorContentService aContentService, Object[] elements) {

		for (Object element : elements)
			super.add(element);

		contentService = aContentService;
	}

	@Override
	public boolean add(Object o) {
		if (contributor != null) {
			contentService.rememberContribution(contributor, firstClassContributor, o);
		}
		return super.add(o);
	}

	@Override
	public boolean remove(Object o) {
		contentService.forgetContribution(o);
		return super.remove(o);
	}


	@Override
	public void clear() {
		Iterator it = iterator();
		while (it.hasNext())
			contentService.forgetContribution(it.next());
		super.clear();
	}

	/**
	 *
	 * @return The current contributor.
	 */
	public INavigatorContentDescriptor getContributor() {
		return contributor;
	}

	/**
	 *
	 * @return The current contributor.
	 */
	public INavigatorContentDescriptor getFirstClassContributor() {
		return firstClassContributor;
	}

	/**
	 *
	 * @param newContributor The contributor to record for the next series of adds.
	 * @param theFirstClassContributor The first class contributor associated with the newContributor.
	 */
	public void setContributor(INavigatorContentDescriptor newContributor, INavigatorContentDescriptor theFirstClassContributor) {
		contributor = newContributor;
		firstClassContributor = theFirstClassContributor;
	}

	/**
	 * @param contents
	 */
	public void setContents(Object[] contents) {
		super.clear();
		if(contents != null)
			for (Object content : contents)
				add(content);

	}

	@Override
	public Iterator iterator() {
		return new Iterator() {

			Iterator delegateIterator = ContributorTrackingSet.super.iterator();
			Object current;

			@Override
			public boolean hasNext() {
				return delegateIterator.hasNext();
			}

			@Override
			public Object next() {
				current = delegateIterator.next();
				return current;
			}

			@Override
			public void remove() {
				delegateIterator.remove();
				contentService.forgetContribution(current);
			}
		};
	}

}
