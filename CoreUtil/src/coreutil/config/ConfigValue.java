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


import java.io.*;

import coreutil.logging.*;



public class ConfigValue extends Node_Base {

	// Data Members
	protected	String						m_value;
	protected	ConfigValueSubstituter		m_configValueSubstituter	= null;


	//*********************************
	public ConfigValue(String p_name, String p_value) {
		super(p_name);
		m_value = p_value;

		// The way the database config loaders work when they find an :xml: value that needs to be parsed, they create this object with NULL name/value and then fill them in on Init(), so we have to handle that.
		if (m_value != null) {
			if (m_value.contains(ConfigValueSubstituter.SUBSTITUTION_DELIMITER))
				m_configValueSubstituter = new ConfigValueSubstituter();
		}
	}


	//*********************************
	public boolean SetValue(String  p_value)
	{
		m_value		= p_value;

		if (m_value.contains(ConfigValueSubstituter.SUBSTITUTION_DELIMITER))
			m_configValueSubstituter = new ConfigValueSubstituter();
		else
			m_configValueSubstituter = null;

		return true;
	}


	//*********************************
	public String GetStringValue() {
		if (m_configValueSubstituter != null)
			return m_configValueSubstituter.ReplaceValuesInString(m_value);

		return m_value;
	}


	//*********************************
	public Boolean GetBooleanValue() {
		String t_value = GetStringValue();	// We need to use GetStringValue() here instead of m_value because the addition of the substitution functionality now means that we have to be sure that if this value has substitutions in it that they are cleared before we try to do the boolean conversion.
		try {
			if (t_value.trim().isBlank()) {
				Logger.LogError("ConfigValue.GetBooleanValue() failed with error: config option [" + m_name + "] has an empty value.");
				return null;
			}

			if (t_value.equalsIgnoreCase("true") ||
				t_value.equalsIgnoreCase("yes") ||
				t_value.equalsIgnoreCase("1"))
			{
				return true;
			}
			else
				return false;
		}
		catch (Throwable t_error) {
			Logger.LogError("ConfigValue.GetBooleanValue() failed to convert the string [" + t_value + "] to a boolean.");
			return null;
		}
	}


	//*********************************
	public Integer GetIntValue() {
		String t_value = GetStringValue();	// We need to use GetStringValue() here instead of m_value because the addition of the substitution functionality now means that we have to be sure that if this value has substitutions in it that they are cleared before we try to do the int conversion.
		try {
			if (t_value.trim().isBlank()) {
				Logger.LogError("ConfigValue.GetIntValue() failed with error: config option [" + m_name + "] has an empty value.");
				return null;
			}

			return Integer.parseInt(t_value);
		}
		catch (Throwable t_error) {
			Logger.LogError("ConfigValue.GetIntValue() failed to convert the string [" + t_value + "] to an integer.");
			return null;
		}
	}


	//*********************************
	public Float GetFloatValue() {
		String t_value = GetStringValue();	// We need to use GetStringValue() here instead of m_value because the addition of the substitution functionality now means that we have to be sure that if this value has substitutions in it that they are cleared before we try to do the float conversion.
		try {
			if (t_value.trim().isBlank()) {
				Logger.LogError("ConfigValue.GetFloatValue() failed with error: config option [" + m_name + "] has an empty value.");
				return null;
			}

			return Float.parseFloat(t_value);
		}
		catch (Throwable t_error) {
			Logger.LogError("ConfigValue.GetFloatValue() failed to convert the string [" + t_value + "] to a float.");
			return null;
		}
	}


	//*********************************
	public Double GetDoubleValue() {
		String t_value = GetStringValue();	// We need to use GetStringValue() here instead of m_value because the addition of the substitution functionality now means that we have to be sure that if this value has substitutions in it that they are cleared before we try to do the double conversion.
		try {
			if (t_value.trim().isBlank()) {
				Logger.LogError("ConfigValue.GetDoubleValue() failed with error: config option [" + m_name + "] has an empty value.");
				return null;
			}

			return Double.parseDouble(t_value);
		}
		catch (Throwable t_error) {
			Logger.LogError("ConfigValue.GetDoubleValue() failed to convert the string [" + t_value + "] to a float.");
			return null;
		}
	}


	//*********************************
	@Override
	public boolean WriteToXML(Writer p_writer, int p_indentCount) {
		try {
			StringBuilder t_indention = new StringBuilder();
			for (int i = 0; i < p_indentCount; i++)
				t_indention.append("\t");

			p_writer.write(t_indention + "<Value name=\"" + m_name + "\"");

			if (m_description != null)
				p_writer.write(" description=\"" + m_description + "\"");

			p_writer.write(">" + m_value + "</Value>\n");
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigValue.WriteToXML() failed with exception : ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String toString()
	{
		if(m_description != null)
		{
			return m_description;
		}
		else
		{
			return m_name;
		}
	}
}
