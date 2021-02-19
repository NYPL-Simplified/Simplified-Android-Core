package org.nypl.drm.core;

import com.io7m.jnull.Nullable;

/**
 * <p>The type of net providers.</p>
 */

public interface AdobeAdeptNetProviderType
{
  /**
   * <p>Open a new stream to <tt>url</tt>, using <tt>method</tt>, delivering
   * data to <tt>client</tt>. Optional data to be sent to the destination may be
   * given in <tt>post_data</tt> of type <tt>post_data_content_type</tt>.</p>
   *
   * <p>In practical terms, making a <tt>POST</tt> request to
   * <tt>http://example.com</tt> would imply that <tt>method == "POST"</tt>,
   * <tt>url == "http://example.com"</tt>. Typically, <tt>post_data_content_type
   * == "application/www-url-form-encoded"</tt> and <tt>post_data</tt> is an
   * arbitrary series of bytes.</p>
   *
   * @param method                 The request method
   * @param url                    The URL
   * @param client                 The stream client
   * @param post_data_content_type The post data type, if any
   * @param post_data              The post data, if any
   *
   * @return A new stream, or <tt>null</tt> if the stream cannot be opened for
   * any reason. Implementations are encouraged to notify the stream client of
   * specific errors.
   */

  @Nullable AdobeAdeptStreamType newStream(
    String method,
    String url,
    @Nullable AdobeAdeptStreamClientType client,
    @Nullable String post_data_content_type,
    @Nullable byte[] post_data);

  /**
   * Cancel the currently running download.
   */

  void cancel();
}
