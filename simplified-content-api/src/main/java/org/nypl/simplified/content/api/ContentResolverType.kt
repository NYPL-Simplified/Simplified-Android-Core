package org.nypl.simplified.content.api

import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI

/**
 * The interface exposed by content resolvers that take `content://` URIs and return
 * [InputStream] values.
 *
 * This interface is necessary due to Android's incessant use of the awful [android.net.Uri]
 * class which complicates unit testing. This interface exposes a sensible interface that
 * uses the rather-more-civilized [java.net.URI] class.
 */

interface ContentResolverType {

  /**
   * Open an input stream for the given `content://` URI.
   *
   * @throws IllegalArgumentException If the URI is not a content URI
   * @return `null` if no content provider can handle the given URI
   */

  fun openInputStream(uri: String): InputStream? =
    this.openInputStream(URI.create(uri))

  /**
   * Open an input stream for the given `content://` URI.
   *
   * @throws IllegalArgumentException If the URI is not a content URI
   * @return `null` if no content provider can handle the given URI
   */

  fun openInputStream(uri: URI): InputStream?

  /**
   * Open an input stream for the given `content://` URI.
   *
   * @throws IllegalArgumentException If the URI is not a content URI
   * @throws FileNotFoundException If no content provider can handle the given URI
   */

  @Throws(FileNotFoundException::class)
  fun openInputStreamOrThrow(uri: URI): InputStream {
    return this.openInputStream(uri) ?: throw FileNotFoundException(uri.toString())
  }

  /**
   * Open an input stream for the given `content://` URI.
   *
   * @throws IllegalArgumentException If the URI is not a content URI
   * @throws FileNotFoundException If no content provider can handle the given URI
   */

  @Throws(FileNotFoundException::class)
  fun openInputStreamOrThrow(uri: String): InputStream =
    this.openInputStreamOrThrow(URI.create(uri))
}
