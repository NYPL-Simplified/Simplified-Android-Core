package org.nypl.simplified.tests.sandbox

import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.io7m.jfunctional.Option
import org.joda.time.DateTime
import org.librarysimplified.services.api.ServiceDirectoryProviderType
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails
import org.nypl.simplified.books.book_registry.BookStatusRevokeErrorDetails
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSCategory
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.taskrecorder.api.TaskStepResolution
import org.nypl.simplified.tests.MockBooksController
import org.nypl.simplified.tests.MockDocumentStore
import org.nypl.simplified.tests.MockProfilesController
import org.nypl.simplified.tests.MutableServiceDirectory
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetail
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetailParameters
import org.nypl.simplified.ui.screen.ScreenSizeInformation
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class BookDetailActivity : AppCompatActivity(), ServiceDirectoryProviderType {

  private lateinit var executor: ScheduledExecutorService
  private lateinit var fragment: Fragment
  private lateinit var registry: BookRegistryType
  private var randomStates = false

  override val serviceDirectory = MutableServiceDirectory()

  private fun bookStatusValues(feedEntry: FeedEntry.FeedEntryOPDS): List<BookStatus> {
    val statusValues = mutableListOf<BookStatus>()

    statusValues.add(
      BookStatus.Held.HeldReady(
        id = feedEntry.bookID,
        endDate = null,
        isRevocable = false
      )
    )
    statusValues.add(
      BookStatus.Held.HeldReady(
        id = feedEntry.bookID,
        endDate = DateTime.now().plusHours(2),
        isRevocable = false
      )
    )
    statusValues.add(
      BookStatus.Held.HeldReady(
        id = feedEntry.bookID,
        endDate = DateTime.now().plusDays(3),
        isRevocable = false
      )
    )
    statusValues.add(
      BookStatus.Held.HeldReady(
        id = feedEntry.bookID,
        endDate = DateTime.now().plusWeeks(4),
        isRevocable = false
      )
    )

    statusValues.add(
      BookStatus.Held.HeldInQueue(
        id = feedEntry.bookID,
        queuePosition = 2,
        startDate = DateTime.now().plusHours(1),
        isRevocable = true,
        endDate = DateTime.now().plusHours(2)
      )
    )
    statusValues.add(
      BookStatus.Held.HeldInQueue(
        id = feedEntry.bookID,
        queuePosition = 2,
        startDate = DateTime.now().plusDays(3),
        isRevocable = true,
        endDate = DateTime.now().plusDays(4)
      )
    )
    statusValues.add(
      BookStatus.Held.HeldInQueue(
        id = feedEntry.bookID,
        queuePosition = 2,
        startDate = DateTime.now().plusWeeks(5),
        isRevocable = true,
        endDate = DateTime.now().plusWeeks(6)
      )
    )

    statusValues.add(
      BookStatus.Loaned.LoanedNotDownloaded(
        id = feedEntry.bookID,
        loanExpiryDate = null,
        returnable = false
      )
    )
    statusValues.add(
      BookStatus.Loaned.LoanedNotDownloaded(
        id = feedEntry.bookID,
        loanExpiryDate = DateTime.now().plusHours(1),
        returnable = true
      )
    )

    statusValues.add(
      BookStatus.Loaned.LoanedDownloaded(
        id = feedEntry.bookID,
        loanExpiryDate = null,
        returnable = false
      )
    )
    statusValues.add(
      BookStatus.Loaned.LoanedDownloaded(
        id = feedEntry.bookID,
        loanExpiryDate = DateTime.now().plusHours(1),
        returnable = true
      )
    )

    statusValues.add(
      BookStatus.FailedRevoke(
        id = feedEntry.bookID,
        result = TaskResult.Failure(listOf(
          TaskStep(
            "Did something",
            TaskStepResolution.TaskStepFailed<BookStatusRevokeErrorDetails>(
              message = "Failed at it",
              errorValue = BookStatusRevokeErrorDetails.NotRevocable("Not revocable"),
              exception = null
            )
          )
        ))
      )
    )

    statusValues.add(
      BookStatus.FailedLoan(
        id = feedEntry.bookID,
        result = TaskResult.Failure(listOf(
          TaskStep(
            "Did something",
            TaskStepResolution.TaskStepFailed<BookStatusDownloadErrorDetails>(
              message = "Failed at it",
              errorValue = BookStatusDownloadErrorDetails.TimedOut("Timed out", mapOf()),
              exception = null
            )
          )
        ))
      )
    )

    statusValues.add(
      BookStatus.RequestingRevoke(
        id = feedEntry.bookID
      )
    )

    statusValues.add(
      BookStatus.RequestingDownload(
        id = feedEntry.bookID
      )
    )
    statusValues.add(
      BookStatus.Downloading(
        id = feedEntry.bookID,
        currentTotalBytes = (Math.random() * 1000.0).toLong(),
        expectedTotalBytes = 1000L,
        detailMessage = "Detail?"
      )
    )
    statusValues.add(
      BookStatus.FailedDownload(
        id = feedEntry.bookID,
        result = TaskResult.Failure(listOf(
          TaskStep(
            "Did something",
            TaskStepResolution.TaskStepFailed<BookStatusDownloadErrorDetails>(
              message = "Failed at it",
              errorValue = BookStatusDownloadErrorDetails.TimedOut("Timed out", mapOf()),
              exception = null
            )
          )
        ))
      )
    )

    return statusValues.toList()
  }

  private fun makeEntry(resources: Resources): OPDSAcquisitionFeedEntry {
    return OPDSAcquisitionFeedEntry.newBuilder(
      "id",
      "Title",
      DateTime(),
      OPDSAvailabilityLoanable.get()
    )
      .addAuthor("Author 0")
      .addAuthor("Author 1")
      .addAuthor("Author 2")
      .setSummaryOption(Option.some(resources.getString(R.string.html)))
      .setPublishedOption(Option.some(DateTime.now()))
      .setPublisherOption(Option.some("Book LLC"))
      .setDistribution("Word Cannon Co.")
      .addCategory(OPDSCategory(
        "Action",
        "http://librarysimplified.org/terms/genres/Simplified/",
        Option.none()
      ))
      .addCategory(OPDSCategory(
        "Adventure",
        "http://librarysimplified.org/terms/genres/Simplified/",
        Option.none()
      ))
      .addCategory(OPDSCategory(
        "After Dinner",
        "urn:ignored",
        Option.none()
      ))
      .addCategory(OPDSCategory(
        "Doorstop",
        "http://librarysimplified.org/terms/genres/Simplified/",
        Option.none()
      ))
      .build()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setContentView(R.layout.fragment_host)

    val books = MockBooksController()
    val documents = MockDocumentStore()
    this.registry = BookRegistry.create()
    this.serviceDirectory.putService(BookRegistryType::class.java, this.registry)
    this.serviceDirectory.putService(BookRegistryReadableType::class.java, this.registry)
    this.serviceDirectory.putService(UIThreadServiceType::class.java, object : UIThreadServiceType {})
    this.serviceDirectory.putService(ScreenSizeInformationType::class.java, ScreenSizeInformation(this.resources))
    this.serviceDirectory.putService(DocumentStoreType::class.java, documents)
    this.serviceDirectory.putService(ProfilesControllerType::class.java, MockProfilesController)
    this.serviceDirectory.putService(BooksControllerType::class.java, books)

    val feedEntry =
      FeedEntry.FeedEntryOPDS(this.makeEntry(this.resources))

    this.fragment =
      CatalogFragmentBookDetail.create(
        CatalogFragmentBookDetailParameters(
          accountId = MockProfilesController.profileAccountCurrent().id,
          feedEntry = feedEntry
        ))

    this.registry.update(
      BookWithStatus(
        book = Book(
          id = feedEntry.bookID,
          account = AccountID.generate(),
          cover = null,
          thumbnail = null,
          entry = feedEntry.feedEntry,
          formats = listOf()
        ),
        status = BookStatus.Loanable(
          id = feedEntry.bookID
        )
      )
    )

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.fragmentHolder, this.fragment, "MAIN")
      .commit()
  }

  override fun onStart() {
    super.onStart()

    this.executor = Executors.newScheduledThreadPool(1)
    this.executor.scheduleAtFixedRate({
      if (this.randomStates) {
        val feedEntry =
          FeedEntry.FeedEntryOPDS(this.makeEntry(this.resources))
        val statusValues =
          this.bookStatusValues(feedEntry)

        this.registry.update(BookWithStatus(
          book = Book(
            id = feedEntry.bookID,
            account = AccountID.generate(),
            cover = null,
            thumbnail = null,
            entry = feedEntry.feedEntry,
            formats = listOf()
          ),
          status = statusValues.random()
        ))
      }
    },
      2_000L,
      2_000L,
      TimeUnit.MILLISECONDS)
  }

  override fun onStop() {
    super.onStop()

    this.executor.shutdown()
  }
}