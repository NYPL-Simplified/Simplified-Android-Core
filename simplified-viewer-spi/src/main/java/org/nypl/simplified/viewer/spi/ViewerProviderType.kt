package org.nypl.simplified.viewer.spi

import android.app.Activity
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat

/**
 * The interface exposed by viewers.
 */

interface ViewerProviderType {

  val name: String

  fun canSupport(
    book: Book,
    format: BookFormat
  ): Boolean

  fun open(
    activity: Activity,
    book: Book,
    format: BookFormat
  )

}
