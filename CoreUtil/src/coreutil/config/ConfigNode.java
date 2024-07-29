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

import java.io.*;
import java.util.*;
import java.sql.*;



public class ConfigNode extends Node_Base {

	// Data Members
	private	LinkedList<ConfigNode>	m_childNodes 		= new LinkedList<ConfigNode>();
	private	LinkedList<ConfigValue>	m_childValues 		= new LinkedList<ConfigValue>();

	// I need this for the code generator code so that it can access values that are at a parent level and thereby make it unnecessary to duplicate data throughout the tree.
	private	ConfigNode				m_parentNode	= null;


	//*********************************
	public ConfigNode(String p_name, ConfigNode p_parent) {
		super(p_name);

		m_name			= p_name;
		m_parentNode	= p_parent;
	}


	/*********************************
	 * XXXX NOTE: THIS ALLOWS YOU TO CREATE DUPLICATE CHILD NODES!! XXXX
	 * It does NOT check to see if a child node of the same name exists.  This is done on purpose!
	 * @param p_fullName
	 * @param p_setDirty
	 * @return
	 */
	public ConfigNode AddNode(String p_fullName)
	{
		try {
			String[]	t_splitName		= p_fullName.split("\\.");
			int			t_finalIndex	= t_splitName.length - 1;
			ConfigNode	t_currentNode	= this;
			ConfigNode	t_nextNode		= null;

			for (int i = 0; i < t_splitName.length; ++i) {
				if (i < t_finalIndex) {	// For the node names in the path...
					t_nextNode = t_currentNode.FindChildNode(t_splitName[i]);
					if (t_nextNode == null) {	// Build the path as we go if the one or more of the nodes doesn't exist.
						t_nextNode = new ConfigNode(t_splitName[i], t_currentNode);
						t_currentNode.AddNode(t_nextNode);
					}

					t_currentNode = t_nextNode;
				}
				else {	// And lastly, the node name at the end.  This has to be done this way to handle the fact that you can be adding duplicate nodes.
					t_nextNode = new ConfigNode(t_splitName[i], t_currentNode);
					t_currentNode.AddNode(t_nextNode);
				}
			}

			return t_nextNode;
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigNode.AddNode() failed with error: ", t_error);
			return null;
		}
	}


	//*********************************
	/**
	 * Adds child node objects (usually created as a config parser is traversing its source and creating the config tree).
	 * @param p_newNode
	 * @throws Exception
	 */
	private void AddNode(ConfigNode p_newNode) throws Exception {
		m_childNodes.add(p_newNode);
	}


	//*********************************
	private ConfigNode FindChildNode(String p_name) {
		for (ConfigNode t_nextChild: m_childNodes) {
			if (t_nextChild.GetName().compareToIgnoreCase(p_name) == 0)
				return t_nextChild;
		}

		return null;
	}


	//*********************************
	public ConfigNode GetNode(String p_fullName) {
		try {
			String[]	t_splitName		= p_fullName.split("\\.");
			int			t_finalIndex	= t_splitName.length - 1;
			ConfigNode	t_currentNode	= this;
			ConfigNode	t_nextNode		= null;

			for (int i = 0; i < t_splitName.length; ++i) {
				t_nextNode = t_currentNode.FindChildNode(t_splitName[i]);
				if (t_nextNode == null)	// If the next node doesn't exist in this set, then we return NULL and the ConfigManager will try the next set and so-on.
					return null;

				t_currentNode = t_nextNode;

				// If we're at the final name in the path, then we've found what we wanted and can return.
				if (i == t_finalIndex)
					return t_nextNode;
			}

			return null;	// Technically, you can't get here, but the compiler won't compile this without it.
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigNode.GetNode() failed with error: ", t_error);
			return null;
		}
	}


	/*********************************
	 * XXXX NOTE: THIS ALLOWS YOU TO CREATE DUPLICATE VALUE NODES!! XXXX
	 * It does NOT check to see if a value node of the same name exists.  This is done on purpose!
	 * @param p_fullName This can be just the new value name if you are adding it directly to the target node or it can be a full path name and the function will traverse the tree, creating the path as it goes if necessary, and then add the value at the end of the path.
	 * @param p_value
	 * @param p_setDirty
	 * @return
	 */
	public ConfigValue AddValue(String 	p_fullName,
							   String 	p_value)
	{
		try {
			String[]	t_splitName		= p_fullName.split("\\.");
			int			t_finalIndex	= t_splitName.length - 1;
			ConfigNode	t_currentNode	= this;
			ConfigNode	t_nextNode		= null;
			ConfigValue t_newValue		= null;

			for (int i = 0; i < t_splitName.length; ++i) {
				if (i < t_finalIndex) {	// For the node names in the path...
					t_nextNode = t_currentNode.FindChildNode(t_splitName[i]);
					if (t_nextNode == null) {	// Build the path as we go if the one or more of the nodes doesn't exist.
						t_nextNode = new ConfigNode(t_splitName[i], t_currentNode);
						t_currentNode.AddNode(t_nextNode);
					}

					t_currentNode = t_nextNode;
				}
				else {	// And lastly, the value name at the end.
					t_newValue = new ConfigValue(t_splitName[i], p_value);
					t_currentNode.AddValue(t_newValue);
				}
			}

			return t_newValue;
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigNode.AddValue() failed with error: ", t_error);
			return null;
		}
	}


	//*********************************
	/**
	 * Adds child value objects (usually created as a config parser is traversing its source and creating the config tree).
	 * @param p_newNode
	 * @throws Exception
	 */
	private void AddValue(ConfigValue p_newNode) throws Exception {
		m_childValues.add(p_newNode);
	}


	//*********************************
	private ConfigValue FindChildValue(String p_name) {
		for (ConfigValue t_nextValue: m_childValues) {
			if (t_nextValue.GetName().compareToIgnoreCase(p_name) == 0)
				return t_nextValue;
		}

		return null;
	}


	//*********************************
	public ConfigValue GetValue(String p_fullName) {
		try {
			String[]	t_splitName		= p_fullName.split("\\.");
			int			t_finalIndex	= t_splitName.length - 1;
			ConfigNode	t_currentNode	= this;
			ConfigNode	t_nextNode		= null;

			for (int i = 0; i < t_splitName.length; ++i) {
				if (i < t_finalIndex) {
					t_nextNode = t_currentNode.FindChildNode(t_splitName[i]);
					if (t_nextNode == null)	// If the next node doesn't exist in this set, then we return NULL and the ConfigManager will try the next set and so-on.
						return null;

					t_currentNode = t_nextNode;
				}
				else	// If we're at the final name in the path, then we can let FindChildValue() handle the last name.
					return t_currentNode.FindChildValue(t_splitName[i]);
			}

			return null;	// Technically, you can't get here, but the compiler won't compile this without it.
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigNode.GetValue() failed with error: ", t_error);
			return null;
		}
	}


	//*********************************
	public int GetChildNodeCount() {
		return m_childNodes.size();
	}


	//*********************************
	public ListIterator<ConfigNode> GetChildNodeIterator() {
		return m_childNodes.listIterator();
	}


	//*********************************
	public LinkedList<ConfigNode> GetChildNodeList() {
		return m_childNodes;
	}


	//*********************************
	public LinkedList<ConfigValue> GetChildValueList() {
		return m_childValues;
	}


	//*********************************
	public ConfigNode GetParentNode() {
		return m_parentNode;
	}


	//*********************************
	@Override
	public boolean WriteToXML(Writer p_writer, int p_indentCount) {
		try {
			StringBuilder t_indention = new StringBuilder();
			for (int i = 0; i < p_indentCount; i++)
				t_indention.append("\t");

			if (m_childNodes.size() == 0) {
				if ((m_name == null) || m_name.isBlank())	// This is a kludge to handle the nameless root node of the config tree.
					p_writer.write(t_indention + "</Config>\n");
			}
			else {
				if ((m_name == null) || m_name.isBlank())	// This is a kludge to handle the nameless root node of the config tree.
					p_writer.write(t_indention + "<Config>\n");
				else {
					p_writer.write(t_indention + "<Node name=\"" + m_name + "\"");

					if (m_description != null)
						p_writer.write(" description=\"" + m_description + "\"");

					p_writer.write(">\n");
				}

				// Since the child values are almost always above any child nodes because they are used as descriptive meta-data for this node, we will just default to putting them before any child nodes.
				for (ConfigValue t_nextValue: m_childValues) {
					if (!t_nextValue.WriteToXML(p_writer, p_indentCount + 1))
						return false;	// If we fail somewhere in the tree, we'll just back directly out.
				}

				for (ConfigNode t_nextNode: m_childNodes) {
					if (!t_nextNode.WriteToXML(p_writer, p_indentCount + 1))
						return false;	// If we fail somewhere in the tree, we'll just back directly out.
				}

				if ((m_name == null) || m_name.isBlank())	// This is a kludge to handle the nameless root node of the config tree.
					p_writer.write(t_indention + "</Config>\n");
				else
					p_writer.write(t_indention + "</Node>\n");
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigNode.WriteToXML() failed with exception : ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String toString()
	{
		return this.m_name;
	}
}
