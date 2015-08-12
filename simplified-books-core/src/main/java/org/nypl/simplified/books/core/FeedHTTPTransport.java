package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSFeedTransportType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * An implementation of the {@link OPDSFeedTransportType} interface that uses an
 * {@link HTTPType} instance for communication, supporting optional
 * authentication.
 */

public final class FeedHTTPTransport
  implements OPDSFeedTransportType<OptionType<HTTPAuthType>>
{
  private final HTTPType http;

  private FeedHTTPTransport(final HTTPType in_http)
  {
    this.http = NullCheck.notNull(in_http);
  }

  /**
   * @param http An HTTP interface
   *
   * @return A new transport
   */

  public static OPDSFeedTransportType<OptionType<HTTPAuthType>> newTransport(
    final HTTPType http)
  {
    return new FeedHTTPTransport(http);
  }

  @Override public InputStream getStream(
    final OptionType<HTTPAuthType> auth,
    final URI uri)
    throws IOException
  {
    final HTTPResultType<InputStream> r = this.http.get(auth, uri, 0L);
    return r.matchResult(
      new HTTPResultMatcherType<InputStream, InputStream, IOException>()
      {
        @Override
        public InputStream onHTTPError(final HTTPResultError<InputStream> e)
          throws IOException
        {
          throw new IOException(
            String.format(
              "Server error for URI %s: %d (%s)",
              uri,
              e.getStatus(),
              e.getMessage()));
        }

        @Override public InputStream onHTTPException(
          final HTTPResultException<InputStream> e)
          throws IOException
        {
          throw new IOException(e.getError());
        }

        @Override
        public InputStream onHTTPOK(final HTTPResultOKType<InputStream> e)
          throws IOException
        {
          return e.getValue();
        }
      });
  }
}
