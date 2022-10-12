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

import java.util.Stack;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.ui.macros.Activator;
import org.eclipse.e4.ui.macros.internal.EditorUtils;
import org.eclipse.e4.ui.macros.internal.UserNotifications;
import org.eclipse.swt.widgets.Event;

/**
 * Used to record commands being executed in the current macro.
 */
public class CommandManagerExecutionListener implements IExecutionListener {

	private final EMacroService fMacroService;

	private static class ParameterizedCommandAndTrigger {

		private ParameterizedCommand parameterizedCommand;
		private Object trigger;

		private ParameterizedCommandAndTrigger(ParameterizedCommand parameterizedCommand, Object trigger) {
			this.parameterizedCommand = parameterizedCommand;
			this.trigger = trigger;
		}

		@Override
		public String toString() {
			if (parameterizedCommand == null) {
				return "parameterizedCommand == null"; //$NON-NLS-1$
			}
			return parameterizedCommand.getId();
		}
	}

	/**
	 * A stack to keep information on the parameterized commands and what triggered
	 * it.
	 */
	private final Stack<ParameterizedCommandAndTrigger> fParameterizedCommandsAndTriggerStack = new Stack<>();

	/**
	 * The handler service.
	 */
	private final EHandlerService fHandlerService;

	/**
	 * The Eclipse context for dependency injection.
	 */
	private IEclipseContext fEclipseContext;

	/**
	 * @param macroService
	 *            the macro service
	 * @param handlerService
	 *            the handler service (used to execute actions).
	 * @param eclipseContext
	 *            Eclipse context for dependency injection.
	 */
	public CommandManagerExecutionListener(EMacroService macroService, EHandlerService handlerService,
			IEclipseContext eclipseContext) {
		this.fMacroService = macroService;
		this.fHandlerService = handlerService;
		this.fEclipseContext = eclipseContext;
	}

	@Override
	public void notHandled(String commandId, NotHandledException exception) {
		popCommand(commandId);
	}

	@Override
	public void postExecuteFailure(String commandId, ExecutionException exception) {
		popCommand(commandId);
	}

	private ParameterizedCommandAndTrigger popCommand(String commandId) {
		ParameterizedCommandAndTrigger commandAndTrigger = null;
		while (!fParameterizedCommandsAndTriggerStack.empty()) {
			commandAndTrigger = fParameterizedCommandsAndTriggerStack.pop();
			if (commandAndTrigger != null && commandAndTrigger.parameterizedCommand != null) {
				if (commandId.equals(commandAndTrigger.parameterizedCommand.getCommand().getId())) {
					return commandAndTrigger;
				}
			}
		}
		if (commandAndTrigger != null) {
			Activator.log(new RuntimeException(
					String.format("Expected to find %s in parameterizedCommand stack. Found: %s", commandId, //$NON-NLS-1$
							commandAndTrigger)));
		}
		return null;
	}

	@Override
	public void postExecuteSuccess(String commandId, Object returnValue) {
		ParameterizedCommandAndTrigger commandAndTrigger = popCommand(commandId);
		if (commandAndTrigger == null) {
			// Can happen if we didn't get the preExecute (i.e., the toggle
			// macro record is executed and post executed only (the pre execute
			// is skipped because recording still wasn't in place).
			//
			// Another reason could be that it was an event generated for another
			// editor, not the one we should recording.
			return;
		}
		if (fMacroService.isRecording()) {
			// Record it if needed.
			if (fMacroService.canRecordCommand(commandId)) {
				if (commandAndTrigger.trigger instanceof Event) {
					Event swtEvent = (Event) commandAndTrigger.trigger;
					// Only record commands executed in the initial editor.
					if (fFilter.acceptEvent(swtEvent)) {
						fMacroService.addMacroInstruction(new MacroInstructionForParameterizedCommand(
								commandAndTrigger.parameterizedCommand, swtEvent, this.fHandlerService), swtEvent,
								EMacroService.PRIORITY_HIGH);
					}
				} else {
					fMacroService.addMacroInstruction(new MacroInstructionForParameterizedCommand(
							commandAndTrigger.parameterizedCommand, this.fHandlerService));
				}
			}
		}
	}

	@Override
	public void preExecute(String commandId, ExecutionEvent event) {
		if (acceptEvent(event)) {
			if ("org.eclipse.ui.edit.findReplace".equals(commandId)) { //$NON-NLS-1$
				// We can't deal with find/replace at this point. Let the user know.
				UserNotifications userNotifications = ContextInjectionFactory.make(UserNotifications.class,
						fEclipseContext);
				try {
					userNotifications.notifyFindReplace();
				} finally {
					ContextInjectionFactory.uninject(userNotifications, fEclipseContext);
				}
			}
		}
		// Let's check if it should actually be recorded.
		if (fMacroService.canRecordCommand(commandId)) {
			if (!acceptEvent(event)) {
				fParameterizedCommandsAndTriggerStack.add(null);
				return;
			}
			ParameterizedCommand command = ParameterizedCommand.generateCommand(event.getCommand(),
					event.getParameters());
			fParameterizedCommandsAndTriggerStack.add(new ParameterizedCommandAndTrigger(command, event.getTrigger()));
		}
	}

	/**
	 * Filter to accept or reject an event (accepting means we can generate a macro
	 * instruction for it and false means we shouldn't).
	 */
	public static interface IFilter {

		/**
		 * If the given event should have a macro instruction created to it,
		 * {@code true} should be returned and if no macro instruction should be created
		 * from the filter, {@code false} should be returned.
		 *
		 * @param swtEvent
		 *            the event to be filtered.
		 *
		 * @return {@code true} if the given swtEvent should be accepted and
		 *         {@code false} otherwise.
		 */
		boolean acceptEvent(Event swtEvent);
	}

	private boolean acceptEvent(ExecutionEvent event) {
		Object trigger = event.getTrigger();
		if (trigger instanceof Event) {
			Event swtEvent = (Event) trigger;
			return fFilter.acceptEvent(swtEvent);
		}
		return true;
	}

	/**
	 * Filter to accept or reject an event (accepting means we can generate a macro
	 * instruction for it and false means we shouldn't).
	 *
	 * Note: mocked in tests using reflection.
	 */
	private IFilter fFilter = new IFilter() {

		@Override
		public boolean acceptEvent(Event swtEvent) {
			if (EditorUtils.getActiveEditorStyledText(fEclipseContext) != EditorUtils
					.getTargetStyledText(fMacroService.getMacroRecordContext())) {
				// Note: it previously checked swtEvent.widget, but sometimes the event was
				// generated from the wrong control (i.e., opening a new editor and doing
				// some action sometimes had the widget from a different editor).
				return false;
			}
			return true;
		}
	};
}
