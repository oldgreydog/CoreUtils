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



import java.util.concurrent.locks.*;
import java.util.*;

import coreutil.configclassloader.*;
import coreutil.logging.*;



/**
 * A ConfigValueSet is any collection of config values that come from a particular single source such as a
 * file, database table or network source.  It can even be a set of virtual config values that are built up
 * from one or a few values to provide config subtrees that would otherwise have to be put into files, for example.
 * <br>
 * <p>It took a very long time to hit this problem, but I finally have a case where I need to use a pseudo-config set
 * from one project in another project where it can't be included directly.  That forced me to add the ability
 * for the ConfigManager to load config sets from a config block already loaded.  I decided that, rather than just
 * setting up the pseudo-config class for this type of loading, I'd just do it for all sets.  This has the side-effect
 * of letting you move the inclusion of config files on the command line into the main config file.  If you choose
 * to use this option, it means that the adding or removing of config files no longer requires synching across any
 * startup scripts that would otherwise be affected.
 * <br>
 * <p>Here is a sample of the config block you need to add to your main config file if you want to use this:
 * <br>
 * <br><pre>	&lt;Node name="configManager"&gt;
		&lt;Node name="configSource"&gt;
			&lt;Value name="class"&gt;coreutil.config.FileConfigValueSet&lt;/Value&gt;
			&lt;Value name="addPosition"	description="first,last"&gt;last&lt;/Value&gt;

			&lt;Node name="options"&gt;
				&lt;!--&lt;Value name="fileName"&gt;DAOModulesConfig.xml&lt;/Value&gt;--&gt;
			&lt;/Node&gt;
		&lt;/Node&gt;
	&lt;/Node&gt;
</pre>
 *
 * @author kaylor
 *
 */
public abstract class ConfigValueSet implements LoadableClass_Base {

	static private final String		PRAMETER_ADD_POSITION		= "addPosition";
	static private final String		ADD_POSITION_FIRST			= "first";
	static private final String		ADD_POSITION_LAST			= "last";

	public enum ADD_POSITION {
		FIRST,
		LAST
	}


	// Data members
	protected	ConfigNode		m_rootNode			= null;
	protected	String			m_setName			= null;

	protected	ADD_POSITION	m_addPosition		= ADD_POSITION.FIRST;

	protected final ReentrantReadWriteLock 		m_readWriteLock = new ReentrantReadWriteLock(true);	// The TRUE turns on "fair" scheduling in the lock.


	//*********************************
	public ConfigValueSet(String p_setName) {
		m_setName = p_setName;
	}


	/*********************************
	 * This is used to set parameter values when this set is being loaded via the config file inclusion method.
	 */
	@Override
	public boolean SetParameter(String p_parameterName, String p_value) {
		if (p_parameterName.equalsIgnoreCase(PRAMETER_ADD_POSITION)) {
			if (p_value.equalsIgnoreCase(ADD_POSITION_FIRST))
				m_addPosition = ADD_POSITION.FIRST;
			else
				m_addPosition = ADD_POSITION.LAST;

			return true;
		}
		else {
			Logger.LogError("ConfigValueSet.SetParameter() received an unknown parameter [" + p_parameterName + "].");
			return false;
		}
	}


	/*********************************
	 * For config sets that are loaded from config, this should have been set as a parameter telling the code whether to push the new config set on the front of the queue or add it to the end.
	 */
	public ADD_POSITION GetAddPosition() {
		return m_addPosition;
	}


	/*********************************
	 * This is only used when this set is being loaded via the config file inclusion method.  It shouldn't be
	 * called until all of the options parameters have been passed in through SetParameter().
	 */
	abstract public boolean InitFromConfig();


	/*********************************
	 * In applications that don't have real-time management APIs that allow for proper
	 * configuration changes while the application is running, we need a way to force
	 * the reloading of the config info from the various sources to retrieve any changes
	 * that might have been made since the last load/reload.
	 */
	abstract public boolean Reload();


	//*********************************
	/**
	 * I changed something in the code generator that caused it to need to get the root node directly, so I had to add this.  I doubt anyone else will need it.
	 * @return
	 */
	public ConfigNode GetRootNode() {
		try {
			m_readWriteLock.readLock().lock();	// Still need to lock this since it's possible that a reload could be happening and even just getting the root node has to be protected from that.
			return m_rootNode;
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigValueSet.GetRootNode() failed with error: ", t_error);
		}
		finally {
			m_readWriteLock.readLock().unlock();
		}

		return null;
	}


	//*********************************
	public ConfigNode GetNode(String p_fullName) {
		try {
			m_readWriteLock.readLock().lock();

			return m_rootNode.GetNode(p_fullName);
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigValueSet.GetNode() failed with error: ", t_error);
		}
		finally {
			m_readWriteLock.readLock().unlock();
		}

		return null;
	}


	//*********************************
	public String GetValue(String p_fullName) {
		try {
			m_readWriteLock.readLock().lock();

			ConfigNode t_targetNode = GetNode(p_fullName);
			if ((t_targetNode != null) && t_targetNode.IsValue())
				return ((ConfigValue)t_targetNode).GetValue();
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigValueSet.GetValue() failed with error: ", t_error);
		}
		finally {
			m_readWriteLock.readLock().unlock();
		}

		return null;
	}


	//*********************************
	public LinkedList<ConfigNode> GetChildNodeList() {
		return m_rootNode.GetChildNodeList();
	}


	//*********************************
	public void SetSetName(String p_newSetName) {
		m_setName = p_newSetName;
	}


	//*********************************
	public String GetSetName() {
		return m_setName;
	}
}
