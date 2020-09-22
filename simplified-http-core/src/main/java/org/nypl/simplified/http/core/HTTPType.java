package org.nypl.simplified.http.core;

import com.io7m.jfunctional.OptionType;

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
   * Retrieve the content at {@code uri}, using authentication details
   * {@code auth}. The content returned will have been requested with the
   * initial byte offset {@code offset}.
   *
   * @param auth    The authentication details, if any
   * @param uri     The URI
   * @param offset  The byte offset
   * @param noCache True to add the request property "Cache-Control", "no-cache".
   *
   * @return A result
   */

  HTTPResultType<InputStream> get(
    final OptionType<HTTPAuthType> auth,
    final URI uri,
    final long offset,
    final Boolean noCache);

  /**
   * Make a put request to {@code uri}, and return the results,
   * using authentication details {@code auth}.
   *
   * @param auth   The authentication details, if any
   * @param uri    The URI
   *
   * @return A result
   */

  HTTPResultType<InputStream> put(
      final OptionType<HTTPAuthType> auth,
      final URI uri);

  /**
   * Make a put request to {@code uri}, and return the results,
   * using authentication details {@code auth}.
   *
   * @param auth   The authentication details, if any
   * @param uri    The URI
   *
   * @return A result
   */

  HTTPResultType<InputStream> put(
    final OptionType<HTTPAuthType> auth,
    final URI uri,
    final byte[] data,
    final String content_type);

  /**
   * Make a post request to {@code uri}, post {@code data}, and return the results,
   * using authentication details {@code auth}.
   *
   * @param auth          The authentication details, if any
   * @param uri           The URI
   * @param data          The data to post
   * @param content_type  The content type to send with the post request
   *
   * @return A result
   */

  HTTPResultType<InputStream> post(
    final OptionType<HTTPAuthType> auth,
    final URI uri,
    final byte[] data,
    final String content_type);


  /**
   * @param auth          The authentication details, if any
   * @param uri           The URI
   * @param content_type  The content type to send with the post request
   *
   * @return A result   */
  HTTPResultType<InputStream> delete(
    final OptionType<HTTPAuthType> auth,
    final URI uri,
    final String content_type);


  /**
   * Peek at the URI {@code uri}, using authentication details {@code auth}.
   *
   * @param auth The authentication details, if any
   * @param uri  The URI
   *
   * @return A result
   */

  HTTPResultType<InputStream> head(
    final OptionType<HTTPAuthType> auth,
    final URI uri);
}
