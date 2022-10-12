package org.eclipse.e4.core.macros;

/**
 * Package providing abstractions for dealing with macro record/playback.
 *
 * <p>
 * The basic idea in this implementation is that the macro plugin is a thin
 * layer used to record macro instructions and then play it back. The actual
 * work is done on other plugins, which should be macro-aware.
 * </p>
 *
 * <p>
 * The macro recording doesn't only map to text editions. Anything done in the
 * IDE could be recorded in a macro to be played back later on. For instance, it
 * may be possible to record and playback changes to preferences, importing some
 * git repository, finding text, etc. Later on, when playing it back, it should
 * be even possible to ask if the user wants to edit some of the values which
 * were saved during record.
 * </p>
 *
 * <p>
 * Still, note that while that support may be possible in the future, currently
 * the implementation only targets the text editor and its operations.
 * </p>
 *
 *
 * <hr/>
 * <h3>Rationale</h3>
 *
 * <p>
 * The basic idea is that editors listen or detect when macro record mode is
 * active and upon entering macro mode, they make sure actions are properly
 * tracked so that they can be played back accordingly.
 * </p>
 *
 * <p>
 * -- note that it is important that things are not the other way around: the
 * macro plugin is a thin layer to start/stop macro recording and provide the
 * basic abstractions and management of macros, it is up to the clients to be
 * aware that they are in macro record mode and act accordingly, issuing
 * commands to be recorded and later played back.
 * </p>
 *
 * <hr/>
 * <h3>Related APIs</h3>
 *
 * <p>
 * Note that currently the only APIs available are exposed as interfaces:
 * </p>
 *
 * <p>
 * {@link org.eclipse.e4.core.macros.EMacroService} can be used to:
 *
 * <ul>
 * <li>query the current record/playback state;</li>
 * <li>listen changes in the current record/playback state;</li>
 * <li>add macro instructions when in record mode;</li>
 * <li>start and stop the macro record;</li>
 * <li>playback a previous macro;</li>
 * <li>get commands customization from the
 * org.eclipse.e4.core.macros.commandHandling extension point;</li>
 * <li>programatically customize commands.</li>
 * </ul>
 * </p>
 *
 * <p>
 * {@link org.eclipse.e4.core.macros.IMacroInstruction}: A macro is actually
 * composed of multiple macro instructions. Each time the user does some action
 * which should be recorded, a macro instruction should be created and added to
 * the EMacroService. (note that currently the basic macro abstraction -- IMacro
 * -- is only internally available, but the idea is that a macro is composed of
 * multiple macro instructions).
 * </p>
 *
 * <p>
 * {@link org.eclipse.e4.core.macros.IMacroInstructionFactory}: provides a way
 * to recreate an {@link org.eclipse.e4.core.macros.IMacroInstruction} from its
 * id and persisted contents.
 * </p>
 *
 * <p>
 * {@link org.eclipse.e4.core.macros.IMacroPlaybackContext}: received by a macro
 * instruction when it is being played back.
 * </p>
 *
 * <p>
 * {@link org.eclipse.e4.core.macros.IMacroStateListener}: listener to hear
 * changes in the record/playback state (added to the EMacroService).
 * </p>
 *
 *
 * <hr/>
 * <h3>Actually dealing with macro record/playback</h3>
 *
 * <p>
 * Clients should:
 * <ul>
 * <li>listen when a macro record will start to set up their internal state and
 * add if needed, add listeners which records actions/events when they happen so
 * that they're properly added the macro being recorded;</li>
 * <li>listen when a macro record session finished to remove any used listener
 * and restore previous state;</li>
 * <li>listen when a macro playback starts/finishes and set up/reset the
 * internal state properly</li>
 * </ul>
 * </p>
 *
 * <p>
 * -- Note: it is possible for the user to start a record mode and playback a
 * previous macro in such a mode (although the opposite is not true).
 * </p>
 *
 * <h4>Scenario 1:</h4>
 *
 * <p>
 * Setting up state when entering macro record/playback mode:
 * </p>
 *
 * <p>
 * This gives an example where some state must be set for both macro and record
 * mode (i.e., disabling code-completion) and in the record mode it also has to
 * record keypresses to be played back later on.
 * </p>
 *
 * <pre>
 * public class MyMacroAwareEditor implements IMacroStateListener {
 *
 * 	public void init(final IEditorSite site) {
 * 		fMacroService = site.getService(EMacroService.class);
 * 		if (fMacroService != null) {
 * 			// Start listening to the service so that events are properly
 * 			// recorded.
 * 			fMacroService.addmacroStateListener(this);
 * 			if (fMacroService.isRecording() || fMacroService.isPlayingBack()) {
 * 				this.macroStateChanged(fMacroService);
 * 			}
 * 		}
 * 	}
 *
 * 	public final void macroStateChanged(EMacroService macroService) {
 * 		// Note: state must be set with care because record/playback may
 * 		// happen simultaneously.
 * 		if (macroService.isRecording() || macroService.isPlayingBack()) {
 * 			// Code completion disabled in both, record and playback.
 * 			this.disableCodeCompletion();
 * 		} else {
 * 			this.enableCodeCompletion();
 * 		}
 *
 * 		if (macroService.isRecording()) {
 * 			this.startRecordingKeyEvents(macroService);
 * 		} else {
 * 			this.endRecordingKeyEvents();
 * 		}
 * 	}
 *
 * 	protected void startRecordingKeyEvents(final EMacroService macroService) {
 * 		// Note: if simultaneously recording/playing back, this may be called
 * 		// multiple times.
 * 		if (this.fKeyEventListener == null) {
 * 			this.fKeyEventListener = new Listener() {
 * 				&#64;Override
 * 				public void handleEvent(Event event) {
 * 					if (event.type == SWT.KeyDown && fMacroService.isRecording()) {
 * 						fMacroService.addMacroInstruction(new KeyDownMacroInstruction(event));
 * 					}
 * 				}
 * 			};
 * 			this.addListener(SWT.KeyDown, this.fKeyEventListener);
 * 		}
 * 	}
 *
 * 	protected void endRecordingKeyEvents() {
 * 		if (this.fKeyEventListener != null) {
 * 			this.removeListener(SWT.KeyDown, this.fKeyEventListener);
 * 			this.fKeyEventListener = null;
 * 		}
 * 	}
 *
 * }
 * </pre>
 *
 * <p>
 * An actual implementation using this strategy is:
 * </p>
 *
 * <p>
 * {@link org.eclipse.ui.workbench.texteditor.macros.internal.MacroStyledTextInstaller}
 * </p>
 *
 *
 * <h4>Scenario 2</h4>
 *
 * <p>
 * Just issuing macro instructions when no setup is needed:
 * </p>
 *
 * <pre>
 * public class MyMacroAwareClass {
 *
 *		&#64;Inject
 *     	EMacroService fMacroService;
 *
 *		public void run(String myCommand) {
 *			...
 *			// Actually run command
 *			if(fMacroService.isRecording()){
 *				fMacroService.addMacroInstruction(new MyRunCommandMacroInstruction(myCommand));
 *			}
 *		}
 * }
 *
 * </pre>
 *
 * <p>
 * An actual implementation using this strategy is:
 * {@link org.eclipse.e4.ui.macros.internal.keybindings.CommandManagerExecutionListener}
 * </p>
 *
 * <p>
 * -- Note: commands registered through the eclipse commands extensions will be
 * already recorded by default if they aren't registered in the
 * org.eclipse.e4.core.macros.commandHandling extension point and
 * their activation will already be recorded for proper playback by default (so,
 * clients only actually need to customize actions which currently aren't
 * implemented as eclipse actions or to disable recording of some eclipse
 * action).
 * </p>
 *
 * <hr/>
 * <h3>Macro playback</h3>
 *
 * <p>
 * After a command is actually added to the EMacroService and a record session
 * is finished, all the recorded IMacroInstructions will be saved to disk using
 * the contents of their
 * {@link org.eclipse.e4.core.macros.IMacroInstruction#toMap()} method to enable
 * playing it back later on.
 * </p>
 *
 * <p>
 * Afterward, when it is time to play a macro instruction back, the contents of
 * the map are gotten from disk and will be recreated through factories
 * registered in the org.eclipse.e4.core.macros.macroInstructionsFactory
 * extension point (so it is important to note that if a custom macro instruction
 * is created, a factory to recreate it must be registered).
 * </p>
 *
 * <p>
 * On playback, the macro instruction must perform the same action which was
 * recorded (so, for instance, if a command to delete the current line of the
 * editor was issued, on playback the current line of the current editor must be
 * deleted).
 * </p>
 *
 *
 **/
