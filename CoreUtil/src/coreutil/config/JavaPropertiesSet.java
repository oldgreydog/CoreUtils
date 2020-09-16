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
import java.util.Map.*;
import java.util.*;

import coreutil.logging.*;



/**
	Enables integration of legacy Java properties files into the ConfigManager.  They can be in either
	the standard text or newer XML formats.

	<p>When the properties file is loaded, this class will convert it to the ConfigManager in-memory
	that is used by all of the config sets.  But it will also keep a {@link java.util.Properties} object,
	too, so that the properties object can handle saving any changes back to the file.  To that end,
	if a value is changed, it is changed in both the in-memory tree and in the <code>Properties</code>
	object.</p>
 */
public class JavaPropertiesSet extends ConfigValueSet {

	static private final String		PRAMETER_FILE_NAME		= "fileName";


	//Data members
	private File			m_configFile	= null;
	private Properties		m_properties	= null;		// Keep the Properties object so that it can write back to the file if necessary.
	private boolean			m_isXml			= false;

	// Parameters for loading from a config file definition.
	private String			m_filePath		= null;


	//*********************************
	public JavaPropertiesSet() {
		super("file");
	}


	//*********************************
	public JavaPropertiesSet(String p_setName) {
		super(p_setName);
	}


	/*********************************
	 * This is used to set parameter values when this set is being loaded via the config file inclusion method.
	 */
	@Override
	public boolean SetParameter(String p_parameterName, String p_value) {
		if (p_parameterName.equalsIgnoreCase(PRAMETER_FILE_NAME))
			m_filePath = p_value;
		else
			return super.SetParameter(p_parameterName, p_value);

		return true;
	}


	/*********************************
	 * This is only used when this set is being loaded via the config file inclusion method.  It shouldn't be
	 * called until all of the options parameters have been passed in through SetParameter().
	 */
	@Override
	public boolean InitFromConfig() {
		return Load(m_filePath);
	}


	//*********************************
	public boolean Load(String p_fullPathName) {
		try {
			m_configFile = new File(p_fullPathName);

			if (!m_configFile.exists()) {
				Logger.LogError("JavaPropertiesSet.Load() could not open the file [" + p_fullPathName + "].");
				return false;
			}

			// We have to enable adding nodes to this config set because we are going to use the java utilities to parse the properties file and then we are going to transfer the java properties into this set.
			SetNodesCanBeAdded(true);

			// Use the java utility to parse the property file.
			m_properties = new Properties();
			FileInputStream t_inputStream = new FileInputStream(p_fullPathName);
			m_properties.load(t_inputStream);

			if (m_rootNode == null)
				m_rootNode = new ConfigNode("root", null);

			// Transfer the config values from the properties object into this config set.
			for (Entry<Object, Object> t_nextEntry: m_properties.entrySet())
				super.SetValue((String)t_nextEntry.getKey(), (String)t_nextEntry.getValue());	// Now that SetValue() is overloaded on this subclass, we have to be sure to only call the super's implementation here.

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("JavaPropertiesSet.Load() failed with exception : ", t_error);
			return false;
		}
	}


	//*********************************
	public boolean LoadXML(String p_fullPathName) {
		FileInputStream t_inputStream = null;
		try {
			m_configFile = new File(p_fullPathName);

			if (!m_configFile.exists()) {
				Logger.LogError("JavaPropertiesSet.LoadXML() could not open the file [" + p_fullPathName + "].");
				return false;
			}

			// We have to enable adding nodes to this config set because we are going to use the java utilities to parse the properties file and then we are going to transfer the java properties into this set.
			SetNodesCanBeAdded(true);

			// Use the java utility to parse the property file.
			t_inputStream	= new FileInputStream(p_fullPathName);
			m_properties	= new Properties();
			m_properties.loadFromXML(t_inputStream);
			m_isXml			= true;

			if (m_rootNode == null)
				m_rootNode = new ConfigNode("root", null);

			// Transfer the config values from the properties object into this config set.
			for (Entry<Object, Object> t_nextEntry: m_properties.entrySet())
				super.SetValue((String)t_nextEntry.getKey(), (String)t_nextEntry.getValue());	// Now that SetValue() is overloaded on this subclass, we have to be sure to only call the super's implementation here.

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("JavaPropertiesSet.LoadXML() failed with exception : ", t_error);
			return false;
		}
		finally {
			if (t_inputStream != null)
				try { t_inputStream.close(); } catch (Throwable t_dontCare) {}
		}
	}


	//*********************************
	@Override
	public boolean Save() {
		FileOutputStream t_outputStream = null;
		try {
			m_readWriteLock.writeLock().lock();

			t_outputStream = new FileOutputStream(m_configFile);
			if (m_isXml)
				m_properties.storeToXML(t_outputStream, null);
			else
				m_properties.store(t_outputStream, null);
		}
		catch (Throwable t_error) {
			Logger.LogException("JavaPropertiesSet.Save() failed with exception : ", t_error);
			return false;
		}
		finally {
			m_readWriteLock.writeLock().unlock();

			if (t_outputStream != null)
				try { t_outputStream.close(); } catch (Throwable t_dontCare) {}
		}

		return true;
	}


	/*********************************
	 * In applications that don't have real-time management APIs that allow for proper
	 * configuration changes while the application is running, we need a way to force
	 * the reloading of the config info from the various sources to retrieve any changes
	 * that might have been made since the last load/reload.
	 */
	@Override
	public boolean Reload() {
		FileInputStream t_inputStream = null;
		try {
			if ((m_configFile == null) || !m_configFile.exists()) {
				Logger.LogError("JavaPropertiesSet has not been initialized yet.");
				return false;
			}

			// Load the file first.
			t_inputStream	= new FileInputStream(m_configFile);
			m_properties	= new Properties();

			if (m_isXml)
				m_properties.loadFromXML(t_inputStream);
			else
				m_properties.load(t_inputStream);


			// Now that the reload is complete, we can get the write lock and swap out the new tree for the old one.
			try {
				m_readWriteLock.writeLock().lock();

				m_rootNode = new ConfigNode("root", null);	// Effectively clears the old properties from the set so that we can re-insert them here.

				// Transfer the config values from the properties object into this config set.
				for (Entry<Object, Object> t_nextEntry: m_properties.entrySet())
					super.SetValue((String)t_nextEntry.getKey(), (String)t_nextEntry.getValue());
			}
			catch (Throwable t_error) {
				Logger.LogException("JavaPropertiesSet.Reload() failed with error: ", t_error);
				return false;
			}
			finally {
				m_readWriteLock.writeLock().unlock();
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("JavaPropertiesSet.LoadXML() failed with exception : ", t_error);
			return false;
		}
		finally {
			if (t_inputStream != null)
				try { t_inputStream.close(); } catch (Throwable t_dontCare) {}
		}
	}


	//*********************************
	@Override
	public void SetValue(String p_fullName, String p_value) {
		try {
			m_readWriteLock.writeLock().lock();

			// Pass the call to the base class to change the value in this set.
			super.SetValue(p_fullName, p_value);

			// Now change the value in the properties so that if it needs to be written back to the file, it will be easy to let the Properies class do that work for us.
			m_properties.setProperty(p_fullName, p_value);
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigManager.GetValue() failed with error: ", t_error);
		}
		finally {
			m_readWriteLock.writeLock().unlock();
		}
	}

	//*********************************
	@Override
	public String toString()
	{
		return m_configFile.getName();
	}

}
