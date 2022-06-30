package org.nypl.simplified.viewer.audiobook.session

import org.librarysimplified.audiobook.feedbooks.FeedbooksPlayerExtensionConfiguration
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLink
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.viewer.audiobook.AudioBookPlayerParameters
import org.nypl.simplified.viewer.audiobook.protection.FeedbooksHttpClientFactory
import org.nypl.simplified.viewer.audiobook.protection.UpdateManifestFetcher
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.HttpFetcher
import org.readium.r2.shared.fetcher.RoutingFetcher
import org.readium.r2.shared.publication.Contributor
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

internal class PublicationAdapter(
  private val manifestDownloader: ManifestDownloader,
  private val feedbooksConfiguration: FeedbooksPlayerExtensionConfiguration?
) {

  fun createPublication(
    playerManifest: PlayerManifest,
    parameters: AudioBookPlayerParameters,
    credentials: AccountAuthenticationCredentials?
  ): Publication {

    val builder = when {
      playerManifest.readingOrder.any { it.expires } ->
        createOverdrivePublication(playerManifest, parameters, credentials)
      else ->
        createRegularPublication(playerManifest, parameters)
    }

    return builder.build()
  }

  private fun createOverdrivePublication(
    playerManifest: PlayerManifest,
    parameters: AudioBookPlayerParameters,
    credentials: AccountAuthenticationCredentials?
  ): Publication.Builder {
    val manifest =
      createOverdriveManifest(playerManifest, parameters.opdsEntry)
    return Publication.Builder(
      manifest = manifest,
      fetcher = createOverdriveFetcher(manifest, parameters, credentials)
    )
  }

  private fun createOverdriveManifest(
    playerManifest: PlayerManifest,
    opdsEntry: OPDSAcquisitionFeedEntry
  ): Manifest {
    val readingOrder = playerManifest.readingOrder
      .filterIsInstance(PlayerManifestLink.LinkBasic::class.java)
      .map { it.toLink() }

    val adaptedReadingOrder = UpdateManifestFetcher.adaptReadingOrder(readingOrder)

    return Manifest(
      metadata = createMetadata(playerManifest, opdsEntry),
      readingOrder = adaptedReadingOrder,
      tableOfContents = adaptedReadingOrder
    )
  }

  private fun createOverdriveFetcher(
    manifest: Manifest,
    parameters: AudioBookPlayerParameters,
    credentials: AccountAuthenticationCredentials?
  ): Fetcher {
    return UpdateManifestFetcher(
      HttpFetcher(
        client = DefaultHttpClient()
      ),
      manifest
    ) {
      manifestDownloader.downloadManifest(parameters, credentials)
        .map { createOverdriveManifest(it, parameters.opdsEntry) }
    }
  }

  private fun createRegularPublication(
    playerManifest: PlayerManifest,
    parameters: AudioBookPlayerParameters,
  ): Publication.Builder {
    return Publication.Builder(
      manifest = createRegularManifest(playerManifest, parameters.opdsEntry),
      fetcher = createRegularFetcher()
    )
  }

  private fun createRegularManifest(
    playerManifest: PlayerManifest,
    opdsEntry: OPDSAcquisitionFeedEntry
  ): Manifest {
    val readingOrder = playerManifest.readingOrder
      .filterIsInstance(PlayerManifestLink.LinkBasic::class.java)
      .map { it.toLink() }

    return Manifest(
      metadata = createMetadata(playerManifest, opdsEntry),
      readingOrder = readingOrder,
      tableOfContents = readingOrder
    )
  }

  private fun createRegularFetcher(): Fetcher {
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

    return RoutingFetcher(
      listOfNotNull(
        feedbooksRoute,
        fallbackRoute
      )
    )
  }

  private fun createMetadata(
    playerManifest: PlayerManifest,
    opdsEntry: OPDSAcquisitionFeedEntry
  ): Metadata {
    val authors = opdsEntry.authors
      .map { Contributor(it) }

    return Metadata(
      identifier = playerManifest.metadata.identifier,
      localizedTitle = LocalizedString(opdsEntry.title),
      authors = authors
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
