package org.nypl.simplified.tests.books.audio

import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLink
import org.librarysimplified.audiobook.manifest.api.PlayerManifestMetadata
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParsersType
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType
import org.librarysimplified.audiobook.parser.api.ParseResult
import java.net.URI

object AudioBookSucceedingParsers : ManifestParsersType {

  val playerManifest =
    PlayerManifest(
      originalBytes = ByteArray(23),
      readingOrder = listOf(PlayerManifestLink.LinkBasic(URI.create("http://www.example.com"))),
      metadata = PlayerManifestMetadata(
        title = "A book",
        identifier = "c925eb26-ab0c-44e2-9bec-ca4c38c0b6c8",
        encrypted = null
      ),
      links = listOf(),
      extensions = listOf()
    )

  override fun parse(
    uri: URI,
    streams: ByteArray
  ): ParseResult<PlayerManifest> {
    return ParseResult.Success(
      warnings = listOf(),
      result = playerManifest
    )
  }

  override fun parse(
    uri: URI,
    streams: ByteArray,
    extensions: List<ManifestParserExtensionType>
  ): ParseResult<PlayerManifest> {
    return parse(uri, streams)
  }
}
