/*******************************************************************************
 * Copyright (c) 2017 Fabio Zadrozny and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabio Zadrozny - initial API and implementation - http://eclip.se/8519
 *******************************************************************************/
package org.eclipse.e4.ui.macros;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Plug-in activator for the macros ui.
 */
public class Activator extends AbstractUIPlugin {

	/**
	 * Default plug-in activator instance.
	 */
	private static Activator plugin;

	/**
	 * Upon creating it, set it as the default instance in the class.
	 */
	public Activator() {
		super();
		plugin = this;
	}

	/**
	 * @return the default plug-in activator instance.
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Logs a message.
	 *
	 * @param severity
	 *            the {@link IStatus} severity
	 * @param message
	 *            the status message
	 */
	public static void log(int severity, String message) {
		log(new Status(severity, plugin.getBundle().getSymbolicName(), message));
	}

	/**
	 * Logs an exception.
	 *
	 * @param exception
	 *            the exception to be logged
	 */
	public static void log(Throwable exception) {
		log(new Status(IStatus.ERROR, plugin.getBundle().getSymbolicName(), exception.getMessage(), exception));
	}

	/**
	 * Logs a status.
	 *
	 * @param status
	 *            the status to be logged
	 */
	public static void log(IStatus status) {
		try {
			if (plugin != null) {
				plugin.getLog().log(status);
			} else {
				// The plugin is not available. Just print to stderr.
				System.err.println(status);
			}
		} catch (Exception e) {
			// Print the original status if something happened
			System.err.println(status);
		}
	}
}