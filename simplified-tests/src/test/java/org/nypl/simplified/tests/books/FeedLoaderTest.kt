package org.nypl.simplified.tests.books

import com.google.common.util.concurrent.ListeningExecutorService
import org.librarysimplified.http.api.LSHTTPAuthorizationType
import org.mockito.Mockito
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.formats.BookFormatSupport
import org.nypl.simplified.books.formats.BookFormatSupportParameters
import org.nypl.simplified.content.api.ContentResolverType
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSFeedTransportType
import org.nypl.simplified.opds.core.OPDSSearchParser
import java.io.FileNotFoundException
import java.net.URI

class FeedLoaderTest : FeedLoaderContract() {

  override fun createFeedLoader(exec: ListeningExecutorService): FeedLoaderType {
    val entryParser =
      OPDSAcquisitionFeedEntryParser.newParser()
    val parser =
      OPDSFeedParser.newParser(entryParser)
    val transport =
      OPDSFeedTransportType<LSHTTPAuthorizationType?> { context, uri, method ->
        uri.toURL().openStream()
      }

    val searchParser = OPDSSearchParser.newParser()
    val bookRegistry = BookRegistry.create()
    val bundledContent = BundledContentResolverType { uri ->
      throw FileNotFoundException(uri.toASCIIString())
    }

    val bookFormatSupport =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsAdobeDRM = false,
          supportsAxisNow = false,
          supportsAudioBooks = null
        )
      )

    val contentResolver =
      Mockito.mock(ContentResolverType::class.java)

    return FeedLoader.create(
      bookFormatSupport = bookFormatSupport,
      bundledContent = bundledContent,
      contentResolver = contentResolver,
      exec = exec,
      parser = parser,
      searchParser = searchParser,
      transport = transport
    )
  }

  override fun resource(name: String): URI {
    return FeedLoaderContract::class.java.getResource(name)!!.toURI()
  }
}
