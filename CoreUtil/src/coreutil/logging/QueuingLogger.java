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


import java.time.*;
import java.util.concurrent.*;



/*
 * There may come a time where you are forced to do something stupid like log messages to a database (which is why
 * this class exists).  When you have to log to a target that may have a transaction time many orders of magnitude
 * slower than the console or a file buffer, then you can subclass this logger to decouple the slow logger from the main
 * log thread.  I would also suggest that you never set the log level for this type of logger higher than, say, ERROR to
 * minimize how many messages have to be sent through it.
 */
public abstract class QueuingLogger extends Logger {

	class MessageQueueThread extends Thread {
		protected	QueuingLogger	m_logger;

		public MessageQueueThread(QueuingLogger p_logger) {
			m_logger = p_logger;
		}


		@Override
		public void run() {
			Logger.MessageInfo t_message = null;
			int t_errorCount = 0;
			while (true) {
				try {
					if (interrupted()) {
						System.out.println("QueuingLogger's message processing thread was interrupted and will shut down.");
						return;
					}

					t_message = m_messageQueue.take();	// This blocks if the queue is empty.

					if (t_message.m_typeID == MESSAGE_LEVEL_STOP_THREAD)
						return;

					while (!LogQueuedMessage(t_message)) {
						// This only short-circuits the shutdown (dumps the queue) if the attempt to log the message fails.  This could happen, for example, if this is logging to a database and the app shuts down the db connections before it shuts down logging.  If that happens, you were going to lose all of those log messages anyway.
						if (m_shutdown) {
							m_messageQueue.clear();
							return;
						}

						if (++t_errorCount > 10)
							return;	// We don't want to get caught in an error loop.  If we get ten errors, it's time to quit.

						Logger.LogError("QueuingLogger.run() failed to send message and it will be skipped: " + t_message.toString());
						Thread.sleep(1000);	// We probably had a comm. failure, so lets just sleep and retry.
					}

					t_errorCount = 0;
				}
				catch (InterruptedException t_interrupted) {
					System.out.println("QueuingLogger's message processing thread was interrupted and will shut down.");
					return;
				}
				catch (Throwable t_error) {
					System.out.println("QueuingLogger's message processing thread failed with exception : " + t_error);
					System.out.println("The message that failed : " + t_message.toString());
					if (++t_errorCount > 10)
						return;	// We don't want to get caught in an error loop.  If we get ten errors, it's time to quit.
				}
			}
		}
	}



	protected 	final 	LinkedBlockingQueue<MessageInfo>	m_messageQueue 		= new LinkedBlockingQueue<MessageInfo>();
	protected 	MessageQueueThread							m_messageProcessingThread;


	//*********************************
	public QueuingLogger() {
		m_messageProcessingThread = new MessageQueueThread(this);
		m_messageProcessingThread.start();
	}


	//*********************************
	@Override
	protected final void InternalShutdown() {
		try {
			m_shutdown = true;

			// This shutdown method of putting a "flag" message on the queue and waiting for the thread to find it and stop can block an app from shutting down if there are a ridiculous number of messages in the queue and the log target (i.e. a database) is very slow.
			// If you can't prevent the number of messages but you don't care if they disappear on shutdown, then you can call m_messageQueue.clear() to clean out the queue before putting the flag message in.
			m_messageQueue.add(new MessageInfo("", MESSAGE_LEVEL_STOP_THREAD, "", ZonedDateTime.now(s_timezoneID), 0));

			m_messageProcessingThread.join();

			QueueLoggerShutdown();
		}
		catch (Throwable t_error) {
			System.out.println("QueuingLogger.InternalShutdown() failed with error : " + t_error);
		}
	}


	//*********************************
	abstract protected void QueueLoggerShutdown();


	//*********************************
	@Override
	protected void LogMessage(MessageInfo p_message) {
		if (!m_shutdown && (p_message.m_typeID <= GetMaxLoggingLevel()))
			m_messageQueue.add(p_message);
	}


	//*********************************
	abstract protected boolean LogQueuedMessage(MessageInfo p_message);
}
