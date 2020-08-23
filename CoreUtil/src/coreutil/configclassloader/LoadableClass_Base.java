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
