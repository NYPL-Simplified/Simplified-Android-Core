package org.nypl.simplified.tests.local.books

import com.google.common.util.concurrent.ListeningExecutorService
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import org.nypl.simplified.books.core.BookDatabaseEntrySnapshot
import org.nypl.simplified.books.core.BookDatabaseReadableType
import org.nypl.simplified.books.core.BookFormats
import org.nypl.simplified.books.core.BookID
import org.nypl.simplified.books.core.FeedLoader
import org.nypl.simplified.books.core.FeedLoaderType
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSFeedTransportType
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.tests.books.FeedLoaderContract
import java.net.URI

class FeedLoaderTest : FeedLoaderContract() {

  override fun createFeedLoader(exec: ListeningExecutorService): FeedLoaderType {

    val database = object: BookDatabaseReadableType {
      override fun databaseGetEntrySnapshot(book: BookID): OptionType<BookDatabaseEntrySnapshot> {
        return Option.none()
      }
      override fun databaseGetBooks(): Set<BookID> {
        return setOf()
      }
    }

    val entryParser =
      OPDSAcquisitionFeedEntryParser.newParser(BookFormats.supportedBookMimeTypes())
    val parser =
      OPDSFeedParser.newParser(entryParser)
    val transport = OPDSFeedTransportType<OptionType<HTTPAuthType>> {
      context, uri, method -> uri.toURL().openStream()
    }

    val search = OPDSSearchParser.newParser()
    return FeedLoader.newFeedLoader(exec, database, parser, transport, search)
  }

  override fun resource(name: String): URI {
    return FeedLoaderContract::class.java.getResource(name).toURI()
  }

}
