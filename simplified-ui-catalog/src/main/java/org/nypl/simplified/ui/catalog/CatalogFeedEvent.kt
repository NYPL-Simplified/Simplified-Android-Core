package org.nypl.simplified.ui.catalog

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import java.net.URI

sealed class CatalogFeedEvent {

  data class OpenErrorPage(
    val parameters: ErrorPageParameters
  ) : CatalogFeedEvent()

  data class LoginRequired(
    val account: AccountID
  ) : CatalogFeedEvent()

  data class OpenFeed(
    val feedArguments: CatalogFeedArguments
  ) : CatalogFeedEvent()

  data class OpenBookDetail(
    val feedArguments: CatalogFeedArguments,
    val opdsEntry: FeedEntry.FeedEntryOPDS
  ) : CatalogFeedEvent()

  data class OpenViewer(
    val book: Book,
    val format: BookFormat
  ) : CatalogFeedEvent()

  data class DownloadWaitingForExternalAuthentication(
    val bookID: BookID,
    val downloadURI: URI
  ) : CatalogFeedEvent()
}
