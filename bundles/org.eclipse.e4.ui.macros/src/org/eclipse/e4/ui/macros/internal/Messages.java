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

import org.eclipse.osgi.util.NLS;

/**
 *
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.e4.ui.macros.internal.messages"; //$NON-NLS-1$
	public static String Activator_ErrorMacroRecording;
	public static String UserNotifications_DontShowAgain;
	public static String UserNotifications_EditorChangedMessage;
	public static String UserNotifications_EditorChangedTitle;
	public static String UserNotifications_FindReplaceDialogMessage;
	public static String UserNotifications_FindReplaceDialogTitle;
	public static String UserNotifications_NoEditorForPlaybackMsg;
	public static String UserNotifications_NoEditorForPlaybackTitle;
	public static String UserNotifications_NoEditorForRecordMsg;
	public static String UserNotifications_NoEditorForRecordTitle;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
