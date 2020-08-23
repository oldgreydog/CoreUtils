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



import coreutil.logging.*;


/**
 * This helper class will take in a string with tagged substitution values and will respectively replace them with config values or metric values.<p>
 *
 * The config value substitutions are open/close tagged with double percent signs (%%).<p>
 *
 * For example:<p>
 *
 * site_%%siteInfo.siteCustomerID%%<br>
 *
 * @author wkaylor
 *
 */
public class ConfigValueSubstituter {


	static public final String	SUBSTITUTION_DELIMITER		= "%%";


	//===========================================
	/**
	 *
	 * @param p_sourceString
	 * @return
	 */
	public String ReplaceValuesInString(String p_sourceString) {
		try {
			String			t_sourceString	= p_sourceString;
			StringBuilder	t_result		= new StringBuilder();
			int				t_newIndex		= 0;
			int				t_oldIndex		= 0;
			String			t_valueName;
			String			t_value;
			while ((t_newIndex = t_sourceString.indexOf("%%", t_newIndex)) >= 0) {
				t_result.append(t_sourceString.substring(t_oldIndex, t_newIndex));
				t_oldIndex = t_newIndex + 2;

				t_valueName = t_sourceString.substring(t_oldIndex, (t_newIndex = t_sourceString.indexOf(SUBSTITUTION_DELIMITER, t_oldIndex)));
				t_oldIndex = t_newIndex + 2;

				t_value = ConfigManager.GetValue(t_valueName);
				if (t_value == null) {
					Logger.LogError("ConfigValueSubstituter.ReplaceValuesInString() failed to find the config value [" + t_valueName + "] to substitue into the string [" + p_sourceString + "].");
	//				return null;
					t_result.append(SUBSTITUTION_DELIMITER + t_valueName + SUBSTITUTION_DELIMITER);		// We'll put the value back in the way we found it just in case this wasn't supposed to be replaced.
				}
				else
					t_result.append(t_value);

				t_newIndex += 2;
			}

			if (t_oldIndex >= 0)
				t_result.append(t_sourceString.substring(t_oldIndex));


			return t_result.toString();
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigValueSubstituter.ReplaceValuesInString() failed with error: ", t_error);
			return null;
		}
	}
}
