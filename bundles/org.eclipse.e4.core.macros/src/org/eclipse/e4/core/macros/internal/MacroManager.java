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

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.e4.core.macros.Activator;
import org.eclipse.e4.core.macros.CancelMacroException;
import org.eclipse.e4.core.macros.CancelMacroRecordingException;
import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroInstruction;
import org.eclipse.e4.core.macros.IMacroInstructionFactory;
import org.eclipse.e4.core.macros.IMacroInstructionsListener;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.core.macros.IMacroRecordContext;
import org.eclipse.e4.core.macros.IMacroStateListener;
import org.eclipse.e4.core.macros.IMacroStateListener.StateChange;
import org.eclipse.e4.core.macros.MacroPlaybackException;

/**
 * Macro manager (pure java, without any OSGI requirements).
 */
public class MacroManager {

	private static final String XML_EXT = ".xml"; //$NON-NLS-1$

	private static final String TEMP_MACRO_PREFIX = "temp_macro_"; //$NON-NLS-1$

	/**
	 * The max number of temporary macros to be kept (can't be lower than 1).
	 */
	private int fMaxNumberOfTemporaryMacros = 5;

	/**
	 * Sets the maximum number of temporary macros (so, if more than
	 * {@code maxNumberOfTemporaryMacros} are created, the oldest ones will be
	 * removed until the number of temporary macros equals
	 * {@code maxNumberOfTemporaryMacros}).
	 *
	 * @param maxNumberOfTemporaryMacros
	 *            The max number of temporary macros to be kept (must be &ge; 1).
	 */
	public void setMaxNumberOfTemporaryMacros(int maxNumberOfTemporaryMacros) {
		Assert.isTrue(maxNumberOfTemporaryMacros >= 1);
		fMaxNumberOfTemporaryMacros = maxNumberOfTemporaryMacros;
	}

	/**
	 * Provides the maximum number of temporary macros to be kept (if there are more
	 * macros than the value returned, older temporary macros should be removed).
	 *
	 * @return the maximum number of temporary macros to be kept.
	 */
	public int getMaxNumberOfTemporaryMacros() {
		return fMaxNumberOfTemporaryMacros;
	}

	/**
	 * The directories where macros should be looked up. The first directory is the
	 * one where macros are persisted.
	 */
	private File[] fMacrosDirectories;

	/**
	 * Holds the macro currently being recorded (if we are in record mode). If
	 * not in record mode, should be {@code null}.
	 */
	private ComposableMacro fMacroBeingRecorded;

	/**
	 * Holds the last recorded or played back macro.
	 */
	private IMacro fLastMacro;

	/**
	 * Flag indicating whether we are playing back a macro.
	 */
	private boolean fIsPlayingBack = false;

	/**
	 * Flag indicating whether we're recording a macro.
	 */
	private boolean fIsRecording = false;

	/**
	 * State to be used when recording macro.
	 */
	private IMacroRecordContext fMacroRecordContext;

	/**
	 * State to be used when playing back macro.
	 */
	private IMacroPlaybackContext fMacroPlaybackContext;

	/**
	 * Creates a manager for macros which will read macros from the given
	 * directories.
	 *
	 * @param macrosDirectories
	 *            the directories where macros should be looked up. The first
	 *            directory is the one where macros are persisted. If there are 2
	 *            macros which would end up having the same name, the one in the
	 *            directory that appears last is the one which is used.
	 */
	public MacroManager(File... macrosDirectories) {
		setMacrosDirectories(macrosDirectories);
	}

	/**
	 * Sets the macro directories which should be tracked for macros.
	 *
	 * @param macrosDirectories
	 *            the directories where macros should be looked up. The first
	 *            directory is the one where macros are persisted. If there are 2
	 *            macros which would end up having the same name, the one in the
	 *            directory that appears last is the one which is used.
	 */
	public void setMacrosDirectories(File... macrosDirectories) {
		Assert.isNotNull(macrosDirectories);
		for (File file : macrosDirectories) {
			Assert.isNotNull(file);
		}
		fMacrosDirectories = macrosDirectories;
		reloadMacros();
	}

	/**
	 * Return {@code true} if a macro is currently being recorded, and {@code false}
	 * otherwise.
	 *
	 * @return whether a macro is currently being recorded.
	 */
	public boolean isRecording() {
		return fIsRecording;
	}

	/**
	 * Return {@code true} if a macro is currently being played back, and
	 * {@code false} otherwise.
	 *
	 * @return whether a macro is currently being played back.
	 */
	public boolean isPlayingBack() {
		return fIsPlayingBack;
	}

	/**
	 * Adds a macro instruction to the macro currently being recorded. Does nothing
	 * if no macro is being recorded.
	 *
	 * @param macroInstruction
	 *            the macro instruction to be recorded.
	 * @throws CancelMacroRecordingException
	 *             if the macro recording has been cancelled.
	 */
	public void addMacroInstruction(IMacroInstruction macroInstruction) throws CancelMacroRecordingException {
		ComposableMacro macroBeingRecorded = fMacroBeingRecorded;
		if (macroBeingRecorded != null) {
			macroBeingRecorded.addMacroInstruction(macroInstruction);
			for (IMacroInstructionsListener listener : fMacroInstructionsListeners) {
				listener.postAddMacroInstruction(macroInstruction);
			}
		}
	}

	/**
	 * Adds a macro instruction to be added to the current macro being recorded. The
	 * difference between this method and
	 * {@link #addMacroInstruction(IMacroInstruction)} is that it is meant to be
	 * used when an event may trigger the creation of multiple macro instructions
	 * and only one of those should be recorded.
	 *
	 * For instance, if a given KeyDown event is recorded in a StyledText and later
	 * an action is triggered by this event, the recorded action should overwrite
	 * the KeyDown event.
	 *
	 * @param macroInstruction
	 *            the macro instruction to be added to the macro currently being
	 *            recorded.
	 * @param event
	 *            the event that triggered the creation of the macro instruction to
	 *            be added. If there are multiple macro instructions added for the
	 *            same event, only the one with the highest priority will be kept
	 *            (if 2 events have the same priority, the last one will replace the
	 *            previous one).
	 * @param priority
	 *            the priority of the macro instruction being added (to be compared
	 *            against the priority of other added macro instructions for the
	 *            same event).
	 * @throws CancelMacroRecordingException
	 *             if the macro recording has been cancelled.
	 * @see #addMacroInstruction(IMacroInstruction)
	 */
	public void addMacroInstruction(IMacroInstruction macroInstruction, Object event, int priority)
			throws CancelMacroRecordingException {
		ComposableMacro macroBeingRecorded = fMacroBeingRecorded;
		if (macroBeingRecorded != null) {
			if (macroBeingRecorded.addMacroInstruction(macroInstruction, event, priority)) {
				for (IMacroInstructionsListener listener : fMacroInstructionsListeners) {
					listener.postAddMacroInstruction(macroInstruction);
				}
			}
		}
	}

	private static final class MacroRecordContext implements IMacroRecordContext {

		private final Map<Object, Object> fContext = new HashMap<>();

		@Override
		public Object get(String key) {
			return fContext.get(key);
		}

		@Override
		public void set(String key, Object value) {
			fContext.put(key, value);
		}
	}

	/**
	 * Toggles the macro record (either starts recording or stops an existing
	 * record).
	 *
	 * @param macroService
	 *            service to record macros.
	 * @param macroInstructionIdToFactory
	 *            a mapping of the available macro instruction ids to the factory
	 *            which is able to recreate the related macro instruction.
	 */
	public void toggleMacroRecord(final EMacroService macroService,
			Map<String, IMacroInstructionFactory> macroInstructionIdToFactory) {
		if (fIsPlayingBack) {
			// Can't toggle the macro record mode while playing back.
			return;
		}
		if (fMacroBeingRecorded == null) {
			// Start recording
			fIsRecording = true;
			fMacroRecordContext = new MacroRecordContext();
			fMacroBeingRecorded = new ComposableMacro(macroInstructionIdToFactory);
			for (IMacroStateListener listener : fStateListeners) {
				SafeRunner.run(() -> listener.macroRecordContextCreated(fMacroRecordContext));
			}
			if (!notifyMacroStateChange(macroService, StateChange.RECORD_STARTED)) {
				stopRecording(macroService);
			}
		} else {
			stopRecording(macroService);
		}
	}

	/**
	 * Stops the macro recording.
	 *
	 * @param macroService
	 *            service to record macros.
	 */
	private void stopRecording(final EMacroService macroService) {
		try {
			fIsRecording = false;
			// Notify before saving macro (but after the flag to know whether
			// we're recording is set).
			notifyMacroStateChange(macroService, StateChange.RECORD_FINISHED);
			fMacroBeingRecorded.clearCachedInfo();
			if (fMacroBeingRecorded.getLength() > 0) {
				// No point in saving an empty macro.
				saveTemporaryMacro(fMacroBeingRecorded);
				fLastMacro = fMacroBeingRecorded;
			}
		} finally {
			fMacroBeingRecorded = null;
			fMacroRecordContext = null;
		}
	}

	/**
	 * Helper class to store a path an a time.
	 */
	public static final class StoredMacroReference {

		public final Path fPath;
		public final long fLastModified;

		public StoredMacroReference(Path path, long lastModified) {
			fPath = path;
			fLastModified = lastModified;
		}

	}

	/**
	 * Saves a composable macro as a temporary macro on disk.
	 *
	 * @param macro
	 *            the macro to be recorded as a temporary macro.
	 */
	private void saveTemporaryMacro(ComposableMacro macro) {
		if (fMacrosDirectories == null || fMacrosDirectories.length == 0) {
			return;
		}
		// The first one is the one we use as a working directory to store
		// temporary macros.
		File macroDirectory = fMacrosDirectories[0];
		if (!macroDirectory.isDirectory()) {
			Activator.log(IStatus.ERROR,
					String.format("Unable to save macro. Expected: %s to be a directory.", macroDirectory)); //$NON-NLS-1$
			return;
		}

		List<StoredMacroReference> storedMacroReferences = listTemporaryMacroReferences(macroDirectory);

		try {
			Path tempFile = Files.createTempFile(Paths.get(macroDirectory.toURI()), TEMP_MACRO_PREFIX, XML_EXT);
			Files.write(tempFile, macro.toXMLBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			Activator.log(e);
			return; // Can't create file at expected place;
		}

		// Remove older files
		while (storedMacroReferences.size() >= fMaxNumberOfTemporaryMacros) {
			StoredMacroReference removeFile = storedMacroReferences.remove(storedMacroReferences.size() - 1);
			try {
				Files.deleteIfExists(removeFile.fPath);
			} catch (Exception e) {
				Activator.log(e);
			}
		}
	}

	/**
	 * Provides the path/time for the temporary macros at a given directory as n
	 * list sorted such that the last element is the oldest one and the first is the
	 * newest.
	 *
	 * @param macroDirectory
	 *            the directory from where we should get the temporary macros.
	 *
	 * @return the temporary macros available along with their path and creation
	 *         time
	 */
	public List<StoredMacroReference> listTemporaryMacroReferences(File macroDirectory) {
		// It is a sorted list and not a tree map to deal with the case of
		// multiple times pointing to the same file (although hard to happen,
		// it is not impossible).
		List<StoredMacroReference> storedMacroReferences = new ArrayList<>();

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(macroDirectory.toURI()),
				new DirectoryStream.Filter<Path>() {

					@Override
					public boolean accept(Path entry) {
						String name = entry.getFileName().toString().toLowerCase();
						return name.startsWith(TEMP_MACRO_PREFIX) && name.endsWith(XML_EXT);
					}
				})) {
			for (Path p : directoryStream) {
				storedMacroReferences
						.add(new StoredMacroReference(p, Files.getLastModifiedTime(p).to(TimeUnit.NANOSECONDS)));
			}
		} catch (IOException e1) {
			Activator.log(e1);
		}

		// Sort by reversed modified time (because it is faster to remove the
		// last element from an ArrayList later on).
		Collections.sort(storedMacroReferences, new Comparator<StoredMacroReference>() {

			@Override
			public int compare(StoredMacroReference o1, StoredMacroReference o2) {
				return Long.compare(o2.fLastModified, o1.fLastModified);
			}
		});
		return storedMacroReferences;
	}

	/**
	 * Notifies that a macro state change occurred (see
	 * {@link org.eclipse.e4.core.macros.IMacroStateListener}).
	 *
	 * @return false if a listener has throw a CancelMacroException and true
	 *         otherwise.
	 */
	private boolean notifyMacroStateChange(final EMacroService macroService, final StateChange stateChange) {
		boolean okToGo = true;
		for (final IMacroStateListener listener : fStateListeners) {
			try {
				listener.macroStateChanged(macroService, stateChange);
			} catch (CancelMacroException e) {
				okToGo = false;
			} catch (Exception e) {
				Activator.log(e);
			}
		}
		return okToGo;
	}

	/**
	 * Plays back the last recorded macro.
	 *
	 * @param macroService
	 *            the macro service (used to notify listeners of the change.
	 * @param macroPlaybackContext
	 *            a context to be used to playback the macro (passed to the macro to
	 *            be played back).
	 * @throws MacroPlaybackException
	 *             if some error happens when running the macro.
	 */
	public void playbackLastMacro(EMacroService macroService, final IMacroPlaybackContext macroPlaybackContext)
			throws MacroPlaybackException {
		if (fLastMacro != null && !fIsPlayingBack) {
			// Note that we can play back while recording, but we can't change
			// the recording mode while playing back.
			fIsPlayingBack = true;
			try {
				fMacroPlaybackContext = macroPlaybackContext;
				for (IMacroStateListener listener : fStateListeners) {
					SafeRunner.run(() -> listener.macroPlaybackContextCreated(macroPlaybackContext));
				}

				if (notifyMacroStateChange(macroService, StateChange.PLAYBACK_STARTED)) {
					fLastMacro.playback(macroPlaybackContext);
				}
			} finally {
				fIsPlayingBack = false;
				notifyMacroStateChange(macroService, StateChange.PLAYBACK_FINISHED);
				fMacroPlaybackContext = null;
			}
		}
	}

	/**
	 * A list with the listeners to be notified of changes in the macro service.
	 */
	private final ListenerList<IMacroStateListener> fStateListeners = new ListenerList<>();

	/**
	 * Adds a macro listener to be notified on changes in the macro record/playback
	 * state.
	 *
	 * @param listener
	 *            the listener to be added.
	 */
	public void addMacroStateListener(IMacroStateListener listener) {
		fStateListeners.add(listener);
	}

	/**
	 * @param listener
	 *            the macro listener which should no longer be notified of changes.
	 */
	public void removeMacroStateListener(IMacroStateListener listener) {
		fStateListeners.remove(listener);
	}

	/**
	 * Provides the listeners which are currently registered to be notified of
	 * changes in the macro state.
	 *
	 * @return the currently registered listeners.
	 */
	public IMacroStateListener[] getMacroStateListeners() {
		Object[] listeners = fStateListeners.getListeners();
		IMacroStateListener[] macroStateListeners = new IMacroStateListener[listeners.length];
		System.arraycopy(listeners, 0, macroStateListeners, 0, listeners.length);
		return macroStateListeners;
	}

	/**
	 * Reloads the macros available from the disk.
	 */
	public void reloadMacros() {
		for (File macroDirectory : fMacrosDirectories) {
			if (macroDirectory.isDirectory()) {
				List<StoredMacroReference> storedMacroReferences = listTemporaryMacroReferences(macroDirectory);
				if (storedMacroReferences.size() > 0) {
					fLastMacro = new SavedXMLMacro(storedMacroReferences.get(0).fPath.toFile());
					return; // Load the last from the first directory (others aren't used for the last
							// macro).
				}
			} else {
				Activator.log(IStatus.ERROR, String.format("Expected: %s to be a directory.", macroDirectory)); //$NON-NLS-1$
			}
		}
	}

	private final ListenerList<IMacroInstructionsListener> fMacroInstructionsListeners = new ListenerList<>();

	/**
	 * Adds a macro instructions listener (it may be added to validate the current
	 * state of the macro recording).
	 *
	 * @param macroInstructionsListener
	 *            the listener for macro instructions.
	 */
	public void addMacroInstructionsListener(IMacroInstructionsListener macroInstructionsListener) {
		fMacroInstructionsListeners.add(macroInstructionsListener);
	}

	/**
	 * Removes a macro instructions listener.
	 *
	 * @param macroInstructionsListener
	 *            the listener for macro instructions.
	 */
	public void removeMacroInstructionsListener(IMacroInstructionsListener macroInstructionsListener) {
		fMacroInstructionsListeners.remove(macroInstructionsListener);
	}

	/**
	 * Provides the macro record context or {@code null} if the macro engine is
	 * not recording.
	 *
	 * @return the macro record context created when macro record started or
	 *         {@code null} if it is not currently recording.
	 */
	public IMacroRecordContext getMacroRecordContext() {
		return fMacroRecordContext;
	}

	/**
	 * Provides the macro playback context or {@code null} if the macro engine
	 * is not playing back.
	 *
	 * @return the macro playback context created when macro playback started or
	 *         {@code null} if it is not currently recording.
	 */
	public IMacroPlaybackContext getMacroPlaybackContext() {
		return fMacroPlaybackContext;
	}

	/**
	 * Provides the number of macro instructions in the macro being currently
	 * recorded or -1 if there is no macro being recorded.
	 *
	 * @return the number of macro instructions in the macro currently recorded or
	 *         -1 if no macro is being recorded.
	 */
	public int getLengthOfMacroBeingRecorded() {
		if (fMacroBeingRecorded != null) {
			return fMacroBeingRecorded.getLength();
		}
		return -1;
	}


}
