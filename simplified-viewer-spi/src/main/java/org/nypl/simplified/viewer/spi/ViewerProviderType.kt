package org.nypl.simplified.viewer.spi

import android.app.Activity
import one.irradia.mime.api.MIMEType
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
   * Determine if the current provider can support books of the given type. Note that this method
   * is advisory in the sense that providers may return false positives if support for a given
   * type is conditional upon information only known at book loading time. Essentially, this
   * method may return `true` for all types that are potentially supported, but the actual support
   * may be more restricted. For accurate results, see [canSupport].
   *
   * @return `true` if the provider can support the given type
   */

  fun canPotentiallySupportType(
    type: MIMEType
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
