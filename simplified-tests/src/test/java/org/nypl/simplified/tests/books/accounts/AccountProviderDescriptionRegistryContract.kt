package org.nypl.simplified.tests.books.accounts

import android.content.Context
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.api.AccountProviderResolutionListenerType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.api.AccountSearchQuery
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent.SourceFailed
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent.StatusChanged
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent.Updated
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryStatus
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryStatus.Idle
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryStatus.Refreshing
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType.SourceResult
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.tests.mocking.MockAccountProviders
import org.slf4j.Logger
import java.net.URI

abstract class AccountProviderDescriptionRegistryContract {

  private lateinit var events: MutableList<AccountProviderRegistryEvent>

  protected abstract val logger: Logger

  protected abstract val context: Context

  protected abstract fun createRegistry(
    defaultProvider: AccountProviderType,
    sources: List<AccountProviderSourceType>
  ): org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType

  @BeforeEach
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
        listOf()
      )

    registry.events.subscribe { e -> this.events.add(e) }

    val notFound =
      registry.findAccountProviderDescription(
        URI.create("urn:uuid:6ba13d1e-c790-4247-9c80-067c6a7257f0")
      )

    Assertions.assertEquals(Idle, registry.status)
    Assertions.assertEquals(null, notFound)
  }

  /**
   * A crashing source raises the right events.
   */

  @Test
  fun testCrashingSource() {
    val registry =
      this.createRegistry(
        MockAccountProviders.fakeProvider("urn:fake:0"),
        listOf(CrashingSource())
      )

    registry.events.subscribe { e -> this.events.add(e) }
    registry.refresh(true)

    Assertions.assertEquals(Idle, registry.status)
    Assertions.assertEquals(3, this.events.size)

    run {
      this.events.removeAt(0) as StatusChanged
    }

    run {
      val event = this.events.removeAt(0) as SourceFailed
      Assertions.assertEquals(CrashingSource::class.java, event.clazz)
    }

    run {
      this.events.removeAt(0) as StatusChanged
    }
  }

  /**
   * Refreshing with a usable source works.
   */

  @Test
  fun testRefresh() {
    val registry =
      this.createRegistry(
        MockAccountProviders.fakeProvider("urn:fake:0"),
        listOf(OKSource())
      )

    registry.events.subscribe { this.events.add(it) }
    registry.refresh(true)

    val description0 =
      registry.findAccountProviderDescription(URI.create("urn:0"))
    val description1 =
      registry.findAccountProviderDescription(URI.create("urn:1"))
    val description2 =
      registry.findAccountProviderDescription(URI.create("urn:2"))

    Assertions.assertEquals(Idle, registry.status)

    Assertions.assertEquals(URI.create("urn:0"), description0!!.id)
    Assertions.assertEquals(URI.create("urn:1"), description1!!.id)
    Assertions.assertEquals(URI.create("urn:2"), description2!!.id)

    Assertions.assertEquals(5, this.events.size)

    run {
      this.events.removeAt(0) as StatusChanged
    }
    run {
      Assertions.assertEquals(URI.create("urn:0"), (this.events.removeAt(0) as Updated).id)
    }
    run {
      Assertions.assertEquals(URI.create("urn:1"), (this.events.removeAt(0) as Updated).id)
    }
    run {
      Assertions.assertEquals(URI.create("urn:2"), (this.events.removeAt(0) as Updated).id)
    }
    run {
      this.events.removeAt(0) as StatusChanged
    }
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
        listOf(OKSource(), OKAncientSource())
      )

    registry.events.subscribe { this.events.add(it) }
    registry.refresh(true)

    val description0 =
      registry.findAccountProviderDescription(URI.create("urn:0"))
    val description1 =
      registry.findAccountProviderDescription(URI.create("urn:1"))
    val description2 =
      registry.findAccountProviderDescription(URI.create("urn:2"))

    Assertions.assertEquals(Idle, registry.status)

    Assertions.assertEquals(URI.create("urn:0"), description0!!.id)
    Assertions.assertEquals(URI.create("urn:1"), description1!!.id)
    Assertions.assertEquals(URI.create("urn:2"), description2!!.id)

    Assertions.assertNotEquals(
      DateTime.parse("1900-01-01T00:00:00Z"), description0.updated
    )
    Assertions.assertNotEquals(
      DateTime.parse("1900-01-01T00:00:00Z"), description1.updated
    )
    Assertions.assertNotEquals(
      DateTime.parse("1900-01-01T00:00:00Z"), description2.updated
    )

    Assertions.assertEquals(5, this.events.size)

    run {
      this.events.removeAt(0) as StatusChanged
    }
    run {
      Assertions.assertEquals(URI.create("urn:0"), (this.events.removeAt(0) as Updated).id)
    }
    run {
      Assertions.assertEquals(URI.create("urn:1"), (this.events.removeAt(0) as Updated).id)
    }
    run {
      Assertions.assertEquals(URI.create("urn:2"), (this.events.removeAt(0) as Updated).id)
    }
    run {
      this.events.removeAt(0) as StatusChanged
    }
  }

  /**
   * Even if a source crashes, the working sources are used.
   */

  @Test
  fun testRefreshWithCrashing() {
    val registry =
      this.createRegistry(
        MockAccountProviders.fakeProvider("urn:fake:0"),
        listOf(OKSource(), CrashingSource())
      )

    registry.events.subscribe { this.events.add(it) }
    registry.refresh(true)

    val description0 =
      registry.findAccountProviderDescription(URI.create("urn:0"))
    val description1 =
      registry.findAccountProviderDescription(URI.create("urn:1"))
    val description2 =
      registry.findAccountProviderDescription(URI.create("urn:2"))

    Assertions.assertEquals(Idle, registry.status)

    Assertions.assertEquals(URI.create("urn:0"), description0!!.id)
    Assertions.assertEquals(URI.create("urn:1"), description1!!.id)
    Assertions.assertEquals(URI.create("urn:2"), description2!!.id)

    Assertions.assertEquals(6, this.events.size)
    run {
      this.events.removeAt(0) as StatusChanged
    }
    run {
      Assertions.assertEquals(URI.create("urn:0"), (this.events.removeAt(0) as Updated).id)
    }
    run {
      Assertions.assertEquals(URI.create("urn:1"), (this.events.removeAt(0) as Updated).id)
    }
    run {
      Assertions.assertEquals(URI.create("urn:2"), (this.events.removeAt(0) as Updated).id)
    }
    run {
      Assertions.assertEquals(CrashingSource::class.java, (this.events.removeAt(0) as SourceFailed).clazz)
    }
    run {
      this.events.removeAt(0) as StatusChanged
    }
  }

  /**
   * Even if a source fails, the working sources are used.
   */

  @Test
  fun testRefreshWithFailing() {
    val registry =
      this.createRegistry(
        MockAccountProviders.fakeProvider("urn:fake:0"),
        listOf(OKSource(), FailingSource())
      )

    registry.events.subscribe { this.events.add(it) }
    registry.refresh(true)

    val description0 =
      registry.findAccountProviderDescription(URI.create("urn:0"))
    val description1 =
      registry.findAccountProviderDescription(URI.create("urn:1"))
    val description2 =
      registry.findAccountProviderDescription(URI.create("urn:2"))

    Assertions.assertEquals(Idle, registry.status)

    Assertions.assertEquals(URI.create("urn:0"), description0!!.id)
    Assertions.assertEquals(URI.create("urn:1"), description1!!.id)
    Assertions.assertEquals(URI.create("urn:2"), description2!!.id)

    Assertions.assertEquals(6, this.events.size)

    run {
      this.events.removeAt(0) as StatusChanged
    }
    run {
      Assertions.assertEquals(URI.create("urn:0"), (this.events.removeAt(0) as Updated).id)
    }
    run {
      Assertions.assertEquals(URI.create("urn:1"), (this.events.removeAt(0) as Updated).id)
    }
    run {
      Assertions.assertEquals(URI.create("urn:2"), (this.events.removeAt(0) as Updated).id)
    }
    run {
      Assertions.assertEquals(FailingSource::class.java, (this.events.removeAt(0) as SourceFailed).clazz)
    }
    run {
      this.events.removeAt(0) as StatusChanged
    }
  }

  /**
   * Trying to update with an outdated description returns the newer description.
   */

  @Test
  fun testUpdateIgnoreOld() {
    val registry =
      this.createRegistry(
        MockAccountProviders.fakeProvider("urn:fake:0"),
        listOf(OKSource(), OKAncientSource())
      )

    registry.events.subscribe { this.events.add(it) }
    registry.refresh(true)

    val existing0 =
      registry.findAccountProviderDescription(URI.create("urn:0"))!!

    val changed =
      registry.updateDescription(existing0)

    Assertions.assertEquals(Idle, registry.status)
    Assertions.assertEquals(existing0, changed)
    Assertions.assertEquals(existing0, registry.accountProviderDescriptions()[existing0.id])
  }

  /**
   * Trying to update with an outdated provider returns the newer provider.
   */

  @Test
  fun testUpdateIgnoreProviderOld() {
    val registry =
      this.createRegistry(
        MockAccountProviders.fakeProvider("urn:fake:0"),
        listOf()
      )

    registry.events.subscribe { this.events.add(it) }
    registry.refresh(true)

    val existing0 =
      MockAccountProviders.fakeProvider("urn:fake:0")

    val older0 =
      MockAccountProviders.fakeProvider("urn:fake:0")
        .copy(updated = DateTime.parse("1900-01-01T00:00:00Z"))

    val initial =
      registry.updateProvider(existing0)
    val changed =
      registry.updateProvider(older0)

    Assertions.assertEquals(Idle, registry.status)
    Assertions.assertEquals(existing0, initial)
    Assertions.assertEquals(existing0, changed)
    Assertions.assertEquals(registry.resolvedProviders[existing0.id], existing0)
  }

  /**
   * Refreshing publishes the correct status.
   */

  @Test
  fun testRefreshStatus() {
    val registry =
      this.createRegistry(
        MockAccountProviders.fakeProvider("urn:fake:0"),
        listOf(OKSource())
      )

    val eventsWithRefreshing =
      mutableListOf<AccountProviderRegistryStatus>()

    registry.events.subscribe {
      eventsWithRefreshing.add(registry.status)
    }

    registry.refresh(true)

    Assertions.assertEquals(5, eventsWithRefreshing.size)
    Assertions.assertEquals(Refreshing::class.java, eventsWithRefreshing[0].javaClass)
    Assertions.assertEquals(Refreshing::class.java, eventsWithRefreshing[1].javaClass)
    Assertions.assertEquals(Refreshing::class.java, eventsWithRefreshing[2].javaClass)
    Assertions.assertEquals(Refreshing::class.java, eventsWithRefreshing[3].javaClass)
    Assertions.assertEquals(Idle::class.java, eventsWithRefreshing[4].javaClass)

    Assertions.assertEquals(Idle, registry.status)
  }

  companion object {

    val description0 =
      AccountProviderDescription(
        id = URI.create("urn:0"),
        title = "Title 0",
        updated = DateTime.now(),
        links = listOf(),
        images = listOf(),
        isAutomatic = false,
        isProduction = true,
        location = null
      )

    private fun fail(): TaskResult.Failure<AccountProviderType> {
      val taskRecorder = TaskRecorder.create()
      val exception = Exception()
      taskRecorder.currentStepFailed(
        message = "x",
        errorCode = "unexpectedException",
        exception = exception
      )
      return taskRecorder.finishFailure()
    }

    val description1 =
      AccountProviderDescription(
        id = URI.create("urn:1"),
        title = "Title 1",
        updated = DateTime.now(),
        links = listOf(),
        images = listOf(),
        isAutomatic = false,
        isProduction = true,
        location = null
      )

    val description2 =
      AccountProviderDescription(
        id = URI.create("urn:2"),
        title = "Title 2",
        updated = DateTime.now(),
        links = listOf(),
        images = listOf(),
        isAutomatic = false,
        isProduction = true,
        location = null
      )

    val descriptionOld0 =
      AccountProviderDescription(
        id = URI.create("urn:0"),
        title = "Title 0",
        updated = DateTime.parse("1900-01-01T00:00:00Z"),
        links = listOf(),
        images = listOf(),
        isAutomatic = false,
        isProduction = true,
        location = null
      )

    val descriptionOld1 =
      AccountProviderDescription(
        id = URI.create("urn:1"),
        title = "Title 1",
        updated = DateTime.parse("1900-01-01T00:00:00Z"),
        links = listOf(),
        images = listOf(),
        isAutomatic = false,
        isProduction = true,
        location = null
      )

    val descriptionOld2 =
      AccountProviderDescription(
        id = URI.create("urn:2"),
        title = "Title 2",
        updated = DateTime.parse("1900-01-01T00:00:00Z"),
        links = listOf(),
        images = listOf(),
        isAutomatic = false,
        isProduction = true,
        location = null
      )
  }

  class OKAncientSource : AccountProviderSourceType {
    override fun load(context: Context, includeTestingLibraries: Boolean): SourceResult {
      return SourceResult.SourceSucceeded(
        mapOf(
          Pair(descriptionOld0.id, descriptionOld0),
          Pair(descriptionOld1.id, descriptionOld1),
          Pair(descriptionOld2.id, descriptionOld2)
        )
      )
    }

    override fun query(context: Context, query: AccountSearchQuery): SourceResult {
      return this.load(context, query.includeTestingLibraries)
    }

    override fun clear(context: Context) {}

    override fun canResolve(description: AccountProviderDescription): Boolean {
      return false
    }

    override fun resolve(
      onProgress: AccountProviderResolutionListenerType,
      description: AccountProviderDescription
    ): TaskResult<AccountProviderType> {
      throw IllegalStateException()
    }
  }

  class OKSource : AccountProviderSourceType {
    override fun load(context: Context, includeTestingLibraries: Boolean): SourceResult {
      return SourceResult.SourceSucceeded(
        mapOf(
          Pair(description0.id, description0),
          Pair(description1.id, description1),
          Pair(description2.id, description2)
        )
      )
    }

    override fun query(context: Context, query: AccountSearchQuery): SourceResult {
      return load(context, query.includeTestingLibraries)
    }

    override fun clear(context: Context) {}

    override fun canResolve(description: AccountProviderDescription): Boolean {
      return false
    }

    override fun resolve(
      onProgress: AccountProviderResolutionListenerType,
      description: AccountProviderDescription
    ): TaskResult<AccountProviderType> {
      throw IllegalStateException()
    }
  }

  class CrashingSource : AccountProviderSourceType {
    override fun load(context: Context, includeTestingLibraries: Boolean): SourceResult {
      throw Exception()
    }

    override fun query(context: Context, query: AccountSearchQuery): SourceResult {
      throw Exception()
    }

    override fun clear(context: Context) {}

    override fun canResolve(description: AccountProviderDescription): Boolean {
      return false
    }

    override fun resolve(
      onProgress: AccountProviderResolutionListenerType,
      description: AccountProviderDescription
    ): TaskResult<AccountProviderType> {
      throw IllegalStateException()
    }
  }

  class FailingSource : AccountProviderSourceType {
    override fun load(context: Context, includeTestingLibraries: Boolean): SourceResult {
      return SourceResult.SourceFailed(mapOf(), java.lang.Exception())
    }

    override fun query(context: Context, query: AccountSearchQuery): SourceResult {
      return this.load(context, query.includeTestingLibraries)
    }

    override fun clear(context: Context) {}

    override fun canResolve(description: AccountProviderDescription): Boolean {
      return false
    }

    override fun resolve(
      onProgress: AccountProviderResolutionListenerType,
      description: AccountProviderDescription
    ): TaskResult<AccountProviderType> {
      throw IllegalStateException()
    }
  }
}
