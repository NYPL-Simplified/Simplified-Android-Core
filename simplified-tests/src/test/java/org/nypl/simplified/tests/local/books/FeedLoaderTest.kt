package org.nypl.simplified.tests.local.books

import android.content.ContentResolver
import com.google.common.util.concurrent.ListeningExecutorService
import com.io7m.jfunctional.OptionType
import org.mockito.Mockito
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSFeedTransportType
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.tests.books.FeedLoaderContract
import java.io.FileNotFoundException
import java.net.URI

class FeedLoaderTest : FeedLoaderContract() {

  override fun createFeedLoader(exec: ListeningExecutorService): org.nypl.simplified.feeds.api.FeedLoaderType {

    val entryParser =
      OPDSAcquisitionFeedEntryParser.newParser(BookFormats.supportedBookMimeTypes())
    val parser =
      OPDSFeedParser.newParser(entryParser)
    val transport = OPDSFeedTransportType<OptionType<HTTPAuthType>> { context, uri, method ->
      uri.toURL().openStream()
    }

    val searchParser = OPDSSearchParser.newParser()
    val bookRegistry = BookRegistry.create()
    val bundledContent = org.nypl.simplified.books.bundled.api.BundledContentResolverType { uri ->
      throw FileNotFoundException(uri.toASCIIString())
    }

    val contentResolver = Mockito.mock(ContentResolver::class.java)
    return FeedLoader.create(
      contentResolver = contentResolver,
      exec = exec,
      parser = parser,
      searchParser = searchParser,
      transport = transport,
      bookRegistry = bookRegistry,
      bundledContent = bundledContent
    )
  }

  override fun resource(name: String): URI {
    return FeedLoaderContract::class.java.getResource(name).toURI()
  }
}
