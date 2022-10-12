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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.core.runtime.Assert;
import org.eclipse.e4.core.macros.IMacroInstruction;
import org.eclipse.e4.core.macros.IMacroInstructionFactory;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.core.macros.MacroPlaybackException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A macro that is created from a sequence of instructions which are stored
 * in-memory (and may be persisted later on).
 */
public class ComposableMacro implements IMacro {

	public static final String XML_MACRO_TAG = "macro"; //$NON-NLS-1$

	public static final String XML_INSTRUCTION_TAG = "instruction"; //$NON-NLS-1$

	public static final String XML_ID_ATTRIBUTE = "id"; //$NON-NLS-1$

	public static final String XML_BASE64_ATTRIBUTE = "base64"; //$NON-NLS-1$

	public static final String XML_BASE64_ATTRIBUTE_TRUE = "true"; //$NON-NLS-1$

	public static final String XML_DEFINITION_TAG = "definition"; //$NON-NLS-1$

	public static final String XML_KEY_TAG = "key"; //$NON-NLS-1$

	public static final String XML_VALUE_TAG = "value"; //$NON-NLS-1$

	/**
	 * Provides the macro instruction id to an implementation which is able to
	 * recreate it.
	 */
	private final Map<String, IMacroInstructionFactory> fMacroInstructionIdToFactory;

	/**
	 * The macro instructions which compose this macro.
	 */
	private List<IMacroInstruction> fMacroInstructions = new ArrayList<>();

	private static class IndexAndPriority {

		private final int fIndex;
		private final int fPriority;

		private IndexAndPriority(int index, int priority) {
			fIndex = index;
			fPriority = priority;
		}
	}

	/**
	 * Map of an event to the current index of the macro instruction in
	 * fMacroInstructions and the priority for the given macro instruction.
	 */
	private final Map<Object, IndexAndPriority> fEventToPlacement = new HashMap<>();

	/**
	 * @param macroInstructionIdToFactory
	 *            Only macros instructions which have ids available as keys in the
	 *            macroInstructionIdToFactory will be accepted.
	 */
	public ComposableMacro(Map<String, IMacroInstructionFactory> macroInstructionIdToFactory) {
		fMacroInstructionIdToFactory = macroInstructionIdToFactory;
	}

	/**
	 * Checks whether the added macro instruction is suitable to be added to this
	 * macro.
	 *
	 * @param macroInstruction
	 *            the macro instruction to be checked.
	 */
	private void checkMacroInstruction(IMacroInstruction macroInstruction) {
		Assert.isTrue(
				fMacroInstructionIdToFactory == null
						|| fMacroInstructionIdToFactory.containsKey(macroInstruction.getId()),
				String.format("Macro instruction: %s not properly registered through a %s extension point.", //$NON-NLS-1$
						macroInstruction.getId(), MacroServiceImpl.MACRO_INSTRUCTION_FACTORY_EXTENSION_POINT));
	}

	/**
	 * Adds a new macro instruction to this macro.
	 *
	 * @param macroInstruction
	 *            the macro instruction to be appended to this macro.
	 */
	public void addMacroInstruction(IMacroInstruction macroInstruction) {
		checkMacroInstruction(macroInstruction);
		fMacroInstructions.add(macroInstruction);
	}

	/**
	 * Adds a macro instruction to be added to the current macro being recorded.
	 * This method should be used when an event may trigger the creation of multiple
	 * macro instructions and only one of those should be recorded.
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
	 * @return true if the macro instruction was actually added and false otherwise.
	 */
	public boolean addMacroInstruction(IMacroInstruction macroInstruction, Object event, int priority) {
		Assert.isNotNull(event);
		IndexAndPriority currentIndexAndPriority = fEventToPlacement.get(event);
		if (currentIndexAndPriority == null) {
			addMacroInstruction(macroInstruction);
			fEventToPlacement.put(event, new IndexAndPriority(fMacroInstructions.size() - 1, priority));
			return true;
		}
		if (priority >= currentIndexAndPriority.fPriority) {
			checkMacroInstruction(macroInstruction);
			fMacroInstructions.set(currentIndexAndPriority.fIndex, macroInstruction);
			fEventToPlacement.put(event, new IndexAndPriority(currentIndexAndPriority.fIndex, priority));
			return true;
		}
		return false;
	}

	/**
	 * Clears information obtained during recording which should be no longer needed
	 * after the macro is properly composed.
	 */
	public void clearCachedInfo() {
		fEventToPlacement.clear();
	}

	@Override
	public void playback(IMacroPlaybackContext macroPlaybackContext) throws MacroPlaybackException {
		for (IMacroInstruction macroInstruction : fMacroInstructions) {
			macroInstruction.execute(macroPlaybackContext);
		}
	}

	/**
	 * Actually returns the bytes to be written to the disk to be loaded back later
	 * on (the actual load and playback is later done by {@link SavedXMLMacro}).
	 *
	 * @return an UTF-8 encoded array of bytes which can be used to rerun the macro
	 *         later on.
	 * @throws IOException
	 *             if some error happens converting the macro to XML.
	 */
	public byte[] toXMLBytes() throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://xml.org/sax/features/namespaces", false); //$NON-NLS-1$
			factory.setFeature("http://xml.org/sax/features/validation", false); //$NON-NLS-1$
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false); //$NON-NLS-1$
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$

			DocumentBuilder documentBuilder = factory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();
			Element root = document.createElement(XML_MACRO_TAG);

			for (IMacroInstruction macroInstruction : fMacroInstructions) {
				Map<String, String> map = macroInstruction.toMap();
				Iterator<Entry<String, String>> iterator = map.entrySet().iterator();

				Element instructionElement = document.createElement(XML_INSTRUCTION_TAG);
				instructionElement.setAttribute(XML_ID_ATTRIBUTE, macroInstruction.getId());
				Element instructionDefinition = document.createElement(XML_DEFINITION_TAG);
				while (iterator.hasNext()) {
					Entry<String, String> entry = iterator.next();

					instructionDefinition
							.appendChild(setTextContent(document.createElement(XML_KEY_TAG), entry.getKey()));
					instructionDefinition
							.appendChild(setTextContent(document.createElement(XML_VALUE_TAG), entry.getValue()));
				}
				instructionElement.appendChild(instructionDefinition);
				root.appendChild(instructionElement);
			}
			document.appendChild(root);

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); //$NON-NLS-1$//$NON-NLS-2$

			DOMSource source = new DOMSource(document);
			StreamResult outputTarget = new StreamResult(outputStream);
			transformer.transform(source, outputTarget);
			return outputStream.toByteArray();
		} catch (DOMException | IllegalArgumentException | ParserConfigurationException
				| TransformerFactoryConfigurationError | TransformerException e) {
			throw new IOException("Error converting macro to XML.", e); //$NON-NLS-1$
		}
	}

	private Element setTextContent(Element element, String str) {
		if (isAsciiPrintable(str)) {
			element.setTextContent(str);
		} else {
			element.setTextContent(new String(Base64.getEncoder().encode(str.getBytes(StandardCharsets.UTF_8)),
					StandardCharsets.UTF_8));
			element.setAttribute(XML_BASE64_ATTRIBUTE, XML_BASE64_ATTRIBUTE_TRUE);
		}
		return element;
	}

	private static boolean isAsciiPrintable(String str) {
		int length = str.length();
		for (int i = 0; i < length; i++) {
			char c = str.charAt(i);
			if (!(c >= 32 && c < 127)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Provides the number of macro instructions which have been added to this
	 * macro.
	 *
	 * @return the number of macro instructions in this macro.
	 */
	public int getLength() {
		return fMacroInstructions.size();
	}
}
