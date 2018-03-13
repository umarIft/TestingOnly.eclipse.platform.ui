/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.ui.internal.workbench;

import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	/**
	 * The bundle symbolic name.
	 */
	public static final String PI_WORKBENCH = "org.eclipse.e4.ui.workbench"; //$NON-NLS-1$

	private static Activator activator;

	private BundleContext context;
	private ServiceTracker locationTracker;

	private ServiceTracker debugTracker;
	private ServiceTracker logTracker;

	/** Tracks all bundles which are in the state: RESOLVED, STARTING, ACTIVE or STOPPING. */
	private BundleTracker resolvedBundles;

	/** A BundleTrackerCustomizer which is able to resolve a bundle to the a symbolic name. */
	private final BundleFinder bundleFinder = new BundleFinder();

	private DebugTrace trace;

	/**
	 * Get the default activator.
	 * 
	 * @return a BundleActivator
	 */
	public static Activator getDefault() {
		return activator;
	}

	/**
	 * @return the bundle object
	 */
	public Bundle getBundle() {
		return context.getBundle();
	}

	/**
	 * @param bundleName
	 *            the bundle symbolic name
	 * @return A bundle if found, or <code>null</code>
	 */
	public Bundle getBundleForName(String bundleName) {
		return bundleFinder.findBundle(bundleName);
	}

	/**
	 * @return this bundles context
	 */
	public BundleContext getContext() {
		return context;
	}

	/**
	 * @return the instance Location service
	 */
	public Location getInstanceLocation() {
		if (locationTracker == null) {
			Filter filter = null;
			try {
				filter = context.createFilter(Location.INSTANCE_FILTER);
			} catch (InvalidSyntaxException e) {
				// ignore this. It should never happen as we have tested the
				// above format.
			}
			locationTracker = new ServiceTracker(context, filter, null);
			locationTracker.open();
		}
		return (Location) locationTracker.getService();
	}

	public void start(BundleContext context) throws Exception {
		activator = this;
		this.context = context;

		// track required bundles
		resolvedBundles = new BundleTracker(context, Bundle.RESOLVED | Bundle.STARTING
				| Bundle.ACTIVE | Bundle.STOPPING, bundleFinder);
		resolvedBundles.open();
	}

	public void stop(BundleContext context) throws Exception {
		if (locationTracker != null) {
			locationTracker.close();
			locationTracker = null;
		}
		if (debugTracker != null) {
			trace = null;
			debugTracker.close();
			debugTracker = null;
		}
		if (logTracker != null) {
			logTracker.close();
			logTracker = null;
		}
		if (resolvedBundles != null) {
			// the close of the BundleTracker will also remove all entries form the BundleFinder
			resolvedBundles.close();
			resolvedBundles = null;
		}
	}

	public DebugOptions getDebugOptions() {
		if (debugTracker == null) {
			if (context == null)
				return null;
			debugTracker = new ServiceTracker(context, DebugOptions.class.getName(), null);
			debugTracker.open();
		}
		return (DebugOptions) debugTracker.getService();
	}

	public DebugTrace getTrace() {
		if (trace == null) {
			trace = getDebugOptions().newDebugTrace(PI_WORKBENCH);
		}
		return trace;
	}

	public static void trace(String option, String msg, Throwable error) {
		final DebugOptions debugOptions = activator.getDebugOptions();
		if (debugOptions.isDebugEnabled()
				&& debugOptions.getBooleanOption(PI_WORKBENCH + option, false)) {
			System.out.println(msg);
			if (error != null) {
				error.printStackTrace(System.out);
			}
		}
		activator.getTrace().trace(option, msg, error);
	}

	public LogService getLogService() {
		LogService logService = null;
		if (logTracker != null) {
			logService = (LogService) logTracker.getService();
		} else {
			if (context != null) {
				logTracker = new ServiceTracker(context, LogService.class.getName(), null);
				logTracker.open();
				logService = (LogService) logTracker.getService();
			}
		}
		if (logService == null) {
			logService = new LogService() {
				public void log(int level, String message) {
					log(null, level, message, null);
				}

				public void log(int level, String message, Throwable exception) {
					log(null, level, message, exception);
				}

				public void log(ServiceReference sr, int level, String message) {
					log(sr, level, message, null);
				}

				public void log(ServiceReference sr, int level, String message, Throwable exception) {
					if (level == LogService.LOG_ERROR) {
						System.err.print("ERROR: "); //$NON-NLS-1$
					} else if (level == LogService.LOG_WARNING) {
						System.err.print("WARNING: "); //$NON-NLS-1$
					} else if (level == LogService.LOG_INFO) {
						System.err.print("INFO: "); //$NON-NLS-1$
					} else if (level == LogService.LOG_DEBUG) {
						System.err.print("DEBUG: "); //$NON-NLS-1$
					} else {
						System.err.print("log level " + level + ": "); //$NON-NLS-1$ //$NON-NLS-2$
					}
					System.err.println(message);
					if (exception != null) {
						exception.printStackTrace(System.err);
					}
				}
			};
		}
		return logService;
	}

	public static void log(int level, String message) {
		LogService logService = activator.getLogService();
		if (logService != null)
			logService.log(level, message);
	}

	public static void log(int level, String message, Throwable exception) {
		LogService logService = activator.getLogService();
		if (logService != null)
			logService.log(level, message, exception);
	}

}
