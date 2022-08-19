package org.nypl.simplified.ui.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.Visibility.INVISIBLE
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.SettableFuture
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.testUtils.TestCoroutineRule
import org.nypl.simplified.ui.catalog.ProgressBarMatchers.withProgress
import org.nypl.simplified.ui.catalog.RecyclerViewEspressoUtils.atPositionOnView
import org.nypl.simplified.ui.catalog.RecyclerViewEspressoUtils.hasAdapterItemCount
import org.nypl.simplified.ui.catalog.withoutGroups.BookIdleViewHolder.Companion.DOWNLOAD_COMPLETE_FOOTER_DElAY
import org.nypl.simplified.ui.catalog.withoutGroups.BookItem
import org.nypl.simplified.ui.catalog.withoutGroups.BookItem.Idle.IdleActions
import org.nypl.simplified.ui.catalog.withoutGroups.CatalogPagedAdapter
import org.nypl.simplified.ui.catalog.withoutGroups.DownloadState

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class CatalogPagedViewHoldersTest {
  private lateinit var scenario: FragmentScenario<TestFragment>

  @get: Rule
  val instantExecutorRule = InstantTaskExecutorRule()

  @get:Rule
  val testCoroutineRule = TestCoroutineRule()

  private lateinit var bookItems: List<BookItem>

  @MockK private lateinit var mockBookCoverProviderType: BookCoverProviderType
  @MockK private lateinit var mockBuildConfig: BuildConfigurationServiceType

  private lateinit var testThumbnailFuture: SettableFuture<Unit>

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxed = true)

    testThumbnailFuture = SettableFuture.create()
    every {
      mockBookCoverProviderType.loadThumbnailInto(any(), any(), any(), any())
    } returns FluentFuture.from(testThumbnailFuture)

    scenario = launchFragmentInContainer(
      themeResId = R.style.SimplifiedTheme_NoActionBar,
      instantiate = { TestFragment(mockBookCoverProviderType, mockBuildConfig, testCoroutineRule) }
    )
  }

  @Test
  fun `error book items display title and button clicks invoke expected error actions`() {
    val mockEntry = mockk<FeedEntry.FeedEntryOPDS> {
      every { feedEntry.title } returns "errorTitle"
    }

    val mockFailure = mockk<TaskResult.Failure<Unit>>()
    val mockActions = mockk<BookItem.Error.ErrorActions>(relaxUnitFun = true)

    bookItems = listOf<BookItem>(
      BookItem.Error(
        mockEntry,
        mockFailure,
        mockActions
      )
    )

    scenario.onFragment {
      it.submitList(bookItems)
    }

    onView(withId(TestFragment.listId)).check(hasAdapterItemCount(1))
    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellErrorTitle),
              withText("errorTitle")
            )
          )
        )
      )
    )

    onView(withId(R.id.bookCellErrorButtonDismiss)).perform(click())
    verify { mockActions.dismiss() }

    onView(withId(R.id.bookCellErrorButtonDetails)).perform(click())
    verify { mockActions.details() }

    onView(withId(R.id.bookCellErrorButtonRetry)).perform(click())
    verify { mockActions.retry() }
  }

  @Test
  fun `idle books display title, author, contentDescription, and format as expected`() {
    val mockActions = mockk<IdleActions> {
      every { primaryButton() } returns null
      every { secondaryButton() } returns null
      every { openBookDetail() } just runs
    }

    bookItems = listOf<BookItem>(
      BookItem.Idle(
        CatalogTestUtils.buildTestFeedEntryOPDS(title = "idleTitle", author = "idleAuthor"),
        mockActions
      )
    )

    mockkObject(CatalogBookAccessibilityStrings)
    every { CatalogBookAccessibilityStrings.coverDescription(any(), any()) } returns "stubbedContentDescription"

    scenario.onFragment {
      it.submitList(bookItems)
    }

    onView(withId(TestFragment.listId)).check(hasAdapterItemCount(1))
    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleTitle),
              withText("idleTitle")
            )
          )
        )
      )
    )
    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleAuthor),
              withText("idleAuthor")
            )
          )
        )
      )
    )
    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleMeta),
              withText("eBook")
            )
          )
        )
      )
    )

    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleCover),
              withContentDescription("stubbedContentDescription")
            )
          )
        )
      )
    )
  }

  @Test
  fun `idle books show progress image until thumbnail future resolves and image is visible`() {
    val mockActions = mockk<IdleActions> {
      every { primaryButton() } returns null
      every { secondaryButton() } returns null
      every { openBookDetail() } just runs
    }

    bookItems = listOf<BookItem>(
      BookItem.Idle(
        CatalogTestUtils.buildTestFeedEntryOPDS(),
        mockActions
      )
    )

    scenario.onFragment {
      it.submitList(bookItems)
    }

    onView(withId(R.id.bookCellIdleCover)).check(matches(withEffectiveVisibility(INVISIBLE)))
    onView(withId(R.id.bookCellIdleCoverProgress)).check(matches(withEffectiveVisibility(VISIBLE)))

    testThumbnailFuture.set(Unit)

    onView(withId(R.id.bookCellIdleCover)).check(matches(withEffectiveVisibility(VISIBLE)))
    onView(withId(R.id.bookCellIdleCoverProgress)).check(matches(withEffectiveVisibility(INVISIBLE)))
  }

  @Test
  fun `idle books handles clicks to open book details`() {
    val mockActions = mockk<IdleActions> {
      every { primaryButton() } returns null
      every { secondaryButton() } returns null
      every { openBookDetail() } just runs
    }
    bookItems = listOf<BookItem>(
      BookItem.Idle(
        CatalogTestUtils.buildTestFeedEntryOPDS(),
        mockActions
      )
    )

    scenario.onFragment {
      it.submitList(bookItems)
    }

    testThumbnailFuture.set(Unit)

    onView(withId(R.id.bookCellIdle)).perform(click())
    verify { mockActions.openBookDetail() }

    onView(withId(R.id.bookCellIdleTitle)).perform(click())
    verify { mockActions.openBookDetail() }

    onView(withId(R.id.bookCellIdleCover)).perform(click())
    verify { mockActions.openBookDetail() }
  }

  @Test
  fun `idle books displays configurable button text and content descriptions`() {
    val primaryButtonConfig = BookItem.Idle.IdleButtonConfig(
      { "testPrimaryButtonText" },
      { "testPrimaryButtonDescription" },
      {}
    )

    val secondaryButtonConfig = BookItem.Idle.IdleButtonConfig(
      { "testSecondaryButtonText" },
      { "testSecondaryButtonDescription" },
      {}
    )

    val mockActions = mockk<IdleActions> {
      every { primaryButton() } returns primaryButtonConfig
      every { secondaryButton() } returns secondaryButtonConfig
      every { openBookDetail() } just runs
    }

    bookItems = listOf<BookItem>(
      BookItem.Idle(
        CatalogTestUtils.buildTestFeedEntryOPDS(),
        mockActions
      )
    )

    scenario.onFragment {
      it.submitList(bookItems)
    }

    onView(withId(TestFragment.listId)).check(hasAdapterItemCount(1))
    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdlePrimaryButton),
              withText("testPrimaryButtonText")
            )
          )
        )
      )
    )
    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdlePrimaryButton),
              withContentDescription("testPrimaryButtonDescription")
            )
          )
        )
      )
    )
    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleSecondaryButton),
              withText("testSecondaryButtonText")
            )
          )
        )
      )
    )
    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleSecondaryButton),
              withContentDescription("testSecondaryButtonDescription")
            )
          )
        )
      )
    )
  }

  @Test
  fun `idle books hide buttons when no button configuration is provided`() {
    val primaryButtonConfig = null
    val secondaryButtonConfig = null

    val mockActions = mockk<IdleActions> {
      every { primaryButton() } returns primaryButtonConfig
      every { secondaryButton() } returns secondaryButtonConfig
      every { openBookDetail() } just runs
    }

    bookItems = listOf<BookItem>(
      BookItem.Idle(
        CatalogTestUtils.buildTestFeedEntryOPDS(),
        mockActions
      )
    )

    scenario.onFragment {
      it.submitList(bookItems)
    }

    onView(withId(TestFragment.listId)).check(hasAdapterItemCount(1))
    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdlePrimaryButton),
              withEffectiveVisibility(GONE)
            )
          )
        )
      )
    )
    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleSecondaryButton),
              withEffectiveVisibility(GONE)
            )
          )
        )
      )
    )
  }

  @Test
  fun `idle books show or hide format label as specified by buildConfig`() {
    val mockActions = mockk<IdleActions> {
      every { primaryButton() } returns null
      every { secondaryButton() } returns null
      every { openBookDetail() } just runs
    }

    bookItems = listOf<BookItem>(
      BookItem.Idle(
        CatalogTestUtils.buildTestFeedEntryOPDS(),
        mockActions
      )
    )

    every { mockBuildConfig.showFormatLabel } returns false
    scenario.recreate()
    scenario.onFragment {
      it.submitList(bookItems)
    }

    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleMeta),
              withEffectiveVisibility(GONE)
            )
          )
        )
      )
    )

    every { mockBuildConfig.showFormatLabel } returns true
    scenario.recreate()
    scenario.onFragment {
      it.submitList(bookItems)
    }

    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleMeta),
              withEffectiveVisibility(VISIBLE)
            )
          )
        )
      )
    )
  }

  @Test
  fun `idle books show expiry info when present on item, otherwise expiry info is hidden`() {
    val mockActions = mockk<IdleActions> {
      every { primaryButton() } returns null
      every { secondaryButton() } returns null
      every { openBookDetail() } just runs
    }

    val format = DateTimeFormat.forPattern("M/d/y")
    val testExpiry = DateTime.parse("07/01/2022", format)

    bookItems = listOf<BookItem>(
      BookItem.Idle(
        CatalogTestUtils.buildTestFeedEntryOPDS(),
        mockActions,
        testExpiry
      ),
      BookItem.Idle(
        CatalogTestUtils.buildTestFeedEntryOPDS(),
        mockActions
      ),
    )

    scenario.onFragment {
      it.submitList(bookItems)
    }

    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleExpiryInfo),
              withText("Available until Fri, Jul 1")
            )
          )
        )
      )
    )
    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          1,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleExpiryInfo),
              withEffectiveVisibility(INVISIBLE)
            )
          )
        )
      )
    )
  }

  @Test
  fun `idle books with InProgress download shows downloadFooter and hides buttons`() {
    val mockActions = mockk<IdleActions> {
      every { primaryButton() } returns null
      every { secondaryButton() } returns null
      every { openBookDetail() } just runs
    }

    bookItems = listOf<BookItem>(
      BookItem.Idle(
        entry = CatalogTestUtils.buildTestFeedEntryOPDS(),
        actions = mockActions,
        loanExpiry = null,
        downloadState = DownloadState.InProgress(50)
      )
    )

    scenario.onFragment { it.submitList(bookItems) }

    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleDownloadFooter),
              withEffectiveVisibility(VISIBLE)
            )
          )
        )
      )
    )
    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdlePrimaryButton),
              withEffectiveVisibility(GONE)
            )
          )
        )
      )
    )
    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleSecondaryButton),
              withEffectiveVisibility(GONE)
            )
          )
        )
      )
    )
  }

  @Test
  fun `idle books with InProgress download shows indeterminate progress bar, hide progress text`() {
    val mockActions = mockk<IdleActions> {
      every { primaryButton() } returns null
      every { secondaryButton() } returns null
      every { openBookDetail() } just runs
    }

    bookItems = listOf<BookItem>(
      BookItem.Idle(
        entry = CatalogTestUtils.buildTestFeedEntryOPDS(),
        actions = mockActions,
        loanExpiry = null,
        downloadState = DownloadState.InProgress(null)
      )
    )

    scenario.onFragment { it.submitList(bookItems) }

    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleDownloadBar),
              withProgress(shouldBeIndeterminate = true),
              withEffectiveVisibility(VISIBLE)
            )
          )
        )
      )
    )

    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleDownloadPercentage),
              withEffectiveVisibility(INVISIBLE)
            )
          )
        )
      )
    )
  }

  @Test
  fun `idle books with InProgress download shows determinate progress bar and percentage text`() {
    val mockActions = mockk<IdleActions> {
      every { primaryButton() } returns null
      every { secondaryButton() } returns null
      every { openBookDetail() } just runs
    }

    bookItems = listOf<BookItem>(
      BookItem.Idle(
        entry = CatalogTestUtils.buildTestFeedEntryOPDS(),
        actions = mockActions,
        loanExpiry = null,
        downloadState = DownloadState.InProgress(50)
      )
    )

    scenario.onFragment { it.submitList(bookItems) }

    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleDownloadBar),
              withProgress(expectedProgress = 50, shouldBeIndeterminate = false),
              withEffectiveVisibility(VISIBLE)
            )
          )
        )
      )
    )

    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleDownloadPercentage),
              withEffectiveVisibility(VISIBLE),
              withText("50%")
            )
          )
        )
      )
    )
  }

  @Test
  fun `idle books with download complete shows completion message and hides footer after some time`() =
    testCoroutineRule.dispatcher.runBlockingTest {
      val mockActions = mockk<IdleActions> {
        every { primaryButton() } returns null
        every { secondaryButton() } returns null
        every { openBookDetail() } just runs
      }

      bookItems = listOf<BookItem>(
        BookItem.Idle(
          entry = CatalogTestUtils.buildTestFeedEntryOPDS(),
          actions = mockActions,
          loanExpiry = null,
          downloadState = DownloadState.Complete
        )
      )

      scenario.onFragment { it.submitList(bookItems) }
      testCoroutineRule.pauseDispatcher()

      onView(withId(TestFragment.listId)).check(
        matches(
          atPositionOnView(
            0,
            hasDescendant(
              allOf(
                withId(R.id.bookCellIdleDownloadFooter),
                withEffectiveVisibility(VISIBLE)
              )
            )
          )
        )
      )

      testCoroutineRule.advanceTimeBy(DOWNLOAD_COMPLETE_FOOTER_DElAY)

      onView(withId(TestFragment.listId)).check(
        matches(
          atPositionOnView(
            0,
            hasDescendant(
              allOf(
                withId(R.id.bookCellIdleDownloadFooter),
                withEffectiveVisibility(GONE)
              )
            )
          )
        )
      )
    }

  @Test
  fun `idle books with download complete shows buttons`() {
    val mockActions = mockk<IdleActions> {
      every { primaryButton() } returns null
      every { secondaryButton() } returns null
      every { openBookDetail() } just runs
    }

    bookItems = listOf<BookItem>(
      BookItem.Idle(
        entry = CatalogTestUtils.buildTestFeedEntryOPDS(),
        actions = mockActions,
        loanExpiry = null,
        downloadState = DownloadState.Complete
      )
    )

    scenario.onFragment { it.submitList(bookItems) }

    onView(withId(TestFragment.listId)).check(
      matches(
        atPositionOnView(
          0,
          hasDescendant(
            allOf(
              withId(R.id.bookCellIdleButtons),
              withEffectiveVisibility(VISIBLE)
            )
          )
        )
      )
    )

    testCoroutineRule.advanceUntilIdle()
  }
}

object ProgressBarMatchers {
  fun withProgress(expectedProgress: Int = 0, shouldBeIndeterminate: Boolean = false) =
    object : BoundedMatcher<View, ProgressBar>(ProgressBar::class.java) {
      override fun describeTo(description: Description?) {
        description?.appendText("has progress or is indeterminate")
      }

      override fun matchesSafely(item: ProgressBar?): Boolean {
        return item?.let {
          it.progress == expectedProgress && it.isIndeterminate == shouldBeIndeterminate
        } ?: false
      }
    }
}

class TestFragment(
  private val bookCoverProvider: BookCoverProviderType,
  private val buildConfig: BuildConfigurationServiceType,
  private val scope: CoroutineScope
) : Fragment() {
  lateinit var recyclerView: RecyclerView
  lateinit var withoutGroupsAdapter: CatalogPagedAdapter

  companion object {
    const val listId = Int.MAX_VALUE
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    recyclerView = RecyclerView(requireContext())
    recyclerView.id = listId
    recyclerView.layoutManager = LinearLayoutManager(context)
    return recyclerView
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    withoutGroupsAdapter = CatalogPagedAdapter(bookCoverProvider, buildConfig, scope)
    recyclerView.adapter = withoutGroupsAdapter
  }

  fun submitList(list: List<BookItem>) {
    lifecycleScope.launch {
      withoutGroupsAdapter.submitData(PagingData.from(list))
      withoutGroupsAdapter.notifyDataSetChanged()
    }
  }
}
