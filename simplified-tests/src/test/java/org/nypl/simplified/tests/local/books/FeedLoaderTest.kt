package org.nypl.simplified.tests.local.books

import com.google.common.util.concurrent.ListeningExecutorService
import com.io7m.jfunctional.OptionType
import org.nypl.simplified.books.book_database.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.bundled_content.BundledContentResolverType
import org.nypl.simplified.books.feeds.FeedLoader
import org.nypl.simplified.books.feeds.FeedLoaderType
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSFeedTransportType
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.tests.books.FeedLoaderContract
import java.io.FileNotFoundException
import java.net.URI

class FeedLoaderTest : FeedLoaderContract() {

  override fun createFeedLoader(exec: ListeningExecutorService): FeedLoaderType {

    val entryParser =
      OPDSAcquisitionFeedEntryParser.newParser(BookFormats.supportedBookMimeTypes())
    val parser =
      OPDSFeedParser.newParser(entryParser)
    val transport = OPDSFeedTransportType<OptionType<HTTPAuthType>> {
      context, uri, method -> uri.toURL().openStream()
    }

    val searchParser = OPDSSearchParser.newParser()
    val bookRegistry = BookRegistry.create()
    val bundledContent = BundledContentResolverType {
      uri -> throw FileNotFoundException(uri.toASCIIString())
    }

    return FeedLoader.create(exec, parser, searchParser, transport, bookRegistry, bundledContent)
  }

  override fun resource(name: String): URI {
    return FeedLoaderContract::class.java.getResource(name).toURI()
  }

}
