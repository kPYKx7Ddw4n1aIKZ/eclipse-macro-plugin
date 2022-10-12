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
package org.eclipse.e4.ui.macros.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.contexts.Context;
import org.eclipse.core.commands.contexts.ContextManager;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.e4.core.commands.CommandServiceAddon;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroRecordContext;
import org.eclipse.e4.core.macros.IMacroStateListener;
import org.eclipse.e4.core.macros.internal.MacroManager;
import org.eclipse.e4.core.macros.internal.MacroServiceImpl;
import org.eclipse.e4.ui.bindings.BindingServiceAddon;
import org.eclipse.e4.ui.bindings.EBindingService;
import org.eclipse.e4.ui.bindings.internal.BindingTable;
import org.eclipse.e4.ui.bindings.internal.BindingTableManager;
import org.eclipse.e4.ui.bindings.keys.KeyBindingDispatcher;
import org.eclipse.e4.ui.bindings.keys.KeyBindingDispatcher.KeyDownFilter;
import org.eclipse.e4.ui.macros.internal.keybindings.CommandManagerExecutionListener;
import org.eclipse.e4.ui.macros.internal.keybindings.CommandManagerExecutionListenerInstaller;
import org.eclipse.e4.ui.services.ContextServiceAddon;
import org.eclipse.e4.ui.services.EContextService;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("restriction")
public class KeyBindingDispatcherMacroIntegrationTest {

	private static final String ID_DIALOG = "org.eclipse.ui.contexts.dialog";
	private static final String ID_DIALOG_AND_WINDOW = "org.eclipse.ui.contexts.dialogAndWindow";
	private static final String ID_WINDOW = "org.eclipse.ui.contexts.window";

	final static String[] CONTEXTS = { ID_DIALOG_AND_WINDOW, "DAW", null, ID_DIALOG, "Dialog", ID_DIALOG_AND_WINDOW,
			ID_WINDOW, "Window", ID_DIALOG_AND_WINDOW, };

	private static final String TEST_CAT1 = "test.cat1";
	private static final String TEST_ID1 = "test.id1";

	static class CallHandler {
		public boolean q1;
		public boolean q2;

		@CanExecute
		public boolean canExecute() {
			q1 = true;
			return true;
		}

		@Execute
		public Object execute() {
			q2 = true;
			if (q1) {
				return Boolean.TRUE;
			}
			return Boolean.FALSE;
		}
	}

	private Display display;
	private IEclipseContext workbenchContext;
	private CallHandler handler;
	private File fMacrosDirectory;
	private Shell shell;
	private StyledText styledText;
	private KeyDownFilter dispatcherListener;

	private void defineCommands(IEclipseContext context) {
		ECommandService cs = workbenchContext.get(ECommandService.class);
		Category category = cs.defineCategory(TEST_CAT1, "CAT1", null);
		cs.defineCommand(TEST_ID1, "ID1", null, category, null);
		ParameterizedCommand cmd = cs.createCommand(TEST_ID1, null);
		EHandlerService hs = workbenchContext.get(EHandlerService.class);
		handler = new CallHandler();
		hs.activateHandler(TEST_ID1, handler);
		EBindingService bs = workbenchContext.get(EBindingService.class);
		// As we're using the default display, we have to generate a unique sequence
		// otherwise the default installed key filter may eat it.
		TriggerSequence seq = bs.createSequence("CTRL+I");
		Binding db = createDefaultBinding(bs, seq, cmd);
		bs.activateBinding(db);
	}

	private Binding createDefaultBinding(EBindingService bs, TriggerSequence sequence, ParameterizedCommand command) {

		Map<String, String> attrs = new HashMap<>();
		attrs.put("schemeId", "org.eclipse.ui.defaultAcceleratorConfiguration");

		return bs.createBinding(sequence, command, ID_WINDOW, attrs);
	}

	private void defineContexts(IEclipseContext context) {
		ContextManager contextManager = context.get(ContextManager.class);
		for (int i = 0; i < CONTEXTS.length; i += 3) {
			Context c = contextManager.getContext(CONTEXTS[i]);
			c.define(CONTEXTS[i + 1], null, CONTEXTS[i + 2]);
		}

		EContextService cs = context.get(EContextService.class);
		cs.activateContext(ID_DIALOG_AND_WINDOW);
		cs.activateContext(ID_WINDOW);
	}

	private void defineBindingTables(IEclipseContext context) {
		BindingTableManager btm = context.get(BindingTableManager.class);
		ContextManager cm = context.get(ContextManager.class);
		btm.addTable(new BindingTable(cm.getContext(ID_DIALOG_AND_WINDOW)));
		btm.addTable(new BindingTable(cm.getContext(ID_WINDOW)));
		btm.addTable(new BindingTable(cm.getContext(ID_DIALOG)));
	}

	/**
	 * Closes the welcome view (if being shown)
	 */
	public static void closeWelcomeView() {
		IWorkbench workbench;
		try {
			workbench = PlatformUI.getWorkbench();
		} catch (IllegalStateException e) {
			return; // No welcome view to close (workbench not available).
		}
		IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
		IViewReference[] viewReferences = workbenchWindow.getActivePage().getViewReferences();
		for (IViewReference ref : viewReferences) {
			if (ref.getPartName().equals("Welcome")) {
				workbenchWindow.getActivePage().hideView(ref);
			}
		}
	}

	@Before
	public void setUp() throws Exception {
		closeWelcomeView();
		display = Display.getDefault();
		shell = new Shell(display, SWT.NONE);
		styledText = new StyledText(shell, SWT.NONE);

		IEclipseContext globalContext = Activator.getDefault().getGlobalContext();
		workbenchContext = globalContext.createChild("workbenchContext");
		ContextInjectionFactory.make(CommandServiceAddon.class, workbenchContext);
		ContextInjectionFactory.make(ContextServiceAddon.class, workbenchContext);
		ContextInjectionFactory.make(BindingServiceAddon.class, workbenchContext);
		defineContexts(workbenchContext);
		defineBindingTables(workbenchContext);
		defineCommands(workbenchContext);

		KeyBindingDispatcher dispatcher = new KeyBindingDispatcher();
		workbenchContext.set(KeyBindingDispatcher.class, dispatcher);
		ContextInjectionFactory.inject(dispatcher, workbenchContext);

		workbenchContext.set(EMacroService.class.getName(),
				ContextInjectionFactory.make(MacroServiceImpl.class, workbenchContext));

		dispatcherListener = dispatcher.getKeyDownFilter();
		display.addFilter(SWT.KeyDown, dispatcherListener);
		display.addFilter(SWT.Traverse, dispatcherListener);

		assertFalse(handler.q2);

		fMacrosDirectory = folder.getRoot();
		EMacroService macroService = workbenchContext.get(EMacroService.class);
		MacroServiceImpl macroServiceImplementation = (MacroServiceImpl) macroService;
		MacroManager macroManager = macroServiceImplementation.getMacroManager();
		macroManager.setMacrosDirectories(fMacrosDirectory);
		Predicate<IConfigurationElement> filterMacroListeners = new Predicate<IConfigurationElement>() {

			@Override
			public boolean test(IConfigurationElement t) {
				String namespace = t.getNamespaceIdentifier();
				return namespace.equals("org.eclipse.e4.ui.macros") || namespace.equals("org.eclipse.e4.core.macros");
			}
		};

		Field field = MacroServiceImpl.class.getDeclaredField("fFilterMacroListeners");
		field.setAccessible(true);
		field.set(macroServiceImplementation, filterMacroListeners);
	}

	@After
	public void tearDown() throws Exception {
		workbenchContext.dispose();
		workbenchContext = null;
		styledText.dispose();
		styledText = null;
		shell.dispose();
		shell = null;
	}

	private void notifyCtrlI(Control control) {
		Event event = new Event();
		event.type = SWT.KeyDown;
		event.keyCode = SWT.CTRL;
		control.notifyListeners(SWT.KeyDown, event);

		event = new Event();
		event.type = SWT.KeyDown;
		event.stateMask = SWT.CTRL;
		event.keyCode = 'I';
		control.notifyListeners(SWT.KeyDown, event);
	}

	private void startRecording(EMacroService macroService) throws Exception {
		macroService.toggleMacroRecord();
		assertTrue(macroService.isRecording());
		IMacroRecordContext macroRecordContext = macroService.getMacroRecordContext();
		macroRecordContext.set("target_styled_text", styledText); // org.eclipse.e4.ui.macros.Activator.TARGET_STYLED_TEXT

		Assert.assertEquals(
				Arrays.asList("org.eclipse.e4.ui.macros.internal.actions.MacroUIUpdater",
						"org.eclipse.e4.ui.macros.internal.keybindings.CommandManagerExecutionListenerInstaller"),
				getRegisteredClasses(macroService));
		IMacroStateListener[] macroStateListeners = ((MacroServiceImpl) macroService)
				.getMacroStateListeners();
		boolean found = false;
		for (IMacroStateListener iMacroStateListener : macroStateListeners) {
			if (iMacroStateListener instanceof CommandManagerExecutionListenerInstaller) {
				CommandManagerExecutionListenerInstaller commandManagerExecutionListenerInstaller = (CommandManagerExecutionListenerInstaller) iMacroStateListener;
				// Make sure that we accept any event while testing.
				CommandManagerExecutionListener commandManagerExecutionListener = commandManagerExecutionListenerInstaller
						.getCommandManagerExecutionListener();
				Field field = CommandManagerExecutionListener.class.getDeclaredField("fFilter");
				field.setAccessible(true);
				field.set(commandManagerExecutionListener, new CommandManagerExecutionListener.IFilter() {

					@Override
					public boolean acceptEvent(Event swtEvent) {
						return true;
					}
				});
				found = true;
			}
		}
		Assert.assertTrue(found);
	}

	private void finishRecording(EMacroService macroService) {
		macroService.toggleMacroRecord();
		assertFalse(macroService.isRecording());
	}

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void testMacroIntegration() throws Exception {
		EMacroService macroService = workbenchContext.get(EMacroService.class);
		// Before the first record, it should be empty (loaded on demand).
		Assert.assertEquals(new ArrayList<>(), getRegisteredClasses(macroService));
		startRecording(macroService);

		notifyCtrlI(styledText);
		assertTrue(handler.q2);
		assertEquals(((MacroServiceImpl) macroService).getMacroManager().getLengthOfMacroBeingRecorded(), 1);

		finishRecording(macroService);

		handler.q2 = false;
		macroService.playbackLastMacro();
		assertTrue(handler.q2);
	}

	public List<String> getRegisteredClasses(EMacroService macroService) {
		IMacroStateListener[] macroStateListeners = ((MacroServiceImpl) macroService)
				.getMacroStateListeners();
		List<String> classes = Arrays.stream(macroStateListeners).map(m -> m.getClass().getName()).sorted()
				.collect(Collectors.toList());
		return classes;
	}

	@Test
	public void testMacroIntegrationSaveRestore() throws Exception {
		FilenameFilter macrosFilter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml");
			}
		};
		assertEquals(0, fMacrosDirectory.list(macrosFilter).length);

		EMacroService macroService = workbenchContext.get(EMacroService.class);

		assertFalse(handler.q2);

		startRecording(macroService);
		notifyCtrlI(styledText);
		assertTrue(handler.q2);
		assertEquals(((MacroServiceImpl) macroService).getMacroManager().getLengthOfMacroBeingRecorded(), 1);
		finishRecording(macroService);

		// Macro was saved in the dir.
		assertEquals(1, fMacrosDirectory.list(macrosFilter).length);

		// Check if reloading from disk and playing it back works.
		((MacroServiceImpl) macroService).getMacroManager().reloadMacros();
		handler.q2 = false;
		macroService.playbackLastMacro();
		assertTrue(handler.q2);
	}

}
