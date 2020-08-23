/*
	Copyright 2016 Wes Kaylor

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
import java.util.concurrent.locks.*;

import coreutil.configclassloader.*;
import coreutil.logging.*;



/**
 * ConfigManager was born from my irritation with the limitations of java properties.  Yes, you could create dotted names (i.e. logging.maxLevel=10)
 * but you couldn't create repeating name blocks and the duplication of the parent names for each value was sloppy and didn't enforce grouping of values or subgroups.
 * <p>
 * <p>Instead, I wanted to use XML for the files with a matching in-memory representation that was extremely simple.  That allowed me to create groupings of values of
 * arbitrary depth and complexity which are easily accessible with a simple dotted pathname.</p>
 * <p>
 * <p>Once I got that done, I realized that by adding a simple stack mechanism that I could push as many different value sets into the manager as I liked and they
 * would all appear to be one seamless namespace.  That means you can combine any number of sets from any combination of sources you wanted: files, database tables or
 * even remote network sources.  Only the XML and property file set classes are included in this library because sets like database and network need to depend on things
 * that I didn't want to include here.  However, there are one or more sets in the examples that you can use as templates to fill out with your preferred access code.</p>
 * <p>
 * <p>To change config values while the app is running, you have two different paths: change the external source (i.e. file, db, etc.) and reload or enable in-memory editing.
 * If you want to change values by changing the source, you will need to create a reload thread in your app that will wake up on some interval and call ConfigManager.Reload().
 * That will re-read the source and create a new in-memory config tree to replace the older one.  How long the interval is is up to you.  You could even make it a config option
 * itself.</p>
 * <p>
 * <p>If you want to be able to change values in-memory, you have to tell the manager which config paths are editable.  By default, attempts to edit values is blocked for
 * all paths.  To enable editing for certain values or subtrees of values you call SetTheseOptionsWritable() with a string of paths separated by colons (":").  For example,
 * sending in the string "<b>logging.consoleLogger.maxLoggingLevel:defaultDB.*</b>" would enable in-memory editing of the max logging level for the console logger and any value
 * path that starts with "defaultDB.".  Any config set you create should be written to handle writing back to the source if you plan to enable in-memory editing.</p>
 * <p>
 * <p>I added the ability to define custom config sets to be loaded from a config section that looks like this:</p>
 * <p>
 * 	<pre><code>&lt;Node name="configManager"&gt;
		&lt;Node name="configSource"&gt;
			&lt;Value name="class"&gt;coreservices.model.util.config.PsuedoCompanyConfigSet&lt;/Value&gt;
			&lt;Node name="options"&gt;
				&lt;Value name="addPosition"	description="first,last"&gt;last&lt;/Value&gt;
			&lt;/Node&gt;
		&lt;/Node&gt;
	&lt;/Node&gt;</code></pre>
	</p>
 * <p>
 * <p>You can have as many <code>configSource</code> blocks as you need.  The <code>class</code> value is the full package path name of the class to be created and
 * you can add as many nodes and/or values as you like to the <code>options</code> block.  <code>addPosition</code> is the only required option.  It tells the ConfigManager
 * whether you want to add the new set to the front ("<code>first</code>") of the queue because it will be heavily used by the code and therefore should be one of the first
 * sets searched for values or to the end ("<code>last</code>") of the queue because it'll be rarely used and it would be a waste to force the ConfigManager to search it
 * for matches before getting to the set that is more likely to have the desired value.</p>
*/
public class ConfigManager {

	static private final String		CONFIG_KEY_CONFIG_SOURCE	= "configSource";
	static private final String		CONFIG_KEY_CLASS			= "class";
	static private final String		CONFIG_KEY_ADD_POSITION		= "addPosition";
	static private final String		CONFIG_KEY_OPTIONS			= "options";

	static private final String		ADD_POSITION_FIRST			= "first";
	static private final String		ADD_POSITION_LAST			= "last";


	static private final	ReentrantReadWriteLock 		s_readWriteLock		= new ReentrantReadWriteLock(true);	// The TRUE turns on "fair" scheduling in the lock.
	static private final	LinkedList<ConfigValueSet>	s_valueSetList		= new LinkedList<ConfigValueSet>();
	static private			Vector<Vector<String>>		s_writableOptions	= new Vector<Vector<String>>();


	//*********************************
	private ConfigManager() {}


	//===========================================
	static public void AddValueSetFirst(ConfigValueSet p_newValueSet) {
		try {
			s_readWriteLock.writeLock().lock();
			if (p_newValueSet != null)
				s_valueSetList.addFirst(p_newValueSet);
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigManager.AddValueSetFirst() failed with error: ", t_error);
		}
		finally {
			s_readWriteLock.writeLock().unlock();
		}
	}


	//===========================================
	static public void AddValueSetLast(ConfigValueSet p_newValueSet) {
		try {
			s_readWriteLock.writeLock().lock();
			if (p_newValueSet != null)
				s_valueSetList.addLast(p_newValueSet);
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigManager.AddValueSetLast() failed with error: ", t_error);
		}
		finally {
			s_readWriteLock.writeLock().unlock();
		}
	}


	//===========================================
	static public boolean AddValueSetsFromConfig() {
		try {
			// Load the output handlers from the "broker.outputs" block in the config file.
			Vector<LoadableClass_Base> t_newOutputHandlers = ConfigClassLoader.LoadClasses("configManager", "configSource");
			if (t_newOutputHandlers == null) {
				Logger.LogError("ConfigManager.AddValueSetsFromConfig() failed to load the [configManager] config node.");
				return false;
			}

			ConfigValueSet t_newConfigSource;
			for (LoadableClass_Base t_nextObject: t_newOutputHandlers) {
				t_newConfigSource = (ConfigValueSet)t_nextObject;

				if (!t_newConfigSource.InitFromConfig()) {
					Logger.LogError("ConfigManager.AddValueSetsFromConfig() failed to initialize the new config source.");
					return false;
				}

				if (t_newConfigSource.GetAddPosition() == ConfigValueSet.ADD_POSITION.FIRST)
					AddValueSetFirst(t_newConfigSource);
				else
					AddValueSetLast(t_newConfigSource);
			}


//			ConfigNode t_configSourceConfigNode = GetNode("configManager");
//			if ((t_configSourceConfigNode != null) && !LoadConfigSource(t_configSourceConfigNode)) {
//				Logger.LogError("ConfigManager.AddValueSetsFromConfig() failed to load config sources from the config definitions.");
//				return false;
//			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigManager.AddValueSetsFromConfig() failed with error: ", t_error);
			return false;
		}
	}


	//===========================================
	/**
	 * NOTE!!! If multiple instances of a set are loaded, then you will get a TRUE even if the one you want isn't present!
	 * If you need more specificity in the set names to get passed this problem, then you will have to add that in some way.
	 */
	static public boolean ContainsValueSet(String p_targetValueSetName) {
		try {
			s_readWriteLock.readLock().lock();

			for (ConfigValueSet t_nextSet: s_valueSetList) {
				if (t_nextSet.GetSetName().equalsIgnoreCase(p_targetValueSetName))
					return true;
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigManager.ContainsValueSet() failed with error: ", t_error);
			return false;
		}
		finally {
			s_readWriteLock.readLock().unlock();
		}

		return true;
	}


	//===========================================
	static public void SetTheseOptionsWritable(String p_writableOptionList) {
		if (p_writableOptionList.isEmpty())
			return;

		String t_separateOptions[] = p_writableOptionList.split(":");
		Vector<String>	t_nextOptionParts;
		String			t_parts[];
		for (String t_nextOption: t_separateOptions) {
			t_parts				= t_nextOption.split("\\.");
			t_nextOptionParts	= new Vector<String>(t_parts.length);
			for (String t_nextPart: t_parts)
				t_nextOptionParts.add(t_nextPart);

			s_writableOptions.add(t_nextOptionParts);
		}
	}


	//===========================================
	static public boolean SaveChanges() {
		// Because of the extreme time cost of some of the reloads, I moved the reload locking inside the individual value sets so that the locking can be done in the smallest unit necessary so that contention is minimized as much as possible.
		for (ConfigValueSet t_nextValueSet: s_valueSetList) {
			if (!t_nextValueSet.Save()) {
				Logger.LogError("ConfigManager.SaveChanges() failed to save config set [" + t_nextValueSet.GetSetName() + "].");
				return false;
			}
		}

		return true;
	}


	//===========================================
	static public boolean ReloadTargetConfigurations(String p_targetName) {
		// Because of the extreme time cost of some of the reloads, I moved the reload locking inside the individual value sets so that the locking can be done in the smallest unit necessary so that contention is minimized as much as possible.
		for (ConfigValueSet t_nextValueSet: s_valueSetList) {
			if (t_nextValueSet.GetSetName().compareToIgnoreCase(p_targetName) == 0)
				if (!t_nextValueSet.Reload()) {
					Logger.LogError("ConfigManager.ReloadTargetConfigurations() failed to reload config set [" + p_targetName + "].");
					return false;
				}
		}

		return true;
	}


	//===========================================
	static public boolean ReloadAllConfigurations() {
		// I'm not sure why I didn't do this originally in the code, but I'm going to add it now.  I think it makes sense to be sure that any changes are saved before we do a reload.
		if (!SaveChanges())
			return false;

		// Because of the extreme time cost of some of the reloads, I moved the reload locking inside the individual value sets so that the locking can be done in the smallest unit necessary so that contention is minimized as much as possible.
		for (ConfigValueSet t_nextValueSet: s_valueSetList) {
			if (!t_nextValueSet.Reload()) {
				Logger.LogError("ConfigManager.ReloadAllConfigurations() failed to reload config set [" + t_nextValueSet.GetSetName() + "].");
				return false;
			}
		}

		return true;
	}


	//===========================================
	static public boolean Close(boolean p_saveChanges) {
		try {
			s_readWriteLock.writeLock().lock();

			if (p_saveChanges) {
				SaveChanges();
			}

			s_valueSetList.clear();	// Remove all configuration value sets now that they have been saved.
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigManager.Close() failed with error: ", t_error);
			return false;
		}
		finally {
			s_readWriteLock.writeLock().unlock();
		}

		return true;
	}


	//===========================================
	static public ConfigNode GetNode(String p_fullName) {
		try {
			s_readWriteLock.readLock().lock();

			// Iterate through the value sets to find the first one that has a match for this node path name.
			ConfigNode t_targetNode;
			for (ConfigValueSet t_nextValueSet: s_valueSetList) {
				if ((t_targetNode = t_nextValueSet.GetNode(p_fullName)) != null)
					return t_targetNode;
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigManager.GetNode() failed with error: ", t_error);
		}
		finally {
			s_readWriteLock.readLock().unlock();
		}

		return null;
	}


	//===========================================
	/**
	 * This special function will return ALL instances of the node name found in all of the sets.
	 * This now lets us load multiple config sets that have duplicate child nodes and get access
	 * to all of them, not just the first one found.  I added this to allow me to generate multiple
	 * rest APIs with their respective config files and load them separately at the command line
	 * instead of having to merge them before use.
	 * @param p_fullName
	 * @return
	 */
	static public LinkedList<ConfigNode> GetNodeInAllSets(String p_fullName) {
		try {
			s_readWriteLock.readLock().lock();

			// Iterate through the value sets to find the first one that has a match for this node path name.
			LinkedList<ConfigNode>	t_results	= new LinkedList<ConfigNode>();
			ConfigNode t_targetNode;
			for (ConfigValueSet t_nextValueSet: s_valueSetList) {
				if ((t_targetNode = t_nextValueSet.GetNode(p_fullName)) != null)
					t_results.add(t_targetNode);
			}

			return t_results;
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigManager.GetNode() failed with error: ", t_error);
			return null;
		}
		finally {
			s_readWriteLock.readLock().unlock();
		}
	}


	//===========================================
	static public boolean SetValue(String p_fullName, String p_value) {
		try {
			s_readWriteLock.writeLock().lock();

			// Before we aquire the lock, we need to figure out if this option is even writable.
			boolean t_optionIsWritable = false;
			String	t_optionNameParts[]	= p_fullName.split("\\.");

		outerLoop:
			for (Vector<String> t_nextRule: s_writableOptions) {
				// If the target option name is shorter than the rule, then it can't be writable be this rule so we'll move on to the next rule.
				if (t_optionNameParts.length < t_nextRule.size())
					continue;

				// If the option name length is equal to the rule, then the it must match the whole rule.
				// If the rule is shorter than the option name, then only the first parts of the option name have to match the rule parts to pass as writable.
				for (int i = 0; ((i < t_optionNameParts.length) && (i < t_nextRule.size())); i++) {
					if (t_nextRule.get(i).compareTo("*") == 0)	// If the rule for this part of the name is == *, then it doesn't matter what the name part is, it's a match so we'll go to the next rule.
						continue;

					if (t_nextRule.get(i).compareToIgnoreCase(t_optionNameParts[i]) != 0)
						continue outerLoop;	// If the name part doesn't match, then it fails this rule and we need to go on to the next rule.
				}

				// If we get here, then the name passed all of the parts of the rule so we are done.
				t_optionIsWritable = true;
				break;
			}

			if (!t_optionIsWritable)
				return false;


			ConfigValue t_targetNode = (ConfigValue)GetNode(p_fullName);	// This does the recursing-through-the-tree thing for us, so it will either return a ConfigValue object or NULL.
			if (t_targetNode == null) {
				Logger.LogError("ConfigManager.SetValue() did not find the value [" + p_fullName + "] so it can not be changed.  Node creation is not currently supported.");
				return false;
			}

			t_targetNode.SetValue(p_value, true);
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigManager.SetValue() failed with error: ", t_error);
			return false;
		}
		finally {
			s_readWriteLock.writeLock().unlock();
		}

		return true;
	}


	//===========================================
	static public String GetValue(String p_fullName) {
		try {
			s_readWriteLock.readLock().lock();

			ConfigValue t_targetNode;
			for (ConfigValueSet t_nextValueSet: s_valueSetList) {
				if ((t_targetNode = (ConfigValue)t_nextValueSet.GetNode(p_fullName)) != null)
					return t_targetNode.GetValue();
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigManager.GetValue() failed with error: ", t_error);
		}
		finally {
			s_readWriteLock.readLock().unlock();
		}

		return null;
	}


	//===========================================
	static public boolean GetBooleanValue(String p_fullName) {
		try {
			s_readWriteLock.readLock().lock();

			ConfigNode t_targetNode = GetNode(p_fullName);
			if (t_targetNode == null) {
				Logger.LogError("ConfigManager.GetBooleanValue() did not find the boolean config option [" + p_fullName + "].  A default value of FALSE will be returned.");
				return false;
			}
			else if (!t_targetNode.IsValue()) {
				Logger.LogError("ConfigManager.GetBooleanValue() found a node at path [" + p_fullName + "], not a value.  A default value of FALSE will be returned.");
				return false;
			}

			return ((ConfigValue)t_targetNode).GetBooleanValue();
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigManager.GetBooleanValue() failed with error: ", t_error);
			return false;
		}
		finally {
			s_readWriteLock.readLock().unlock();
		}
	}


	//===========================================
	static public Integer GetIntValue(String p_fullName) {
		try {
			s_readWriteLock.readLock().lock();

			ConfigNode t_targetNode = GetNode(p_fullName);
			if (t_targetNode == null) {
				Logger.LogError("ConfigManager.GetIntValue() did not find the boolean config option [" + p_fullName + "].  A default value of NULL will be returned.");
				return null;
			}
			else if (!t_targetNode.IsValue()) {
				Logger.LogError("ConfigManager.GetIntValue() found a node at path [" + p_fullName + "], not a value.  A default value of NULL will be returned.");
				return null;
			}

			return ((ConfigValue)t_targetNode).GetIntValue();
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigManager.GetIntValue() failed with error: ", t_error);
			return null;
		}
		finally {
			s_readWriteLock.readLock().unlock();
		}
	}


	//===========================================
	static public Double GetDoublValue(String p_fullName) {
		try {
			s_readWriteLock.readLock().lock();

			ConfigNode t_targetNode = GetNode(p_fullName);
			if (t_targetNode == null) {
				Logger.LogError("ConfigManager.GetDoublValue() did not find the boolean config option [" + p_fullName + "].  A default value of NULL will be returned.");
				return null;
			}
			else if (!t_targetNode.IsValue()) {
				Logger.LogError("ConfigManager.GetDoublValue() found a node at path [" + p_fullName + "], not a value.  A default value of NULL will be returned.");
				return null;
			}

			return ((ConfigValue)t_targetNode).GetDoubleValue();
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigManager.GetDoublValue() failed with error: ", t_error);
			return null;
		}
		finally {
			s_readWriteLock.readLock().unlock();
		}
	}
}
