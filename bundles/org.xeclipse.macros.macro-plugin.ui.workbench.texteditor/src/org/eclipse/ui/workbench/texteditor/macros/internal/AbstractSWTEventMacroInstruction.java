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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.Assert;
import org.eclipse.e4.core.macros.IMacroInstruction;
import org.eclipse.swt.widgets.Event;

/**
 * Base class for a macro instruction based on events from a given type.
 * <p>
 * Note that this macro instruction records only the {@code character},
 * {@code stateMask}, {@code keyCode}, {@code keyLocation} and {@code detail}.
 * The actual event {@code type} is meant to be obtained from the class which
 * overrides it or passed when needed.
 * <p>
 * The actual fields stored may grow over time: subclasses should be sure to
 * provide default values if not present.
 */
public abstract class AbstractSWTEventMacroInstruction implements IMacroInstruction {

	private static final String CHARACTER = "character"; //$NON-NLS-1$

	private static final String STATE_MASK = "stateMask"; //$NON-NLS-1$

	private static final String KEY_CODE = "keyCode"; //$NON-NLS-1$

	private static final String DETAIL = "detail"; //$NON-NLS-1$

	private static final String KEY_LOCATION = "keyLocation"; //$NON-NLS-1$

	protected final Event fEvent;

	/**
	 * @param event
	 *            the event for which the macro instruction is being created.
	 */
	public AbstractSWTEventMacroInstruction(Event event) {
		// Create a new event (we want to make sure that only the given info is
		// really needed on playback and don't want to keep a reference to the
		// original widget).
		Assert.isTrue(event.type == getEventType());
		Event newEvent = copyEvent(event);

		this.fEvent = newEvent;
	}

	/**
	 * Provides the event type of the events that this macro instruction is related
	 * to.
	 *
	 * @return the event type of the events that this macro instruction is related
	 *         to.
	 */
	protected abstract int getEventType();

	/**
	 * Helper to create a copy of some event.
	 *
	 * @param event
	 *            the event to be copied.
	 * @return a copy of the passed event.
	 */
	protected Event copyEvent(Event event) {
		Event newEvent = new Event();
		newEvent.keyCode = event.keyCode;
		newEvent.stateMask = event.stateMask;
		newEvent.type = event.type;
		newEvent.character = event.character;
		newEvent.detail = event.detail;
		newEvent.keyLocation = event.keyLocation;
		return newEvent;
	}

	/**
	 * Actually creates an event based on the contents previously gotten from
	 * {@link #toMap()}.
	 */
	protected static Event createEventFromMap(Map<String, String> map, int eventType) {
		Event event = new Event();
		event.type = eventType;

		String keyCode = map.get(KEY_CODE);
		if (keyCode != null) {
			event.keyCode = Integer.parseInt(keyCode);
		} else {
			event.keyCode = 0;
		}

		String stateMask = map.get(STATE_MASK);
		if (stateMask != null) {
			event.stateMask = Integer.parseInt(stateMask);
		} else {
			event.stateMask = 0;
		}

		String character = map.get(CHARACTER);
		if (character != null) {
			event.character = character.charAt(0);
		} else {
			event.character = '\0';
		}

		String detail = map.get(DETAIL);
		if (detail != null) {
			event.detail = Integer.parseInt(detail);
		} else {
			event.detail = 0;
		}

		String keyLocation = map.get(KEY_LOCATION);
		if (keyLocation != null) {
			event.keyLocation = Integer.parseInt(keyLocation);
		} else {
			event.keyLocation = 0;
		}
		return event;
	}

	@Override
	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<>();

		// Only save non-default values.

		if (fEvent.keyCode != 0) {
			map.put(KEY_CODE, Integer.toString(fEvent.keyCode));
		}

		if (fEvent.stateMask != 0) {
			map.put(STATE_MASK, Integer.toString(fEvent.stateMask));
		}

		if (fEvent.character != '\0') {
			map.put(CHARACTER, Character.toString(fEvent.character));
		}

		if (fEvent.detail != 0) {
			map.put(DETAIL, Integer.toString(fEvent.detail));
		}

		if (fEvent.keyLocation != 0) {
			map.put(KEY_LOCATION, Integer.toString(fEvent.keyLocation));
		}
		return map;
	}

}
