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


import java.util.concurrent.*;


public abstract class QueuingLogger extends Logger {

	class MessageQueueRunnable implements Runnable {
		protected	QueuingLogger	m_logger;

		public MessageQueueRunnable(QueuingLogger p_logger) {
			m_logger = p_logger;
		}


		@Override
		public void run() {
			Logger.MessageInfo t_message = null;
			int t_errorCount = 0;
			while (true) {
				try {
					t_message = m_messageQueue.take();	// This blocks if the queue is empty.

					if (t_message.m_typeID == MESSAGE_LEVEL_STOP_THREAD)
						return;

					while (!LogQueuedMessage(t_message)) {
						if (m_shutdown) {
							m_messageQueue.clear();
							return;
						}

						Logger.LogError("QueuingLogger.run() failed to send message and it will be skipped: " + t_message.toString());
						Thread.sleep(1000);	// We probably had a comm. failure, so lets just sleep and retry.
					}
				}
				catch (Throwable t_error) {
					System.out.println("The QueuingLogger's message processing thread failed with exception : " + t_error);
					System.out.println("The message that failed : " + t_message.toString());
					if (++t_errorCount > 10)
						return;	// We don't want to get caught in an error loop.  If we get ten errors, it's time to quit.
				}
			}
		}
	}



	protected 	final 	LinkedBlockingQueue<MessageInfo>	m_messageQueue 		= new LinkedBlockingQueue<MessageInfo>();
	protected 	Thread										m_messageProcessingThread;


	//*********************************
	public QueuingLogger() {
		m_messageProcessingThread = new Thread(new MessageQueueRunnable(this));
		m_messageProcessingThread.start();
	}


	//*********************************
	@Override
	protected final void InternalShutdown() {
		try {
			m_shutdown = true;

			m_messageQueue.add(new MessageInfo("", MESSAGE_LEVEL_STOP_THREAD, "", 0));
			//System.out.println("QueuingLogger.InternalShutdown() added shutdown message to the queue for logger [" + m_configSectionName + "].");
			m_messageProcessingThread.join();
			//System.out.println("QueuingLogger.InternalShutdown() completed 'join' with queuing thread for logger [" + m_configSectionName + "].");
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
