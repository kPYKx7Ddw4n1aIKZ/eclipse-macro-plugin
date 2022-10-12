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
 * An instance of this interface can be notified of changes during macro
 * recording.
 */
public interface IMacroInstructionsListener {

	/**
	 * Called after a given macro instruction is added to the macro.
	 *
	 * @param macroInstruction
	 *            the macro instruction added to the current macro.
	 */
	void postAddMacroInstruction(IMacroInstruction macroInstruction);
}
