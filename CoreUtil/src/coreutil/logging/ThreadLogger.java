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


import java.util.*;

import coreutil.config.*;
import coreutil.logging.Logger.*;




/*
 * This is a special-use logger.  When you want to capture the output of a single thread, an instance of this logger
 * can be added to the Logger.  When the thread reaches a point where it's done with the special logging, it can get
 * the buffer of log messages from the instance and then can write it to file, for example.
 */
public class ThreadLogger extends Logger_Base {

	/*
	 * There are special cases where a child thread is created by a parent thread and we need to be able to add
	 * the child's logging to the parent's thread log.
	 *
	 * Handling child thread logging required a completely different way of handling the life cycle of the ThreadLogger class.
	 * I changed it so that these static functions handle everything so that it would be much simpler for the end user to deal with.
	 */
	static private TreeMap<Long, ThreadLogger>		s_threadLoggerMap	= new TreeMap<Long, ThreadLogger>();


	//===========================================
	static public synchronized ThreadLogger AddThreadLogger(long p_threadID) {
		ThreadLogger t_newThreadLogger = new ThreadLogger(p_threadID);
		s_threadLoggerMap.put(p_threadID, t_newThreadLogger);
		Logger.AddLoggerInstance(t_newThreadLogger);

		return t_newThreadLogger;
	}

	//===========================================
	static public synchronized void RemoveThreadLogger(long p_threadID) {
		ThreadLogger t_threadLogger = s_threadLoggerMap.remove(p_threadID);
		if (t_threadLogger != null)
			Logger.RemoveLoggerInstance(t_threadLogger);
	}

	//===========================================
	static public synchronized void AddChildThreadIDToParent(long p_parentThreadID, long p_childThreadID) {
		ThreadLogger t_parentThreadLogger = s_threadLoggerMap.get(p_parentThreadID);
		if (t_parentThreadLogger != null)
			t_parentThreadLogger.AddChildThreadID(p_childThreadID);
	}

	//===========================================
	static public synchronized void RemoveChildThreadIDFromParent(long p_parentThreadID, long p_childThreadID) {
		ThreadLogger t_parentThreadLogger = s_threadLoggerMap.get(p_parentThreadID);
		if (t_parentThreadLogger != null)
			t_parentThreadLogger.RemoveChildThreadID(p_childThreadID);
	}



	// Data Members
	private	StringBuilder			m_messageBuffer		= new StringBuilder();
	private	long					m_threadID;
	private	TreeMap<Long, Long>		m_childThreadMap	= new TreeMap<Long, Long>();	// There are special cases such as SQLQueue where a child thread is created by a parent thread and we need to be able to add the child's logging to the parent's thread log.




	//*********************************
	protected ThreadLogger(long p_threadID) {
		m_configSectionName = "threadLogger";
		m_threadID			= p_threadID;

		String t_fileMaxLoggingLevel = ConfigManager.GetStringValue("logging." + m_configSectionName + ".maxLoggingLevel");
		if (t_fileMaxLoggingLevel != null)
			SetMaxLoggingLevel(Integer.parseInt(t_fileMaxLoggingLevel));
	}


	//*********************************
	protected void AddChildThreadID(long p_childThreadID) {
		m_childThreadMap.put(p_childThreadID, p_childThreadID);
	}


	//*********************************
	protected void RemoveChildThreadID(long p_childThreadID) {
		m_childThreadMap.remove(p_childThreadID);
	}


	//*********************************
	@Override
	public void InternalShutdown() {
		m_shutdown = true;
	}


	//*********************************
	@Override
	public void LogMessage(MessageInfo p_message) {
		// Log the message if it comes from the parent thread or the child thread.
		if (((p_message.m_threadID == m_threadID) ||
			  m_childThreadMap.containsKey(p_message.m_threadID)) &&
			(p_message.m_typeID <= GetMaxLoggingLevel()))
		{
			m_messageBuffer.append(p_message.m_typeString + " " + p_message.m_timeString + " | " + p_message.m_threadID + " | " + p_message.m_message + "\n");
		}
	}


	//*********************************
	public StringBuilder GetLogMessages() {
		return m_messageBuffer;
	}
}
