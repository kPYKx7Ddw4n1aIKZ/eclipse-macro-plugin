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

import java.util.Map;

/**
 * Context passed when playing back a macro.
 */
public interface IMacroPlaybackContext extends IMacroContext {

	/**
	 * Runs a macro instruction given its id and parameters.
	 *
	 * @param macroInstructionId
	 *            the id of the macro instruction to be run.
	 * @param macroInstructionParameters
	 *            the parameters to create the macro instruction.
	 * @throws Exception
	 *             if it was not possible to create the macro instruction with the
	 *             given parameters.
	 */
	void runMacroInstruction(String macroInstructionId, Map<String, String> macroInstructionParameters)
			throws Exception;

}
