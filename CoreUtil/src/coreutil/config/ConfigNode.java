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



public class ConfigNode {

	// Data Members
	protected	String					m_name;
	protected	String					m_description	= null;
	protected	LinkedList<ConfigNode>	m_children 		= new LinkedList<ConfigNode>();
	protected	boolean					m_isDirty 		= false;

	// I need this for the code generator code so that it can access values that are at a parent level and thereby make it unnecessary to duplicate data throughout the tree.
	protected	ConfigNode				m_parentNode	= null;


	//*********************************
	public ConfigNode(String p_name, ConfigNode p_parent) {
		m_name			= p_name;
		m_parentNode	= p_parent;
	}


	/*********************************
	 * This is a bu-tugly kludge that I had to add because it was decided that we were going to use this config code to read in config files that were not of my making (i.e. not my format).  This lead to having to deal with multiple other tag layouts when writing the files back out so the normal "add an overloaded write function" thing didn't work so this is the trick I chose to figure out which type of node I was dealing with.
	 * @return
	 */
	public boolean IsValue() {
		return false;
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
	public String GetDescription() {
		return m_description;
	}


	//*********************************
	public void SetDescription(String  p_description)
	{
		m_description = p_description;
	}


	//*********************************
	public void SetNodeValue(String  p_fullName,
							 String  p_value,
							 boolean p_setDirty)
	{
		ConfigValue t_targetNode = (ConfigValue)GetNode(p_fullName);	// This does the recursing-through-the-tree thing for us, so it will either return a ConfigValue object or NULL.
		if (t_targetNode != null)
			t_targetNode.SetValue(p_value, p_setDirty);
		else {
			String[] t_splitName = p_fullName.split("\\.");
			AddValue(t_splitName, 0, p_value, p_setDirty);
		}
	}


	/*********************************
	 * XXXX NOTE: THIS ALLOWS YOU TO CREATE DUPLICATE CHILD NODES!! XXXX
	 * It does NOT check to see if a child node of the same name exists.  This is done on purpose!
	 * @param p_fullName
	 * @param p_setDirty
	 * @return
	 */
	public ConfigNode AddNode(String 	p_fullName,
							  boolean 	p_setDirty)
	{
		String[] t_splitName = p_fullName.split("\\.");
		if ((m_name.length() == 0) &&
			(t_splitName.length > 1))
		{	// This is a kludge to handle the nameless root node of the config tree.
			return AddNode(t_splitName, 0, p_setDirty);
		}

		if (t_splitName.length > 1) {
			return AddNode(t_splitName, 0, p_setDirty);
		}

		ConfigNode t_newNode = new ConfigNode(p_fullName, this);

		m_children.add(t_newNode);

		if (p_setDirty) {
			m_isDirty = true;
			t_newNode.SetNodeDirty();
		}

		return t_newNode;
	}


	/*********************************
	 * XXXX NOTE: THIS ALLOWS YOU TO CREATE DUPLICATE VALUE NODES!! XXXX
	 * It does NOT check to see if a value node of the same name exists.  This is done on purpose!
	 * @param p_fullName
	 * @param p_value
	 * @param p_setDirty
	 * @return
	 */
	public ConfigNode AddValue(String 	p_fullName,
							  String 	p_value,
							  boolean 	p_setDirty)
	{
		String[] t_splitName = p_fullName.split("\\.");
		if (t_splitName.length > 1)
		{
			Logger.LogError("ConfigNode.AddValue() is only used to add value nodes directly to tree nodes.  It does not parse name values to traverse a tree.  Use AddNode() instead.");
			return null;
		}

		ConfigNode t_newValue = new ConfigValue(p_fullName, p_value, this);

		m_children.add(t_newValue);

		if (p_setDirty) {
			m_isDirty = true;
			t_newValue.SetNodeDirty();
		}

		return t_newValue;
	}


	//*********************************
	protected ConfigNode AddNode(String[] 	p_splitName,
								 int 		p_nextIndex,
								 boolean 	p_setDirty)
	{
		if (p_nextIndex == (p_splitName.length - 1)) {	// If we have reached the last name field in the .-seperated full name, then we can just use AddNode() and just pass in that last name.
			return AddNode(p_splitName[p_nextIndex], p_setDirty);
		}

		ConfigNode t_nextChild = GetNode(p_splitName[p_nextIndex]);	// Find the child node that matches the current name from the split name.
		if (t_nextChild == null) {	// We will recursively build the node tree as we find internal nodes that do not exist.
			t_nextChild = AddNode(p_splitName[p_nextIndex], p_setDirty);
		}

		return t_nextChild.AddNode(p_splitName, p_nextIndex + 1, p_setDirty);
	}


	//*********************************
	protected ConfigNode AddValue(String[] 	p_splitName,
								  int 		p_nextIndex,
								  String 	p_value,
								  boolean 	p_setDirty)
	{
		if (p_nextIndex == (p_splitName.length - 1)) {	// If we have reached the last name field in the .-seperated full name, then we can just use AddNode() and just pass in that last name.
			return AddValue(p_splitName[p_nextIndex], p_value, p_setDirty);
		}

		ConfigNode t_nextChild = GetNode(p_splitName[p_nextIndex]);	// Find the child node that matches the current name from the split name.
		if (t_nextChild == null) {	// We will recursively build the node tree as we find internal nodes that do not exist.
			t_nextChild = AddNode(p_splitName[p_nextIndex], p_setDirty);
		}

		return t_nextChild.AddValue(p_splitName, p_nextIndex + 1, p_value, p_setDirty);
	}


	//*********************************
	/**
	 * Adds both child nodes and values.
	 * @param p_newNode
	 * @param p_addAsDirty Load() needs to add children without them getting marked as dirty, so this should be FALSE in those cases.  This should be TRUE only where code is actually adding a truly new child that needs to be saved at some point.
	 * @throws Exception
	 */
	public void AddChildNode(ConfigNode p_newNode, boolean p_addAsDirty) throws Exception {
		m_children.add(p_newNode);

		if (p_addAsDirty) {
			m_isDirty = true;
			p_newNode.SetNodeDirty();
		}
	}


	//*********************************
	public int GetChildNodeCount() {
		return m_children.size();
	}


	//*********************************
	public ListIterator<ConfigNode> GetChildNodeIterator() {
		return m_children.listIterator();
	}


	//*********************************
	public LinkedList<ConfigNode> GetChildNodeList() {
		return m_children;
	}


	//*********************************
	public String GetNodeValue(String p_fullName) {
		ConfigValue t_targetNode = (ConfigValue)GetNode(p_fullName);	// This does the recursing-through-the-tree thing for us, so it will either return a ConfigValue object or NULL.
		if (t_targetNode != null)
			return t_targetNode.GetValue();

		return null;
	}


	//*********************************
	public Integer GetIntValue(String p_fullName) {
		ConfigValue t_targetNode = (ConfigValue)GetNode(p_fullName);	// This does the recursing-through-the-tree thing for us, so it will either return a ConfigValue object or NULL.
		if (t_targetNode != null)
			return t_targetNode.GetIntValue();

		return null;
	}


	//*********************************
	public Float GetFloatValue(String p_fullName) {
		ConfigValue t_targetNode = (ConfigValue)GetNode(p_fullName);	// This does the recursing-through-the-tree thing for us, so it will either return a ConfigValue object or NULL.
		if (t_targetNode != null)
			return t_targetNode.GetFloatValue();

		return null;
	}


	//*********************************
	public Boolean GetBooleanValue(String p_fullName) {
		ConfigValue t_targetNode = (ConfigValue)GetNode(p_fullName);	// This does the recursing-through-the-tree thing for us, so it will either return a ConfigValue object or NULL.
		if (t_targetNode != null)
			return t_targetNode.GetBooleanValue();

		return null;
	}


	//*********************************
	public ConfigNode GetNode(String p_fullName) {
		String[] t_splitName = p_fullName.split("\\.");
		if ((m_name.length() == 0) &&
			(t_splitName.length > 1))
		{	// This is a kludge to handle the nameless root node of the config tree.
			return GetNode(t_splitName, 0);
		}

		if (t_splitName.length > 1) {
			return GetNode(t_splitName, 0);
		}

		if (m_name.compareToIgnoreCase(p_fullName) == 0)
			return this;

		ConfigNode t_nextChild;
		ListIterator<ConfigNode> t_childIterator = m_children.listIterator();
		while (t_childIterator.hasNext()) {
			t_nextChild = t_childIterator.next();
			if (t_nextChild.GetName().compareToIgnoreCase(p_fullName) == 0)
				return t_nextChild;
		}

		return null;
	}


	//*********************************
	public ConfigNode GetNode(String[] p_splitName, int p_nextIndex) {
		if (p_nextIndex == (p_splitName.length - 1)) {	// If we have reached the last name field in the .-seperated full name, then we can just use AddNode() and just pass in that last name.
			return GetNode(p_splitName[p_nextIndex]);
		}

		ConfigNode t_nextChild = GetNode(p_splitName[p_nextIndex]);
		if (t_nextChild != null)
			return t_nextChild.GetNode(p_splitName, p_nextIndex + 1);

		return null;
	}


	//*********************************
	public ConfigNode GetParentNode() {
		return m_parentNode;
	}


	//*********************************
	public boolean WriteToXML(Writer p_writer, int p_indentCount) {
		try {
			StringBuilder t_indention = new StringBuilder();
			for (int i = 0; i < p_indentCount; i++)
				t_indention.append("\t");

			if (m_children.size() == 0) {
				if (m_name.length() == 0)	// This is a kludge to handle the nameless root node of the config tree.
					p_writer.write(t_indention + "</Config>\n");
			}
			else {
				if (m_name.length() == 0)	// This is a kludge to handle the nameless root node of the config tree.
					p_writer.write(t_indention + "<Config>\n");
				else {
					p_writer.write(t_indention + "<Node name=\"" + m_name + "\"");

					if (m_description != null)
						p_writer.write(" description=\"" + m_description + "\"");

					p_writer.write(">\n");
				}

				ListIterator<ConfigNode> t_childIterator = m_children.listIterator();
				while (t_childIterator.hasNext()) {
					if (!t_childIterator.next().WriteToXML(p_writer, p_indentCount + 1))
						return false;	// If we fail somewhere in the tree, we'll just back directly out.
				}

				if (m_name.length() == 0)	// This is a kludge to handle the nameless root node of the config tree.
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
	public boolean RemoveNode(String p_targetNodeName) {
		ConfigNode t_targetNode = GetNode(p_targetNodeName);
		if (t_targetNode == null) {
			//Logger.LogError("ConfigNode.RemoveNode() did not find the target node [" + p_targetNodeName + "].");
			return true;	// If it's not here, then technically the remove "succeeded".
		}

		ConfigNode t_parentNode = t_targetNode.GetParentNode();
		if (t_parentNode != null)
			return t_parentNode.RemoveChildNode(p_targetNodeName);
		else
			return RemoveChildNode(p_targetNodeName);	// This is the root node so we just need to call RemoveChildNode() on it.
	}


	//*********************************
	protected boolean RemoveChildNode(String p_targetNodeName) {
		ListIterator<ConfigNode> t_childIterator = m_children.listIterator();
		ConfigNode t_nextChild;
		while (t_childIterator.hasNext()) {
			t_nextChild = t_childIterator.next();
			if (t_nextChild.GetName().compareToIgnoreCase(p_targetNodeName) == 0) {
				t_childIterator.remove();
				m_isDirty = true;
				return true;
			}
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
