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



import coreutil.config.*;



public interface LoadableClass_Base {


	//*********************************
	/**
	 * As the config for the loadable classes is parsed, the loader passes each value found under "Options" to the new class instance through this function.
	 * That makes if possible for a generic loader to pass any number and type of parameters from the config file into the object.
	 * @param p_parameterName
	 * @param p_value
	 * @return
	 */
	public boolean SetParameter(String p_parameterName, String p_value);


	//*********************************
	/**
	 * It's possible for an options block to have a subtree of nodes as well as normal single values, so this lets those special cases be handled.
	 * @param p_configNode
	 * @return
	 */
	public default boolean SetParameter(ConfigNode p_configNode) {
		return false;
	}
}
