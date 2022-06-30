package org.nypl.simplified.viewer.audiobook.protection

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.shared.fetcher.FailureResource
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.util.Try

internal class UpdateManifestFetcher(
  private val childFetcher: Fetcher,
  private val initialManifest: Manifest,
  private val downloadManifest: suspend () -> Try<Manifest, Exception>
) : Fetcher {

  private var manifest: Try<Manifest, Exception>? = Try.success(initialManifest)

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
    initialManifest.readingOrder

  override fun get(link: Link): Resource {
    if (!link.expires) {
      return childFetcher.get(link)
    }

    val index = link.readingOrderIndex
      ?: return failureResource(link)

    return UpdateManifestResource(
      index,
      link,
      childFetcher,
      ::getManifest,
      ::invalidateManifest
    )
  }

  override suspend fun close() {
    childFetcher.close()
  }

  private fun failureResource(link: Link): FailureResource =
    FailureResource(
      link,
      Resource.Exception.NotFound(
        IllegalStateException("Expiring link with no readingOrder index.")
      )
    )

  private val Link.readingOrderIndex: Int?
    get() = properties["readingOrderIndex"] as? Int

  private val Link.expires: Boolean
    get() = (properties["expires"] as? Boolean) ?: false

  companion object {

    fun adaptReadingOrder(links: List<Link>): List<Link> {
      return links.mapIndexed { index, link ->
        val additionalProperties = mapOf("readingOrderIndex" to index)
        link.addProperties(additionalProperties)
      }
    }
  }
}
