/*
	Copyright 2020 Wes Kaylor

	This file is part of CoreUtil.

	CoreUtil is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	CoreUtil is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public License
	along with CoreUtil.  If not, see <http://www.gnu.org/licenses/>.
 */


package coreutil.config;



import coreutil.logging.*;

import javax.xml.parsers.*;
import java.util.*;
import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;



public class XMLConfigParser extends DefaultHandler {

	final static public		String	CONFIG_TAG			= "Config";
	final static public		String	NODE_TAG			= "Node";
	final static public		String	VALUE_TAG			= "Value";
	final static public		String	NAME_ATTR			= "name";
	final static public		String	DESCRIPTION_ATTR	= "description";
	final static public 	String	XML_HEADER			= "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	protected	LinkedList<ConfigNode>	m_nodeStack 	= new LinkedList<ConfigNode>();
	protected	ConfigNode 				m_rootNode 		= new ConfigNode(null, null);	// The config code expects there to be an empty root node holding all of the children so that is works with child value nodes that don't have a root parent node.
	protected	boolean					m_inValue		= false;
	protected	ConfigValue				m_currentValue	= null;

	public ConfigNode ParseConfigString(String p_configInfo, boolean p_addHeader) throws RuntimeException {
		try {
			String t_configInfo;
			if (p_addHeader)
				t_configInfo = XML_HEADER + p_configInfo;
			else
				t_configInfo = p_configInfo;

			SAXParser t_parser = SAXParserFactory.newInstance().newSAXParser();
			t_parser.parse(new ByteArrayInputStream(t_configInfo.getBytes("UTF-8")), this);

			return m_rootNode;
		}
		catch (Throwable t_error) {
			Logger.LogFatal("XMLConfigParser.ParseConfigString() failed with the error: ", t_error);
			Logger.LogFatal("XMLConfigParser.ParseConfigString() failed for string [" + p_configInfo + "]");
			return null;
		}
	}

	public ConfigNode ParseConfigFile(File p_configFile) throws RuntimeException {
		try {
			SAXParser t_parser = SAXParserFactory.newInstance().newSAXParser();
			t_parser.parse(new FileInputStream(p_configFile), this);

			return m_rootNode;
		}
		catch (Throwable t_error) {
			Logger.LogFatal("XMLConfigParser.ParseConfigFile() failed with the error: ", t_error);
			return null;
		}
	}

	@Override
	public void startElement(String		p_uri,
							 String		p_localName,
							 String		p_qName,
							 Attributes	p_attributes)
		throws SAXException
	{
		if (p_qName.compareToIgnoreCase(CONFIG_TAG) == 0) {
			m_nodeStack.addFirst(m_rootNode);	// Push the root node on the stack so that the parser will have some place to start appending the child nodes.
		}
		else if ((p_qName.compareToIgnoreCase(NODE_TAG) == 0) ||
				 (p_qName.compareToIgnoreCase(VALUE_TAG) == 0))
		{
			try {
				ConfigNode	t_parentNode, t_newNode;
				ConfigValue	t_newValue;
				if (m_nodeStack.size() == 0) {
					t_newNode = new ConfigNode(p_attributes.getValue(NAME_ATTR), null);
					m_rootNode = t_newNode;
					m_nodeStack.addFirst(t_newNode);	// Add the new node to the XML parser's stack so that it can receive new child nodes or a value in the following parse.
				}
				else if (p_qName.compareToIgnoreCase(VALUE_TAG) == 0) {
					t_parentNode	= m_nodeStack.element();
					t_newValue		= t_parentNode.AddValue(p_attributes.getValue(NAME_ATTR), "");	// Add the new value node to the config tree...

					// Description is an optional attribute so we'll check for it here and add it if it exists.
					String t_description = p_attributes.getValue(DESCRIPTION_ATTR);
					if (t_description != null)
						t_newValue.SetDescription(t_description);


					// If we are in a <Value> tag, then turn this flag on so that we can store its value.
					m_inValue		= true;
					m_currentValue	= t_newValue;
				}
				else {
					t_newNode = m_nodeStack.element().AddNode(p_attributes.getValue(NAME_ATTR));
					m_nodeStack.addFirst(t_newNode);	// Add the new node to the XML parser's stack so that it can receive new child nodes or a value in the following parse.

					// Description is an optional attribute so we'll check for it here and add it if it exists.
					String t_description = p_attributes.getValue(DESCRIPTION_ATTR);
					if (t_description != null)
						t_newNode.SetDescription(t_description);
				}
			}
			catch (Exception t_error) {
				Logger.LogException("XMLConfigParser.startElement() failed with error: ", t_error);
				throw new SAXException(t_error);
			}
		}
		else {
			throw new SAXException("XMLConfigParser.startElement() found an unknown node type [" + p_qName + "]");
		}
	}

	@Override
	public void characters(char[]	p_ch,
						   int		p_start,
						   int		p_length)
		throws SAXException
	{
		if (!m_inValue)
			return;

		String t_newValue = new String(p_ch, p_start, p_length);
		//t_newValue = t_newValue.trim();

		if (t_newValue.length() > 0) {
			if (m_currentValue == null) {
				throw new SAXException("XMLConfigParser.characters() was called without a valid current value object.");
			}

			String t_existingValue = m_currentValue.GetStringValue();
			m_currentValue.SetValue(t_existingValue + t_newValue);
		}
	}

	@Override
	public void ignorableWhitespace (char	p_ch[],
									 int	p_start,
									 int	p_length)
		throws SAXException
	{
		if (!m_inValue)
			return;

		String t_newValue = new String(p_ch, p_start, p_length);
		if (t_newValue.length() > 0) {
			if (m_currentValue == null) {
				throw new SAXException("XMLConfigParser.ignorableWhitespace() was called without a valid current value object.");
			}

			String t_existingValue = m_currentValue.GetStringValue();
			m_currentValue.SetValue(t_existingValue + t_newValue);
		}
	}

	@Override
	public void endElement(String p_uri,
						   String p_localName,
						   String p_qName)
		throws SAXException
	{
		if (p_qName.compareToIgnoreCase(NODE_TAG) == 0) {
			m_nodeStack.removeFirst();
		}
		else if (p_qName.compareToIgnoreCase(VALUE_TAG) == 0) {
			m_inValue		= false;
			m_currentValue	= null;
		}
	}
}
