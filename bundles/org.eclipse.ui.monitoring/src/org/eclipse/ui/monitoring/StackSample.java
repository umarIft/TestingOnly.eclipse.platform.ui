/*******************************************************************************
 * Copyright (C) 2014, Google Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marcus Eng (Google) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.monitoring;

import java.lang.management.ThreadInfo;

/**
 * A sample of the stack that contains the stack traces and the time stamp.
 *
 * @since 1.0
 */
public class StackSample {
	private final long timestamp;
	private final ThreadInfo[] traces;

	public StackSample(long timestamp, ThreadInfo[] traces) {
		this.timestamp = timestamp;
		this.traces = traces;
	}

	/**
	 * Returns the time stamp for this {@code StackSample}.
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * Returns an array of {@code ThreadInfo} for this {@code StackSample}. The display thread is
	 * always the first in the array.
	 */
	public ThreadInfo[] getStackTraces() {
		return traces;
	}
}
