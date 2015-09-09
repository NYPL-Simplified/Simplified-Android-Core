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
import org.nypl.simplified.opds.core.OPDSFeedTransportException;
import org.nypl.simplified.opds.core.OPDSFeedTransportHTTPException;
import org.nypl.simplified.opds.core.OPDSFeedTransportIOException;
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
    throws OPDSFeedTransportException
  {
    final HTTPResultType<InputStream> r = this.http.get(auth, uri, 0L);
    return r.matchResult(
      new HTTPResultMatcherType<InputStream, InputStream,
        OPDSFeedTransportException>()
      {
        @Override
        public InputStream onHTTPError(final HTTPResultError<InputStream> e)
          throws OPDSFeedTransportException
        {
          throw new OPDSFeedTransportHTTPException(e.getMessage(), e.getStatus());
        }

        @Override public InputStream onHTTPException(
          final HTTPResultException<InputStream> e)
          throws OPDSFeedTransportException
        {
          final Exception er = e.getError();
          final IOException ex = new IOException(er);
          throw new OPDSFeedTransportIOException(er.getMessage(), ex);
        }

        @Override
        public InputStream onHTTPOK(final HTTPResultOKType<InputStream> e)
          throws OPDSFeedTransportException
        {
          return e.getValue();
        }
      });
  }
}
