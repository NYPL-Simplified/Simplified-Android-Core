package org.nypl.simplified.app;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * The default bitmap transport, mapping URIs to streams.
 */

public final class BitmapTransport
{
  private static final PartialFunctionType<URI, InputStream, IOException> INSTANCE;

  static {
    INSTANCE = new PartialFunctionType<URI, InputStream, IOException>() {
      @Override public InputStream call(
        final URI u)
        throws IOException
      {
        return NullCheck.notNull(NullCheck.notNull(u).toURL().openStream());
      }
    };
  }

  /**
   * @return The default bitmap transport
   */

  public static PartialFunctionType<URI, InputStream, IOException> get()
  {
    return BitmapTransport.INSTANCE;
  }

  private BitmapTransport()
  {
    throw new UnreachableCodeException();
  }
}
