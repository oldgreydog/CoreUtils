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


package coreutil.logging;


import coreutil.config.*;
import coreutil.logging.Logger.*;


public class ConsoleLogger extends Logger_Base {

	// Data Members


	//*********************************
	public ConsoleLogger() {
		m_configSectionName = "ConsoleLogger";

		String t_fileMaxLoggingLevel = ConfigManager.GetStringValue("logging." + m_configSectionName + ".maxLoggingLevel");
		if (t_fileMaxLoggingLevel != null)
			SetMaxLoggingLevel(Integer.parseInt(t_fileMaxLoggingLevel));
	}


	//*********************************
	@Override
	public void InternalShutdown() {
		m_shutdown = true;
		System.out.flush();
		System.err.flush();		// just in case other threads might have put something on the err output...
	}


	//*********************************
	@Override
	public void LogMessage(MessageInfo p_message) {
		if (m_shutdown)
			return;

		if (p_message.m_typeID <= GetMaxLoggingLevel()) {
			System.out.println(p_message.toString());	// Changed this to use the toString() on the MessageInfo class to do the output.  In cases where two or more loggers (i.e. File and Console) use the same output format, then we can take advantage of caching of the default output string that toString() now does.
		}
	}
}
