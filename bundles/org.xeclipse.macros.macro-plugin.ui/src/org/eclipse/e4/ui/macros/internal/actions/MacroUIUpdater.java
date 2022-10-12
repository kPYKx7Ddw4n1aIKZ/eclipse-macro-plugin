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
package org.eclipse.e4.ui.macros.internal.actions;

import javax.inject.Inject;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroInstruction;
import org.eclipse.e4.core.macros.IMacroInstructionsListener;
import org.eclipse.e4.core.macros.IMacroStateListener;
import org.eclipse.e4.ui.macros.internal.UserNotifications;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

/**
 * Makes sure that the toolbar elements are kept properly updated even if the
 * macro is programmatically stopped.
 */
public class MacroUIUpdater implements IMacroStateListener {

	/**
	 * A listener which will show messages to the user while he types macro
	 * instructions.
	 */
	private static final class MacroInstructionsListener implements IMacroInstructionsListener {

		/**
		 * Helper class for giving notifications to the user.
		 */
		private UserNotifications fUserNotifications;

		/**
		 * @param userNotifications
		 *            the helper class for giving notifications to the user.
		 */
		public MacroInstructionsListener(UserNotifications userNotifications) {
			this.fUserNotifications = userNotifications;
		}

		@Override
		public void postAddMacroInstruction(IMacroInstruction macroInstruction) {
			this.fUserNotifications.setMessage(Messages.KeepMacroUIUpdated_RecordedInMacro + macroInstruction);
		}
	}

	private boolean wasRecording = false;

	private boolean wasPlayingBack = false;

	private IMacroInstructionsListener fMacroInstructionsListener;

	private UserNotifications fUserNotifications;

	@Inject
	private IEclipseContext fEclipseContext;

	private UserNotifications getUserNotifications() {
		if (fUserNotifications == null) {
			fUserNotifications = ContextInjectionFactory.make(UserNotifications.class, fEclipseContext);
		}
		return fUserNotifications;
	}

	@Override
	public void macroStateChanged(EMacroService macroService, StateChange stateChange) {
		// Update the toggle action state.
		ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
		commandService.refreshElements(ToggleMacroRecordAction.COMMAND_ID, null);

		// Show a message to the user saying about the macro state.
		if (macroService.isRecording() != wasRecording) {
			if (!wasRecording) {
				getUserNotifications().setMessage(Messages.KeepMacroUIUpdated_StartMacroRecord);
			} else {
				// When we stop the record, clear the message.
				getUserNotifications().setMessage(null);
			}
			wasRecording = macroService.isRecording();
		}
		if (macroService.isPlayingBack() != wasPlayingBack) {
			if (!wasPlayingBack) {
				getUserNotifications().setMessage(Messages.KeepMacroUIUpdated_StartMacroPlayback);
			} else {
				// When we stop the playback, clear the message.
				getUserNotifications().setMessage(null);
			}
			wasPlayingBack = macroService.isPlayingBack();
		}

		if (macroService.isRecording()) {
			if (fMacroInstructionsListener == null) {
				fMacroInstructionsListener = new MacroInstructionsListener(getUserNotifications());
				macroService.addMacroInstructionsListener(fMacroInstructionsListener);
			}
		} else {
			if (fMacroInstructionsListener != null) {
				macroService.removeMacroInstructionsListener(fMacroInstructionsListener);
				fMacroInstructionsListener = null;
			}
		}
	}
}
