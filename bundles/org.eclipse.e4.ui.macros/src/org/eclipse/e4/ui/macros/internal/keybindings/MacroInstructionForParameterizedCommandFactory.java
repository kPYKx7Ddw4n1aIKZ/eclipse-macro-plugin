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
package org.eclipse.e4.ui.macros.internal.keybindings;

import java.util.Map;
import javax.inject.Inject;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.macros.IMacroInstruction;
import org.eclipse.e4.core.macros.IMacroInstructionFactory;

/**
 * Factory for macro instructions which were created from parameterized
 * commands.
 */
public class MacroInstructionForParameterizedCommandFactory implements IMacroInstructionFactory {

	@Inject
	private CommandManager fCommandManager;

	@Inject
	private EHandlerService fHandlerService;

	@Override
	public IMacroInstruction create(Map<String, String> stringMap) throws Exception {
		return MacroInstructionForParameterizedCommand.fromMap(stringMap, fCommandManager, fHandlerService);
	}

}