package org.nypl.simplified.viewer.spi

import android.app.Activity
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat

/**
 * The interface exposed by viewers.
 */

interface ViewerProviderType {

  /**
   * The name of the viewer provider. This is typically a reverse-dns style name and is
   * used for diagnostic information.
   */

  val name: String

  /**
   * Determine if the current provider can support the given book. Implementations are
   * expected to return `true` iff they can support a book, and consumers of this API
   * are expected to implement some sort of intelligent selection strategy to pick the
   * "best" provider they know about. Iff `true` is returned, then it is safe to call
   * [open] with the given book.
   *
   * @return `true` if the provider can support the given book
   */

  fun canSupport(
    preferences: ViewerPreferences,
    book: Book,
    format: BookFormat
  ): Boolean

  /**
   * Open a viewer for the given book.
   *
   * Implementations should fail gracefully if [canSupport] previously returned `false` for
   * this book, but a consumer went ahead and called this method anyway.
   */

  fun open(
    activity: Activity,
    preferences: ViewerPreferences,
    book: Book,
    format: BookFormat
  )
}
