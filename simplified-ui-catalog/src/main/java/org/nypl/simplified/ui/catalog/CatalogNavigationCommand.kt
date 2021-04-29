package org.nypl.simplified.ui.catalog

import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.FeedEntry
import java.net.URI

sealed class CatalogNavigationCommand {

  /**
   * The catalog wants to open a book detail page.
   */

  class OpenBookDetail(
    val feedArguments: CatalogFeedArguments,
    val entry: FeedEntry.FeedEntryOPDS
  ) : CatalogNavigationCommand()

  /**
   * The catalog wants to open an external login form for downloading a book.
   */

  class OpenBookDownloadLogin(
    val bookID: BookID,
    val downloadURI: URI
  ) : CatalogNavigationCommand()

  /**
   * A catalog screen wants to open a feed.
   */

  class OpenFeed(
    val feedArguments: CatalogFeedArguments
  ) : CatalogNavigationCommand()

  /**
   * A catalog screen wants to open a viewer for a book
   */

  class OpenViewer(
    val book: Book,
    val format: BookFormat
  ) : CatalogNavigationCommand()

  object OnSAML20LoginSucceeded : CatalogNavigationCommand()
}
