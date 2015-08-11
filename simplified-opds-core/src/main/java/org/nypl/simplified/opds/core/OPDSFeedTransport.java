package org.nypl.simplified.opds.core;

import com.io7m.jnull.NullCheck;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * The default implementation of the {@link OPDSFeedTransportType} interface.
 */

public final class OPDSFeedTransport
{
  private OPDSFeedTransport()
  {

  }

  /**
   * @return A transport that uses whatever is the current URL resolver to open
   * a stream of the correct type to the given URI.
   */

  public static OPDSFeedTransportType newTransport()
  {
    /**
     * XXX: Blindly trust the scheme of the given URI?
     */

    return new OPDSFeedTransportType()
    {
      @Override public InputStream getStream(
        final URI uri)
        throws IOException
      {
        return NullCheck.notNull(NullCheck.notNull(uri.toURL()).openStream());
      }
    };
  }
}
