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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.core.macros.MacroPlaybackException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Actually loads a macro from a XML file to be played back. Works with the
 * contents saved from {@code ComposableMacro#toXMLBytes()}.
 */
public class SavedXMLMacro implements IMacro {

	/**
	 * The file which contains the contents of the macro.
	 */
	private final File fFile;

	/**
	 * Creates a macro which is backed by the contents of a XML file.
	 *
	 * @param file
	 *            the file with the contents of the macro.
	 */
	public SavedXMLMacro(File file) {
		fFile = file;
	}

	@Override
	public void playback(IMacroPlaybackContext macroPlaybackContext) throws MacroPlaybackException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://xml.org/sax/features/namespaces", false); //$NON-NLS-1$
			factory.setFeature("http://xml.org/sax/features/validation", false); //$NON-NLS-1$
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false); //$NON-NLS-1$
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$
			// The contents to execute are actually built at:
			// org.eclipse.e4.core.macros.internal.ComposableMacro.toXMLBytes()
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(fFile), "UTF-8"))) { //$NON-NLS-1$
				DocumentBuilder parser = factory.newDocumentBuilder();
				Document document = parser.parse(new InputSource(reader));
				NodeList childNodes = document.getChildNodes();

				int length = childNodes.getLength();
				if (length != 1) {
					throw new MacroPlaybackException(MessageFormat.format(Messages.SavedJSMacro_MacrosEvalError,
							"Expected only one root in the macro XML.")); //$NON-NLS-1$
				}
				Node macroRoot = childNodes.item(0);
				for (Node macroInstructionNode : getNodesAsCollection(macroRoot, ComposableMacro.XML_INSTRUCTION_TAG)) {
					NamedNodeMap macroInstructionNodeAttributes = macroInstructionNode.getAttributes();
					if (macroInstructionNodeAttributes == null) {
						continue;
					}
					Node idAttribute = macroInstructionNodeAttributes.getNamedItem(ComposableMacro.XML_ID_ATTRIBUTE);
					if (idAttribute == null) {
						continue;
					}
					String macroInstructionId = idAttribute.getNodeValue();
					for (Node definitionNode : getNodesAsCollection(macroInstructionNode,
							ComposableMacro.XML_DEFINITION_TAG)) {
						Map<String, String> macroInstructionParameters = new HashMap<>();
						String key = null;
						String val = null;
						for (Node keyOrVal : getNodesAsCollection(definitionNode, ComposableMacro.XML_KEY_TAG,
								ComposableMacro.XML_VALUE_TAG)) {
							if (keyOrVal.getNodeName().equals(ComposableMacro.XML_KEY_TAG)) {
								key = getTextContent(keyOrVal);
							} else {
								val = getTextContent(keyOrVal);
								Assert.isNotNull(key, "XML malformed, received value without key."); //$NON-NLS-1$
								macroInstructionParameters.put(key, val);
								key = null;
								val = null;
							}
						}
						macroPlaybackContext.runMacroInstruction(macroInstructionId, macroInstructionParameters);
					}
				}
			}
		} catch (Exception e) {
			throw new MacroPlaybackException(
					MessageFormat.format(Messages.SavedJSMacro_MacrosEvalError, e.getMessage()), e);
		}
	}

	private String getTextContent(Node keyOrVal) {
		String str = keyOrVal.getTextContent();
		NamedNodeMap attributes = keyOrVal.getAttributes();
		if (attributes != null) {
			Node base64Attribute = attributes.getNamedItem(ComposableMacro.XML_BASE64_ATTRIBUTE);
			if (base64Attribute != null) {
				if (ComposableMacro.XML_BASE64_ATTRIBUTE_TRUE.equals(base64Attribute.getNodeValue())) {
					str = new String(Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8)),
							StandardCharsets.UTF_8);
				}
			}
		}
		return str;
	}

	private Collection<Node> getNodesAsCollection(final Node node, String... filterNodeName) {
		NodeList childNodes = node.getChildNodes();
		int length = childNodes.getLength();
		Set<String> filterNodeNames = new HashSet<>(Arrays.asList(filterNodeName));
		List<Node> result = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			Node item = childNodes.item(i);
			if (filterNodeName.length == 0 || filterNodeNames.contains(item.getNodeName())) {
				result.add(item);
			}
		}
		return result;
	}
}
