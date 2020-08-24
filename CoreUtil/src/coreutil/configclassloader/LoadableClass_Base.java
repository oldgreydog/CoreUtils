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
	public boolean SetParameter(String p_parameterName, String p_value);


	//*********************************
	/**
	 * It's possible for an options block to have a subtree of nodes as well as the normal values, so this lets those special cases be handled.
	 * @param p_configNode
	 * @return
	 */
	public default boolean SetParameter(ConfigNode p_configNode) {
		return false;
	}
}
