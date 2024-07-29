package coreutil.config;

import java.io.Writer;

abstract public class Node_Base {

	// Data Members
	protected	String					m_name			= null;
	protected	String					m_description	= null;


	//*********************************
	public Node_Base(String p_name) {
		m_name = p_name;
	}


	/*********************************
	 * This is a special-use function that was required by one of the config file parsers (not mine!).  For some reason, the parser was splitting the tag value over two calls to the characters() function so that the name was getting split so I had to add this so that the name could be assembled by multiple passes if necessary.
	 * @param p_newName
	 */
	public void SetName(String p_newName) {
		m_name = p_newName;
	}


	//*********************************
	public String GetName() {
		return m_name;
	}


	//*********************************
	public void SetDescription(String  p_description)
	{
		m_description = p_description;
	}


	//*********************************
	public String GetDescription() {
		return m_description;
	}


	//*********************************
	abstract public boolean WriteToXML(Writer p_writer, int p_indentCount);

}
