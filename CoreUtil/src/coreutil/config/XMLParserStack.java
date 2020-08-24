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



import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;



public class XMLParserStack extends DefaultHandler {

	final static public 	String	XML_HEADER			= "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	protected	LinkedList<DefaultHandler>	m_handlerStack 	= new LinkedList<DefaultHandler>();



	//*****************************
	public void PushXMLHandler(DefaultHandler p_newXMLHandler) {
		m_handlerStack.push(p_newXMLHandler);
	}


	//*****************************
	public void PopXMLHandler() {
		m_handlerStack.pop();
	}


	//*****************************
	public int GetStackDepth() {
		return m_handlerStack.size();
	}


	//*****************************
	@Override
	public void notationDecl(String name,
							 String publicId,
							 String systemId)
		throws SAXException
	{
		m_handlerStack.element().notationDecl(name, publicId, systemId);
	}


	//*****************************
	@Override
	public void unparsedEntityDecl(String name,
								   String publicId,
								   String systemId,
								   String notationName)
		throws SAXException
	{
		m_handlerStack.element().unparsedEntityDecl(name, publicId, systemId, notationName);
	}


	//*****************************
	@Override
	public void setDocumentLocator (Locator locator)
	{
		m_handlerStack.element().setDocumentLocator(locator);
	}


	//*****************************
	@Override
	public void startDocument ()
		throws SAXException
	{
		m_handlerStack.element().startDocument();
	}


	//*****************************
	@Override
	public void endDocument ()
		throws SAXException
	{
		m_handlerStack.element().endDocument();
	}


	//*****************************
	@Override
	public void startPrefixMapping (String prefix,
									String uri)
		throws SAXException
	{
		m_handlerStack.element().startPrefixMapping(prefix, uri);
	}


	//*****************************
	@Override
	public void endPrefixMapping (String prefix)
		throws SAXException
	{
		m_handlerStack.element().endPrefixMapping(prefix);
	}


	//*****************************
	@Override
	public void startElement(String		uri,
							 String		localName,
							 String		qName,
							 Attributes attributes)
	 	throws SAXException
	{
		m_handlerStack.element().startElement(uri, localName, qName, attributes);
	}


	//*****************************
	@Override
	public void endElement(String uri,
						   String localName,
						   String qName)
		throws SAXException
	{
		m_handlerStack.element().endElement(uri, localName, qName);
	}


	//*****************************
	@Override
	public void characters(char[]	ch,
						   int		start,
						   int		length)
		throws SAXException
	{
		m_handlerStack.element().characters(ch, start, length);
	}


	//*****************************
	@Override
	public void ignorableWhitespace (char	ch[],
									 int	start,
									 int	length)
		throws SAXException
	{
		m_handlerStack.element().ignorableWhitespace(ch, start, length);
	}


	//*****************************
	@Override
	public void processingInstruction (String target, String data)
		throws SAXException
	{
		m_handlerStack.element().processingInstruction(target, data);
	}


	//*****************************
	@Override
	public void skippedEntity (String name)
		throws SAXException
	{
		m_handlerStack.element().skippedEntity(name);
	}


	//*****************************
	@Override
	public void warning (SAXParseException e)
		throws SAXException
	{
		m_handlerStack.element().warning(e);
	}


	//*****************************
	@Override
	public void error (SAXParseException e)
		throws SAXException
	{
		m_handlerStack.element().error(e);
	}


	//*****************************
	@Override
	public void fatalError (SAXParseException e)
		throws SAXException
	{
		m_handlerStack.element().fatalError(e);
	}

}
