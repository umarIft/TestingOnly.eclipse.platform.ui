/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     C. Sean Young <csyoung@google.com> - Bug 436645
 ******************************************************************************/

package org.eclipse.ui.internal.navigator.extensions;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * A reference meant to be a key for an evaluation cache.
 *
 * @since 3.3
 * @version 4.4
 */
public class EvaluationReference extends SoftReference {

	private final int hashCode;

	private final String typeName;

	/**
	 * @param referent
	 *            The object to be referenced, must be non-null
	 */
	public EvaluationReference(Object referent) {
		super(referent);
		hashCode = referent.hashCode();
		typeName = referent.getClass().getName();
	}
	/**
	 *
	 * @param referent
	 *            The object to be referenced, must be non-null
	 * @param queue
	 *            The ReferenceQueue to register this instance in
	 */
	public EvaluationReference(Object referent, ReferenceQueue queue) {
		super(referent, queue);
		hashCode = referent.hashCode();
		typeName = referent.getClass().getName();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return hashCode;
	}

	/**
	 * Returns true if the Object given is also an EvaluationReference and if the
	 * referents are equal, or if the referent is null (implying clearing or collection),
	 * if the given Object is exactly this EvaluationReference object.
	 */
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		else if (obj == this)
			return true;
		else if (obj instanceof EvaluationReference) {
			// Don't get if not given an evaluation reference to prevent
			// unnecessary accesses keeping the SoftReference "fresh".
			Object myObj = get();
			// If the inner object is null, then the only EvaluationReference
			// that is equal is itself (checked above).
			if (myObj == null)
				return false;
			EvaluationReference otherRef = (EvaluationReference) obj;
			if (hashCode != otherRef.hashCode)
				return false;
			// Not comparing type; it is valid for two objects of different
			// types to be equal.
			Object otherObj = otherRef.get();
			if (otherObj == null)
				return false;
			return myObj == otherObj || myObj.equals(otherObj);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		Object referent = get();
		if(referent == null)
			return "Evalutation[type="+ typeName +"]";  //$NON-NLS-1$//$NON-NLS-2$
		return "Evalutation[referent="+ referent +"]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
