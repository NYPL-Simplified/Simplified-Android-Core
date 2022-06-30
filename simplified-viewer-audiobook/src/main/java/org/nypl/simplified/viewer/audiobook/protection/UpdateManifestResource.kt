package org.nypl.simplified.viewer.audiobook.protection

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.shared.fetcher.FailureResource
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.ResourceTry
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.tryRecover

internal class UpdateManifestResource(
  private val index: Int,
  private val fallbackLink: Link,
  private val baseFetcher: Fetcher,
  private val getManifest: suspend () -> Try<Manifest, Exception>,
  private val invalidateManifest: suspend () -> Unit
) : Resource {

  private var baseResource: Resource? = null

  private val mutex: Mutex = Mutex()

  private suspend fun getBaseResource(): Resource =
    baseResource ?: run {
      getManifest().fold(
        onSuccess = {
          baseFetcher.get(it.readingOrder[index].href)
        },
        onFailure = {
          val exception = Resource.Exception.Other(Exception("Couldn't get a fresh manifest."))
          FailureResource(fallbackLink, exception)
        }
      ).also { baseResource = it }
    }

  private suspend fun<S> invalidateManifestOnFailure(
    runnable: suspend () -> ResourceTry<S>
  ): ResourceTry<S> =
    runnable().tryRecover {
      invalidateManifest()
      runnable()
    }

  override suspend fun link(): Link = mutex.withLock {
    getBaseResource().link()
  }

  override suspend fun length(): ResourceTry<Long> = mutex.withLock {
    invalidateManifestOnFailure { getBaseResource().length() }
  }

  override suspend fun read(range: LongRange?): ResourceTry<ByteArray> = mutex.withLock {
    invalidateManifestOnFailure { getBaseResource().read(range) }
  }

  override suspend fun close() = mutex.withLock {
    baseResource?.close()
    Unit
  }
}
