package org.nypl.simplified.opds.core;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * A supplier of date/time parsers.
 */

public final class OPDSDateParsers {
  private OPDSDateParsers() {

  }

  /**
   * @return A properly configured date/time parser.
   */

  public static DateTimeFormatter dateTimeParser() {
    return ISODateTimeFormat.dateTimeParser().withZoneUTC();
  }
}
