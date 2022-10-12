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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.e4.core.macros.IMacroInstruction;
import org.eclipse.e4.core.macros.IMacroInstructionFactory;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.core.macros.MacroPlaybackException;
import org.eclipse.e4.core.macros.internal.ComposableMacro;
import org.eclipse.e4.core.macros.internal.SavedXMLMacro;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SavedXMLMacroTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private static class MyMacroInstruction implements IMacroInstruction {

		private static String SPECIAL_CHARS = "!@#$%^&*()<--\n\r\b\t\0&";
		private Map<String, String> map;

		public MyMacroInstruction(Map<String, String> stringMap) {
			this.map = stringMap;
			Assert.isTrue(new MyMacroInstruction().toMap().equals(stringMap));
		}

		public MyMacroInstruction() {
			map = new HashMap<>();
			map.put("attr", "value");
			map.put("attr1", SPECIAL_CHARS);
			map.put(SPECIAL_CHARS, "value1");
			map.put(SPECIAL_CHARS + "\n", SPECIAL_CHARS + "\n");
		}

		@Override
		public String getId() {
			return "my_macro_instruction";
		}

		@Override
		public void execute(IMacroPlaybackContext macroPlaybackContext) throws MacroPlaybackException {
			macroPlaybackContext.set("played", true);
		}

		@Override
		public Map<String, String> toMap() {
			return map;
		}
	}

	private static class MyMacroPlaybackContext implements IMacroPlaybackContext {

		private Map<String, Object> ctx = new HashMap<>();

		@Override
		public Object get(String key) {
			return ctx.get(key);
		}

		@Override
		public void set(String key, Object value) {
			ctx.put(key, value);
		}

		@Override
		public void runMacroInstruction(String macroInstructionId, Map<String, String> macroInstructionParameters)
				throws Exception {
			new MyMacroInstruction(macroInstructionParameters).execute(this);
		}
	}

	private static class MyMacroInstructionFactory implements IMacroInstructionFactory {

		@Override
		public IMacroInstruction create(Map<String, String> stringMap) throws Exception {
			return new MyMacroInstruction(stringMap);
		}
	}

	@Test
	public void testSavedXMLMacro() throws Exception {
		Map<String, IMacroInstructionFactory> macroInstructionIdToFactory = new HashMap<>();
		MyMacroInstruction macroInstruction = new MyMacroInstruction();
		macroInstructionIdToFactory.put(macroInstruction.getId(), new MyMacroInstructionFactory());
		ComposableMacro composable = new ComposableMacro(macroInstructionIdToFactory);
		composable.addMacroInstruction(macroInstruction);
		byte[] xmlBytes = composable.toXMLBytes();
		File tempFile = folder.newFile("saved_xml.xml");
		Files.write(tempFile.toPath(), xmlBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING);

		SavedXMLMacro macro = new SavedXMLMacro(tempFile);
		IMacroPlaybackContext macroPlaybackContext = new MyMacroPlaybackContext();
		macro.playback(macroPlaybackContext);
		assertEquals(macroPlaybackContext.get("played"), true);
	}
}
