package org.nypl.simplified.ui.catalog

import android.content.Context
import android.content.res.Resources
import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.PagingData
import androidx.recyclerview.widget.ListUpdateCallback
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.fail
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.testUtils.RxSchedulerJUnit5Extension
import org.nypl.simplified.testUtils.getOrAwaitValue
import org.nypl.simplified.ui.catalog.withoutGroups.BookItem
import org.nypl.simplified.ui.catalog.withoutGroups.CatalogPagedAdapter2Diffing
import org.nypl.simplified.ui.thread.api.UIExecutor
import java.net.URI
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@ExtendWith(RxSchedulerJUnit5Extension::class)
class CatalogFeedViewModelTest {
  private lateinit var subject: CatalogFeedViewModel

  @MockK lateinit var mockResources: Resources
  @MockK lateinit var mockProfilesController: ProfilesControllerType
  @MockK lateinit var mockFeedLoader: FeedLoaderType
  @MockK lateinit var mockBookRegistry: BookRegistryType
  @MockK lateinit var mockBuildConfiguration: BuildConfigurationServiceType
  @MockK lateinit var mockAnalytics: AnalyticsType
  @MockK lateinit var mockBorrowViewModel: CatalogBorrowViewModel
  @MockK lateinit var mockFeedArguments: CatalogFeedArguments
  @MockK lateinit var mockListener: FragmentListenerType<CatalogFeedEvent>
  @MockK lateinit var mockUiExecutor: UIExecutor

  private val testDispatcher = TestCoroutineDispatcher()

  @BeforeEach
  internal fun setUp() {
    MockKAnnotations.init(this)
    Dispatchers.setMain(testDispatcher)

    every { mockProfilesController.accountEvents() } returns Observable.empty()
    every { mockBookRegistry.bookEvents() } returns Observable.empty()

    subject = CatalogFeedViewModel(
      resources = mockResources,
      profilesController = mockProfilesController,
      feedLoader = mockFeedLoader,
      bookRegistry = mockBookRegistry,
      buildConfiguration = mockBuildConfiguration,
      analytics = mockAnalytics,
      borrowViewModel = mockBorrowViewModel,
      feedArguments = mockFeedArguments,
      listener = mockListener,
      uiExecutor = mockUiExecutor,
      pagingFetchDispatcher = testDispatcher,
      doInitialLoad = false
    )
  }

  @AfterEach
  internal fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  internal fun `registers self with registry for book events`() {
    verify { mockBookRegistry.bookEvents() }
  }

  @Test
  internal fun `buildBookItems retrieves bookStatus and builds items for entries`() = testDispatcher.runBlockingTest {
    val bookId1 = BookID.newFromText("testBook1")
    val bookId2 = BookID.newFromText("testBook2")
    val mockEntry1 = mockk<FeedEntry.FeedEntryOPDS> { every { bookID } returns bookId1 }
    val mockEntry2 = mockk<FeedEntry.FeedEntryOPDS> { every { bookID } returns bookId2 }
    val mockLoanableStatus = mockk<BookStatus.Loanable>()

    every { mockBookRegistry.bookOrNull(any()) } returns mockk {
      every { status } returns mockLoanableStatus
    }

    val entries = PagingData.from(listOf(
      mockEntry1,
      mockEntry2,
      mockk<FeedEntry.FeedEntryCorrupt>()
    ))

    val differ = AsyncPagingDataDiffer(
      diffCallback = CatalogPagedAdapter2Diffing.comparisonCallback,
      updateCallback = NoopListCallback(),
    )

    val itemsData = subject.buildBookItems(entries)
    differ.submitData(itemsData)

    advanceUntilIdle()
    val results = differ.snapshot().items

    results[0].let {
      if (it is BookItem.Idle) {
        it.entry shouldBe mockEntry1
      } else fail("BookItem should be expected type")
    }

    results[1].let {
      if (it is BookItem.Idle) {
        it.entry shouldBe mockEntry2
      } else fail("BookItem should be expected type")
    }

    results[2] shouldBeInstanceOf BookItem.Corrupt::class.java

    verify { mockBookRegistry.bookOrNull(bookId1) }
    verify { mockBookRegistry.bookOrNull(bookId2) }
    confirmVerified(mockBookRegistry)
  }

  @Test
  internal fun `buildBookItem when FailedDownload builds error item`() {
    val testEntry = CatalogTestUtils.buildTestFeedEntryOPDS()
    val mockResult = mockk<TaskResult.Failure<Unit>>()
    val mockBook = mockk<Book>(relaxed = true) {
      every { entry.title } returns "testTitle"
    }
    val bookWithStatus = BookWithStatus(
      mockBook,
      mockk<BookStatus.FailedDownload> { every { result } returns mockResult }
    )
    val mockListener = mockk<CatalogPagedViewListener>(relaxed = true)
    val result = subject.buildBookItem(testEntry, bookWithStatus, mockListener)

    if (result is BookItem.Error) {
      result.entry shouldBe testEntry
      result.failure shouldBe mockResult

      result.actions.dismiss()
      verify { mockListener.dismissBorrowError(testEntry) }

      result.actions.details()
      verify { mockListener.showTaskError(mockBook, mockResult) }

      result.actions.retry()
      verify { mockListener.borrowMaybeAuthenticated(mockBook) }
    } else fail("Provided BookStatus should map to Error item")
  }

  @Test
  internal fun `buildBookItem when FailedLoan builds error item`() {
    val testEntry = CatalogTestUtils.buildTestFeedEntryOPDS()
    val mockResult = mockk<TaskResult.Failure<Unit>>()
    val mockBook = mockk<Book>(relaxed = true) {
      every { entry.title } returns "testTitle"
    }
    val bookWithStatus = BookWithStatus(
      mockBook,
      mockk<BookStatus.FailedLoan> { every { result } returns mockResult }
    )
    val mockListener = mockk<CatalogPagedViewListener>(relaxed = true)
    val result = subject.buildBookItem(testEntry, bookWithStatus, mockListener)

    if (result is BookItem.Error) {
      result.entry shouldBe testEntry
      result.failure shouldBe mockResult

      result.actions.dismiss()
      verify { mockListener.dismissBorrowError(testEntry) }

      result.actions.details()
      verify { mockListener.showTaskError(mockBook, mockResult) }

      result.actions.retry()
      verify { mockListener.borrowMaybeAuthenticated(mockBook) }
    } else fail("Provided BookStatus should map to Error item")
  }

  @Test
  internal fun `buildBookItem when FailedRevoke builds error item`() {
    val testEntry = CatalogTestUtils.buildTestFeedEntryOPDS()
    val mockResult = mockk<TaskResult.Failure<Unit>>()
    val mockBook = mockk<Book>(relaxed = true) {
      every { entry.title } returns "testTitle"
    }
    val bookWithStatus = BookWithStatus(
      mockBook,
      mockk<BookStatus.FailedRevoke> { every { result } returns mockResult }
    )
    val mockListener = mockk<CatalogPagedViewListener>(relaxed = true)
    val result = subject.buildBookItem(testEntry, bookWithStatus, mockListener)

    if (result is BookItem.Error) {
      result.entry shouldBe testEntry
      result.failure shouldBe mockResult

      result.actions.dismiss()
      verify { mockListener.dismissBorrowError(testEntry) }

      result.actions.details()
      verify { mockListener.showTaskError(mockBook, mockResult) }

      result.actions.retry()
      verify { mockListener.revokeMaybeAuthenticated(mockBook) }
    } else fail("Provided BookStatus should map to Error item")
  }

  @Test
  internal fun `buildBookItem when HeldInQueue and revocable builds item with cancel button`() {
    val testEntry = CatalogTestUtils.buildTestFeedEntryOPDS()
    val mockBook = mockk<Book>(relaxed = true)
    val bookWithStatus = BookWithStatus(
      mockBook,
      mockk<BookStatus.Held.HeldInQueue> { every { isRevocable } returns true }
    )
    val mockListener = mockk<CatalogPagedViewListener>(relaxed = true)
    val result = subject.buildBookItem(testEntry, bookWithStatus, mockListener)

    if (result is BookItem.Idle) {
      result.entry shouldBe testEntry

      result.actions.openBookDetail()
      verify { mockListener.openBookDetail(testEntry) }

      result.actions.primaryButton()?.let {
        it.onClick()
        verify { mockListener.revokeMaybeAuthenticated(mockBook) }

        val mockContext = mockk<Context> {
          every { getString(R.string.catalogCancelHold) } returns "catalogCancelHold"
          every {
            getString(R.string.catalogAccessibilityBookRevokeHold)
          } returns "catalogAccessibilityBookRevokeHold"
        }
        it.getText(mockContext) shouldBe "catalogCancelHold"
        it.getDescription(mockContext) shouldBe "catalogAccessibilityBookRevokeHold"
      } ?: run { fail("Missing primary button") }

      result.actions.secondaryButton() shouldBe null
    } else fail("Provided BookStatus should map to Idle item")
  }

  @Test
  internal fun `buildBookItem when HeldInQueue but irrevocable builds item with no buttons`() {
    val testEntry = CatalogTestUtils.buildTestFeedEntryOPDS()
    val mockBook = mockk<Book>(relaxed = true)
    val bookWithStatus = BookWithStatus(
      mockBook,
      mockk<BookStatus.Held.HeldInQueue> { every { isRevocable } returns false }
    )
    val mockListener = mockk<CatalogPagedViewListener>(relaxed = true)
    val result = subject.buildBookItem(testEntry, bookWithStatus, mockListener)

    if (result is BookItem.Idle) {
      result.entry shouldBe testEntry

      result.actions.openBookDetail()
      verify { mockListener.openBookDetail(testEntry) }

      result.actions.primaryButton() shouldBe null
      result.actions.secondaryButton() shouldBe null
    } else fail("Provided BookStatus should map to Idle item")
  }

  @Test
  internal fun `buildBookItem when HeldReady and revocable builds item with revoke and get buttons`() {
    val testEntry = CatalogTestUtils.buildTestFeedEntryOPDS()
    val mockBook = mockk<Book>(relaxed = true)
    val bookWithStatus = BookWithStatus(
      mockBook,
      mockk<BookStatus.Held.HeldReady> { every { isRevocable } returns true }
    )
    val mockListener = mockk<CatalogPagedViewListener>(relaxed = true)
    val result = subject.buildBookItem(testEntry, bookWithStatus, mockListener)

    if (result is BookItem.Idle) {
      result.entry shouldBe testEntry

      result.actions.openBookDetail()
      verify { mockListener.openBookDetail(testEntry) }

      result.actions.primaryButton()?.let {
        it.onClick()
        verify { mockListener.revokeMaybeAuthenticated(mockBook) }

        val mockContext = mockk<Context> {
          every { getString(R.string.catalogCancelHold) } returns "catalogCancelHold"
          every {
            getString(R.string.catalogAccessibilityBookRevokeHold)
          } returns "catalogAccessibilityBookRevokeHold"
        }
        it.getText(mockContext) shouldBe "catalogCancelHold"
        it.getDescription(mockContext) shouldBe "catalogAccessibilityBookRevokeHold"

      } ?: run { fail("Missing primary button") }

      result.actions.secondaryButton()?.let {
        it.onClick()
        verify { mockListener.borrowMaybeAuthenticated(mockBook) }

        val mockContext = mockk<Context> {
          every { getString(R.string.catalogGet) } returns "catalogGet"
          every {
            getString(R.string.catalogAccessibilityBookBorrow)
          } returns "catalogAccessibilityBookBorrow"
        }
        it.getText(mockContext) shouldBe "catalogGet"
        it.getDescription(mockContext) shouldBe "catalogAccessibilityBookBorrow"

      } ?: run { fail("Missing secondary button") }
    } else fail("Provided BookStatus should map to Idle item")
  }

  @Test
  internal fun `buildBookItem when HeldReady and irrevocable builds item with only get button`() {
    val testEntry = CatalogTestUtils.buildTestFeedEntryOPDS()
    val mockBook = mockk<Book>(relaxed = true)
    val bookWithStatus = BookWithStatus(
      mockBook,
      mockk<BookStatus.Held.HeldReady> { every { isRevocable } returns false }
    )
    val mockListener = mockk<CatalogPagedViewListener>(relaxed = true)
    val result = subject.buildBookItem(testEntry, bookWithStatus, mockListener)

    if (result is BookItem.Idle) {
      result.entry shouldBe testEntry

      result.actions.openBookDetail()
      verify { mockListener.openBookDetail(testEntry) }

      result.actions.primaryButton()?.let {
        it.onClick()
        verify { mockListener.borrowMaybeAuthenticated(mockBook) }

        val mockContext = mockk<Context> {
          every { getString(R.string.catalogGet) } returns "catalogGet"
          every {
            getString(R.string.catalogAccessibilityBookBorrow)
          } returns "catalogAccessibilityBookBorrow"
        }
        it.getText(mockContext) shouldBe "catalogGet"
        it.getDescription(mockContext) shouldBe "catalogAccessibilityBookBorrow"

      } ?: run { fail("Missing primary button") }

      result.actions.secondaryButton() shouldBe null
    } else fail("Provided BookStatus should map to Idle item")
  }

  @Test
  internal fun `buildBookItem when Holdable builds item with reserve button`() {
    val testEntry = CatalogTestUtils.buildTestFeedEntryOPDS()
    val mockBook = mockk<Book>(relaxed = true)
    val bookWithStatus = BookWithStatus(
      mockBook,
      mockk<BookStatus.Holdable>()
    )
    val mockListener = mockk<CatalogPagedViewListener>(relaxed = true)
    val result = subject.buildBookItem(testEntry, bookWithStatus, mockListener)

    if (result is BookItem.Idle) {
      result.entry shouldBe testEntry

      result.actions.openBookDetail()
      verify { mockListener.openBookDetail(testEntry) }

      result.actions.primaryButton()?.let {
        it.onClick()
        verify { mockListener.reserveMaybeAuthenticated(mockBook) }

        val mockContext = mockk<Context> {
          every { getString(R.string.catalogReserve) } returns "catalogReserve"
          every {
            getString(R.string.catalogAccessibilityBookReserve)
          } returns "catalogAccessibilityBookReserve"
        }
        it.getText(mockContext) shouldBe "catalogReserve"
        it.getDescription(mockContext) shouldBe "catalogAccessibilityBookReserve"

      } ?: run { fail("Missing primary button") }

      result.actions.secondaryButton() shouldBe null
    }
  }

  @Test
  internal fun `buildBookItem when Loanable builds item with get button`() {
    val testEntry = CatalogTestUtils.buildTestFeedEntryOPDS()
    val mockBook = mockk<Book>(relaxed = true)
    val bookWithStatus = BookWithStatus(
      mockBook,
      mockk<BookStatus.Loanable>()
    )
    val mockListener = mockk<CatalogPagedViewListener>(relaxed = true)
    val result = subject.buildBookItem(testEntry, bookWithStatus, mockListener)

    if (result is BookItem.Idle) {
      result.entry shouldBe testEntry

      result.actions.openBookDetail()
      verify { mockListener.openBookDetail(testEntry) }

      result.actions.primaryButton()?.let {
        it.onClick()
        verify { mockListener.borrowMaybeAuthenticated(mockBook) }

        val mockContext = mockk<Context> {
          every { getString(R.string.catalogGet) } returns "catalogGet"
          every {
            getString(R.string.catalogAccessibilityBookBorrow)
          } returns "catalogAccessibilityBookBorrow"
        }
        it.getText(mockContext) shouldBe "catalogGet"
        it.getDescription(mockContext) shouldBe "catalogAccessibilityBookBorrow"

      } ?: run { fail("Missing primary button") }

      result.actions.secondaryButton() shouldBe null
    } else fail("Provided BookStatus should map to Idle item")
  }

  @Test
  internal fun `buildBookItem when Revoked builds item with no buttons`() {
    val testEntry = CatalogTestUtils.buildTestFeedEntryOPDS()
    val mockBook = mockk<Book>(relaxed = true)
    val bookWithStatus = BookWithStatus(
      mockBook,
      mockk<BookStatus.Revoked>()
    )
    val mockListener = mockk<CatalogPagedViewListener>(relaxed = true)
    val result = subject.buildBookItem(testEntry, bookWithStatus, mockListener)

    if (result is BookItem.Idle) {
      result.entry shouldBe testEntry

      result.actions.openBookDetail()
      verify { mockListener.openBookDetail(testEntry) }

      result.actions.primaryButton() shouldBe null
      result.actions.secondaryButton() shouldBe null
    } else fail("Provided BookStatus should map to Idle item")
  }

  @Test
  internal fun `buildBookItem when LoanedDownloaded builds item with read button if epub or pdf format`() {
    val testEntry = CatalogTestUtils.buildTestFeedEntryOPDS()

    val mockBookEPUB = mockk<Book>(relaxed = true) {
      every { findPreferredFormat() } returns mockk<BookFormat.BookFormatEPUB>()
    }
    val mockBookPDF = mockk<Book>(relaxed = true) {
      every { findPreferredFormat() } returns mockk<BookFormat.BookFormatPDF>()
    }

    listOf(mockBookEPUB, mockBookPDF).forEach { mockBook ->
      val mockListener = mockk<CatalogPagedViewListener>(relaxed = true)
      val bookWithStatus = BookWithStatus(
        mockBook,
        mockk<BookStatus.Loaned.LoanedDownloaded>()
      )
      val result = subject.buildBookItem(testEntry, bookWithStatus, mockListener)

      if (result is BookItem.Idle) {
        result.entry shouldBe testEntry

        result.actions.openBookDetail()
        verify { mockListener.openBookDetail(testEntry) }

        result.actions.primaryButton()?.let {
          it.onClick()
          verify { mockListener.openViewer(mockBook, any()) }

          val mockContext = mockk<Context> {
            every { getString(R.string.catalogRead) } returns "catalogRead"
            every {
              getString(R.string.catalogAccessibilityBookRead)
            } returns "catalogAccessibilityBookRead"
          }
          it.getText(mockContext) shouldBe "catalogRead"
          it.getDescription(mockContext) shouldBe "catalogAccessibilityBookRead"

        } ?: run { fail("Missing primary button") }

        result.actions.secondaryButton() shouldBe null
      } else fail("Provided BookStatus should map to Idle item")
    }
  }

  @Test
  internal fun `buildBookItem when LoanedDownloaded builds item with listen button if audiobook`() {
    val testEntry = CatalogTestUtils.buildTestFeedEntryOPDS()
    val mockBook = mockk<Book>(relaxed = true) {
      every { findPreferredFormat() } returns mockk<BookFormat.BookFormatAudioBook>()
    }
    val bookWithStatus = BookWithStatus(
      mockBook,
      mockk<BookStatus.Loaned.LoanedDownloaded>()
    )
    val mockListener = mockk<CatalogPagedViewListener>(relaxed = true)
    val result = subject.buildBookItem(testEntry, bookWithStatus, mockListener)

    if (result is BookItem.Idle) {
      result.entry shouldBe testEntry

      result.actions.openBookDetail()
      verify { mockListener.openBookDetail(testEntry) }

      result.actions.primaryButton()?.let {
        it.onClick()
        verify { mockListener.openViewer(mockBook, any()) }

        val mockContext = mockk<Context> {
          every { getString(R.string.catalogListen) } returns "catalogListen"
          every {
            getString(R.string.catalogAccessibilityBookListen)
          } returns "catalogAccessibilityBookListen"
        }
        it.getText(mockContext) shouldBe "catalogListen"
        it.getDescription(mockContext) shouldBe "catalogAccessibilityBookListen"

      } ?: run { fail("Missing primary button") }

      result.actions.secondaryButton() shouldBe null
    } else fail("Provided BookStatus should map to Idle item")
  }

  @Test
  internal fun `buildBookItem when LoanedNotDownloaded builds item with download button`() {
    val testEntry = CatalogTestUtils.buildTestFeedEntryOPDS()
    val mockBook = mockk<Book>(relaxed = true)
    val bookWithStatus = BookWithStatus(
      mockBook,
      mockk<BookStatus.Loaned.LoanedNotDownloaded>()
    )
    val mockListener = mockk<CatalogPagedViewListener>(relaxed = true)
    val result = subject.buildBookItem(testEntry, bookWithStatus, mockListener)

    if (result is BookItem.Idle) {
      result.entry shouldBe testEntry

      result.actions.openBookDetail()
      verify { mockListener.openBookDetail(testEntry) }

      result.actions.primaryButton()?.let {
        it.onClick()
        verify { mockListener.borrowMaybeAuthenticated(mockBook) }

        val mockContext = mockk<Context> {
          every { getString(R.string.catalogDownload) } returns "catalogDownload"
          every {
            getString(R.string.catalogAccessibilityBookDownload)
          } returns "catalogAccessibilityBookDownload"
        }
        it.getText(mockContext) shouldBe "catalogDownload"
        it.getDescription(mockContext) shouldBe "catalogAccessibilityBookDownload"

      } ?: run { fail("Missing primary button") }

      result.actions.secondaryButton() shouldBe null
    } else fail("Provided BookStatus should map to Idle item")
  }

  @Test
  internal fun `buildBookItem when Downloading state builds InProgress item with progress`() {
    val testEntry = CatalogTestUtils.buildTestFeedEntryOPDS()
    val bookWithStatus = BookWithStatus(
      mockk(relaxed = true) {
        every { entry.title } returns "testTitle"
      },
      mockk<BookStatus.Downloading> {
        every { progressPercent } returns 80.0
      }
    )
    val mockListener = mockk<CatalogPagedViewListener>(relaxed = true)
    val result = subject.buildBookItem(testEntry, bookWithStatus, mockListener)

    if (result is BookItem.InProgress) {
      result.title shouldBe "testTitle"
      result.isIndeterminate shouldBe false
      result.progress shouldBe 80
    } else fail("Provided BookStatus should map to InProgress item")
  }

  @ParameterizedTest
  @MethodSource("inProgressBookStatuses")
  internal fun `buildBookItem builds indeterminate inProgress item for these states`(
    providedStatus: BookStatus
  ) {
    val testEntry = CatalogTestUtils.buildTestFeedEntryOPDS()
    val bookWithStatus = BookWithStatus(
      mockk(relaxed = true) {
        every { entry.title } returns "testTitle"
      },
      providedStatus
    )

    val mockListener = mockk<CatalogPagedViewListener>(relaxed = true)
    val result = subject.buildBookItem(testEntry, bookWithStatus, mockListener)

    if (result is BookItem.InProgress) {
      result.title shouldBe "testTitle"
      result.isIndeterminate shouldBe true
      result.progress shouldBe 0
    } else fail("Provided BookStatus should map to InProgress item")
  }

  companion object {
    @JvmStatic
    fun inProgressBookStatuses(): Stream<BookStatus> {
      val bookId = BookID.newFromText("testBookId")
      return Stream.of(
        BookStatus.RequestingDownload(bookId),
        BookStatus.RequestingLoan(bookId, "requestingLoanMessage"),
        BookStatus.RequestingRevoke(bookId),
        BookStatus.DownloadExternalAuthenticationInProgress(bookId),
        BookStatus.DownloadWaitingForExternalAuthentication(bookId, URI.create("downloadURI"))
      )
    }
  }
}

class NoopListCallback : ListUpdateCallback {
  override fun onChanged(position: Int, count: Int, payload: Any?) {}
  override fun onMoved(fromPosition: Int, toPosition: Int) {}
  override fun onInserted(position: Int, count: Int) {}
  override fun onRemoved(position: Int, count: Int) {}
}
