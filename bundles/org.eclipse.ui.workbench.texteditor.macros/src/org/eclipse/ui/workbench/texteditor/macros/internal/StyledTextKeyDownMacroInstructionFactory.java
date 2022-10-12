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
package org.eclipse.ui.workbench.texteditor.macros.internal;

import java.util.Map;
import org.eclipse.e4.core.macros.IMacroInstruction;
import org.eclipse.e4.core.macros.IMacroInstructionFactory;

/**
 * A factory which will create macro instructions for styled text key presses.
 */
public class StyledTextKeyDownMacroInstructionFactory implements IMacroInstructionFactory {

	@Override
	public IMacroInstruction create(Map<String, String> stringMap) {
		return StyledTextKeyDownMacroInstruction.fromMap(stringMap);
	}

}
