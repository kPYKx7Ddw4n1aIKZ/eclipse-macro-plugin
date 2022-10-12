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
 * The context bound to the macro record or playback.
 * {@link org.eclipse.e4.core.macros.IMacroStateListener Macro state listeners}
 * registered with a {@link EMacroService} may use it as a simple key-value
 * store to keep macro-specific state during macro recording and playback.
 */
public interface IMacroContext {

	/**
	 * Gets a value for a given key stored in the macro context or {@code null} if
	 * it's not available.
	 *
	 * @param key
	 *            the key of the variable to be retrieved.
	 * @return the object related to that variable.
	 */
	public Object get(String key);

	/**
	 * Sets a value to a given key, which may be later retrieved through
	 * {@link #get(String)}.
	 *
	 * @param key
	 *            the key of the variable to store.
	 * @param value
	 *            the value to be stored for that key.
	 */
	public void set(String key, Object value);

}
