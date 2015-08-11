package org.nypl.simplified.http.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;

import java.io.InputStream;
import java.net.URI;

/**
 * Mindlessly simple synchronous HTTP requests.
 */

public interface HTTPType
{
  /**
   * Retrieve the content at {@code uri}, using authentication details
   * {@code auth}. The content returned will have been requested with the
   * initial byte offset {@code offset}.
   *
   * @param auth   The authentication details, if any
   * @param uri    The URI
   * @param offset The byte offset
   *
   * @return A result
   */

  HTTPResultType<InputStream> get(
    final OptionType<HTTPAuthType> auth,
    final URI uri,
    final long offset);

  /**
   * Peek at the URI {@code uri}, using authentication details {@code auth}.
   *
   * @param auth The authentication details, if any
   * @param uri  The URI
   *
   * @return A result
   */

  HTTPResultType<Unit> head(
    final OptionType<HTTPAuthType> auth,
    final URI uri);
}
