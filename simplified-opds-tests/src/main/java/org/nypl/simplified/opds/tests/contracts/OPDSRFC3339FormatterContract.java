package org.nypl.simplified.opds.tests.contracts;

import java.util.Calendar;

import org.nypl.simplified.opds.core.OPDSRFC3339Formatter;
import org.nypl.simplified.opds.tests.utilities.TestUtilities;

import com.io7m.jnull.NullCheckException;

@SuppressWarnings("boxing") public final class OPDSRFC3339FormatterContract implements
  OPDSRFC3339FormatterContractType
{
  public OPDSRFC3339FormatterContract()
  {
    // Nothing
  }

  @Override public void testDate0()
    throws Exception
  {
    final Calendar c =
      OPDSRFC3339Formatter.parseRFC3339Date("2015-05-01T23:11:15-00:00");
    TestUtilities.assertEquals(c.get(Calendar.YEAR), 2015);
    TestUtilities.assertEquals(c.get(Calendar.MONTH), 4); // Starts from 0
    TestUtilities.assertEquals(c.get(Calendar.DAY_OF_MONTH), 1);
    TestUtilities.assertEquals(c.get(Calendar.HOUR_OF_DAY), 23);
    TestUtilities.assertEquals(c.get(Calendar.MINUTE), 11);
    TestUtilities.assertEquals(c.get(Calendar.SECOND), 15);
    TestUtilities.assertEquals(c.get(Calendar.MILLISECOND), 0);
  }

  @Override public void testDate1()
    throws Exception
  {
    final Calendar c =
      OPDSRFC3339Formatter.parseRFC3339Date("2015-05-01T23:11:15.237-00:00");
    TestUtilities.assertEquals(c.get(Calendar.YEAR), 2015);
    TestUtilities.assertEquals(c.get(Calendar.MONTH), 4); // Starts from 0
    TestUtilities.assertEquals(c.get(Calendar.DAY_OF_MONTH), 1);
    TestUtilities.assertEquals(c.get(Calendar.HOUR_OF_DAY), 23);
    TestUtilities.assertEquals(c.get(Calendar.MINUTE), 11);
    TestUtilities.assertEquals(c.get(Calendar.SECOND), 15);
    TestUtilities.assertEquals(c.get(Calendar.MILLISECOND), 237);
  }

  @Override public void testDate2()
    throws Exception
  {
    final Calendar c =
      OPDSRFC3339Formatter.parseRFC3339Date("2015-05-01T23:11:15Z");
    TestUtilities.assertEquals(c.get(Calendar.YEAR), 2015);
    TestUtilities.assertEquals(c.get(Calendar.MONTH), 4); // Starts from 0
    TestUtilities.assertEquals(c.get(Calendar.DAY_OF_MONTH), 1);
    TestUtilities.assertEquals(c.get(Calendar.HOUR_OF_DAY), 23);
    TestUtilities.assertEquals(c.get(Calendar.MINUTE), 11);
    TestUtilities.assertEquals(c.get(Calendar.SECOND), 15);
    TestUtilities.assertEquals(c.get(Calendar.MILLISECOND), 0);
  }

  @Override public void testDate3()
    throws Exception
  {
    final Calendar c =
      OPDSRFC3339Formatter.parseRFC3339Date("2015-05-01T23:11:15.237Z");
    TestUtilities.assertEquals(c.get(Calendar.YEAR), 2015);
    TestUtilities.assertEquals(c.get(Calendar.MONTH), 4); // Starts from 0
    TestUtilities.assertEquals(c.get(Calendar.DAY_OF_MONTH), 1);
    TestUtilities.assertEquals(c.get(Calendar.HOUR_OF_DAY), 23);
    TestUtilities.assertEquals(c.get(Calendar.MINUTE), 11);
    TestUtilities.assertEquals(c.get(Calendar.SECOND), 15);
    TestUtilities.assertEquals(c.get(Calendar.MILLISECOND), 237);
  }

  @Override public void testNull()
    throws Exception
  {
    boolean caught = false;

    try {
      OPDSRFC3339Formatter.parseRFC3339Date((String) TestUtilities
        .unexpectedlyNull());
    } catch (final NullCheckException e) {
      caught = true;
    }

    if (caught == false) {
      throw new AssertionError("Failed to raise exception");
    }
  }
}
