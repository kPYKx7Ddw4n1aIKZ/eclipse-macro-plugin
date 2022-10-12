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
 * Extension with the public API for dealing with macros.
 *
 * Can be accessed by getting it as a service:
 * <p>
 * &nbsp;&nbsp;&nbsp;site.getService(EMacroService.class)
 * </p>
 * or by having it injected:
 * <p>
 * &nbsp;&nbsp;&nbsp;@Inject<br/>
 * &nbsp;&nbsp;&nbsp;EMacroService fMacroService;
 * </p>
 * <p>
 * The idea is that clients will become aware that a macro record is taking
 * place (through {@link #isRecording()} and will add their related macro
 * instructions through {@link #addMacroInstruction(IMacroInstruction)}.
 * </p>
 * <p>
 * It is also important to note that any macro instruction added through
 * {@link #addMacroInstruction} must have an {@link IMacroInstructionFactory}
 * registered through the
 * {@code org.eclipse.e4.core.macros.macroInstructionsFactory} extension point
 * (with a match through
 * {@link org.eclipse.e4.core.macros.IMacroInstruction#getId()}).
 * </p>
 */
public interface EMacroService {

	/**
	 * Return {@code true} when a macro is currently being recorded. Note that it is
	 * possible for the user to playback a macro while recording, although the
	 * inverse is not true.
	 *
	 * @return {@code true} when a macro is currently being recorded.
	 */
	boolean isRecording();

	/**
	 * Return {@code true} when a macro is being played back and {@code false} if
	 * there is no macro being played back.
	 *
	 * @return {code true} when a macro is currently being played back.
	 */
	boolean isPlayingBack();

	/**
	 * Adds a macro instruction to be added to the current macro being recorded.
	 * Each type of macro instruction must have an {@link IMacroInstructionFactory}
	 * registered through the
	 * {@code org.eclipse.e4.core.macros.macroInstructionsFactory} extension point
	 * (with a match through
	 * {@link org.eclipse.e4.core.macros.IMacroInstruction#getId()})
	 *
	 * This method is a no-op when no macro being currently recorded.
	 *
	 * @param macroInstruction
	 *            the macro instruction to be added to the macro currently being
	 *            recorded.
	 */
	void addMacroInstruction(IMacroInstruction macroInstruction);

	int PRIORITY_LOW = 0;

	int PRIORITY_HIGH = 10;

	/**
	 * Adds a macro instruction to be added to the current macro being recorded.
	 * This method should be used when an event may trigger the creation of multiple
	 * macro instructions but only one of those should be recorded.
	 *
	 * For instance, if a given {@code KeyDown} event is recorded in a
	 * {@code StyledText} and later an action is triggered by this event, the
	 * recorded action should overwrite the {@code KeyDown} event.
	 *
	 * @param macroInstruction
	 *            the macro instruction to be added to the macro currently being
	 *            recorded.
	 * @param event
	 *            the event that triggered the creation of the macro instruction to
	 *            be added. If there are multiple macro instructions added for the
	 *            same event, only the one with the highest priority will be kept
	 *            (if 2 events have the same priority, the last one will replace the
	 *            previous one).
	 * @param priority
	 *            the priority of the macro instruction being added (to be compared
	 *            against the priority of other added macro instructions for the
	 *            same event).
	 * @see #addMacroInstruction(IMacroInstruction)
	 */
	void addMacroInstruction(IMacroInstruction macroInstruction, Object event, int priority);

	/**
	 * Toggles the macro record mode: if currently not recording, starts recording a
	 * macro, otherwise stops the current recording and saves the macro.
	 *
	 * Note that calling {@link #toggleMacroRecord()} does nothing during play back.
	 * Although it is possible to start recording and then replay a previous macro,
	 * so as to add previously recorded macro instructions to the current macro, the
	 * opposite is not true.
	 */
	void toggleMacroRecord();

	/**
	 * Plays back the last recorded macro.
	 *
	 * Note that is is possible to call this method when recording a macro so as to
	 * add the previously recorded macro instructions to the current macro being
	 * recorded.
	 *
	 * @throws MacroPlaybackException
	 *             if some error happened while recording the macro.
	 */
	void playbackLastMacro() throws MacroPlaybackException;

	/**
	 * Adds a macro state listener to be notified on changes in the macro
	 * record/playback state.
	 *
	 * @param listener
	 *            the listener to be added.
	 */
	void addMacroStateListener(IMacroStateListener listener);

	/**
	 * @param listener
	 *            the macro listener which should no longer be notified of changes.
	 */
	void removeMacroStateListener(IMacroStateListener listener);

	/**
	 * Provides the macro record context or {@code null} if the macro engine is
	 * not recording.
	 *
	 * @return the macro record context created when macro record started or
	 *         {@code null} if not currently recording.
	 */
	IMacroRecordContext getMacroRecordContext();

	/**
	 * Provides the macro playback context or {@code null} if the macro engine
	 * is not playing back.
	 *
	 * @return the macro playback context created when the macro playback
	 *         started or {@code null} if not currently playing back.
	 */
	IMacroPlaybackContext getMacroPlaybackContext();

	// Deal with managing accepted commands during macro record/playback.
	// (by default should load the command behavior through the
	// org.eclipse.e4.core.macros.commandHandling extension point, but it is
	// possible to programmatically change it as needed later on).

	/**
	 * Returns {@code true} if the given Eclipse Core Command should be recorded
	 * and {@code false} otherwise. This specifically means that when a given
	 * Eclipse Core Command is executed, a macro instruction will be created for
	 * it. Likewise, if {@code false} is returned, a macro instruction will not
	 * be created automatically (and as such, it won't be recorded in the
	 * macro). See the {@code org.eclipse.e4.core.macros.commandHandling}
	 * extension point for details.
	 *
	 * @param commandId
	 *            the id of the Eclipse Core Command.
	 *
	 * @return {@code true} if the given Eclipse Core Command should be recorded
	 *         for playback when recording a macro (i.e., an
	 *         {@link org.eclipse.e4.core.macros.IMacroInstruction} will be
	 *         automatically created to play it back when in record mode).
	 *
	 */
	boolean canRecordCommand(String commandId);

	/**
	 * Sets whether a given Eclipse Core Command should have a macro instruction
	 * added automatically while recording a macro (by default, all commands are
	 * recorded, so, this is commonly used to disable the recording of some
	 * command).
	 *
	 * @param commandId
	 *            the Eclipse Core Command id to be customized during macro
	 *            record/playback.
	 *
	 * @param recordInMacro
	 *            if true, the command activation will be automatically recorded in
	 *            the macro -- which means that an
	 *            {@link org.eclipse.e4.core.macros.IMacroInstruction} will be
	 *            automatically created to play it back when in record mode. If
	 *            false, the activation of the command will not be recorded.
	 *
	 * @see {@code org.eclipse.e4.core.macros.commandHandling} extension point
	 */
	void setCanRecordCommand(String commandId, boolean recordInMacro);

	/**
	 * Adds a macro instructions listener (it may be added to validate the current
	 * state of the macro recording).
	 *
	 * @param macroInstructionsListener
	 *            the listener for macro instructions.
	 */
	void addMacroInstructionsListener(IMacroInstructionsListener macroInstructionsListener);

	/**
	 * Removes a macro instructions listener.
	 *
	 * @param macroInstructionsListener
	 *            the listener for macro instructions.
	 */
	void removeMacroInstructionsListener(IMacroInstructionsListener macroInstructionsListener);

}
