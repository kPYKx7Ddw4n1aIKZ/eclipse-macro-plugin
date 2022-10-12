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
package org.eclipse.e4.ui.macros.internal.keybindings;

import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.core.macros.IMacroRecordContext;
import org.eclipse.e4.core.macros.IMacroStateListener;
import org.eclipse.e4.ui.macros.internal.EditorUtils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;

/**
 * A macro state listener that will install the execution listener when in a
 * record context.
 */
public class CommandManagerExecutionListenerInstaller implements IMacroStateListener {

	@Inject
	private CommandManager fCommandManager;

	@Inject
	private EHandlerService fHandlerService;

	@Inject
	private IEclipseContext fEclipseContext;

	@Inject
	@Named(ISources.ACTIVE_EDITOR_NAME)
	@Optional
	private IEditorPart activeEditor;

	private CommandManagerExecutionListener fCommandManagerExecutionListener;

	/**
	 * Gets the command manager execution listener.
	 *
	 * @return the command manager execution listener.
	 */
	public CommandManagerExecutionListener getCommandManagerExecutionListener() {
		return fCommandManagerExecutionListener;
	}

	@Override
	public void macroStateChanged(EMacroService macroService, StateChange stateChange) {
		if (macroService.isRecording()) {
			if (fCommandManagerExecutionListener == null) {
				fCommandManagerExecutionListener = new CommandManagerExecutionListener(macroService, fHandlerService,
						fEclipseContext);
				fCommandManager.addExecutionListener(fCommandManagerExecutionListener);
			}
		} else {
			if (fCommandManagerExecutionListener != null) {
				fCommandManager.removeExecutionListener(fCommandManagerExecutionListener);
				fCommandManagerExecutionListener = null;
			}
		}
	}

	@Override
	public void macroPlaybackContextCreated(IMacroPlaybackContext macroContext) {
		EditorUtils.cacheTargetStyledText(activeEditor, macroContext);
	}

	@Override
	public void macroRecordContextCreated(IMacroRecordContext macroContext) {
		EditorUtils.cacheTargetStyledText(activeEditor, macroContext);
	}
}
