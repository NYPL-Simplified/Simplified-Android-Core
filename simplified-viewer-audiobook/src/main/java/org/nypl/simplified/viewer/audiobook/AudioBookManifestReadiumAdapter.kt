package org.nypl.simplified.viewer.audiobook

import org.librarysimplified.audiobook.feedbooks.FeedbooksPlayerExtensionConfiguration
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLink
import org.nypl.simplified.books.audio.AudioBookManifestStrategyType
import org.nypl.simplified.viewer.audiobook.protection.FeedbooksHttpClientFactory
import org.nypl.simplified.viewer.audiobook.protection.UpdateManifestFetcher
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.HttpFetcher
import org.readium.r2.shared.fetcher.RoutingFetcher
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.publication.encryption.encryption
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.http.DefaultHttpClient

internal class AudioBookManifestReadiumAdapter(
  private val feedbooksConfiguration: FeedbooksPlayerExtensionConfiguration?
) {

  fun createPublication(playerManifest: PlayerManifest, strategy: AudioBookManifestStrategyType): Publication {
    val manifest =
      mapManifest(playerManifest)

  val fetcher =
    createFetcher(playerManifest, strategy)

    return Publication.Builder(
      manifest = manifest,
      fetcher = fetcher
    ).build()
  }

  private fun createFetcher(playerManifest: PlayerManifest, strategy: AudioBookManifestStrategyType): Fetcher {
    val fallbackRoute =
      RoutingFetcher.Route(
        HttpFetcher(
          client = DefaultHttpClient()
        )
      )  { true }


    val feedbooksRoute = feedbooksConfiguration?.let {
      RoutingFetcher.Route(
        HttpFetcher(
          client = FeedbooksHttpClientFactory(it).createHttpClient()
        )
      ) { link -> link.properties.encryption?.scheme == feedbooksConfiguration.encryptionScheme } }


    val routingFetcher = RoutingFetcher(
      listOfNotNull(
        feedbooksRoute,
        fallbackRoute
      )
    )

    /*return if (playerManifest.readingOrder.any { it.expires }) {
      UpdateManifestFetcher(
        routingFetcher,
        downloadManifest
      )
    } else
      routingFetcher*/

    return routingFetcher
  }

  private suspend fun downloadManifest(strategy: AudioBookManifestStrategyType) {

  }

  private fun mapManifest(playerManifest: PlayerManifest): Manifest {
    val readingOrder = playerManifest.readingOrder
      .filterIsInstance(PlayerManifestLink.LinkBasic::class.java)
      .map { it.toLink() }

    return Manifest(
      metadata = Metadata(
        identifier = playerManifest.metadata.identifier,
        localizedTitle = LocalizedString(playerManifest.metadata.title),
      ),
      readingOrder = readingOrder,
      tableOfContents = readingOrder
    )
  }

  private fun PlayerManifestLink.toLink(): Link {
    val encryption = properties.encrypted?.let {
      Encryption(
        scheme = it.scheme,
        algorithm = "dummy" //FIXME: algorithm is mandatory in RWPM, but not in PlayerManifest
      )
    }

    var properties = Properties(mapOf("expires" to expires))
    encryption?.let {
      properties = properties.add(mapOf("encrypted" to it.toJSON().toMap()))
    }

    return Link(
      title = title,
      href = Href(hrefURI.toString()).string,
      duration = duration,
      type = type?.toString(),
      properties = properties
    )
  }
}
