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
 * Listener for changes in the EMacroService.
 */
public interface IMacroStateListener {

	public enum StateChange {
		RECORD_STARTED, PLAYBACK_STARTED, RECORD_FINISHED, PLAYBACK_FINISHED,
	}

	/**
	 * Called when a record started/stopped or a playback started/stopped. Note that
	 * a macro may be played back under a record session (although the opposite is
	 * not true).
	 *
	 * @param macroService
	 *            the macro service where the change happened.
	 * @param stateChange
	 *            the state change which just took place.
	 * @throws CancelMacroException
	 *             to stop the record or playback from happening.
	 */
	public void macroStateChanged(EMacroService macroService, StateChange stateChange)
			throws CancelMacroException;

	/**
	 * Called after the creation of the macro playback context (before notifying
	 * about changes to the macro state or actual playback).
	 *
	 * @param context
	 *            the context for the macro playback.
	 */
	default void macroPlaybackContextCreated(IMacroPlaybackContext context) {
	}

	/**
	 * Called after the creation of the macro record context (before notifying about
	 * changes to the macro state or actual record).
	 *
	 * @param context
	 *            the context for the macro record.
	 */
	default void macroRecordContextCreated(IMacroRecordContext context) {
	}
}
