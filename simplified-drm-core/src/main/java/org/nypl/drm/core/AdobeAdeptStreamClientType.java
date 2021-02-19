package org.nypl.drm.core;

/**
 * <p>The type of stream clients.</p> <p>In the Adobe Adept Connector API, data
 * is not extracted from streams directly, but is delivered to an associated
 * <i>stream client</i>. This type represents the API that must be supported by
 * stream clients.</p>
 */

public interface AdobeAdeptStreamClientType
{
  /**
   * Data is ready to be consumed from the stream.
   *
   * @param offset The byte offset of the data within the stream
   * @param data   The data
   * @param eof    If no more data will be received after this call
   */

  void onBytesReady(
    long offset,
    byte[] data,
    boolean eof);

  /**
   * An error occurred in the stream.
   *
   * @param message The error message
   */

  void onError(
    String message);

  /**
   * The stream could not be initialized. This is an NYPL extension and is not
   * part of the Adobe library's interface.
   *
   * @param e The exception raised
   */

  void onInitializationError(
    Exception e);

  /**
   * The stream properties have been completely consumed and data will likely
   * follow.
   *
   * @see #onBytesReady(long, byte[], boolean)
   */

  void onPropertiesReady();

  /**
   * The given stream property is ready.
   *
   * @param key   The stream property name
   * @param value The value
   */

  void onPropertyReady(
    String key,
    String value);

  /**
   * The total size of the data that will be produced by the stream is now
   * known. This method is not guaranteed to be called, as some streams are of
   * indeterminate lengths.
   *
   * @param size The size of the stream in bytes
   */

  void onTotalLengthReady(
    long size);
}
