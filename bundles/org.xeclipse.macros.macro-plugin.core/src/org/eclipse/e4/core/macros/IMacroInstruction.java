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
 * A step in a macro. A macro may be composed of multiple macro instructions. A
 * macro instruction can be stored on disk for later reconstruction.
 */
public interface IMacroInstruction {

	/**
	 * Returns the id to be used for the macro instruction. This id may be visible
	 * to the user, so it should be something short and readable (such as
	 * {@code KeyDown}, or {@code Command}). Note that an id cannot be changed
	 * afterwards as this id may be written to disk.
	 *
	 * @return the id for the macro instruction.
	 */
	String getId();

	/**
	 * Executes the macro instruction in the given context.
	 *
	 * @param macroPlaybackContext
	 *            the context used to playback the macro.
	 * @throws MacroPlaybackException
	 *             if an error occurred when executing the macro.
	 */
	void execute(IMacroPlaybackContext macroPlaybackContext) throws MacroPlaybackException;

	/**
	 * Converts the macro instruction into a map for serialization, that can be used
	 * to recreate the macro instruction with an {@link IMacroInstructionFactory}
	 * registered through the
	 * {@code org.eclipse.e4.core.macros.macroInstructionsFactory} extension point.
	 *
	 * @return a map that may be serialized and that can be used to recreate the
	 *         macro instruction.
	 */
	Map<String, String> toMap();

}
