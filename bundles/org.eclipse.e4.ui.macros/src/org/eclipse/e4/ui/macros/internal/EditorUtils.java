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
package org.eclipse.e4.ui.macros.internal;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.macros.IMacroContext;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;

/**
 * Utilities related to getting/storing the current editor from/to the macro
 * context.
 */
public class EditorUtils {

	/**
	 * A variable which holds the current editor when macro record or playback
	 * started.
	 */
	private final static String TARGET_STYLED_TEXT = "TARGET_STYLED_TEXT"; //$NON-NLS-1$

	/**
	 * A variable which holds the current editor part when macro record or playback
	 * started.
	 */
	private final static String TARGET_EDITOR_PART = "TARGET_EDITOR_PART"; //$NON-NLS-1$

	/**
	 * Provides the {@link StyledText} from the passed editor or {@code null} if
	 * not available.
	 *
	 * @param editor
	 *            the editor from where the {@link StyledText} should be gotten.
	 *
	 * @return the {@link StyledText} related to the current editor or
	 *         {@code null} if it is not available (i.e., if the editor passed
	 *         is not a text editor).
	 */
	public static StyledText getActiveEditorStyledText(IEditorPart editor) {
		if (editor == null) {
			return null;
		}
		Control control = editor.getAdapter(Control.class);
		StyledText styledText = null;
		if (control instanceof StyledText) {
			styledText = (StyledText) control;
		}
		return styledText;
	}

	/**
	 * Provides the {@link StyledText} which is currently active in the given
	 * eclipse context or {@code null} if not available.
	 *
	 * @param eclipseContext
	 *            the context to get the active editor from (from where the
	 *            {@link StyledText} will be gotten).
	 * @return the {@link StyledText} from the editor which is currently active in
	 *         the context or {@code null}.
	 */
	public static StyledText getActiveEditorStyledText(IEclipseContext eclipseContext) {
		Object active = eclipseContext.getActive(ISources.ACTIVE_EDITOR_NAME);
		if (active instanceof IEditorPart) {
			return EditorUtils.getActiveEditorStyledText((IEditorPart) active);
		}
		return null;
	}

	/**
	 * Caches the current {@link StyledText} as being the one active in the passed
	 * macro context.
	 *
	 * @param activeEditor
	 *            the editor from there the {@link StyledText} should be gotten.
	 *
	 * @param macroContext
	 *            the macro context where it should be set.
	 */
	public static void cacheTargetStyledText(IEditorPart activeEditor, IMacroContext macroContext) {
		if (macroContext != null && activeEditor != null) {
			Object object = macroContext.get(TARGET_STYLED_TEXT);
			if (object == null) {
				macroContext.set(TARGET_STYLED_TEXT, getActiveEditorStyledText(activeEditor));
			}
		}
	}

	/**
	 * Caches the current editor part as being the one active in the passed macro
	 * context.
	 *
	 * @param activeEditor
	 *            the editor which should be cached as the target editor for the
	 *            macro.
	 *
	 * @param macroContext
	 *            the macro context where it should be set.
	 */
	public static void cacheTargetEditorPart(IEditorPart activeEditor, IMacroContext macroContext) {
		if (macroContext != null && activeEditor != null) {
			Object object = macroContext.get(TARGET_EDITOR_PART);
			if (object == null) {
				macroContext.set(TARGET_EDITOR_PART, activeEditor);
			}
		}
	}

	/**
	 * Gets the {@link StyledText} which was set as the current when the macro
	 * context was created.
	 *
	 * @param macroContext
	 *            the macro context.
	 * @return the StyledText which was current when the recording started or null
	 *         if there was no StyledText active when recording started.
	 */
	public static StyledText getTargetStyledText(IMacroContext macroContext) {
		if (macroContext != null) {
			Object object = macroContext.get(TARGET_STYLED_TEXT);
			if (object instanceof StyledText) {
				return (StyledText) object;
			}
		}
		return null;
	}

	/**
	 * Gets the editor part which was set as the current when the macro context was
	 * created.
	 *
	 * @param macroContext
	 *            the macro context.
	 * @return the editor part which was current when the recording started or null
	 *         if there was no editor part active when the context was created.
	 */
	public static IEditorPart getTargetEditorPart(IMacroContext macroContext) {
		if (macroContext != null) {
			Object object = macroContext.get(TARGET_EDITOR_PART);
			if (object instanceof IEditorPart) {
				return (IEditorPart) object;
			}
		}
		return null;
	}

}
