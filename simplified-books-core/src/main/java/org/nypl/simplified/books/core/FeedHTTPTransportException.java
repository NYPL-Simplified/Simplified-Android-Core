/*
 * Copyright © 2015 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.http.core.HTTPProblemReport;
import org.nypl.simplified.opds.core.OPDSFeedTransportException;

/**
 * An HTTP error occurring during fetching of a feed.
 */

public final class FeedHTTPTransportException extends OPDSFeedTransportException
{
  private final OptionType<HTTPProblemReport> report;
  private final int                           code;

  /**
   * Construct an exception.
   *
   * @param in_message The error message
   * @param in_code    The status code
   * @param in_report  The problem report, if any
   */

  public FeedHTTPTransportException(
    final String in_message,
    final int in_code,
    final OptionType<HTTPProblemReport> in_report)
  {
    super(in_message);
    this.code = in_code;
    this.report = NullCheck.notNull(in_report);
  }

  /**
   * Construct an exception.
   *
   * @param in_message The error message
   * @param in_code    The status code
   * @param in_cause   The exception
   * @param in_report  The problem report, if any
   */

  public FeedHTTPTransportException(
    final String in_message,
    final int in_code,
    final Throwable in_cause,
    final OptionType<HTTPProblemReport> in_report)
  {
    super(in_message, in_cause);
    this.code = in_code;
    this.report = NullCheck.notNull(in_report);
  }

  /**
   * @return The HTTP status code
   */

  public int getStatusCode()
  {
    return this.code;
  }

  /**
   * @return The problem report, if any
   */

  public OptionType<HTTPProblemReport> getProblemReport()
  {
    return this.report;
  }
}
