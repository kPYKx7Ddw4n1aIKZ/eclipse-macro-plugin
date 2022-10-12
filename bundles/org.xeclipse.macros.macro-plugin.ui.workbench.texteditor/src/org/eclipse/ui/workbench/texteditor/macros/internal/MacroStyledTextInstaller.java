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
import javax.inject.Named;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroContext;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.core.macros.IMacroRecordContext;
import org.eclipse.e4.core.macros.IMacroStateListener;
import org.eclipse.e4.ui.macros.internal.EditorUtils;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;

/**
 * Used for enabling macro mode in editors as well as starting and stopping the
 * record of keystrokes.
 */
public class MacroStyledTextInstaller implements IMacroStateListener {

	/**
	 * Constant used to keep memento on the macro context.
	 */
	private static final String MACRO_STYLED_TEXT_HANDLER = "MACRO_STYLED_TEXT_HANDLER"; //$NON-NLS-1$

	/**
	 * Constant used to keep macro recorder on the macro context.
	 */
	private static final String MACRO_STYLED_TEXT_INSTALLER_MACRO_RECORDER = "MACRO_STYLED_TEXT_INSTALLER_MACRO_RECORDER"; //$NON-NLS-1$

	@Inject
	@Named(ISources.ACTIVE_EDITOR_NAME)
	@Optional
	private IEditorPart activeEditor;

	@Override
	public void macroPlaybackContextCreated(IMacroPlaybackContext context) {
		EditorUtils.cacheTargetEditorPart(activeEditor, context);
		EditorUtils.cacheTargetStyledText(activeEditor, context);
	}

	@Override
	public void macroRecordContextCreated(IMacroRecordContext context) {
		EditorUtils.cacheTargetEditorPart(activeEditor, context);
		EditorUtils.cacheTargetStyledText(activeEditor, context);
	}

	/**
	 * Implemented to properly deal with macro recording/playback (i.e., the editor
	 * may need to disable content assist during macro recording and it needs to
	 * record keystrokes to be played back afterwards).
	 */
	@Override
	public void macroStateChanged(EMacroService macroService, StateChange stateChange) {
		if (stateChange == StateChange.RECORD_STARTED) {
			enterMacroMode(macroService.getMacroRecordContext(), macroService.getMacroPlaybackContext());
			enableRecording(macroService, macroService.getMacroRecordContext());

		} else if (stateChange == StateChange.RECORD_FINISHED) {
			leaveMacroMode(macroService.getMacroRecordContext());
			disableRecording(macroService.getMacroRecordContext());

		} else if (stateChange == StateChange.PLAYBACK_STARTED) {
			enterMacroMode(macroService.getMacroPlaybackContext(), macroService.getMacroRecordContext());

		} else if (stateChange == StateChange.PLAYBACK_FINISHED) {
			leaveMacroMode(macroService.getMacroPlaybackContext());

		}
	}

	private void enterMacroMode(IMacroContext context, IMacroContext otherContext) {
		StyledText currentStyledText = EditorUtils.getTargetStyledText(context);
		StyledText otherStyledText = EditorUtils.getTargetStyledText(otherContext);
		if (currentStyledText == otherStyledText || currentStyledText == null) {
			return; // If they are the same in both it means we already entered macro mode in the
					// other before (i.e., started record then started playback).
		}
		MacroStyledTextModeHandler handler = (MacroStyledTextModeHandler) context.get(MACRO_STYLED_TEXT_HANDLER);
		if (handler == null) {
			handler = new MacroStyledTextModeHandler(EditorUtils.getTargetEditorPart(context));
			handler.enterMacroMode();
			context.set(MACRO_STYLED_TEXT_HANDLER, handler);
		}
	}

	private void leaveMacroMode(IMacroContext context) {
		// Restores content assist if it was disabled (based on the memento)
		// Note that it may be null if it was not created in this context
		// (i.e.: started record, started playback, end playback).
		Object object = context.get(MACRO_STYLED_TEXT_HANDLER);
		if (object != null) {
			MacroStyledTextModeHandler macroStyledTextHandler = (MacroStyledTextModeHandler) object;
			macroStyledTextHandler.leaveMacroMode();
		}
	}

	private void enableRecording(EMacroService macroService, IMacroRecordContext context) {
		// When recording install a recorder for key events (and uninstall
		// if not recording).
		// Note: affects only current editor
		Object object = context.get(MACRO_STYLED_TEXT_INSTALLER_MACRO_RECORDER);
		if (object == null) {
			if (macroService.isRecording()) {
				StyledText targetStyledText = EditorUtils.getTargetStyledText(context);
				if (targetStyledText != null && !targetStyledText.isDisposed()) {
					StyledTextMacroRecorder styledTextMacroRecorder = new StyledTextMacroRecorder(macroService);
					styledTextMacroRecorder.install(targetStyledText);
					context.set(MACRO_STYLED_TEXT_INSTALLER_MACRO_RECORDER, styledTextMacroRecorder);
				}
			}
		}
	}

	private void disableRecording(IMacroRecordContext context) {
		StyledText currentStyledText = EditorUtils.getTargetStyledText(context);
		Object object = context.get(MACRO_STYLED_TEXT_INSTALLER_MACRO_RECORDER);
		if (object instanceof StyledTextMacroRecorder) {
			StyledTextMacroRecorder styledTextMacroRecorder = (StyledTextMacroRecorder) object;
			if (currentStyledText != null && !currentStyledText.isDisposed()) {
				styledTextMacroRecorder.uninstall(currentStyledText);
			}
		}
	}

}
