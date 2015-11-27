package com.financialanalysis.common;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class DateTimeUtils {
    private static String TIMEZONE_ID = "America/Toronto";

    public static DateTime getToday() {
        return DateTime.now(DateTimeZone.forID(TIMEZONE_ID)).withTimeAtStartOfDay();
    }

    public static DateTimeZone getTimeZone() {
        return DateTimeZone.forID(TIMEZONE_ID);
    }
}
