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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.commands.internal.HandlerServiceImpl;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.macros.Activator;
import org.eclipse.e4.core.macros.IMacroInstruction;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.core.macros.MacroPlaybackException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

/**
 * A macro instruction for parameterized commands.
 */
public class MacroInstructionForParameterizedCommand implements IMacroInstruction {

	private static final String ID = "Command"; //$NON-NLS-1$

	private static final String CHARACTER = "character"; //$NON-NLS-1$

	private static final String TYPE = "type"; //$NON-NLS-1$

	private static final String STATE_MASK = "stateMask"; //$NON-NLS-1$

	private static final String KEY_CODE = "keyCode"; //$NON-NLS-1$

	private static final String COMMAND = "command"; //$NON-NLS-1$

	private static final String NO_EVENT = "no_event"; //$NON-NLS-1$

	private EHandlerService fHandlerService;

	private ParameterizedCommand fCmd;

	private Event fEvent;

	/**
	 * @param cmd
	 *            the command recorded.
	 * @param handlerService
	 *            the service to be used to execute the command.
	 */
	public MacroInstructionForParameterizedCommand(ParameterizedCommand cmd, EHandlerService handlerService) {
		this.fCmd = cmd;
		this.fHandlerService = handlerService;
	}

	/**
	 * @param cmd
	 *            the command recorded.
	 * @param event
	 *            the related event.
	 * @param handlerService
	 *            the service to be used to execute the command.
	 */
	public MacroInstructionForParameterizedCommand(ParameterizedCommand cmd, Event event,
			EHandlerService handlerService) {
		this(cmd, handlerService);

		// Create a new event (we want to make sure that only the given info is
		// really needed on playback and don't want to keep a reference to the
		// original widget).
		Event newEvent = new Event();
		newEvent.keyCode = event.keyCode;
		newEvent.stateMask = event.stateMask;
		newEvent.type = event.type;
		newEvent.character = event.character;

		this.fEvent = newEvent;
	}

	@Override
	public void execute(IMacroPlaybackContext macroPlaybackContext) throws MacroPlaybackException {
		ParameterizedCommand cmd = fCmd;
		if (cmd == null) {
			throw new MacroPlaybackException("Parameterized command not set."); //$NON-NLS-1$
		}
		final EHandlerService handlerService = fHandlerService;
		final Command command = cmd.getCommand();

		final IEclipseContext staticContext = EclipseContextFactory.create("keys-staticContext"); //$NON-NLS-1$
		staticContext.set(Event.class, this.fEvent);

		if (!command.isDefined()) {
			throw new MacroPlaybackException(
					String.format("Command: %s not defined (unable to playback macro).", cmd.getId())); //$NON-NLS-1$
		}

		try {
			boolean commandEnabled = handlerService.canExecute(cmd, staticContext);
			if (!commandEnabled) {
				// This is to handle the following case:
				// 1. Open an editor and record keypresses and an undo
				// 2. Close editor/Open editor
				// 3. Playback macro: at this point, the undo action is actually
				// disabled, so, we need to process the current events in the
				// queue and wait for it to be enabled (or fail if it can't be
				// enabled in the current situation).
				for (int i = 0; i < 100; i++) {
					Display.getCurrent().readAndDispatch();
					commandEnabled = handlerService.canExecute(cmd, staticContext);
					if (commandEnabled) {
						break;
					}
				}
				commandEnabled = handlerService.canExecute(cmd, staticContext);
				if (!commandEnabled) {
					String name;
					try {
						name = cmd.getName();
					} catch (NotDefinedException e) {
						name = Messages.MacroInstructionForParameterizedCommand_CommandUnknown;
					}
					throw new MacroPlaybackException(
							String.format(Messages.MacroInstructionForParameterizedCommand_CommandNotEnabled, name, cmd.getId()));
				}
			}

			handlerService.executeHandler(cmd, staticContext);
			final Object commandException = staticContext.get(HandlerServiceImpl.HANDLER_EXCEPTION);
			if (commandException instanceof Exception) {
				Activator.log((Exception) commandException);
			}

		} finally {
			staticContext.dispose();
		}
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String toString() {
		try {
			return String.format(Messages.MacroInstructionForParameterizedCommand_0, this.fCmd.getName());
		} catch (NotDefinedException e) {
			return String.format(Messages.MacroInstructionForParameterizedCommand_0, "Undefined"); //$NON-NLS-1$
		}
	}

	@Override
	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<>();
		String serialized = fCmd.serialize();
		Assert.isNotNull(serialized);
		map.put(COMMAND, serialized);
		if (this.fEvent == null) {
			map.put(NO_EVENT, NO_EVENT);
		}
		map.put(KEY_CODE, Integer.toString(fEvent.keyCode));
		map.put(STATE_MASK, Integer.toString(fEvent.stateMask));
		map.put(TYPE, Integer.toString(fEvent.type));
		map.put(CHARACTER, Character.toString(fEvent.character));

		return map;
	}

	/**
	 * Deserializes a macro instruction which was created from a parameterized
	 * command.
	 *
	 * @param map
	 *            a map (created from {@link #toMap()}.
	 * @param commandManager
	 *            the command manager used to deserialize commands.
	 * @param keybindingDispatcher
	 *            the dispatcher for commands.
	 * @return a macro instruction created from the map (created from
	 *         {@link #toMap()}.
	 * @throws Exception
	 *             if it was not possible to recreate the macro instruction.
	 */
	/* default */ static MacroInstructionForParameterizedCommand fromMap(Map<String, String> map,
			CommandManager commandManager, EHandlerService keybindingDispatcher) throws Exception {
		Assert.isNotNull(commandManager);
		Assert.isNotNull(map);
		Assert.isNotNull(keybindingDispatcher);
		ParameterizedCommand cmd = commandManager.deserialize(map.get(COMMAND));
		if (map.containsKey(NO_EVENT)) {
			return new MacroInstructionForParameterizedCommand(cmd, keybindingDispatcher);
		}
		Event event = new Event();
		event.keyCode = Integer.parseInt(map.get(KEY_CODE));
		event.stateMask = Integer.parseInt(map.get(STATE_MASK));
		event.type = Integer.parseInt(map.get(TYPE));
		event.character = map.get(CHARACTER).charAt(0);
		return new MacroInstructionForParameterizedCommand(cmd, event, keybindingDispatcher);
	}
}
