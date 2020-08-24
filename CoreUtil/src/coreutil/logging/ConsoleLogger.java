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


public class ConsoleLogger extends Logger {

	// Data Members


	//*********************************
	public ConsoleLogger() {
		m_configSectionName = "ConsoleLogger";

		String t_fileMaxLoggingLevel = ConfigManager.GetValue("logging." + m_configSectionName + ".maxLoggingLevel");
		if (t_fileMaxLoggingLevel != null)
			SetMaxLoggingLevel(Integer.parseInt(t_fileMaxLoggingLevel));
	}


	//*********************************
	@Override
	protected void InternalShutdown() {
		m_shutdown = true;
		System.out.flush();
		System.err.flush();		// just in case other threads might have put something on the err output...
	}


	//*********************************
	@Override
	protected void LogMessage(MessageInfo p_message) {
		if (p_message.m_typeID <= GetMaxLoggingLevel()) {
			System.out.println(p_message.m_typeString + " " + p_message.m_timeString 	+ " | " + p_message.m_threadID + " | " + p_message.m_message);
		}
	}
}
