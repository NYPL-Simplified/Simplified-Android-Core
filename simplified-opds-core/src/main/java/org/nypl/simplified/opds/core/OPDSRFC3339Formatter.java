package org.nypl.simplified.opds.core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * Parser for RFC3339 dates.
 */

public final class OPDSRFC3339Formatter
{
  public static Calendar parseRFC3339Date(
    final String text)
    throws ParseException
  {
    NullCheck.notNull(text);

    Date d = new Date();

    final TimeZone utc = TimeZone.getTimeZone("UTC");
    if (text.endsWith("Z")) {
      final SimpleDateFormat df0 =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
      df0.setTimeZone(utc);
      try {
        d = df0.parse(text);
      } catch (final java.text.ParseException pe) {
        final SimpleDateFormat df1 =
          new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
        df1.setTimeZone(utc);
        df1.setLenient(true);
        d = df1.parse(text);
      }
      return OPDSRFC3339Formatter.toCal(NullCheck.notNull(d));
    }

    /**
     * Remove timezone.
     */

    final String pre = text.substring(0, text.lastIndexOf('-'));

    /**
     * Remove colon from timezone offset.
     */

    String post = text.substring(text.lastIndexOf('-'));
    post =
      post.substring(0, post.indexOf(':'))
        + post.substring(post.indexOf(':') + 1);

    final String new_date = pre + post;
    final SimpleDateFormat df0 =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    df0.setTimeZone(utc);
    try {
      d = df0.parse(new_date);
    } catch (final java.text.ParseException pe) {
      final SimpleDateFormat df1 =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");
      df1.setTimeZone(utc);
      df1.setLenient(true);
      d = df1.parse(new_date);
    }

    return OPDSRFC3339Formatter.toCal(NullCheck.notNull(d));
  }

  private static Calendar toCal(
    final Date d)
  {
    final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    cal.setTime(d);
    return cal;
  }

  private OPDSRFC3339Formatter()
  {
    throw new UnreachableCodeException();
  }
}
