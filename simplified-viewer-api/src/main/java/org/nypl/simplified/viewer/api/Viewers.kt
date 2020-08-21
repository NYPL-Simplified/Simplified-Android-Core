package org.nypl.simplified.viewer.api

import android.app.Activity
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.viewer.spi.ViewerPreferences
import org.nypl.simplified.viewer.spi.ViewerProviderType
import org.slf4j.LoggerFactory
import java.util.ServiceLoader

/**
 * A simple API to find and use a viewer provider for a given book.
 *
 * @see [ViewerProviderType]
 */

object Viewers {

  private val logger = LoggerFactory.getLogger(Viewers::class.java)

  /**
   * Attempt to open a viewer for a given book.
   */

  fun openViewer(
    activity: Activity,
    preferences: ViewerPreferences,
    book: Book,
    format: BookFormat
  ) {
    this.logger.debug("attempting to open: {} ({})", book.id, book.entry.title)
    val providers =
      ServiceLoader.load(ViewerProviderType::class.java)
        .toList()

    this.logger.debug("{} viewer providers available", providers.size)

    for (index in providers.indices) {
      val viewerProvider = providers[index]
      this.logger.debug("[{}]: {}", index, viewerProvider.name)
    }

    this.logger.debug("trying all providers...")
    for (index in providers.indices) {
      val viewerProvider = providers[index]
      val supported = viewerProvider.canSupport(preferences, book, format)
      if (supported) {
        this.logger.debug(
          "[{}] viewer provider {} supports the book, using it!", index, viewerProvider.name)
        viewerProvider.open(activity, preferences, book, format)
        return
      } else {
        this.logger.debug(
          "[{}] viewer provider {} does not support the book", index, viewerProvider.name)
      }
    }
    this.logger.error("no viewer providers can handle the given book")
  }
}
