package org.nypl.simplified.viewer.audiobook.protection

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.readium.r2.shared.fetcher.FailureResource
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.util.Try

class UpdateManifestResourceTest {

  private var manifestCallCount: Int = 0

  @BeforeEach
  fun testSetup() {
    manifestCallCount = 0
  }

  @AfterEach
  fun tearDown() {
  }

  @Test
  fun `expired links are dealt with correctly`() {
    val metadata = Metadata(
      localizedTitle = LocalizedString("titleValue")
    )

    val originalManifest = Manifest(
      metadata = metadata,
      readingOrder = UpdateManifestFetcher.adaptReadingOrder(
        listOf(
          Link("chapter1"),
          Link("chapter2"),
          Link("chapter3")
        )
      )
    )

    val newManifest = Manifest(
      metadata = metadata,
      readingOrder = UpdateManifestFetcher.adaptReadingOrder(
        listOf(
          Link("newLinkToChapter1"),
          Link("newLinkToChapter2"),
          Link("newLinkToChapter3")
        )
      )
    )

    val baseFetcher = object: Fetcher {

      val resourceContent = "Good".toByteArray(Charsets.UTF_8)

      val firstResource = ExpiringResource(
        originalManifest.readingOrder.first(),
        resourceContent,
        3
      )

      val secondResource = ExpiringResource(
        newManifest.readingOrder.first(),
        resourceContent,
        3
      )

      private fun failureResource(link: Link) =
        FailureResource(link, Resource.Exception.NotFound())

      override suspend fun links(): List<Link> =
        originalManifest.readingOrder.subList(0, 1)

      override fun get(link: Link): Resource =
        when (link.href) {
          firstResource.link.href -> firstResource
          secondResource.link.href -> secondResource
          else -> failureResource(link)
      }

      override suspend fun close() {}
    }

    val getManifest = {
      manifestCallCount +=1

      when (manifestCallCount) {
        1 -> Try.success(originalManifest)
        2 -> Try.success(newManifest)
        else -> Try.Failure(Exception("Cannot get a fresh manifest"))
      }
    }

    val updateResource = UpdateManifestResource(
      index = 0,
      fallbackLink = Link("chapter1"),
      baseFetcher = baseFetcher,
      getManifest = getManifest,
      invalidateManifest = {}
    )

    for (i in 0 until 6) {
      val response = runBlocking { updateResource.readAsString().getOrNull() }
      Assert.assertEquals( "Good", response)
    }
  }
}
