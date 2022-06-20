package org.nypl.simplified.viewer.audiobook.protection

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.shared.fetcher.FailureResource
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrDefault

class UpdateManifestFetcher(
  private val childFetcher: Fetcher,
  private val downloadManifest: suspend () -> Try<Manifest, Exception>
) : Fetcher {

  private var manifest: Try<Manifest, Exception>? = null

  private val mutex = Mutex()

  private suspend fun getManifest(): Try<Manifest, Exception> = mutex.withLock {
    manifest
      ?: downloadManifest()
        .also { manifest = it }
  }

  private suspend fun invalidateManifest(): Unit = mutex.withLock {
    manifest = null
  }

  override suspend fun links(): List<Link> =
    getManifest()
      .map { it.readingOrder }
      .getOrDefault(emptyList())
      .mapIndexed { index, link -> link.copy(href = "/chapter$index") }


  override fun get(link: Link): Resource {
    return if (link.properties["expires"] == true) {
      val index = link.href.substringAfter("chapter").toIntOrNull()
        ?: return failureResource(link)

      UpdateManifestResource(
        index,
        link,
        childFetcher,
        ::getManifest,
        ::invalidateManifest
      )
    } else
      childFetcher.get(link)
  }

  override suspend fun close() {
    childFetcher.close()
  }

  private fun failureResource(link: Link): FailureResource =
    FailureResource(
      link,
      Resource.Exception.NotFound(
        IllegalStateException("Unexpected href ${link.href}")
      )
    )
}
