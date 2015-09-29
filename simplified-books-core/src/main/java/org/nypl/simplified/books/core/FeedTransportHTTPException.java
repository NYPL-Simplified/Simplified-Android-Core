package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.http.core.HTTPProblemReport;
import org.nypl.simplified.opds.core.OPDSFeedTransportException;

/**
 * The type of exceptions caused by HTTP errors.
 */

public final class FeedTransportHTTPException extends OPDSFeedTransportException
{
  private final int                           code;
  private final OptionType<HTTPProblemReport> report;

  /**
   * Construct an exception.
   *
   * @param message The message
   * @param in_code The HTTP status code
   */

  public FeedTransportHTTPException(
    final String message,
    final int in_code,
    final OptionType<HTTPProblemReport> in_report)
  {
    super(message);
    this.code = in_code;
    this.report = NullCheck.notNull(in_report);
  }

  /**
   * @return The problem report, if any
   */

  public OptionType<HTTPProblemReport> getProblemReport()
  {
    return this.report;
  }

  /**
   * @return The status code
   */

  public int getCode()
  {
    return this.code;
  }
}
