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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.e4.core.macros.IMacroInstruction;
import org.eclipse.e4.core.macros.IMacroInstructionFactory;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;

/**
 * Provides a way to recreate commands when playing back a macro.
 */
public class MacroPlaybackContextImpl implements IMacroPlaybackContext {

	private final Map<Object, Object> fContext = new HashMap<>();

	private Map<String, IMacroInstructionFactory> fMacroInstructionIdToFactory;

	/**
	 * @param macroInstructionIdToFactory
	 *            a map pointing from the macro instruction id to the factory used
	 *            to create the related macro instruction.
	 */
	public MacroPlaybackContextImpl(Map<String, IMacroInstructionFactory> macroInstructionIdToFactory) {
		fMacroInstructionIdToFactory = macroInstructionIdToFactory;
	}

	@Override
	public Object get(String key) {
		return fContext.get(key);
	}

	@Override
	public void set(String key, Object value) {
		fContext.put(key, value);
	}

	@Override
	public void runMacroInstruction(String macroInstructionId, Map<String, String> macroInstructionParameters)
			throws Exception {
		IMacroInstructionFactory macroFactory = fMacroInstructionIdToFactory.get(macroInstructionId);
		if (macroFactory == null) {
			throw new IllegalStateException("Unable to find IMacroInstructionFactory for macro instruction: " //$NON-NLS-1$
					+ macroInstructionId);
		}

		IMacroInstruction macroInstruction = macroFactory.create(macroInstructionParameters);
		macroInstruction.execute(this);

	}
}
