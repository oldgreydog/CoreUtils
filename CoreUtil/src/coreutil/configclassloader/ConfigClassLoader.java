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


package coreutil.configclassloader;



import java.util.*;

import coreutil.config.*;
import coreutil.logging.*;



/**
 * This loader parses a config group like this one:
 * <br><br>
 * <pre><code>&lt;Node name="stats"&gt;
	&lt;Node name="outputhandler"&gt;
		&lt;Value name="class"&gt;coreutil2.managers.stats.localmanager.output.LoggerHandler&lt;/Value&gt;
		&lt;Node name="options"&gt;
			&lt;Value name="loglevel"&gt;40&lt;/Value&gt;
		&lt;/Node&gt;
	&lt;/Node&gt;
	&lt;Node name="outputhandler"&gt;
		&lt;Value name="class"&gt;coreutil2.managers.stats.localmanager.output.StatsManagerNetHandler&lt;/Value&gt;
		&lt;Node name="options"&gt;
		&lt;/Node&gt;
	&lt;/Node&gt;
&lt;/Node&gt;</code></pre>
 * <br>
 * <p>In this example, the p_configRootPath is [stats] and the p_configSubGroupName is [outputhandler].</p>
 *
 * @author kaylor
 *
 */
public class ConfigClassLoader {

	// Static members

	static private final String		CONFIG_KEY_CLASS			= "class";
	static private final String		CONFIG_KEY_OPTIONS			= "options";



	//*********************************
	/**
	 * This version is the default, simple-case where you only have one root to load from.
	 *
	 * @param p_configRootPath
	 * @param p_configSubGroupName
	 * @return
	 */
	static public Vector<LoadableClass_Base> LoadClasses(String p_configRootPath, String p_configSubGroupName) {
		try {
			// Load the default task generators first.
			ConfigNode t_rootConfigNode = ConfigManager.GetNode(p_configRootPath);
			if (t_rootConfigNode == null) {
				Logger.LogError("ConfigClassLoader.Init() failed to find the [" + p_configRootPath + "] config node.");
				return null;
			}

			return LoadClasses(t_rootConfigNode, p_configSubGroupName);
		}
		catch (Exception t_error) {
			Logger.LogException("ConfigClassLoader.LoadClasses() failed with error: ", t_error);
			return null;
		}
	}


	//*********************************
	/**
	 * This version lets you do more complicated loads where you may have, for example, multiple config files loaded with the same structure
	 * and you need to iterate through them to make sure that you load ALL of the classes that are supposed to be loaded.
	 *
	 * @param p_rootConfigNode
	 * @param p_configSubGroupName
	 * @return
	 */
	static public Vector<LoadableClass_Base> LoadClasses(ConfigNode p_rootConfigNode, String p_configSubGroupName) {
		try {
			Vector<LoadableClass_Base>	t_loadedClasses			= new Vector<>();
			ListIterator<ConfigNode>	t_generatorNodeIterator = p_rootConfigNode.GetChildNodeIterator();
			ConfigNode					t_nextHandlerNode;
			LoadableClass_Base			t_newHandler;
			while (t_generatorNodeIterator.hasNext()) {
				t_nextHandlerNode = t_generatorNodeIterator.next();

				// If the next node is not a target subnode, skip it.  Do this just in case...
				if (t_nextHandlerNode.GetName().compareToIgnoreCase(p_configSubGroupName) != 0)
					continue;

				t_newHandler = InitClass(t_nextHandlerNode);
				if (t_newHandler == null)
				{
					Logger.LogError("ConfigClassLoader.LoadClasses() failed to create a class.");
					t_loadedClasses.clear();	// Remove any generators that have already been created.
					return null;
				}

				t_loadedClasses.add(t_newHandler);
			}

			return t_loadedClasses;
		}
		catch (Exception t_error) {
			Logger.LogException("ConfigClassLoader.LoadClasses() failed with error: ", t_error);
			return null;
		}
	}


	//*********************************
	static private LoadableClass_Base InitClass(ConfigNode p_configInfo) {
		String				t_className		= p_configInfo.GetValue(CONFIG_KEY_CLASS).GetStringValue();
		LoadableClass_Base	t_newHandler;

		try {
			Class<LoadableClass_Base> t_class = (Class<LoadableClass_Base>) Class.forName(t_className);
			t_newHandler = t_class.getDeclaredConstructor().newInstance();

			// We'll iterate through the options and send them to the t_newGenerator as parameters.
			ConfigNode t_handlerOptions = p_configInfo.GetNode(CONFIG_KEY_OPTIONS);
			if (t_handlerOptions != null) {
				for (ConfigValue t_nextValue: t_handlerOptions.GetChildValueList())
					t_newHandler.SetParameter(t_nextValue.GetName(), t_nextValue.GetStringValue());

				// There is at least one special case config setup that has a child node inside the options node so this will handle that.
				for (ConfigNode t_nextNode: t_handlerOptions.GetChildNodeList())
					t_newHandler.SetParameter(t_nextNode);
			}

			return t_newHandler;
		}
		catch (Throwable t_error) {
			Logger.LogFatal("ConfigClassLoader.InitClass() - error instantiating the class [" + ((t_className != null) ? t_className : "NA") + "]: ", t_error);

			return null;
		}
	}

}
