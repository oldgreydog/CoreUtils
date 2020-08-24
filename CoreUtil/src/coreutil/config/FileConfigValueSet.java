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



/**
 * Using the config block form of config file inclusion is optional, but if you want to use it, here is the config block example:
 *  * <br>
 * <p>Here is a sample of the config block you need to add to your main config file if you want to use this:
 * <br>
 * <br><pre>	&lt;Node name="configManager"&gt;
		&lt;Node name="configSource"&gt;
			&lt;Value name="class"&gt;coreutil.config.FileConfigValueSet&lt;/Value&gt;
			&lt;Value name="addPosition"	description="first,last"&gt;last&lt;/Value&gt;

			&lt;Node name="options"&gt;
				&lt;!--&lt;Value name="filePath"&gt;DAOModulesConfig.xml&lt;/Value&gt;--&gt;
			&lt;/Node&gt;
		&lt;/Node&gt;
	&lt;/Node&gt;
</pre>

 * @author kaylor
 *
 */
public class FileConfigValueSet extends ConfigValueSet {

	static private final String		PRAMETER_FILE_PATH		= "filePath";


	//Data members
	private File		m_configFile = null;

	// Parameters for loading from a config file definition.
	private String		m_filePath		= null;


	//*********************************
	public FileConfigValueSet() {
		super("file");
	}


	//*********************************
	public FileConfigValueSet(String p_setName) {
		super(p_setName);
	}


	/*********************************
	 * This is used to set parameter values when this set is being loaded via the config file inclusion method.
	 */
	@Override
	public boolean SetParameter(String p_parameterName, String p_value) {
		if (p_parameterName.equalsIgnoreCase(PRAMETER_FILE_PATH))
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
		m_configFile	= new File(p_fullPathName);
		m_setName		= m_configFile.getName();

		if (!m_configFile.exists()) {
			Logger.LogError("FileConfigValueSet.Load() could not open the file [" + p_fullPathName + "].");
			return false;
		}

		XMLConfigParser t_configParser = new XMLConfigParser();
		if ((m_rootNode = t_configParser.ParseConfigFile(m_configFile)) == null)
			return false;

		return true;
	}


	//*********************************
	@Override
	public boolean Save() {
		try {
			m_readWriteLock.writeLock().lock();

			if (m_rootNode.IsTreeDirty()) {
				FileWriter t_xmlWriter = new FileWriter(m_configFile);
				t_xmlWriter.write(XMLConfigParser.XML_HEADER + "\n");
				if (!m_rootNode.WriteToXML(t_xmlWriter, 0))
					return false;

				t_xmlWriter.close();
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("FileConfigValueSet.Save() failed with exception : ", t_error);
			return false;
		}
		finally {
			m_readWriteLock.writeLock().unlock();
		}

		return true;
	}


	//*********************************
	/**
	 * In applications that don't have real-time management APIs that allow for proper
	 * configuration changes while the application is running, we need a way to force
	 * the reloading of the config info from the various sources to retrieve any changes
	 * that might have been made since the last load/reload.
	 */
	@Override
	public boolean Reload() {
		if (!m_configFile.exists()) {
			Logger.LogError("FileConfigValueSet has not been initialized yet.");
			return false;
		}

		// Load the file first.
		XMLConfigParser t_configParser = new XMLConfigParser();
		ConfigNode t_newRoot = t_configParser.ParseConfigFile(m_configFile);
		if (t_newRoot == null)
			return false;

		// Now that the reload is complete, we can get the write lock and swap out the new tree for the old one.
		try {
			m_readWriteLock.writeLock().lock();

			m_rootNode = t_newRoot;
		}
		catch (Throwable t_error) {
			Logger.LogException("FileConfigValueSet.Reload() failed with error: ", t_error);
			return false;
		}
		finally {
			m_readWriteLock.writeLock().unlock();
		}

		return true;
	}


	//*********************************
	@Override
	public String toString()
	{
		return m_configFile.getName();
	}

}
