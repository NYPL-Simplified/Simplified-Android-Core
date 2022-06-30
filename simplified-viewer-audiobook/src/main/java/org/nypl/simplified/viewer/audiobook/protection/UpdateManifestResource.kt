package org.nypl.simplified.viewer.audiobook.protection

import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.ResourceTry
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.tryRecover

internal class UpdateManifestResource(
  private val index: Int,
  private val fallbackLink: Link,
  private val baseFetcher: Fetcher,
  private val getManifest: suspend () -> Try<Manifest, Exception>,
  private val invalidateManifest: suspend () -> Unit
) : Resource {

  private suspend fun getBaseResource(): ResourceTry<Resource> =
    getManifest().map {
      baseFetcher.get(it.readingOrder[index].href)
    }.mapFailure {
      Resource.Exception.Other(Exception("Couldn't get a fresh manifest."))
    }

  private suspend fun<S> invalidateManifestOnFailure(
    runnable: suspend () -> ResourceTry<S>
  ): ResourceTry<S> =
    runnable().tryRecover {
      invalidateManifest()
      runnable()
    }

  override suspend fun link(): Link =
    getBaseResource().fold(
      onSuccess = { it.link() },
      onFailure = { fallbackLink }
    )

  override suspend fun length(): ResourceTry<Long> =
    invalidateManifestOnFailure { getBaseResource().flatMap {  it.length() } }

  override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
    invalidateManifestOnFailure { getBaseResource().flatMap { it.read() } }

  override suspend fun close() {}
}
