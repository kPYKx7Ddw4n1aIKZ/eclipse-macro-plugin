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
package org.eclipse.e4.core.macros.internal;

import org.eclipse.osgi.util.NLS;

/**
 * @since 0.1.0
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.e4.core.macros.internal.messages"; //$NON-NLS-1$
	public static String SavedJSMacro_MacrosEvalError;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
