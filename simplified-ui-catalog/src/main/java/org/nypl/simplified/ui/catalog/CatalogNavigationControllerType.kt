package org.nypl.simplified.ui.catalog

import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.host.HostNavigationControllerType

/**
 * Navigation functions for the catalog screens.
 */

interface CatalogNavigationControllerType : HostNavigationControllerType {

  /**
   * A catalog screen wants to pop the current screen from the stack.
   */

  fun popBackStack()

  /**
   * The catalog wants to open a book detail page.
   */

  fun openBookDetail(entry: FeedEntryOPDS)

  /**
   * A catalog screen wants to open the error page.
   */

  fun <E : PresentableErrorType> openErrorPage(
    parameters: ErrorPageParameters<E>)

  /**
   * A catalog screen wants to open a feed.
   */

  fun openFeed(feedArguments: CatalogFeedArguments)

  /**
   * A catalog screen wants to open an EPUB reader.
   */

  fun openEPUBReader(
    book: Book,
    format: BookFormat.BookFormatEPUB
  )

  /**
   * A catalog screen wants to open an AudioBook listener.
   */

  fun openAudioBookListener(
    book: Book,
    format: BookFormat.BookFormatAudioBook
  )

  /**
   * A catalog screen wants to open a PDF reader.
   */

  fun openPDFReader(
    book: Book,
    format: BookFormat.BookFormatPDF
  )
}
