package org.lensfield.log;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @author Sam Adams
 */
public class DateTimeUtils {

    private static final String ZULU_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z";
    private static final DateTimeFormatter ZULU_FORMATTER = DateTimeFormat.forPattern(ZULU_DATETIME_PATTERN)
            .withZone(DateTimeZone.UTC);

    public static String formatDateTime(DateTime datetime) {
        return ZULU_FORMATTER.print(datetime);
    }

    public static DateTime parseDateTime(String s) {
        return ZULU_FORMATTER.parseDateTime(s);
    }

}
