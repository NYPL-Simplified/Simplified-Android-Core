package org.nypl.simplified.books.core;

import com.io7m.jfunctional.None;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.http.core.HTTPProblemReport;
import org.nypl.simplified.json.core.JSONSerializerUtilities;
import org.nypl.simplified.opds.core.OPDSFeedTransportException;

import java.io.IOException;

/**
 * The type of exceptions caused by HTTP errors.
 */

public final class FeedHTTPTransportException extends OPDSFeedTransportException
{
  private final int                           code;
  private final OptionType<HTTPProblemReport> report;

  private FeedHTTPTransportException(
    final String message,
    final int in_code,
    final OptionType<HTTPProblemReport> in_report)
  {
    super(message);
    this.code = in_code;
    this.report = NullCheck.notNull(in_report);
  }

  /**
   * Construct an exception.
   *
   * @param message   The message
   * @param in_code   The HTTP status code
   * @param in_report The HTTP problem report, if any
   * @return          A new exception
   */

  public static FeedHTTPTransportException newException(
    final String message,
    final int in_code,
    final OptionType<HTTPProblemReport> in_report)
  {
    NullCheck.notNull(message);
    NullCheck.notNull(in_report);

    final StringBuilder sb = new StringBuilder();
    sb.append(in_code);
    sb.append(": ");
    sb.append(message);
    sb.append("\n");

    in_report.accept(
      new OptionVisitorType<HTTPProblemReport, Unit>()
      {
        @Override public Unit none(final None<HTTPProblemReport> n)
        {
          sb.append("No problem report was provided.\n");
          return Unit.unit();
        }

        @Override public Unit some(final Some<HTTPProblemReport> s)
        {
          sb.append("Problem report:\n");
          try {
            final HTTPProblemReport r = s.get();
            sb.append(
              JSONSerializerUtilities.serializeToString(r.getRawJSON()));
          } catch (final IOException e) {
            sb.append("Could not serialize problem report!\n");
            sb.append(e);
          }
          return Unit.unit();
        }
      });

    return new FeedHTTPTransportException(sb.toString(), in_code, in_report);
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
