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


import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import coreutil.config.*;


/** This logging mechanism allows you to add any number of Logger subclasses to the
 * static instance so that you can get exactly the levels and types of logging you
 * desire.  Each logger instance can have its max logging level set individually.
 * <p>
 * <p>The logger also uses a producer/consumer queue with a separate processing thread
 * to disconnect the application code from the logger instances.  This lets the application
 * code run as fast as possible even if you add a really slow logger such as a
 * database logger.  I hope that in the future the database logging will be removed
 * since it has the potential of never being able to catch up to a server application
 * that is under high constant utilization.
 * <p>
 * <p>In fact, any such subclass that has the potential to be orders of magnitude slower than
 * other loggers should itself institute an internal queue and/or thread mechanism that
 * reduces the speed difference as much as possible.  It could even duplicate the
 * parent queue/thread mechanism so that it doesn't prevent any other logger instances
 * from processing as fast as possible.
 */
public abstract class Logger {

	static 	protected 	final	int		MESSAGE_LEVEL_STOP_THREAD	= -1;
	static 	public 		final	int		MESSAGE_LEVEL_FATAL			= 0;
	static 	public 		final	int		MESSAGE_LEVEL_EXCEPTION		= 5;
	static 	public 		final	int		MESSAGE_LEVEL_ERROR			= 10;
	static 	public 		final	int		MESSAGE_LEVEL_WARNING		= 20;
	static 	public 		final	int		MESSAGE_LEVEL_INFO			= 30;
	static 	public 		final	int		MESSAGE_LEVEL_DEBUG			= 40;
	static 	public 		final	int		MESSAGE_LEVEL_VERBOSE		= 50;

	static 	protected 	final	String	MESSAGE_STRING_FATAL		= "[FATAL]";
	static 	protected 	final	String	MESSAGE_STRING_EXCEPTION	= "[EXCEP]";
	static 	protected 	final	String	MESSAGE_STRING_ERROR		= "[ERROR]";
	static 	protected 	final	String	MESSAGE_STRING_WARNING		= "[WARN ]";
	static 	protected 	final	String	MESSAGE_STRING_INFO			= "[INFO ]";
	static 	protected 	final	String	MESSAGE_STRING_DEBUG		= "[DEBUG]";
	static 	protected 	final	String	MESSAGE_STRING_VERBOSE		= "[VERBO]";

	static 	public 		final	int		LOG_LEVEL_CHECK_INTERVAL_SECONDS	= 60;



	// I didn't like having to have every logger check the system time on EVERY log message (written or not!) nor do I
	// want them to check the config info every time since that's pretty expensive, too, so I
	// set up this thread to wake up every so often to set a flag on all of the loggers to tell them that the next
	// time they get a log message to check the max logging level to see if it has changed.
	static protected class CheckMaxLoggingLevelThread extends Thread {


		//*********************************
		@Override
		public void run() {
			setName("Check max logging level thread");		// Set the thread name so that it is identifiable in the monitor and in error messages.

			while (true) {
				try {
					Thread.sleep(LOG_LEVEL_CHECK_INTERVAL_SECONDS * 1000);	// Sleep until the next check time.
				}
				catch (InterruptedException t_interupted) {
					return;
				}
				catch (Throwable t_error) {
					Logger.LogException("CheckMaxLoggingLevelThread.run() threw an exception: ", t_error);
				}

				Logger.FlagLoggersForMaxLevelCheck();
			}
		}
	}


	static protected class MessageWriterThread extends Thread {

		//*********************************
		@Override
		public void run() {
			setName("Logger message distribution thread");		// Set the thread name so that it is identifiable in the monitor and in error messages.

			try {
				MessageInfo t_nextLogMessage = null;
				while ((t_nextLogMessage = s_messageQueue.take()) != null) {
					if (t_nextLogMessage.m_message == null) {
//						System.out.println("MessageWriterThread.run() is shutting down because it received an empty message.");
						return;
					}

					t_nextLogMessage.FormatTimestamp();		// I moved this timestamp formating inside this thread so that we don't have to pay that price on the calling thread (which is doing real work).
					DistributeMessage(t_nextLogMessage);
				}
			}
			catch (InterruptedException t_interupted) {
				return;
			}
			catch (Throwable t_error) {
				System.err.println("MessageWriterThread.run() threw an exception and shut down: " + t_error.toString());
			}
		}

		//*********************************
		public void Shutdown() {
			Logger.LogDebug("MessageWriterThread.Shutdown() is attempting to shut down the thread.");
			try {
				s_messageQueue.put(new MessageInfo());
			}
			catch (Throwable t_error) {
				System.err.println("MessageWriterThread.Shutdown() threw an exception: " + t_error.toString());
			}
		}
	}


	static public class MessageInfo {
		public String			m_message		= null;		// Set this to NULL as an explicit reminder that this has to be NULL to work as a shutdown signal to the message writer thread.
		public int				m_typeID		= 0;
		public String			m_typeString;
		public ZonedDateTime	m_timeStamp;
		public long				m_threadID;

		public String			m_timeString	= null;


		//*********************************
		public MessageInfo(String p_message, int p_typeID, String p_typeString, ZonedDateTime p_timeStamp, long p_threadID) {
			m_message 		= p_message;
			m_typeID 		= p_typeID;
			m_typeString 	= p_typeString;
			m_timeStamp		= p_timeStamp;
			m_threadID		= p_threadID;
		}


		//*********************************
		/**
		 * This default constructor is only used to signal the message writer thread to shut down when it receives an empty message.
		 */
		public MessageInfo() {}


		//*********************************
		/**
		 * This should only be called by the MessageWriterThread.
		 */
		private void FormatTimestamp() {
			StringBuilder	t_time		= new StringBuilder();
			int				t_hour		= m_timeStamp.getHour();
			int				t_minute	= m_timeStamp.getMinute();
			int				t_seconds	= m_timeStamp.getSecond();

			if (t_hour < 10)
				t_time.append("0");

			t_time.append(t_hour);
			t_time.append(":");

			if (t_minute < 10)
				t_time.append("0");

			t_time.append(t_minute);
			t_time.append(":");

			if (t_seconds < 10)
				t_time.append("0");

			t_time.append(t_seconds);

			m_timeString = t_time.toString();
		}


		//*********************************
		@Override
		public String toString() {
			StringBuilder t_message = new StringBuilder();
			t_message.append(m_typeString + " : " + " " + m_timeString + " | " + m_threadID + " | ");
			if (m_message != null)
				t_message.append(m_message);
			else
				t_message.append(" <null message> ");

			return t_message.toString();
		}
	}


	static 	private 	final 	ReentrantReadWriteLock					s_loggerInstancesLock			= new ReentrantReadWriteLock();
	static 	private 	final 	Vector<Logger_Base>						s_loggerInstances				= new Vector<Logger_Base>();

	static 	private 	final 	LinkedBlockingQueue<MessageInfo>		s_messageQueue					= new LinkedBlockingQueue<>();
	static	protected	MessageWriterThread								s_messageWriterThread			= null;
	static	protected	CheckMaxLoggingLevelThread						s_maxLoggingLevelCheckThread	= null;

	static	protected	ZoneId											s_timezoneID					= null;


	static {
		// You don't even get default logging on app startup if these are in Init(), so I moved them here.
		s_messageWriterThread			= new MessageWriterThread();
		s_messageWriterThread.start();

		s_maxLoggingLevelCheckThread	= new CheckMaxLoggingLevelThread();
		s_maxLoggingLevelCheckThread.start();
	}


	//===========================================
	static public boolean Init() {
		if (s_timezoneID != null) {
			Logger.LogError("Logger.Init() found an existing timezone ID.  The Logger has already been initialized.");
			return false;
		}

		String t_timezoneValue = ConfigManager.GetStringValue("logging.timezone");
		if (t_timezoneValue == null) {
			Logger.LogError("Logger.Init() failed to find the [timezone] value.  The logger time zone will default to the local time zone.");
			s_timezoneID = ZonedDateTime.now().getZone();
		}
		else if (t_timezoneValue.equalsIgnoreCase("local"))
			s_timezoneID = ZonedDateTime.now().getZone();
		else
			s_timezoneID = ZoneId.of("UTC");


		ConfigNode t_logTarget = ConfigManager.GetNode("logging.logTargets");
		if (t_logTarget == null) {
			Logger.LogWarning("Logger.Init() failed to initialize because there is no [logging.logTargets] node in the XML configuration file.");
			return true;	// This isn't necessarily fatal.  Testing code, for example, may not use the config file stuff (or uses it in a non-standard way).
		}

		String		t_targetClassName;
		Logger_Base t_newLogTarget;
		for (ConfigValue t_nextTargetName: t_logTarget.GetChildValueList()) {
			if ((t_targetClassName = t_nextTargetName.GetStringValue()) == null) {
				Logger.LogFatal("Logger.Init() failed to find a class name for a log target.");
				return false;
			}

			try {
				Class<Logger_Base> t_class = (Class<Logger_Base>) Class.forName(t_targetClassName);
				t_newLogTarget = t_class.getDeclaredConstructor().newInstance();

				s_loggerInstances.add(t_newLogTarget);
			}
			catch (Throwable t_error) {
				Logger.LogFatal("Logger.Init() failed to instantiate a log target for class [" + t_targetClassName + "] : ", t_error);
				return false;
			}
		}

		return true;
	}


	//===========================================
	static public void AddLoggerInstance(Logger_Base p_newLogger) {
		try {
			s_loggerInstancesLock.writeLock().lock();
			s_loggerInstances.add(p_newLogger);
		}
		finally {
			s_loggerInstancesLock.writeLock().unlock();
		}
	}


	//===========================================
	static public void RemoveLoggerInstance(Logger_Base p_targetLogger) {
		try {
			s_loggerInstancesLock.writeLock().lock();
			s_loggerInstances.remove(p_targetLogger);
		}
		finally {
			s_loggerInstancesLock.writeLock().unlock();
		}
	}


	//===========================================
	/**
	 * Now that things are set up to allow the configuration of log times in zulu or the local time zone, I needed a way to get event times
	 * for other things in the system that should also follow that same rule without setting up random config values everywhere so I
	 * added this function to handle that.
	 *
	 * @return
	 */
	static public ZonedDateTime GetEventTime() {
		return ZonedDateTime.now(s_timezoneID);
	}


	//===========================================
	static public void Shutdown() {
		Logger.LogDebug("Logger.Shutdown() is starting.");


		try {
			if (s_maxLoggingLevelCheckThread != null) {
				s_maxLoggingLevelCheckThread.interrupt();
				s_maxLoggingLevelCheckThread.join();
				s_maxLoggingLevelCheckThread = null;
			}

			if (s_messageWriterThread != null) {
				s_messageWriterThread.Shutdown();
				s_messageWriterThread.join();	// This join can't be performed inside the lock below because the writer thread may still have messages that it's trying to send and therefore gets caught in a race when DistributeMessage() tries to grab a read lock while we are still in this write lock.
				s_messageWriterThread = null;
			}
		}
		catch (Throwable t_error) {
			System.out.println("Logger.Shutdown() shutdown of the helper threads failed with error: " + t_error);
		}


		try {
			s_loggerInstancesLock.writeLock().lock();

			for (Logger_Base t_nextLogger: s_loggerInstances)
				t_nextLogger.InternalShutdown();

			s_loggerInstances.clear();
		}
		catch (Throwable t_error) {
			System.out.println("Logger.Shutdown() shutdown of the logger instances failed with error: " + t_error);
		}
		finally {
			s_loggerInstancesLock.writeLock().unlock();
		}


//		System.out.println("Logger.Shutdown() completed.");
	}


	//===========================================
	static public void LogFatal(String p_message) {
		try {
			long t_threadID = Thread.currentThread().threadId();
			s_messageQueue.put(new MessageInfo(p_message, MESSAGE_LEVEL_FATAL, MESSAGE_STRING_FATAL, ZonedDateTime.now(s_timezoneID), t_threadID));
		}
		catch (Throwable t_dontCare) {
			System.out.println("Logger.LogFatal() failed with error : " + t_dontCare);
		}
	}


	//===========================================
	static public void LogFatal(String p_message, Throwable p_exception) {
		try {
			// Get the stack trace from the exception
			Writer t_errorStack = new StringWriter();
			if (p_exception != null)
			{
				PrintWriter t_printWriter = new PrintWriter(t_errorStack);
				p_exception.printStackTrace(t_printWriter);
			}

			long	t_threadID	= Thread.currentThread().threadId();
			String	t_message	= p_message + "\n" + t_errorStack;

			s_messageQueue.put(new MessageInfo(t_message, MESSAGE_LEVEL_FATAL, MESSAGE_STRING_FATAL, ZonedDateTime.now(s_timezoneID), t_threadID));
		}
		catch (Throwable t_dontCare) {}
	}


	//===========================================
	/**
	 *
	 * @param p_message
	 * @param p_exception I changed this from "Exception" to "Throwable" so that it works with both "Exception" and "Error" subclasses.
	 */
	static public void LogException(String p_message, Throwable p_exception) {
		try {
			// Get the stack trace from the exception
			Writer t_errorStack = new StringWriter();
			if (p_exception != null)
			{
				PrintWriter t_printWriter = new PrintWriter(t_errorStack);
				p_exception.printStackTrace(t_printWriter);
			}

			long	t_threadID	= Thread.currentThread().threadId();
			String	t_message	= p_message + "\n" + t_errorStack;

			s_messageQueue.put(new MessageInfo(t_message, MESSAGE_LEVEL_EXCEPTION, MESSAGE_STRING_EXCEPTION, ZonedDateTime.now(s_timezoneID), t_threadID));
		}
		catch (Throwable t_dontCare) {}
	}


	//===========================================
	static public void LogError(String p_message) {
		try {
			long t_threadID = Thread.currentThread().threadId();
			s_messageQueue.put(new MessageInfo(p_message, MESSAGE_LEVEL_ERROR, MESSAGE_STRING_ERROR, ZonedDateTime.now(s_timezoneID), t_threadID));
		}
		catch (Throwable t_dontCare) {}
	}


	//===========================================
	static public void LogWarning(String p_message) {
		try {
			long t_threadID = Thread.currentThread().threadId();
			s_messageQueue.put(new MessageInfo(p_message, MESSAGE_LEVEL_WARNING, MESSAGE_STRING_WARNING, ZonedDateTime.now(s_timezoneID), t_threadID));
		}
		catch (Throwable t_dontCare) {}
	}


	//===========================================
	static public void LogInfo(String p_message) {
		try {
			long t_threadID = Thread.currentThread().threadId();
			s_messageQueue.put(new MessageInfo(p_message, MESSAGE_LEVEL_INFO, MESSAGE_STRING_INFO, ZonedDateTime.now(s_timezoneID), t_threadID));
		}
		catch (Throwable t_dontCare) {}
	}


	//===========================================
	static public void LogDebug(String p_message) {
		try {
			long t_threadID = Thread.currentThread().threadId();
			s_messageQueue.put(new MessageInfo(p_message, MESSAGE_LEVEL_DEBUG, MESSAGE_STRING_DEBUG, ZonedDateTime.now(s_timezoneID), t_threadID));
		}
		catch (Throwable t_dontCare) {}
	}


	//===========================================
	static public void LogVerbose(String p_message) {
		try {
			long t_threadID = Thread.currentThread().threadId();
			s_messageQueue.put(new MessageInfo(p_message, MESSAGE_LEVEL_VERBOSE, MESSAGE_STRING_VERBOSE, ZonedDateTime.now(s_timezoneID), t_threadID));
		}
		catch (Throwable t_dontCare) {}
	}


	//===========================================
	static public void DefaultLogger(MessageInfo p_message) {
		System.out.println(p_message.m_typeString + " " + p_message.m_timeString + " | " + p_message.m_threadID + " | " + p_message.m_message);
	}


	//===========================================
	static protected void DistributeMessage(MessageInfo p_message) {
		try {
			s_loggerInstancesLock.readLock().lock();

			if (s_loggerInstances.isEmpty()) {
				DefaultLogger(p_message);

				return;
			}

			for (Logger_Base t_nextLogger: s_loggerInstances)
				t_nextLogger.LogMessage(p_message);
		}
		catch (Throwable t_error) {
			System.out.println("Logger.DistributeMessage() failed with error : " + t_error);
		}
		finally {
			s_loggerInstancesLock.readLock().unlock();
		}

	}


	/*********************************
	 * This lets us update the max logging level in the configuration settings and every so
	 * often update the value dynamically after the config has be reloaded/changed in the
	 * app's memory.  I don't want to be hitting a full ConfigManager get() every time we
	 * log a message because that can be very expensive when we've got a lot of logging going
	 * on.
	 */
	static protected void FlagLoggersForMaxLevelCheck() {
		try {
			s_loggerInstancesLock.writeLock().lock();

			for (Logger_Base t_nextLogger: s_loggerInstances)
				t_nextLogger.SetFlagToCheckMaxLoggingLevel();
		}
		finally {
			s_loggerInstancesLock.writeLock().unlock();
		}
	}
}
