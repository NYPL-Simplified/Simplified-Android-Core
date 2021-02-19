package org.nypl.drm.core;

import com.io7m.jnull.Nullable;

/**
 * <p>The type of streams.</p> <p>When used as part of the Adobe Adept
 * Connector, the streams are almost always HTTP(s) connections, used to
 * authorize devices and download books.</p>
 */

public interface AdobeAdeptStreamType
{
  /**
   * Called when information about the stream should be requested. For example,
   * a stream that represents an HTTP connection would open the connection here
   * and make the initial request.
   */

  void onRequestInfo();

  /**
   * Called when <tt>size</tt> bytes of data starting at <tt>offset</tt> are
   * being requested.
   *
   * @param offset The byte offset
   * @param size   The number of bytes of data requested
   */

  void onRequestBytes(
    long offset,
    long size);

  /**
   * Called when an error has occurred.
   *
   * @param message The error message
   */

  void onError(
    String message);

  /**
   * Called when the stream is to be closed and resources released.
   */

  void onRelease();

  /**
   * Replace the existing stream client, if any, with {@code c}.
   *
   * @param c A new stream client
   */

  void onSetStreamClient(@Nullable AdobeAdeptStreamClientType c);
}
