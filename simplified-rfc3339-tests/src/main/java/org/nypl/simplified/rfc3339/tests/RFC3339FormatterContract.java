package org.nypl.simplified.rfc3339.tests;

import com.io7m.jnull.NullCheckException;

import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.rfc3339.core.RFC3339Formatter;

import java.util.Calendar;

public abstract class RFC3339FormatterContract
{
  /**
   * Construct a contract.
   */

  public RFC3339FormatterContract()
  {
    // Nothing
  }

  private static void dumpCalendar(
    final String text,
    final Calendar c)
  {
    System.out.println("Parsed : " + text);
    System.out.println("Year   : " + c.get(Calendar.YEAR));
    System.out.println("Month  : " + c.get(Calendar.MONTH));
    System.out.println("Day    : " + c.get(Calendar.DAY_OF_MONTH));
    System.out.println("Hour   : " + c.get(Calendar.HOUR_OF_DAY));
    System.out.println("Minute : " + c.get(Calendar.MINUTE));
    System.out.println("Second : " + c.get(Calendar.SECOND));
    System.out.println("Ms     : " + c.get(Calendar.MILLISECOND));
  }

  @Test
  public final void testDate0()
    throws Exception
  {
    final String text = "2015-05-01T23:11:15-00:00";
    final Calendar c = RFC3339Formatter.parseRFC3339Date(text);

    RFC3339FormatterContract.dumpCalendar(text, c);

    Assert.assertEquals(c.get(Calendar.YEAR), 2015);
    Assert.assertEquals(c.get(Calendar.MONTH), 4); // Starts from 0
    Assert.assertEquals(c.get(Calendar.DAY_OF_MONTH), 1);
    Assert.assertEquals(c.get(Calendar.HOUR_OF_DAY), 23);
    Assert.assertEquals(c.get(Calendar.MINUTE), 11);
    Assert.assertEquals(c.get(Calendar.SECOND), 15);
    Assert.assertEquals(c.get(Calendar.MILLISECOND), 0);
  }

  @Test
  public final void testDate1()
    throws Exception
  {
    final String text = "2015-05-01T23:11:15.237-00:00";
    final Calendar c = RFC3339Formatter.parseRFC3339Date(text);

    RFC3339FormatterContract.dumpCalendar(text, c);

    Assert.assertEquals(c.get(Calendar.YEAR), 2015);
    Assert.assertEquals(c.get(Calendar.MONTH), 4); // Starts from 0
    Assert.assertEquals(c.get(Calendar.DAY_OF_MONTH), 1);
    Assert.assertEquals(c.get(Calendar.HOUR_OF_DAY), 23);
    Assert.assertEquals(c.get(Calendar.MINUTE), 11);
    Assert.assertEquals(c.get(Calendar.SECOND), 15);
    Assert.assertEquals(c.get(Calendar.MILLISECOND), 237);
  }

  @Test
  public final void testDate2()
    throws Exception
  {
    final String text = "2015-05-01T23:11:15Z";
    final Calendar c = RFC3339Formatter.parseRFC3339Date(text);

    RFC3339FormatterContract.dumpCalendar(text, c);

    Assert.assertEquals(c.get(Calendar.YEAR), 2015);
    Assert.assertEquals(c.get(Calendar.MONTH), 4); // Starts from 0
    Assert.assertEquals(c.get(Calendar.DAY_OF_MONTH), 1);
    Assert.assertEquals(c.get(Calendar.HOUR_OF_DAY), 23);
    Assert.assertEquals(c.get(Calendar.MINUTE), 11);
    Assert.assertEquals(c.get(Calendar.SECOND), 15);
    Assert.assertEquals(c.get(Calendar.MILLISECOND), 0);
  }

  @Test
  public final void testDate3()
    throws Exception
  {
    final String text = "2015-05-01T23:11:15.237Z";
    final Calendar c = RFC3339Formatter.parseRFC3339Date(text);

    RFC3339FormatterContract.dumpCalendar(text, c);

    Assert.assertEquals(c.get(Calendar.YEAR), 2015);
    Assert.assertEquals(c.get(Calendar.MONTH), 4); // Starts from 0
    Assert.assertEquals(c.get(Calendar.DAY_OF_MONTH), 1);
    Assert.assertEquals(c.get(Calendar.HOUR_OF_DAY), 23);
    Assert.assertEquals(c.get(Calendar.MINUTE), 11);
    Assert.assertEquals(c.get(Calendar.SECOND), 15);
    Assert.assertEquals(c.get(Calendar.MILLISECOND), 237);
  }

  @Test
  public final void testNull()
    throws Exception
  {
    boolean caught = false;

    try {
      RFC3339Formatter.parseRFC3339Date(null);
    } catch (final NullCheckException e) {
      caught = true;
    }

    if (caught == false) {
      throw new AssertionError("Failed to raise exception");
    }
  }
}
