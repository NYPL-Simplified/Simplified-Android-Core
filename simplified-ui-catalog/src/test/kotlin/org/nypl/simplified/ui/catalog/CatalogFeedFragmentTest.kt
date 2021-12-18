package org.nypl.simplified.ui.catalog

import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.paging.Config
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PositionalDataSource
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.core.AllOf.allOf
import org.hamcrest.core.IsEqual.equalTo
import org.hamcrest.core.IsNot.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedGroup
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.listeners.api.FragmentListenerFinder
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.ui.catalog.RecyclerViewEspressoUtils.atPositionOnView
import org.nypl.simplified.ui.catalog.RecyclerViewEspressoUtils.hasAdapterItemCount
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import java.net.URI

@RunWith(AndroidJUnit4::class)
class CatalogFeedFragmentTest {

  private lateinit var scenario: FragmentScenario<CatalogFeedFragment>

  @MockK
  private lateinit var mockBookCoversProvider: BookCoverProviderType

  @MockK
  private lateinit var mockScreenInformation: ScreenSizeInformationType

  @MockK
  private lateinit var mockBuildConfigService: BuildConfigurationServiceType

  @MockK
  private lateinit var mockCatalogBorrowViewModel: CatalogBorrowViewModel

  @MockK
  private lateinit var mockCatalogFeedViewModel: CatalogFeedViewModel

  @MockK
  private lateinit var mockCatalogFeedEventListener: FragmentListenerType<CatalogFeedEvent>

  private var testFeedStateLiveData = MutableLiveData<CatalogFeedState>()

  @Before
  fun setUp() {
    MockKAnnotations.init(this)

    // Mock service directory to return mocked services
    mockkObject(Services)
    every {
      Services.serviceDirectory().requireService(BookCoverProviderType::class.java)
    } returns mockBookCoversProvider
    every {
      Services.serviceDirectory().requireService(ScreenSizeInformationType::class.java)
    } returns mockScreenInformation
    every {
      Services.serviceDirectory().requireService(BuildConfigurationServiceType::class.java)
    } returns mockBuildConfigService

    // Mock ListenerFinder rather than traversing upwards towards a stubbed listener "repository"
    mockkObject(FragmentListenerFinder)
    every {
      FragmentListenerFinder.findListener(any(), CatalogFeedEvent::class.java)
    } returns mockCatalogFeedEventListener

    // Mock ViewModel creation
    mockkConstructor(CatalogBorrowViewModelFactory::class)
    every {
      anyConstructed<CatalogBorrowViewModelFactory>().create(CatalogBorrowViewModel::class.java)
    } returns mockCatalogBorrowViewModel

    mockkConstructor(CatalogFeedViewModelFactory::class)
    every {
      anyConstructed<CatalogFeedViewModelFactory>().create(CatalogFeedViewModel::class.java)
    } returns mockCatalogFeedViewModel

    // Mock feedViewModel to return test-controlled LiveData
    every { mockCatalogFeedViewModel.feedStateLiveData } returns testFeedStateLiveData

    // Stub this for decorator configuration
    every { mockScreenInformation.dpToPixels(any()) } returns 10.0

    val catalogFeedArguments = CatalogFeedArguments.CatalogFeedArgumentsRemote(
      "title",
      CatalogFeedOwnership.OwnedByAccount(AccountID.generate()),
      URI.create("uri"),
      false
    )

    // Set initial state to 'Loading'
    testFeedStateLiveData.value = CatalogFeedState.CatalogFeedLoading(mockk())

    scenario = launchFragmentInContainer(
      fragmentArgs = bundleOf(CatalogFeedFragment.PARAMETERS_ID to catalogFeedArguments),
      themeResId = R.style.SimplifiedTheme_NoActionBar
    )
  }

  @Test
  fun `on CatalogFeedAgeGate state opens AgeGate dialog and hides other views`() {
    testFeedStateLiveData.value = CatalogFeedState.CatalogFeedAgeGate(
      mockk() // Pass in mock FeedArguments as it is not used when handling state
    )

    // Probably a more thorough way to test this
    scenario.onFragment {
      it.childFragmentManager.fragments.size shouldBeEqualTo 1
      it.childFragmentManager.fragments[0] shouldBeInstanceOf AgeGateDialog::class
    }

    onView(withId(R.id.feedEmpty)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedError)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedLoading)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedNavigation)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedWithGroups)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedWithoutGroups)).check(matches(not(isDisplayed())))
  }

  @Test
  fun `on CatalogFeedLoading state shows Loading view and hides others`() {
    testFeedStateLiveData.value = CatalogFeedState.CatalogFeedLoading(
      mockk() // Pass in mock FeedArguments as it is not used when handling state
    )

    onView(withId(R.id.feedLoading)).check(matches(isDisplayed()))

    onView(withId(R.id.feedEmpty)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedError)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedNavigation)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedWithGroups)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedWithoutGroups)).check(matches(not(isDisplayed())))
  }

  @Test
  fun `on CatalogFeedWithGroups state shows WithGroups view and hides others`() {
    val mockWithGroupsFeed: Feed.FeedWithGroups = buildMockFeedWithGroups()

    testFeedStateLiveData.value = CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithGroups(
      mockk(), // Pass in mock FeedArguments as it is not used when handling state
      mockWithGroupsFeed
    )

    onView(withId(R.id.feedWithGroups)).check(matches(isDisplayed()))

    onView(withId(R.id.feedEmpty)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedError)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedLoading)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedNavigation)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedWithoutGroups)).check(matches(not(isDisplayed())))
  }

  @Test
  fun `on CatalogFeedWithGroups state updates adapter with FeedGroup data`() {
    val mockWithGroupsFeed = buildMockFeedWithGroups()

    onView(withId(R.id.feedWithGroupsList)).check(hasAdapterItemCount(0))

    testFeedStateLiveData.value = CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithGroups(
      mockk(), // Pass in mock FeedArguments as it is not used when handling state
      mockWithGroupsFeed
    )

    onView(withId(R.id.feedWithGroupsList)).check(hasAdapterItemCount(2))
    onView(withId(R.id.feedWithGroupsList)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(withText("title1"))
        )
      )
    )
  }

  private fun buildMockFeedWithGroups(): Feed.FeedWithGroups {
    val testFeedGroupsInOrder = mutableListOf(
      FeedGroup("title1", URI("uri1"), emptyList()),
      FeedGroup("title2", URI("uri2"), emptyList()),
    )

    // Mocking FeedWithGroups in lieu of creating a ton of stuff to use 'Feed.fromAcquisitionFeed()'
    val mockWithGroupsFeed: Feed.FeedWithGroups = mockk()
    every { mockWithGroupsFeed.feedGroupsInOrder } returns testFeedGroupsInOrder
    every { mockWithGroupsFeed.feedSearch } returns null
    every { mockWithGroupsFeed.feedTitle } returns "feedTitle"
    every { mockWithGroupsFeed.facetsByGroup } returns emptyMap()
    return mockWithGroupsFeed
  }

  @Test
  fun `on CatalogFeedWithoutGroups shows WithoutGroups view and hides others`() {
    // Mock the FeedEntries as there's no easy way to make these
    // Use Corrupt Feed entries to reduce non-Fragment surface area for this test
    val testEntriesLiveData = listOf<FeedEntry>(
      mockk<FeedEntry.FeedEntryCorrupt>(),
      mockk<FeedEntry.FeedEntryCorrupt>(),
    ).asPagedList()

    testFeedStateLiveData.value = CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithoutGroups(
      mockk(), // Pass in mock FeedArguments as it is not used when handling state
      testEntriesLiveData,
      emptyList(),
      emptyMap(),
      null,
      "title"
    )

    onView(withId(R.id.feedWithoutGroups)).check(matches(isDisplayed()))

    onView(withId(R.id.feedEmpty)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedError)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedLoading)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedNavigation)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedWithGroups)).check(matches(not(isDisplayed())))
  }

  @Test
  fun `on CatalogFeedWithoutGroups observes CatalogFeedWithoutGroups entries and updates adapter on emission`() {
    // Mock the FeedEntries as there's no easy way to make these
    // Use Corrupt Feed entries to reduce non-Fragment surface area for this test
    val testEntriesLiveData = listOf<FeedEntry>(
      mockk<FeedEntry.FeedEntryCorrupt>(),
      mockk<FeedEntry.FeedEntryCorrupt>(),
    ).asPagedList()

    onView(withId(R.id.feedWithoutGroupsList)).check(hasAdapterItemCount(0))

    testFeedStateLiveData.value = CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithoutGroups(
      mockk(), // Pass in mock FeedArguments as it is not used when handling state
      testEntriesLiveData,
      emptyList(),
      emptyMap(),
      null,
      "title"
    )

    onView(withId(R.id.feedWithoutGroupsList)).check(hasAdapterItemCount(2))
  }

  @Test
  fun `on CatalogFeedNavigation shows Navigation view and hides others`() {
    testFeedStateLiveData.value = CatalogFeedState.CatalogFeedLoaded.CatalogFeedNavigation(
      mockk(), // Pass in mock FeedArguments as it is not used when handling state
      null,
      "title"
    )

    onView(withId(R.id.feedNavigation)).check(matches(isDisplayed()))

    onView(withId(R.id.feedEmpty)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedError)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedLoading)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedWithGroups)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedWithoutGroups)).check(matches(not(isDisplayed())))
  }

  @Test
  fun `on CatalogFeedLoadFailed shows Error view and hides others`() {
    testFeedStateLiveData.value = CatalogFeedState.CatalogFeedLoadFailed(
      mockk(), // Pass in mock FeedArguments as it is not used when handling state
      FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral(
        null,
        Exception(),
        "message",
        emptyMap()
      )
    )

    onView(withId(R.id.feedError)).check(matches(isDisplayed()))

    onView(withId(R.id.feedEmpty)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedLoading)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedNavigation)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedWithGroups)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedWithoutGroups)).check(matches(not(isDisplayed())))
  }

  @Test
  fun `on CatalogFeedLoadFailed enables retry button that submits reload when clicked`() {
    every { mockCatalogFeedViewModel.reloadFeed() } just runs

    testFeedStateLiveData.value = CatalogFeedState.CatalogFeedLoadFailed(
      mockk(), // Pass in mock FeedArguments as it is not used when handling state
      FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral(
        null,
        Exception(),
        "message",
        emptyMap()
      )
    )

    onView(allOf(withId(R.id.feedErrorRetry), isDisplayed())).perform(click())
    verify { mockCatalogFeedViewModel.reloadFeed() }
  }

  @Test
  fun `on CatalogFeedLoadFailed enables errorDetails button that show details when clicked`() {
    every { mockCatalogFeedViewModel.showFeedErrorDetails(any()) } just runs

    val failure = FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral(
      null,
      Exception(),
      "message",
      emptyMap()
    )
    testFeedStateLiveData.value = CatalogFeedState.CatalogFeedLoadFailed(
      mockk(), // Pass in mock FeedArguments as it is not used when handling state
      failure
    )

    onView(allOf(withId(R.id.feedErrorDetails), isDisplayed())).perform(click())
    verify { mockCatalogFeedViewModel.showFeedErrorDetails(failure) }
  }

  @Test
  fun `on CatalogFeedEmpty shows Empty view and hides others`() {
    testFeedStateLiveData.value = CatalogFeedState.CatalogFeedLoading(mockk())

    onView(withId(R.id.feedEmpty)).check(matches(not(isDisplayed())))

    testFeedStateLiveData.value = CatalogFeedState.CatalogFeedLoaded.CatalogFeedEmpty(
      mockk(), // Pass in mock FeedArguments as it is not used when handling state
      null,
      "title"
    )

    onView(withId(R.id.feedEmpty)).check(matches(isDisplayed()))

    onView(withId(R.id.feedError)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedLoading)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedNavigation)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedWithGroups)).check(matches(not(isDisplayed())))
    onView(withId(R.id.feedWithoutGroups)).check(matches(not(isDisplayed())))
  }

  @Test
  fun `onBirthYearSelected updates birth year`() {
    every { mockCatalogFeedViewModel.updateBirthYear(any()) } just runs

    val isOver13 = true
    scenario.onFragment {
      it.onBirthYearSelected(isOver13)
    }

    verify { mockCatalogFeedViewModel.updateBirthYear(isOver13) }
  }

  @Test
  fun `search dialog button is disabled until at least two chars are entered into search input`() {
    // In lieu of invocation through OptionsMenu just call the Fragment method directly
    scenario.onFragment {
      it.openSearchDialog(it.requireContext(), mockk())
    }

    // Multiple typeText() calls seems to be causing issues, using replaceText() instead

    onView(withId(android.R.id.button1)).inRoot(isDialog()).check(matches(isNotEnabled()))

    onView(withId(R.id.searchDialogText)).inRoot(isDialog()).perform(replaceText("1"))
    onView(withId(android.R.id.button1)).inRoot(isDialog()).check(matches(isNotEnabled()))

    onView(withId(R.id.searchDialogText)).inRoot(isDialog()).perform(replaceText("22"))
    onView(withId(android.R.id.button1)).inRoot(isDialog()).check(matches(isEnabled()))

    onView(withId(R.id.searchDialogText)).inRoot(isDialog()).perform(replaceText("1"))
    onView(withId(android.R.id.button1)).inRoot(isDialog()).check(matches(isNotEnabled()))
  }
}

// RecyclerView stuff
object RecyclerViewEspressoUtils {
  fun hasAdapterItemCount(expectedCount: Int) =
    ViewAssertion { view, noViewFoundException ->
      if (noViewFoundException != null) throw noViewFoundException
      assertThat((view as RecyclerView).adapter!!.itemCount, equalTo(expectedCount))
    }

  fun atPositionOnView(pos: Int, matcher: Matcher<View>) =
    object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
      override fun describeTo(description: Description?) {
        description?.appendText("has item at position $pos with matcher :")
        matcher.describeTo(description)
      }

      override fun matchesSafely(item: RecyclerView?): Boolean {
        return item?.let {
          val vh = it.findViewHolderForAdapterPosition(pos)
          matcher.matches(vh?.itemView)
        } ?: false
      }
    }
}

// PagedList stuff
fun <T> List<T>.asPagedList() = LivePagedListBuilder(
  createMockDataSourceFactory(this),
  Config(
    enablePlaceholders = false,
    prefetchDistance = size,
    pageSize = if (size == 0) 1 else size
  )
).build()

private fun <T> createMockDataSourceFactory(itemList: List<T>): DataSource.Factory<Int, T> =
  object : DataSource.Factory<Int, T>() {
    override fun create(): DataSource<Int, T> = FakePositionalDataSource(itemList)
  }

// Revisit this to use PageKeyedDataSource (as CatalogPagedDataSource does)
class FakePositionalDataSource<T>(private val itemList: List<T>) : PositionalDataSource<T>() {
  override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<T>) {
    callback.onResult(itemList, 0)
  }

  override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) {
    if (params.startPosition + params.loadSize <= itemList.lastIndex) {
      callback.onResult(
        itemList.subList(
          params.startPosition,
          params.startPosition + params.loadSize
        )
      )
    }
  }
}
