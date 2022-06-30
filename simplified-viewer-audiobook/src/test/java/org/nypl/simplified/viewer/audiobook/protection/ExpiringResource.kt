package org.nypl.simplified.viewer.audiobook.protection

import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.ResourceTry
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Try

class ExpiringResource(
  val link: Link,
  private val content: ByteArray,
  private val validCalls: Int
) : Resource {

  private var callCount = 0

  override suspend fun link(): Link = link

  override suspend fun length(): ResourceTry<Long> {
    callCount++

    return if (callCount > validCalls) {
      Try.failure(Resource.Exception.NotFound())
    } else {
      Try.success(content.size.toLong())
    }
  }


  override suspend fun read(range: LongRange?): ResourceTry<ByteArray> {
    callCount++

    return if (callCount > validCalls) {
      Try.failure(Resource.Exception.NotFound())
    } else {
      Try.success(content)
    }
  }

  override suspend fun close() {}
}
