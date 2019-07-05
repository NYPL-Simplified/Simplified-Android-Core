package org.nypl.simplified.tests.books.accounts

import android.content.Context
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.nypl.simplified.accounts.api.AccountProviderDescriptionMetadata
import org.nypl.simplified.accounts.api.AccountProviderDescriptionType
import org.nypl.simplified.accounts.api.AccountProviderResolutionListenerType
import org.nypl.simplified.accounts.api.AccountProviderResolutionResult
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent.SourceFailed
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent.Updated
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType.SourceResult
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.tests.MockAccountProviders
import org.slf4j.Logger
import java.net.URI

abstract class AccountProviderDescriptionRegistryContract {

  private lateinit var events: MutableList<org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent>

  protected abstract val logger: Logger

  protected abstract val context: Context

  protected abstract fun createRegistry(
    defaultProvider: AccountProviderType,
    sources: List<AccountProviderSourceType>): org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType

  @JvmField
  @Rule
  val expectedException: ExpectedException = ExpectedException.none()

  @Before
  fun testSetup() {
    this.events = mutableListOf()
  }

  /**
   * An empty registry contains nothing.
   */

  @Test
  fun testEmpty() {
    val registry =
      this.createRegistry(
        MockAccountProviders.fakeProvider("urn:fake:0"),
        listOf())

    registry.events.subscribe { e -> this.events.add(e) }

    val notFound =
      registry.findAccountProviderDescription(
        URI.create("urn:uuid:6ba13d1e-c790-4247-9c80-067c6a7257f0"))

    Assert.assertEquals(null, notFound)
  }

  /**
   * A crashing source raises the right events.
   */

  @Test
  fun testCrashingSource() {
    val registry =
      this.createRegistry(
        MockAccountProviders.fakeProvider("urn:fake:0"),
        listOf(CrashingSource()))

    registry.events.subscribe { e -> this.events.add(e) }
    registry.refresh()

    Assert.assertEquals(1, this.events.size)
    val event = this.events[0] as SourceFailed
    Assert.assertEquals(CrashingSource::class.java, event.clazz)
  }

  /**
   * Refreshing with a usable source works.
   */

  @Test
  fun testRefresh() {
    val registry =
      this.createRegistry(
        MockAccountProviders.fakeProvider("urn:fake:0"),
        listOf(OKSource()))

    registry.events.subscribe { this.events.add(it) }
    registry.refresh()

    val description0 =
      registry.findAccountProviderDescription(URI.create("urn:0"))
    val description1 =
      registry.findAccountProviderDescription(URI.create("urn:1"))
    val description2 =
      registry.findAccountProviderDescription(URI.create("urn:2"))

    Assert.assertEquals(URI.create("urn:0"), description0!!.metadata.id)
    Assert.assertEquals(URI.create("urn:1"), description1!!.metadata.id)
    Assert.assertEquals(URI.create("urn:2"), description2!!.metadata.id)

    Assert.assertEquals(3, this.events.size)
    Assert.assertEquals(URI.create("urn:0"), (this.events[0] as Updated).id)
    Assert.assertEquals(URI.create("urn:1"), (this.events[1] as Updated).id)
    Assert.assertEquals(URI.create("urn:2"), (this.events[2] as Updated).id)
  }

  /**
   * Refreshing with a source that provides outdated definitions results in the outdated
   * definitions being ignored.
   */

  @Test
  fun testRefreshIgnoreOld() {
    val registry =
      this.createRegistry(
        MockAccountProviders.fakeProvider("urn:fake:0"),
        listOf(OKSource(), OKAncientSource()))

    registry.events.subscribe { this.events.add(it) }
    registry.refresh()

    val description0 =
      registry.findAccountProviderDescription(URI.create("urn:0"))
    val description1 =
      registry.findAccountProviderDescription(URI.create("urn:1"))
    val description2 =
      registry.findAccountProviderDescription(URI.create("urn:2"))

    Assert.assertEquals(URI.create("urn:0"), description0!!.metadata.id)
    Assert.assertEquals(URI.create("urn:1"), description1!!.metadata.id)
    Assert.assertEquals(URI.create("urn:2"), description2!!.metadata.id)

    Assert.assertNotEquals(
      DateTime.parse("1900-01-01T00:00:00Z"), description0.metadata.updated)
    Assert.assertNotEquals(
      DateTime.parse("1900-01-01T00:00:00Z"), description1.metadata.updated)
    Assert.assertNotEquals(
      DateTime.parse("1900-01-01T00:00:00Z"), description2.metadata.updated)

    Assert.assertEquals(3, this.events.size)
    Assert.assertEquals(URI.create("urn:0"), (this.events[0] as Updated).id)
    Assert.assertEquals(URI.create("urn:1"), (this.events[1] as Updated).id)
    Assert.assertEquals(URI.create("urn:2"), (this.events[2] as Updated).id)
  }

  /**
   * Even if a source crashes, the working sources are used.
   */

  @Test
  fun testRefreshWithCrashing() {
    val registry =
      this.createRegistry(
        MockAccountProviders.fakeProvider("urn:fake:0"),
        listOf(OKSource(), CrashingSource()))

    registry.events.subscribe { this.events.add(it) }
    registry.refresh()

    val description0 =
      registry.findAccountProviderDescription(URI.create("urn:0"))
    val description1 =
      registry.findAccountProviderDescription(URI.create("urn:1"))
    val description2 =
      registry.findAccountProviderDescription(URI.create("urn:2"))

    Assert.assertEquals(URI.create("urn:0"), description0!!.metadata.id)
    Assert.assertEquals(URI.create("urn:1"), description1!!.metadata.id)
    Assert.assertEquals(URI.create("urn:2"), description2!!.metadata.id)

    Assert.assertEquals(4, this.events.size)
    Assert.assertEquals(URI.create("urn:0"), (this.events[0] as Updated).id)
    Assert.assertEquals(URI.create("urn:1"), (this.events[1] as Updated).id)
    Assert.assertEquals(URI.create("urn:2"), (this.events[2] as Updated).id)
    Assert.assertEquals(CrashingSource::class.java, (this.events[3] as SourceFailed).clazz)
  }

  /**
   * Even if a source fails, the working sources are used.
   */

  @Test
  fun testRefreshWithFailing() {
    val registry =
      this.createRegistry(
        MockAccountProviders.fakeProvider("urn:fake:0"),
        listOf(OKSource(), FailingSource()))

    registry.events.subscribe { this.events.add(it) }
    registry.refresh()

    val description0 =
      registry.findAccountProviderDescription(URI.create("urn:0"))
    val description1 =
      registry.findAccountProviderDescription(URI.create("urn:1"))
    val description2 =
      registry.findAccountProviderDescription(URI.create("urn:2"))

    Assert.assertEquals(URI.create("urn:0"), description0!!.metadata.id)
    Assert.assertEquals(URI.create("urn:1"), description1!!.metadata.id)
    Assert.assertEquals(URI.create("urn:2"), description2!!.metadata.id)

    Assert.assertEquals(4, this.events.size)
    Assert.assertEquals(URI.create("urn:0"), (this.events[0] as Updated).id)
    Assert.assertEquals(URI.create("urn:1"), (this.events[1] as Updated).id)
    Assert.assertEquals(URI.create("urn:2"), (this.events[2] as Updated).id)
    Assert.assertEquals(FailingSource::class.java, (this.events[3] as SourceFailed).clazz)
  }

  /**
   * Trying to update with an outdated description returns the newer description.
   */

  @Test
  fun testUpdateIgnoreOld() {
    val registry =
      this.createRegistry(
        MockAccountProviders.fakeProvider("urn:fake:0"),
        listOf(OKSource(), OKAncientSource()))

    registry.events.subscribe { this.events.add(it) }
    registry.refresh()

    val existing0 =
      registry.findAccountProviderDescription(URI.create("urn:0"))!!

    val changed =
      registry.updateDescription(existing0)

    Assert.assertEquals(existing0, changed)
    Assert.assertEquals(existing0, registry.accountProviderDescriptions()[existing0.metadata.id])
  }

  /**
   * Trying to update with an outdated provider returns the newer provider.
   */

  @Test
  fun testUpdateIgnoreProviderOld() {
    val registry =
      this.createRegistry(
        MockAccountProviders.fakeProvider("urn:fake:0"),
        listOf())

    registry.events.subscribe { this.events.add(it) }
    registry.refresh()

    val existing0 =
      MockAccountProviders.fakeProvider("urn:fake:0")

    val older0 =
      MockAccountProviders.fakeProvider("urn:fake:0")
        .copy(updated = DateTime.parse("1900-01-01T00:00:00Z"))

    val initial =
      registry.updateProvider(existing0)
    val changed =
      registry.updateProvider(older0)

    Assert.assertEquals(existing0, initial)
    Assert.assertEquals(existing0, changed)
    Assert.assertEquals(registry.resolvedProviders[existing0.id], existing0)
  }

  companion object {

    val descriptionMeta0 =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:0"),
        title = "Title 0",
        updated = DateTime.now(),
        links = listOf(),
        images = listOf(),
        isAutomatic = false,
        isProduction = true)

    val description0 =
      object : AccountProviderDescriptionType {
        override val metadata: AccountProviderDescriptionMetadata = descriptionMeta0

        override fun resolve(onProgress: AccountProviderResolutionListenerType): AccountProviderResolutionResult {
          return AccountProviderResolutionResult(null, listOf(TaskStep(description = "x", failed = true)))
        }
      }

    val descriptionMeta1 =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:1"),
        title = "Title 1",
        updated = DateTime.now(),
        links = listOf(),
        images = listOf(),
        isAutomatic = false,
        isProduction = true)

    val description1 =
      object : AccountProviderDescriptionType {
        override val metadata: AccountProviderDescriptionMetadata = descriptionMeta1

        override fun resolve(onProgress: AccountProviderResolutionListenerType): AccountProviderResolutionResult {
          return AccountProviderResolutionResult(null, listOf(TaskStep(description = "x", failed = true)))
        }
      }

    val descriptionMeta2 =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:2"),
        title = "Title 2",
        updated = DateTime.now(),
        links = listOf(),
        images = listOf(),
        isAutomatic = false,
        isProduction = true)

    val description2 =
      object : AccountProviderDescriptionType {
        override val metadata: AccountProviderDescriptionMetadata = descriptionMeta2

        override fun resolve(onProgress: AccountProviderResolutionListenerType): AccountProviderResolutionResult {
          return AccountProviderResolutionResult(null, listOf(TaskStep(description = "x", failed = true)))
        }
      }

    val descriptionMetaOld0 =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:0"),
        title = "Title 0",
        updated = DateTime.parse("1900-01-01T00:00:00Z"),
        links = listOf(),
        images = listOf(),
        isAutomatic = false,
        isProduction = true)

    val descriptionOld0 =
      object : AccountProviderDescriptionType {
        override val metadata: AccountProviderDescriptionMetadata = descriptionMetaOld0

        override fun resolve(onProgress: AccountProviderResolutionListenerType): AccountProviderResolutionResult {
          return AccountProviderResolutionResult(null, listOf(TaskStep(description = "x", failed = true)))
        }
      }

    val descriptionMetaOld1 =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:1"),
        title = "Title 1",
        updated = DateTime.parse("1900-01-01T00:00:00Z"),
        links = listOf(),
        images = listOf(),
        isAutomatic = false,
        isProduction = true)

    val descriptionOld1 =
      object : AccountProviderDescriptionType {
        override val metadata: AccountProviderDescriptionMetadata = descriptionMetaOld1

        override fun resolve(onProgress: AccountProviderResolutionListenerType): AccountProviderResolutionResult {
          return AccountProviderResolutionResult(null, listOf(TaskStep(description = "x", failed = true)))
        }
      }

    val descriptionMetaOld2 =
      AccountProviderDescriptionMetadata(
        id = URI.create("urn:2"),
        title = "Title 2",
        updated = DateTime.parse("1900-01-01T00:00:00Z"),
        links = listOf(),
        images = listOf(),
        isAutomatic = false,
        isProduction = true)

    val descriptionOld2 =
      object : AccountProviderDescriptionType {
        override val metadata: AccountProviderDescriptionMetadata = descriptionMetaOld2

        override fun resolve(onProgress: AccountProviderResolutionListenerType): AccountProviderResolutionResult {
          return AccountProviderResolutionResult(null, listOf(TaskStep(description = "x", failed = true)))
        }
      }
  }

  class OKAncientSource : AccountProviderSourceType {
    override fun load(context: Context): SourceResult {
      return SourceResult.SourceSucceeded(
        mapOf(
          Pair(descriptionOld0.metadata.id, descriptionOld0),
          Pair(descriptionOld1.metadata.id, descriptionOld1),
          Pair(descriptionOld2.metadata.id, descriptionOld2)))
    }
  }

  class OKSource : AccountProviderSourceType {
    override fun load(context: Context): SourceResult {
      return SourceResult.SourceSucceeded(
        mapOf(
          Pair(description0.metadata.id, description0),
          Pair(description1.metadata.id, description1),
          Pair(description2.metadata.id, description2)))
    }
  }

  class CrashingSource : AccountProviderSourceType {
    override fun load(context: Context): SourceResult {
      throw Exception()
    }
  }

  class FailingSource : AccountProviderSourceType {
    override fun load(context: Context): SourceResult {
      return SourceResult.SourceFailed(mapOf(), java.lang.Exception())
    }
  }
}