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

package org.nypl.simplified.http.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.json.core.JSONParserUtilities;

import java.io.IOException;
import java.io.InputStream;

/**
 * An HTTP problem report.
 */

public final class HTTPProblemReport
{
  private final ObjectNode raw;

  /**
   * Construct a problem report.
   *
   * @param raw_json The raw JSON comprising the report
   */

  public HTTPProblemReport(final ObjectNode raw_json)
  {
    this.raw = NullCheck.notNull(raw_json);
  }

  /**
   * Parse a problem report from the given stream.
   *
   * @param s The stream
   *
   * @return A problem report
   *
   * @throws IOException On errors
   */

  public static HTTPProblemReport fromStream(final InputStream s)
    throws IOException
  {
    NullCheck.notNull(s);
    final ObjectMapper jom = new ObjectMapper();
    final JsonNode n = jom.readTree(s);
    final ObjectNode o = JSONParserUtilities.checkObject(null, n);
    return new HTTPProblemReport(o);
  }

  /**
   * @param s string value
   * @return problem report
   * @throws IOException exception
   */
  public static HTTPProblemReport fromString(final String s)
    throws IOException
  {
    NullCheck.notNull(s);
    final ObjectMapper jom = new ObjectMapper();
    final JsonNode n = jom.readTree(s);
    final ObjectNode o = JSONParserUtilities.checkObject(null, n);
    return new HTTPProblemReport(o);
  }

  /**
   * @return The raw JSON data
   */

  public ObjectNode getRawJSON()
  {
    return this.raw;
  }

  /**
   * @return The "detail" field from the problem document
   */
  public String getProblemDetail()
  {
    if (this.raw.has("detail")) {
      return this.raw.get("detail").asText();
    }
    return null;
  }

  /**
   * @return The "title" field from the problem document
   */
  public String getProblemTitle()
  {
    if (this.raw.has("title")) {
      return this.raw.get("title").asText();
    }
    return null;
  }


  /**
   * @return The problem type from the JSON data
   */

  public ProblemType getProblemType()
  {
    if (this.raw.has("type")) {
      final String type_value = this.raw.get("type").asText();
      if ("http://librarysimplified.org/terms/problem/loan-limit-reached".equals(type_value)) {
        return ProblemType.LoanLimitReached;
      }
    }
    return ProblemType.Unknown;
  }

  /**
   * @return problem status
   */
  public ProblemStatus getProblemStatus()
  {
    if (this.raw.has("status")) {
      final String type_value = this.raw.get("status").asText();
      if ("401".equals(type_value)) {
        return ProblemStatus.Unauthorized;
      }
    }
    return ProblemStatus.Unknown;
  }

  /**
   * Problem type enum.
   */
  public enum ProblemType
  {
    /**
     * Loan limit reached problem.
     */
    LoanLimitReached,
    /**
     * Unknown problem.
     */
    Unknown
  }


  /**
   *
   */
  public enum ProblemStatus
  {

    /**
     * Unauthorized problem
     */
    Unauthorized,
    /**
     * Unknown problem.
     */
    Unknown
  }
}
