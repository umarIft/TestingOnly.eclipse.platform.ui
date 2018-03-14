/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jeanderson Candido <http://jeandersonbc.github.io> - Bug 414057
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 402445
 *******************************************************************************/
package org.eclipse.jface.viewers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.JFaceResources;

/**
 * A concrete implementation of the <code>IStructuredSelection</code> interface,
 * suitable for instantiating.
 * <p>
 * This class is not intended to be subclassed.
 * </p>
 *
 * @param <E>
 *            Type of the selected element
 * @noextend This class is not intended to be subclassed by clients.
 */
public class StructuredSelection<E> implements IStructuredSelection<E> {

	/**
	 * The element that make up this structured selection.
	 */
	private E[] elements;

	/**
	 * The element comparer, or <code>null</code>
	 */
	private IElementComparer comparer;

	/**
	 * The canonical empty selection. This selection should be used instead of
	 * <code>null</code>.
	 */
	@SuppressWarnings("rawtypes")
	public static final StructuredSelection<?> EMPTY = new StructuredSelection();

	/**
	 * Creates a new empty selection. See also the static field
	 * <code>EMPTY</code> which contains an empty selection singleton.
	 *
	 * @see #EMPTY
	 */
	public StructuredSelection() {
	}

	/**
	 * Creates a structured selection from the given elements. The given element
	 * array must not be <code>null</code>.
	 *
	 * @param elements
	 *            an array of elements
	 */

	public StructuredSelection(E[] elements) {
		Assert.isNotNull(elements);
		@SuppressWarnings("unchecked")
		E[] castedArray = (E[]) new Object[elements.length];
		this.elements = castedArray;
		System.arraycopy(elements, 0, this.elements, 0, elements.length);
	}

	/**
	 * Creates a structured selection containing a single object. The object
	 * must not be <code>null</code>.
	 *
	 * @param element
	 *            the element
	 */
	public StructuredSelection(E element) {
		Assert.isNotNull(element);
		@SuppressWarnings("unchecked")
		E[] castedArray = (E[]) new Object[] { element };
		elements = castedArray;
	}

	/**
	 * Creates a structured selection from the given <code>List</code>.
	 *
	 * @param elements
	 *            list of selected elements
	 */
	public StructuredSelection(List<E> elements) {
		this(elements, null);
	}

	/**
	 * Creates a structured selection from the given <code>List</code> and
	 * element comparer. If an element comparer is provided, it will be used to
	 * determine equality between structured selection objects provided that
	 * they both are based on the same (identical) comparer. See bug
	 *
	 * @param elements
	 *            list of selected elements
	 * @param comparer
	 *            the comparer, or null
	 * @since 3.4
	 */
	public StructuredSelection(List<E> elements, IElementComparer comparer) {
		Assert.isNotNull(elements);
		@SuppressWarnings("unchecked")
		E[] castedArray = (E[]) elements.toArray();
		this.elements = castedArray;
		this.comparer = comparer;
	}

	/**
	 * Returns whether this structured selection is equal to the given object.
	 * Two structured selections are equal if they contain the same elements in
	 * the same order.
	 *
	 * @param o
	 *            the other object
	 * @return <code>true</code> if they are equal, and <code>false</code>
	 *         otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		// null and other classes
		if (!(o instanceof StructuredSelection)) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		StructuredSelection<?> s2 = (StructuredSelection) o;
		// either or both empty?
		if (isEmpty()) {
			return s2.isEmpty();
		}
		if (s2.isEmpty()) {
			return false;
		}

		boolean useComparer = comparer != null && comparer == s2.comparer;

		// size
		int myLen = elements.length;
		if (myLen != s2.elements.length) {
			return false;
		}
		// element comparison
		for (int i = 0; i < myLen; i++) {
			if (useComparer) {
				if (!comparer.equals(elements[i], s2.elements[i])) {
					return false;
				}
			} else {
				if (!elements[i].equals(s2.elements[i])) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public E getFirstElement() {
		return isEmpty() ? null : elements[0];
	}

	@Override
	public boolean isEmpty() {
		return elements == null || elements.length == 0;
	}

	@Override
	public Iterator<E> iterator() {
		@SuppressWarnings("unchecked")
		E[] objects = (E[]) new Object[0];
		return Arrays.asList(elements == null ? objects : elements).iterator();
	}

	@Override
	public int size() {
		return elements == null ? 0 : elements.length;
	}

	@Override
	public E[] toArray() {
		@SuppressWarnings("unchecked")
		E[] objects = (E[]) new Object[0];
		return elements == null ? objects : (E[]) elements.clone();
	}

	@Override
	public List<E> toList() {
		@SuppressWarnings("unchecked")
		E[] objects = (E[]) new Object[0];
		return Arrays.asList(elements == null ? objects : elements);
	}

	/**
	 * Internal method which returns a string representation of this selection
	 * suitable for debug purposes only.
	 *
	 * @return debug string
	 */
	@Override
	public String toString() {
		return isEmpty() ? JFaceResources.getString("<empty_selection>") : toList().toString(); //$NON-NLS-1$
	}
}
