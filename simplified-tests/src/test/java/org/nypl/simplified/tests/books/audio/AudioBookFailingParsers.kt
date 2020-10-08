package org.nypl.simplified.tests.books.audio

import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParsersType
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType
import org.librarysimplified.audiobook.parser.api.ParseError
import org.librarysimplified.audiobook.parser.api.ParseResult
import org.librarysimplified.audiobook.parser.api.ParseWarning
import java.io.IOException
import java.net.URI

object AudioBookFailingParsers : ManifestParsersType {

  override fun parse(
    uri: URI,
    streams: ByteArray
  ): ParseResult<PlayerManifest> {
    return ParseResult.Failure(
      warnings = listOf(
        ParseWarning(uri, "Warning 0", 0, 0, exception = IOException()),
        ParseWarning(uri, "Warning 1", 0, 0, exception = IOException()),
        ParseWarning(uri, "Warning 2", 0, 0, exception = IOException())
      ),
      errors = listOf(
        ParseError(uri, "Error 0", 0, 0, exception = IOException()),
        ParseError(uri, "Error 1", 0, 0, exception = IOException()),
        ParseError(uri, "Error 2", 0, 0, exception = IOException())
      ),
      result = null
    )
  }

  override fun parse(
    uri: URI,
    streams: ByteArray,
    extensions: List<ManifestParserExtensionType>
  ): ParseResult<PlayerManifest> {
    return ParseResult.Failure(
      warnings = listOf(
        ParseWarning(uri, "Warning 0", 0, 0, exception = IOException()),
        ParseWarning(uri, "Warning 1", 0, 0, exception = IOException()),
        ParseWarning(uri, "Warning 2", 0, 0, exception = IOException())
      ),
      errors = listOf(
        ParseError(uri, "Error 0", 0, 0, exception = IOException()),
        ParseError(uri, "Error 1", 0, 0, exception = IOException()),
        ParseError(uri, "Error 2", 0, 0, exception = IOException())
      ),
      result = null
    )
  }
}
