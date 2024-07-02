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


package coreutil.misc;



import coreutil.logging.*;

import java.time.*;
import java.time.temporal.*;
import java.util.*;



public class StringUtilities {

	static public final	int	DATE_ORDER_AMERICAN	= 0;	// MMDDYYYY
	static public final	int	DATE_ORDER_BRITISH	= 1;	// DDMMYYYY
	static public final	int	DATE_ORDER_NUMERIC	= 2;	// YYYYMMDD

	//*********************************
	static public String[] SplitLineFixColumnCount(String p_line, String p_delimiter, int p_expectedFieldCount) {
		String	t_values[]				= new String[p_expectedFieldCount];
		String	t_newValue;
		int		t_valueIndex			= 0;
		int		t_lastDelimiterIndex	= -1;
		int		t_nextDelimiterIndex	= p_line.indexOf(p_delimiter);

		while (t_nextDelimiterIndex >= 0) {
			if (t_valueIndex >= p_expectedFieldCount) {
				Logger.LogError("StringUtilities.SplitLineFixColumnCount() failed with error: the expected column count is less than the actual number of columns in the file data.");
				return null;
			}

			t_values[t_valueIndex++]	= p_line.substring(t_lastDelimiterIndex + 1, t_nextDelimiterIndex).trim();
			t_lastDelimiterIndex		= t_nextDelimiterIndex;
			t_nextDelimiterIndex		= p_line.indexOf(p_delimiter, t_lastDelimiterIndex + 1);
		}

		// Get the last value in the line.
		if (t_lastDelimiterIndex > 0) {
			if (t_lastDelimiterIndex == (p_line.length() - 1)) {
				t_values[t_valueIndex] = null;	// The last field is empty, so we'll just set this value as an empty string so that everything works.
			}
			else {
				t_newValue = p_line.substring(t_lastDelimiterIndex + 1).trim();

				if (t_newValue.length() > 0)
					t_values[t_valueIndex] = t_newValue;
				else
					t_values[t_valueIndex] = null;
			}
		}

		return t_values;
	}


	//*********************************
	static public Vector<String> SplitLine(String p_line, String p_delimiter) {
		Vector<String>	t_values				= new Vector<String>();
		String			t_newValue;
		int				t_valueIndex			= 0;
		int				t_lastDelimiterIndex	= -1;
		int				t_nextDelimiterIndex	= p_line.indexOf(p_delimiter);

		while (t_nextDelimiterIndex >= 0) {
			t_values.add(t_valueIndex++, p_line.substring(t_lastDelimiterIndex + 1, t_nextDelimiterIndex).trim());
			t_lastDelimiterIndex = t_nextDelimiterIndex;
			t_nextDelimiterIndex = p_line.indexOf(p_delimiter, t_lastDelimiterIndex + 1);
		}

		// Get the last value in the line.
		if (t_lastDelimiterIndex > 0) {
			if (t_lastDelimiterIndex == (p_line.length() - 1)) {
				t_values.add(t_valueIndex, null);	// The last field is empty, so we'll just set this value as an empty string so that everything works.
			}
			else {
				t_newValue = p_line.substring(t_lastDelimiterIndex + 1).trim();

				if (t_newValue.length() > 0)
					t_values.add(t_valueIndex, t_newValue);
				else
					t_values.add(t_valueIndex, null);
			}
		}

		return t_values;
	}


	//*********************************
	static public String EscapeSingleQuotesForDB(String p_string) {
		return p_string.replaceAll("'", "''");
	}


	//*********************************
	/**
	 * This is a wrapper to convert calls from existing code to the new FormatData() that takes the p_dateOrderType parameter.
	 * All of the existing code uses the YYYYMMDD ordering, so all we have to do here is default to DATE_ORDER_NUMERIC.
	 */
	static public String FormatDate(LocalDate p_date, String p_separator) {
		return FormatDate(p_date, p_separator, DATE_ORDER_NUMERIC);
	}


	//*********************************
	static public String FormatDate(LocalDate p_date) {
		return FormatDate(p_date, "-", DATE_ORDER_NUMERIC);
	}


	//*********************************
	/**
	 * This is a wrapper to convert calls from existing code to the new FormatData() that takes the p_dateOrderType parameter.
	 * All of the existing code uses the YYYYMMDD ordering, so all we have to do here is default to DATE_ORDER_NUMERIC.
	 */
	static public String FormatDate(LocalDateTime p_dateTime, String p_separator) {
		return FormatDate(p_dateTime.toLocalDate(), p_separator, DATE_ORDER_NUMERIC);
	}


	//*********************************
	static public String FormatDate(LocalDateTime p_dateTime) {
		return FormatDate(p_dateTime.toLocalDate(), "-", DATE_ORDER_NUMERIC);
	}


	//*********************************
	/**
	 * This is a wrapper to convert calls from existing code to the new FormatData() that takes the p_dateOrderType parameter.
	 * All of the existing code uses the YYYYMMDD ordering, so all we have to do here is default to DATE_ORDER_NUMERIC.
	 */
	static public String FormatDate(ZonedDateTime p_zonedDateTime, String p_separator) {
		return FormatDate(p_zonedDateTime.toLocalDate(), p_separator, DATE_ORDER_NUMERIC);
	}


	//*********************************
	static public String FormatDate(ZonedDateTime p_zonedDateTime) {
		return FormatDate(p_zonedDateTime.toLocalDate(), "-", DATE_ORDER_NUMERIC);
	}


	//*********************************
	static public String FormatDate(LocalDate p_date, String p_separator, int p_dateOrderType) {
		StringBuilder	t_todaysDate	= new StringBuilder();
		boolean			t_addMonthZero	= false;
		boolean			t_addDayZero	= false;

		int t_month = p_date.getMonthValue();
		if (t_month < 10)
			t_addMonthZero = true;

		int t_day = p_date.getDayOfMonth();
		if (t_day < 10)
			t_addDayZero = true;

		switch (p_dateOrderType) {
			case DATE_ORDER_AMERICAN:
				if (t_addMonthZero)
					t_todaysDate.append("0");

				t_todaysDate.append(t_month);

				t_todaysDate.append(p_separator);

				if (t_addDayZero)
					t_todaysDate.append("0");

				t_todaysDate.append(t_day);

				t_todaysDate.append(p_separator);

				t_todaysDate.append(Integer.toString(p_date.getYear()));
				break;
			case DATE_ORDER_BRITISH:
				if (t_addDayZero)
					t_todaysDate.append("0");

				t_todaysDate.append(t_day);

				t_todaysDate.append(p_separator);

				if (t_addMonthZero)
					t_todaysDate.append("0");

				t_todaysDate.append(t_month);

				t_todaysDate.append(p_separator);

				t_todaysDate.append(Integer.toString(p_date.getYear()));
				break;
			case DATE_ORDER_NUMERIC:
				t_todaysDate.append(Integer.toString(p_date.getYear()));

				t_todaysDate.append(p_separator);

				if (t_addMonthZero)
					t_todaysDate.append("0");

				t_todaysDate.append(t_month);

				t_todaysDate.append(p_separator);

				if (t_addDayZero)
					t_todaysDate.append("0");

				t_todaysDate.append(t_day);

				break;
		}

		return t_todaysDate.toString();
	}


	//*********************************
	static public String FormatTime(LocalTime p_time) {
		return FormatTime(p_time.getHour(), p_time.getMinute(), p_time.getSecond(), -1, ":");
	}


	//*********************************
	static public String FormatTime(LocalDateTime p_time) {
		return FormatTime(p_time.getHour(), p_time.getMinute(), p_time.getSecond(), -1, ":");
	}


	//*********************************
	static public String FormatTime(LocalDateTime p_time, boolean p_showMilliseconds) {
		return FormatTime(p_time.getHour(), p_time.getMinute(), p_time.getSecond(), (p_showMilliseconds ? p_time.get(ChronoField.MILLI_OF_SECOND) : -1), ":");
	}


	//*********************************
	static public String FormatTime(LocalDateTime p_time, String p_separator) {
		return FormatTime(p_time.getHour(), p_time.getMinute(), p_time.getSecond(), -1, p_separator);
	}


	//*********************************
	static public String FormatTime(ZonedDateTime p_time, boolean p_showMilliseconds) {
		return FormatTime(p_time.getHour(), p_time.getMinute(), p_time.getSecond(), (p_showMilliseconds ? p_time.get(ChronoField.MILLI_OF_SECOND) : -1), ":");
	}


	//*********************************
	static public String FormatTime(ZonedDateTime p_time, String p_separator) {
		return FormatTime(p_time.getHour(), p_time.getMinute(), p_time.getSecond(), -1, p_separator);
	}


	//*********************************
	static public String FormatTime(ZonedDateTime p_time) {
		return FormatTime(p_time.getHour(), p_time.getMinute(), p_time.getSecond(), -1, ":");
	}


	/*********************************
	 *
	 * @param p_hour
	 * @param p_minute
	 * @param p_seconds
	 * @param p_milliseconds Set this to -1 to turn off display of milliseconds.
	 * @return
	 */
	static public String FormatTime(int p_hour, int p_minute, int p_seconds, int p_milliseconds) {
		return FormatTime(p_hour, p_minute, p_seconds, p_milliseconds, ":");
	}

	/*********************************
	 *
	 * @param p_hour
	 * @param p_minute
	 * @param p_seconds     Set this to -1 to turn off display of seconds.
	 * @param p_milliseconds Set this to -1 to turn off display of milliseconds.  I've made the arbitrary decision that this will only display the first 5 characters of the milliseconds.
	 * @param p_separator
	 * @return
	 */
	static public String FormatTime(int p_hour, int p_minute, int p_seconds, int p_milliseconds, String p_separator) {
		StringBuilder t_time = new StringBuilder();

		if (p_hour < 10)
			t_time.append("0");

		t_time.append(p_hour + p_separator);

		if (p_minute < 10)
			t_time.append("0");

		t_time.append(p_minute);

		if (p_seconds >= 0) {
			t_time.append(p_separator);
			if (p_seconds < 10)
				t_time.append("0");

			t_time.append(p_seconds);

			// Optionally add the milliseconds.  We only do this if we added seconds first.
			if (p_milliseconds >= 0) {
				t_time.append(".");

				// I've made the arbitrary decision that I will only display the first 5 characters of the milliseconds, hence the structure of this code block.
				StringBuilder t_milliseconds = new StringBuilder(Integer.toString(p_milliseconds));
				if (t_milliseconds.length() < 4)	// if we only have 4 out of 9 characters, we get nothing but 0's and we're done.
					t_time.append("00000");
				else {
					while (t_milliseconds.length() < 3)
						t_milliseconds.insert(0, "0");

					for (int i = (t_milliseconds.length() - 5); i > 0; --i)
						t_time.append(t_milliseconds);
				}
			}
		}

		return t_time.toString();
	}


	//*********************************
	static public String FormatDateTime(LocalDateTime p_date, String p_seperator, boolean p_showMilliseconds) {
		StringBuilder t_dateTime = new StringBuilder();

		t_dateTime.append(FormatDate(p_date, p_seperator));
		t_dateTime.append(" ");
		t_dateTime.append(FormatTime(p_date.getHour(), p_date.getMinute(), p_date.getSecond(), (p_showMilliseconds ? p_date.get(ChronoField.MILLI_OF_SECOND) : -1)));

		return t_dateTime.toString();
	}


	//*********************************
	static public String FormatDateTime(ZonedDateTime p_date, String p_seperator, boolean p_showMilliseconds) {
		StringBuilder t_dateTime = new StringBuilder();

		t_dateTime.append(FormatDate(p_date, p_seperator));
		t_dateTime.append(" ");
		t_dateTime.append(FormatTime(p_date.getHour(), p_date.getMinute(), p_date.getSecond(), (p_showMilliseconds ? p_date.get(ChronoField.MILLI_OF_SECOND) : -1)));

		return t_dateTime.toString();
	}


	//*********************************
	static public String FormatDateTime(LocalDateTime p_date, String p_seperator, int p_dateOrderType) {
		StringBuilder t_dateTime = new StringBuilder();

		t_dateTime.append(FormatDate(p_date.toLocalDate(), p_seperator, p_dateOrderType));
		t_dateTime.append(" ");
		t_dateTime.append(FormatTime(p_date.getHour(), p_date.getMinute(), p_date.getSecond(), -1));

		return t_dateTime.toString();
	}


	//*********************************
	static public String FormatDateTime(ZonedDateTime p_date, String p_seperator, int p_dateOrderType) {
		StringBuilder t_dateTime = new StringBuilder();

		t_dateTime.append(FormatDate(p_date.toLocalDate(), p_seperator, p_dateOrderType));
		t_dateTime.append(" ");
		t_dateTime.append(FormatTime(p_date.getHour(), p_date.getMinute(), p_date.getSecond(), -1));

		return t_dateTime.toString();
	}

	//*********************************
	static public String FormatDateTime(LocalDateTime p_date, String p_seperator) {
		StringBuilder t_dateTime = new StringBuilder();

		t_dateTime.append(FormatDate(p_date.toLocalDate(), p_seperator, DATE_ORDER_NUMERIC));
		t_dateTime.append(" ");
		t_dateTime.append(FormatTime(p_date.getHour(), p_date.getMinute(), p_date.getSecond(), -1));

		return t_dateTime.toString();
	}


	//*********************************
	static public String FormatDateTime(ZonedDateTime p_date, String p_seperator) {
		StringBuilder t_dateTime = new StringBuilder();

		t_dateTime.append(FormatDate(p_date.toLocalDate(), p_seperator, DATE_ORDER_NUMERIC));
		t_dateTime.append(" ");
		t_dateTime.append(FormatTime(p_date.getHour(), p_date.getMinute(), p_date.getSecond(), -1));

		return t_dateTime.toString();
	}


	//*********************************
	static public String FormatDateTime(LocalDateTime p_date) {
		StringBuilder t_dateTime = new StringBuilder();

		t_dateTime.append(FormatDate(p_date));	// Use the default formating for both the date and time.
		t_dateTime.append(" ");
		t_dateTime.append(FormatTime(p_date));

		return t_dateTime.toString();
	}


	//*********************************
	static public String FormatDateTime(ZonedDateTime p_date) {
		StringBuilder t_dateTime = new StringBuilder();

		t_dateTime.append(FormatDate(p_date));	// Use the default formating for both the date and time.
		t_dateTime.append(" ");
		t_dateTime.append(FormatTime(p_date));

		return t_dateTime.toString();
	}


	//*********************************
	static public String FormatDouble(double p_value, int p_numberOfDecimalPlaces) {
		String t_valueString = Double.toString(p_value);
		if (t_valueString.contains(".")) {
			int t_decimalIndex = t_valueString.indexOf(".");
			if ((t_valueString.length() - (t_decimalIndex + 1)) > p_numberOfDecimalPlaces) {
				t_valueString = t_valueString.substring(0, (t_decimalIndex  + (p_numberOfDecimalPlaces + 1)));
			}
		}
		return t_valueString;
	}


	//*********************************
	static public String FormatMACAddress(String p_value) {
		if (p_value.contains(":"))
			return p_value;

		StringBuilder t_newMAC = new StringBuilder();
		t_newMAC.append(p_value.substring(0, 2));

		int t_startIndex = 2;
		while (t_startIndex < p_value.length()) {
			if((t_startIndex % 2) == 0)
				t_newMAC.append(":");

			t_newMAC.append(p_value.charAt(t_startIndex));

			t_startIndex++;
		}

		return t_newMAC.toString();
	}


	/*********************************
	 * Sometimes it is convenient for logging purposes to be able to add the reference address
	 * so that you can track the life of a particular object more easily.  This function takes
	 * an object reference and gets just the address part out of it.
	 * Ex.  ClipObjectReferenceAddress("" + t_newConnection)
	 * There may be an easier way to get the string version of the reference address info, but
	 * concatenating the reference with the empty string "" will do it.
	 * @param p_fullObjectReference
	 * @return
	 */
	static public String ClipObjectReferenceAddress(String p_fullObjectReference) {
		return p_fullObjectReference.substring(p_fullObjectReference.indexOf('@') + 1);
	}


	//*********************************
	static public String ZeroPadNumber(int p_sourceValue, int p_minimumPlaceCount) {
		try {
			return ZeroPadNumber(Integer.toString(p_sourceValue), p_minimumPlaceCount);
		}
		catch (Throwable t_error) {
			Logger.LogException("StringUtilities.ZeroPadNumber() failed with error: ", t_error);
			return null;
		}
	}


	//*********************************
	static public String ZeroPadNumber(String p_sourceValue, int p_minimumPlaceCount) {
		try {
			String t_valueString = p_sourceValue;
			while (t_valueString.length() < p_minimumPlaceCount)
				t_valueString = "0" + t_valueString;

			return t_valueString;
		}
		catch (Throwable t_error) {
			Logger.LogException("StringUtilities.ZeroPadNumber() failed with error: ", t_error);
			return null;
		}
	}
}
