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
package org.eclipse.e4.core.macros;

/**
 * An exception to be raised if there is some issue when playing back macros.
 */
public class MacroPlaybackException extends Exception {

	/**
	 * @param msg
	 *            message for exception.
	 */
	public MacroPlaybackException(String msg) {
		super(msg);
	}

	/**
	 * @param msg
	 *            message for exception.
	 * @param e
	 *            cause of exception.
	 */
	public MacroPlaybackException(String msg, Exception e) {
		super(msg, e);
	}

	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = -2589166244366784288L;

}
