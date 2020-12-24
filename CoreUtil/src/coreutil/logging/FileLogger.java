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

import java.io.*;
import java.util.*;


public class FileLogger extends Logger {

	//===========================================
	static class FlusherThread extends Thread {

		private FileLogger	m_logger	= null;

		public FlusherThread(FileLogger p_logger) {
			m_logger = p_logger;

			setName("FileLogger flusher thread");	// Set the thread name so that it is identifiable in the monitor and in error messages.
		}

		@Override
		public void run() {
			try {
				while (true) {
					Thread.sleep(60000);	// Sleep for a minute.
					m_logger.Flush();

					// We need to check to see if we were interrupt()ed during the flush before we go back to sleep.
					if (isInterrupted()) {
						Logger.LogInfo("FlusherThread.run() has shut down.");
						return;
					}
				}
			}
			catch (InterruptedException t_interrupted) {
				// You can't log during the shutdown because it causes a race condition.
				//Logger.LogInfo("FlusherThread.run() has shut down.");
			}
			catch (Throwable t_error) {
				Logger.LogException("FlusherThread.run() failed with error: ", t_error);
			}
		}
	}



	// Data Members
	private FileWriter 		m_logWriter			= null;
	private Calendar 		m_tomorrowsDate		= null;
	private FlusherThread	m_flusherThread		= null;




	//*********************************
	public FileLogger() {
		m_configSectionName = "filelogger";

		String t_fileMaxLoggingLevel = ConfigManager.GetValue("logging." + m_configSectionName + ".maxLoggingLevel");
		if (t_fileMaxLoggingLevel != null)
			SetMaxLoggingLevel(Integer.parseInt(t_fileMaxLoggingLevel));

		SetOutputFile();

		m_flusherThread = new FlusherThread(this);
		m_flusherThread.start();
	}


	//*********************************
	protected boolean SetOutputFile()
	{
		// If a writer already exists, close it first.
		if (m_logWriter != null) {
			try {
				m_logWriter.flush();
				m_logWriter.close();
			}
			catch (Throwable t_dontCare) {
			}
			finally {
				m_logWriter = null;
			}
		}

		// Now create the new output file.
		try {
			Calendar t_today 	= Calendar.getInstance();
			int t_month 		= t_today.get(Calendar.MONTH) + 1;
			int t_day 			= t_today.get(Calendar.DAY_OF_MONTH);
			String t_dateString = Integer.toString(t_today.get(Calendar.YEAR)) + "_" + ((t_month < 10) ? "0" + t_month : t_month) + "_" + ((t_day < 10) ? "0" + t_day : t_day);

			String t_pathOnly = ConfigManager.GetValue("logging." + m_configSectionName + ".logDirectory");
			if (t_pathOnly == null) {
				throw new RuntimeException("FileLogger.SetOutputFile() failed : [logging." + m_configSectionName + ".logDirectory] is not set in the configuration info.");
			}

			File t_newPath = new File(t_pathOnly);

			if(!t_newPath.exists() && !t_newPath.mkdirs()) {
				throw new RuntimeException("FileLogger.SetOutputFile() failed to create the [" + t_pathOnly + "] log directory.");
			}
			else if (!t_newPath.isDirectory()) {
				throw new RuntimeException("FileLogger.SetOutputFile() failed : [" + t_pathOnly + "] is not a valid path");
			}

			String p_filenamePrefix = ConfigManager.GetValue("logging." + m_configSectionName + ".appFileNamePrefix");
			if (p_filenamePrefix == null)
				p_filenamePrefix = "";

			File t_newFile = new File(t_pathOnly + "/" + p_filenamePrefix + "_" + t_dateString + ".log");
			m_logWriter = new FileWriter(t_newFile, true);	// If the file already exists because, for example, this app has just been restarted, then this constructor will open it in append mode since the append parameter == TRUE.

			// We'll figure out the next day's date (and set it to 12:00 AM) so that we can easily check for rollover to change the log file.
			m_tomorrowsDate = (Calendar)t_today.clone();
			m_tomorrowsDate.add(Calendar.DATE, 1);
			m_tomorrowsDate.set(Calendar.HOUR, 0);
			m_tomorrowsDate.set(Calendar.HOUR_OF_DAY, 0);
			m_tomorrowsDate.set(Calendar.MINUTE, 0);
			m_tomorrowsDate.set(Calendar.SECOND, 0);
			m_tomorrowsDate.set(Calendar.MILLISECOND, 0);

			return true;
		}
		catch (IOException t_error) {
			System.out.println("FileLogger.SetOutputFile() failed with error: " + t_error);

			// If we failed, then the writer, if it exists, is probably corrupt so we need to dump any reference to it.
			if (m_logWriter != null) {
				try {
					m_logWriter.flush();
					m_logWriter.close();
				}
				catch (Throwable t_dontCare) {
				}
				finally {
					m_logWriter = null;
				}
			}

			return false;
		}
	}


	//*********************************
	@Override
	protected synchronized void InternalShutdown() {
		m_shutdown = true;

		// Kill the thread first.
		try {
			if (m_flusherThread != null) {
				m_flusherThread.interrupt();
				m_flusherThread.join();
			}
		}
		catch (Throwable t_ignore) {
		}

		// Then close the file.
		try {
			if (m_logWriter != null) {
				m_logWriter.flush();
				m_logWriter.close();
				m_logWriter = null;
			}
		}
		catch (Throwable t_ignore) {
		}
	}


	//*********************************
	@Override
	protected void LogMessage(MessageInfo p_message) {
		try {
			if (p_message.m_typeID <= GetMaxLoggingLevel()) {
				// If we're writing to a log file and the date changes, we need to close the current log file and open a new one.
				if ((m_logWriter != null) &&
					(m_tomorrowsDate.compareTo(Calendar.getInstance()) <= 0))
				{
					if (!SetOutputFile())
						return;
				}

				// Since SetOutputFile() could fail (theoretically), I'll just keep this second check for a valid s_logWriter.
				if (m_logWriter != null)
					m_logWriter.write(p_message.m_typeString + " " + p_message.m_timeString + " | " + p_message.m_threadID + " | " + p_message.m_message + "\n");
			}
		}
		catch (IOException t_error) {
			System.out.println("FileLogger.WriteMessage() failed with error: " + t_error);

			// If we get here, the high probability problem was that the log file got deleted or zipped or something while this app was running therefore corrupting the file handle, so we need to recreate the log file to hopefully fix the problem.
			SetOutputFile();

			return;
		}
	}


	//*********************************
	protected synchronized void Flush() {
		try {
			if (m_logWriter != null)
				m_logWriter.flush();
		}
		catch (IOException t_error) {
			System.out.println("FileLogger.Flush() failed with error: " + t_error);
			return;
		}
	}
}
