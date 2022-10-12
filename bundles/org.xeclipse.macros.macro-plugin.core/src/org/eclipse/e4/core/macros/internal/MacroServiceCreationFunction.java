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

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.macros.EMacroService;

/**
 * Creates a
 * {@link org.eclipse.e4.core.macros.internal.MacroServiceImpl} (to be
 * bound to EMacroService).
 *
 * @note internal API: users should generally just get the EMacroService as a
 *       service or with @Inject. Will always return the same instance (only a
 *       single macro service is expected per application).
 */
public class MacroServiceCreationFunction extends ContextFunction {

	private static EMacroService fService;

	@Override
	public Object compute(IEclipseContext context, String contextKey) {
		if (fService == null) {
			fService = ContextInjectionFactory.make(MacroServiceImpl.class, context);
		}
		return fService;
	}
}
