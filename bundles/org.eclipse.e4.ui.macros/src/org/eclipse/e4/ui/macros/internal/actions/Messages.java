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

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
@SuppressWarnings("javadoc")
public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.e4.ui.macros.internal.actions.messages"; //$NON-NLS-1$

	public static String KeepMacroUIUpdated_RecordedInMacro;

	public static String KeepMacroUIUpdated_StartMacroRecord;

	public static String KeepMacroUIUpdated_StartMacroPlayback;

	public static String MacroPlaybackAction_ErrorRunningMacro;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
