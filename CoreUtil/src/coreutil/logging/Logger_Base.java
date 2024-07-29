package coreutil.logging;

import coreutil.config.*;
import coreutil.logging.Logger.*;


abstract public class Logger_Base {

	// Data Members
	protected	int					m_maxLoggingLevel 		= Logger.MESSAGE_LEVEL_WARNING;	// This can be set separately for each logger instance that is added so that each one can decide how much information it is going to log.
	protected	String				m_configSectionName		= null;
	protected	boolean				m_checkMaxLogLevelTime	= false;

	protected	volatile boolean	m_shutdown				= false;


	//*********************************
	protected void SetFlagToCheckMaxLoggingLevel() {
		m_checkMaxLogLevelTime = true;
	}


	/*********************************
	 * This lets us update the max logging level in the configuration settings and every so
	 * often update the value dynamically after the config has be reloaded/changed in the
	 * app's memory.  I don't want to be hitting a full ConfigManager get() every time we
	 * log a message because that can be very expensive when we've got a lot of logging going
	 * on.
	 * @return
	 */
	public int GetMaxLoggingLevel() {
		if (m_checkMaxLogLevelTime) {
			int t_oldMaxLoggingLevel = m_maxLoggingLevel;
			try {
				if (m_configSectionName != null) {
					String t_fileMaxLoggingLevel = ConfigManager.GetStringValue("logging." + m_configSectionName + ".maxLoggingLevel");
					if (t_fileMaxLoggingLevel != null)
						m_maxLoggingLevel = Integer.parseInt(t_fileMaxLoggingLevel);
				}
			}
			catch (Throwable t_error) {
				m_maxLoggingLevel = t_oldMaxLoggingLevel;	// Just in case...
			}

			m_checkMaxLogLevelTime = false;
		}

		return m_maxLoggingLevel;
	}


	//*********************************
	abstract public void LogMessage(MessageInfo p_message);


	//*********************************
	abstract public void InternalShutdown();


	//*********************************
	public void SetMaxLoggingLevel(int p_newLevel) {
		m_maxLoggingLevel = p_newLevel;
	}

}
