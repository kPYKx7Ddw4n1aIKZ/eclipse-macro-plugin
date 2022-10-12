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
 * Factory for macro instructions which had the contents of
 * {@link IMacroInstruction#toMap()} persisted.
 *
 * Should be registered through the
 * org.eclipse.e4.core.macros.macroInstructionsFactory extension point (with a
 * match through {@link org.eclipse.e4.core.macros.IMacroInstruction#getId()}).
 */
public interface IMacroInstructionFactory {

	/**
	 * Creates a macro instruction from a string map obtained from
	 * {@link IMacroInstruction#toMap()}.
	 *
	 * @param stringMap
	 *            a map which was created from {@link IMacroInstruction#toMap()}
	 * @return the {@link IMacroInstruction} created from the given stringMap.
	 * @throws Exception
	 *             if some error happened recreating the macro instruction.
	 */
	IMacroInstruction create(Map<String, String> stringMap) throws Exception;

}
