package org.nypl.simplified.http.core;

import java.io.InputStream;
import java.net.URI;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;

/**
 * Mindlessly simple synchronous HTTP requests.
 */

public interface HTTPType
{
  /**
   * Retrieve the content at <tt>uri</tt>, using authentication details
   * <tt>auth</tt>. The content returned will have been requested with the
   * initial byte offset <tt>offset</tt>.
   *
   * @param auth
   *          The authentication details, if any
   * @param uri
   *          The URI
   * @param offset
   *          The byte offset
   * @return A result
   */

  HTTPResultType<InputStream> get(
    final OptionType<HTTPAuthType> auth,
    final URI uri,
    final long offset);

  /**
   * Peek at the URI <tt>uri</tt>, using authentication details <tt>auth</tt>.
   *
   * @param auth
   *          The authentication details, if any
   * @param uri
   *          The URI
   * 
   * @return A result
   */

  HTTPResultType<Unit> head(
    final OptionType<HTTPAuthType> auth,
    final URI uri);
}
