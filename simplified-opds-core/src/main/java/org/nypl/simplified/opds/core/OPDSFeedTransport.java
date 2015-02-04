package org.nypl.simplified.opds.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import com.io7m.jnull.NullCheck;

public final class OPDSFeedTransport
{
  public static OPDSFeedTransportType newTransport()
  {
    return new OPDSFeedTransportType() {
      @Override public InputStream getStream(
        final URI uri)
        throws IOException
      {
        return NullCheck.notNull(uri.toURL().openStream());
      }
    };
  }
}
