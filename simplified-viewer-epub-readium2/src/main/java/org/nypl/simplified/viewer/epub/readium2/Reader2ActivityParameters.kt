package org.nypl.simplified.viewer.epub.readium2

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.FeedEntry
import java.io.File
import java.io.Serializable

data class Reader2ActivityParameters(
  val accountId: AccountID,
  val bookId: BookID,
  val file: File,
  val drmInfo: BookDRMInformation,
  val entry: FeedEntry.FeedEntryOPDS
) : Serializable
