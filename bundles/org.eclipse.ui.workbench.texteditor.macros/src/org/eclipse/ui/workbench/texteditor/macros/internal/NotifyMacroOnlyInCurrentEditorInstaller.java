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
package org.eclipse.ui.workbench.texteditor.macros.internal;

import javax.inject.Inject;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.macros.CancelMacroException;
import org.eclipse.e4.core.macros.CancelMacroPlaybackException;
import org.eclipse.e4.core.macros.CancelMacroRecordingException;
import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroRecordContext;
import org.eclipse.e4.core.macros.IMacroStateListener;
import org.eclipse.e4.ui.macros.internal.EditorUtils;
import org.eclipse.e4.ui.macros.internal.UserNotifications;
import org.eclipse.swt.custom.StyledText;

/**
 * A listener to the macro state which will enable notifications to the user
 * regarding limitations of recording only in the current editor.
 */
public class NotifyMacroOnlyInCurrentEditorInstaller implements IMacroStateListener {

	private static final String VERIFY_MACRO_ONLY_IN_CURRENT_EDITOR = "VERIFY_MACRO_ONLY_IN_CURRENT_EDITOR"; //$NON-NLS-1$

	@Inject
	private IEclipseContext fEclipseContext;

	private UserNotifications fUserNotifications;

	/**
	 * Provides the class which should be used to give user notifications.
	 *
	 * @return the helper class for giving user notifications.
	 */
	private UserNotifications getUserNotifications() {
		if (fUserNotifications == null) {
			fUserNotifications = ContextInjectionFactory.make(UserNotifications.class, fEclipseContext);
		}
		return fUserNotifications;
	}

	@Override
	public void macroStateChanged(EMacroService macroService, StateChange stateChange)
			throws CancelMacroException {
		if (stateChange == StateChange.RECORD_STARTED) {
			StyledText currentStyledText = EditorUtils.getActiveEditorStyledText(fEclipseContext);
			if (currentStyledText == null) {
				UserNotifications userNotifications = getUserNotifications();
				userNotifications.setMessage(Messages.NotifyMacroOnlyInCurrentEditor_NotRecording);
				userNotifications.notifyNoEditorOnMacroRecordStartup();
				throw new CancelMacroRecordingException();
			}

			IMacroRecordContext context = macroService.getMacroRecordContext();
			NotifyMacroOnlyInCurrentEditorListener notifyMacroOnlyInCurrentEditor = new NotifyMacroOnlyInCurrentEditorListener(
					macroService, fEclipseContext);
			notifyMacroOnlyInCurrentEditor.install();
			context.set(VERIFY_MACRO_ONLY_IN_CURRENT_EDITOR, notifyMacroOnlyInCurrentEditor);

		} else if (stateChange == StateChange.RECORD_FINISHED) {
			IMacroRecordContext context = macroService.getMacroRecordContext();
			Object object = context.get(VERIFY_MACRO_ONLY_IN_CURRENT_EDITOR);
			if (object instanceof NotifyMacroOnlyInCurrentEditorListener) {
				NotifyMacroOnlyInCurrentEditorListener notifyMacroOnlyInCurrentEditor = (NotifyMacroOnlyInCurrentEditorListener) object;
				notifyMacroOnlyInCurrentEditor.uninstall();
			}
		} else if (stateChange == StateChange.PLAYBACK_STARTED) {
			StyledText currentStyledText = EditorUtils.getActiveEditorStyledText(fEclipseContext);
			if (currentStyledText == null) {
				getUserNotifications().notifyNoEditorOnMacroPlaybackStartup();
				throw new CancelMacroPlaybackException();
			}
		}
	}
}
