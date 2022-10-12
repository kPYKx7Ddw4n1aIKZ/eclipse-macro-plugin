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

import java.util.Map;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

/**
 * Toggles macro recording.
 */
public class ToggleMacroRecordAction extends AbstractHandler implements IElementUpdater {

	/**
	 * The id of the toggle macro record action.
	 */
	public static final String COMMAND_ID = "org.eclipse.e4.ui.macros.toggleRecordMacro"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) {
		PlatformUI.getWorkbench().getService(EMacroService.class).toggleMacroRecord();
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void updateElement(UIElement element, Map parameters) {
		element.setChecked(PlatformUI.getWorkbench().getService(EMacroService.class).isRecording());
	}

}
