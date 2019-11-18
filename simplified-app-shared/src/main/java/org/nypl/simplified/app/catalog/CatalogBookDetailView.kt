package org.nypl.simplified.app.catalog

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import com.io7m.jfunctional.Some
import com.io7m.jnull.NullCheck
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.app.NetworkConnectivityType
import org.nypl.simplified.app.R
import org.nypl.simplified.app.ScreenSizeInformationType
import org.nypl.simplified.app.catalog.CatalogFeedArguments.CatalogFeedArgumentsRemote
import org.nypl.simplified.app.login.LoginDialog
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.book_registry.BookStatusEvent.Type.BOOK_CHANGED
import org.nypl.simplified.books.book_registry.BookStatusEvent.Type.BOOK_REMOVED
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.stack.ImmutableStack
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.atomic.AtomicReference

/**
 * A book detail view.
 */

class CatalogBookDetailView(
  private val activity: AppCompatActivity,
  private val inflater: LayoutInflater,
  private val account: AccountType,
  private val coverProvider: BookCoverProviderType,
  private val booksRegistry: BookRegistryReadableType,
  private val analytics: AnalyticsType,
  private val profilesController: ProfilesControllerType,
  private val booksController: BooksControllerType,
  private val screenSizeInformation: ScreenSizeInformationType,
  private val networkConnectivity: NetworkConnectivityType,
  entryInitial: FeedEntryOPDS
) {

  private val entry: AtomicReference<FeedEntryOPDS> = AtomicReference(entryInitial)

  private val bookDebugStatus: TextView
  private val bookDownload: ViewGroup
  private val bookDownloadButtons: LinearLayout
  private val bookDownloading: ViewGroup
  private val bookDownloadingCancel: Button
  private val bookDownloadingFailed: ViewGroup
  private val bookDownloadingFailedButtons: LinearLayout
  private val bookDownloadingFailedText: TextView
  private val bookDownloadingPercentText: TextView
  private val bookDownloadingProgress: ProgressBar
  private val bookDownloadReportButton: Button
  private val bookDownloadText: TextView
  private val bookHeader: ViewGroup
  private val bookHeaderAuthors: TextView
  private val bookHeaderCover: ImageView
  private val bookHeaderFormat: TextView
  private val bookHeaderLeft: ViewGroup
  private val bookHeaderTitle: TextView
  private val relatedBooksButton: Button
  private val relatedLayout: ViewGroup
  private var bookDownloadingFailedDetails: TextView

  /**
   * @return The scrolling view containing the book details
   */

  val scrollView: ScrollView

  init {
    val sv = ScrollView(this.activity)
    this.scrollView = sv

    val p = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    sv.layoutParams = p
    sv.addOnLayoutChangeListener { v, left, top, right, bottom, old_left, old_top, old_right, old_bottom -> sv.scrollY = 0 }

    val layout = this.inflater.inflate(R.layout.book_dialog, sv, false)
    sv.addView(layout)

    /*
     * Show the book status if status debugging is enabled.
     */

    this.bookDebugStatus = layout.findViewById<View>(R.id.book_debug_status) as TextView
    if (this.activity.resources.getBoolean(R.bool.debug_catalog_cell_view_states)) {
      this.bookDebugStatus.visibility = View.VISIBLE
    } else {
      this.bookDebugStatus.visibility = View.GONE
    }

    this.bookHeader =
      layout.findViewById<View>(R.id.book_header) as ViewGroup
    this.bookHeaderLeft =
      this.bookHeader.findViewById<View>(R.id.book_header_left) as ViewGroup
    this.bookHeaderTitle =
      this.bookHeader.findViewById<View>(R.id.book_header_title) as TextView
    this.bookHeaderFormat =
      this.bookHeader.findViewById<View>(R.id.book_header_format) as TextView
    this.bookHeaderCover =
      this.bookHeader.findViewById<View>(R.id.book_header_cover) as ImageView
    this.bookHeaderAuthors =
      this.bookHeader.findViewById<View>(R.id.book_header_authors) as TextView
    this.bookDownloadButtons =
      this.bookHeader.findViewById<View>(R.id.book_dialog_download_buttons) as LinearLayout
    this.bookDownloadingCancel =
      this.bookHeader.findViewById<View>(R.id.book_dialog_downloading_cancel) as Button
    this.bookDownloadingFailedButtons =
      this.bookHeader.findViewById<View>(R.id.book_dialog_downloading_failed_buttons) as LinearLayout

    this.bookDownloading =
      layout.findViewById<View>(R.id.book_dialog_downloading) as ViewGroup
    this.bookDownloadingPercentText =
      this.bookDownloading.findViewById<View>(R.id.book_dialog_downloading_percent_text) as TextView
    this.bookDownloadingProgress =
      this.bookDownloading.findViewById<View>(R.id.book_dialog_downloading_progress) as ProgressBar

    this.bookDownloadingFailed =
      layout.findViewById<View>(R.id.book_dialog_downloading_failed) as ViewGroup
    this.bookDownloadingFailedText =
      this.bookDownloadingFailed.findViewById<View>(R.id.book_dialog_downloading_failed_text) as TextView
    this.bookDownloadingFailedDetails =
      this.bookDownloadingFailed.findViewById<View>(R.id.book_dialog_downloading_failed_details) as TextView

    this.bookDownload =
      layout.findViewById<View>(R.id.book_dialog_download) as ViewGroup
    this.bookDownloadText =
      this.bookDownload.findViewById<View>(R.id.book_dialog_download_text) as TextView

    val summary =
      layout.findViewById<View>(R.id.book_summary_layout) as ViewGroup
    val summarySectionTitle =
      summary.findViewById<View>(R.id.book_summary_section_title) as TextView
    val summaryText =
      summary.findViewById<View>(R.id.book_summary_text) as WebView
    val headerMeta =
      summary.findViewById<View>(R.id.book_header_meta) as TextView
    val readMoreButton =
      summary.findViewById<Button>(R.id.book_summary_read_more_button)

    readMoreButton.setOnClickListener { view ->
      configureSummaryWebViewHeight(summaryText)
      readMoreButton.visibility = View.INVISIBLE
    }

    this.relatedLayout = layout.findViewById(R.id.book_related_layout)
    this.relatedBooksButton = this.relatedLayout.findViewById(R.id.related_books_button)
    this.bookDownloadReportButton = layout.findViewById(R.id.book_dialog_report_button)

    /*
     * Assuming a roughly fixed height for cover images, assume a 4:3 aspect
     * ratio and set the width of the cover layout.
     */

    val coverHeight = this.bookHeaderCover.layoutParams.height
    val coverWidth = (coverHeight.toDouble() / 4.0 * 3.0).toInt()
    this.bookHeaderLeft.layoutParams = LinearLayout.LayoutParams(coverWidth, WRAP_CONTENT)

    /* Configure detail texts. */
    val entryNow = this.entry.get()
    val opdsEntry = entryNow.feedEntry
    configureSummarySectionTitle(summarySectionTitle)

    val bookID = entryNow.bookID

    configureSummaryWebView(opdsEntry, summaryText)
    this.bookHeaderTitle.text = opdsEntry.title
    configureViewTextFormat(this.activity.resources, entryInitial, this.bookHeaderFormat)
    configureViewTextAuthor(opdsEntry, this.bookHeaderAuthors)
    configureViewTextMeta(this.activity.resources, opdsEntry, headerMeta)

    val future =
      this.coverProvider.loadCoverInto(
        entryNow,
        this.bookHeaderCover,
        coverWidth,
        coverHeight)

    Futures.addCallback(future, object : FutureCallback<kotlin.Unit?> {
      override fun onSuccess(result: kotlin.Unit?) {
      }

      override fun onFailure(exception: Throwable) {
        LOG.error("could not load cover: ", exception)
      }
    }, directExecutor())
  }

  private fun showLoginDialog() {
    val dialog = LoginDialog()
    dialog.show(this.activity.supportFragmentManager, "login-dialog")
  }

  private fun onBookStatusMatch(status: BookStatus) {
    // Nothing
  }

  private fun onStatus(
    entry: FeedEntryOPDS,
    status: BookStatus?
  ) {
    if (status != null) {
      UIThread.runOnUIThread { this.onBookStatusMatch(status) }
    }

    val relatedBookLink = this.entry.get().feedEntry.related
    val relatedBookListener = OnClickListener {
      if (relatedBookLink is Some<URI>) {
        val empty =
          ImmutableStack.empty<CatalogFeedArguments>()

        val remoteArgs =
          CatalogFeedArgumentsRemote(
            title = "Related Books",
            upStack = empty,
            drawerShouldOpen = false,
            feedURI = relatedBookLink.get(),
            isSearchResults = false)

        val b = Bundle()
        CatalogFeedActivity.setActivityArguments(b, remoteArgs)
        val i = Intent(this@CatalogBookDetailView.activity, MainCatalogActivity::class.java)
        i.putExtras(b)

        this.activity.startActivity(i, null)
      }
    }

    if (relatedBookLink is Some<URI>) {
      val booksButton = this.relatedBooksButton
      UIThread.runOnUIThread {
        this.relatedLayout.visibility = View.VISIBLE
        booksButton.setOnClickListener(relatedBookListener)
      }
    }

    UIThread.runOnUIThread {
      this.bookDownloadReportButton.setOnClickListener(CatalogBookReport(this.activity, entry))
    }
  }

  fun onBookEvent(event: BookStatusEvent) {
    NullCheck.notNull(event, "Event")

    val updateID = event.book()
    val currentEntry = this.entry.get()
    val currentID = currentEntry.bookID

    if (currentID == updateID) {
      when (event.type()) {
        BOOK_CHANGED -> {
          val bookWithStatus = this.booksRegistry.books().get(updateID)
          if (bookWithStatus != null) {
            this.entry.set(FeedEntryOPDS(bookWithStatus.book.entry))
            UIThread.runOnUIThread {
              this.onBookStatusMatch(bookWithStatus.status)
            }
            return
          }
        }
        BOOK_REMOVED -> {
          // Don't care
        }
      }
    }
  }

  companion object {

    private val GENRES_URI: URI =
      URI.create("http://librarysimplified.org/terms/genres/Simplified/")
    private val GENRES_URI_TEXT: String =
      this.GENRES_URI.toString()
    private val LOG: Logger =
      LoggerFactory.getLogger(CatalogBookDetailView::class.java)

    private fun configureSummarySectionTitle(summarySectionTitle: TextView) {
      summarySectionTitle.text = "Description"
    }

    private fun configureSummaryWebView(
      entry: OPDSAcquisitionFeedEntry,
      summaryText: WebView
    ) {
      val text = StringBuilder()
      text.append("<html>")
      text.append("<head>")
      text.append("<style>body {")
      text.append("padding: 0;")
      text.append("padding-right: 2em;")
      text.append("margin: 0;")
      text.append("}</style>")
      text.append("</head>")
      text.append("<body>")
      text.append(entry.summary)
      text.append("</body>")
      text.append("</html>")

      val summaryTextSettings = summaryText.settings
      summaryTextSettings.allowContentAccess = false
      summaryTextSettings.allowFileAccess = false
      summaryTextSettings.allowFileAccessFromFileURLs = false
      summaryTextSettings.allowUniversalAccessFromFileURLs = false
      summaryTextSettings.blockNetworkLoads = true
      summaryTextSettings.blockNetworkImage = true
      summaryTextSettings.defaultTextEncodingName = "UTF-8"
      summaryTextSettings.defaultFixedFontSize = 14
      summaryTextSettings.defaultFontSize = 14
      summaryText.loadDataWithBaseURL(
        null,
        text.toString(),
        "text/html",
        "UTF-8",
        null)
    }

    /**
     * Configure the given web view to match the height of the rendered content.
     */

    private fun configureSummaryWebViewHeight(summaryText: WebView) {
      summaryText.layoutParams =
        LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, WRAP_CONTENT)
    }

    private fun configureViewTextAuthor(
      entry: OPDSAcquisitionFeedEntry,
      authors: TextView
    ) {
      val buffer = StringBuilder()
      val `as` = entry.authors
      for (index in `as`.indices) {
        val a = `as`[index]
        if (index > 0) {
          buffer.append("\n")
        }
        buffer.append(a)
      }
      authors.text = buffer.toString()
    }

    private fun configureViewTextMeta(
      resources: Resources,
      entry: OPDSAcquisitionFeedEntry,
      meta: TextView
    ) {
      val buffer = StringBuilder()
      this.createViewTextPublicationDate(resources, entry, buffer)
      this.createViewTextPublisher(resources, entry, buffer)
      this.createViewTextCategories(resources, entry, buffer)
      this.createViewTextDistributor(resources, entry, buffer)
      meta.text = buffer.toString()
    }

    private fun createViewTextCategories(
      resources: Resources,
      entry: OPDSAcquisitionFeedEntry,
      buffer: StringBuilder
    ) {

      val cats = entry.categories
      var hasGenres = false
      for (index in cats.indices) {
        val c = cats[index]
        if (this.GENRES_URI_TEXT == c.scheme) {
          hasGenres = true
        }
      }

      if (hasGenres) {
        if (buffer.length > 0) {
          buffer.append("\n")
        }

        buffer.append(resources.getString(R.string.catalog_categories))
        buffer.append(": ")

        for (index in cats.indices) {
          val c = cats[index]
          if (this.GENRES_URI_TEXT == c.scheme) {
            buffer.append(c.effectiveLabel)
            if (index + 1 < cats.size) {
              buffer.append(", ")
            }
          }
        }
      }
    }

    private fun createViewTextPublicationDate(
      resources: Resources,
      entry: OPDSAcquisitionFeedEntry,
      buffer: StringBuilder
    ): String {
      if (buffer.length > 0) {
        buffer.append("\n")
      }

      val publishedOpt = entry.published
      if (publishedOpt is Some<DateTime>) {
        val published = publishedOpt.get()
        val fmt =
          DateTimeFormatterBuilder()
            .appendYear(4, 5)
            .appendLiteral('-')
            .appendMonthOfYear(2)
            .appendLiteral('-')
            .appendDayOfMonth(2)
            .toFormatter()
        buffer.append(resources.getString(R.string.catalog_publication_date))
        buffer.append(": ")
        buffer.append(fmt.print(published))
        return buffer.toString()
      }

      return ""
    }

    private fun createViewTextPublisher(
      resources: Resources,
      entry: OPDSAcquisitionFeedEntry,
      buffer: StringBuilder
    ) {
      val publisher = entry.publisher
      if (publisher is Some<String>) {
        if (buffer.length > 0) {
          buffer.append("\n")
        }

        buffer.append(resources.getString(R.string.catalog_publisher))
        buffer.append(": ")
        buffer.append(publisher.get())
      }
    }

    private fun createViewTextDistributor(
      resources: Resources,
      entry: OPDSAcquisitionFeedEntry,
      buffer: StringBuilder
    ) {
      if (buffer.length > 0
      ) {
        buffer.append("\n")
      }

      buffer.append(String.format(resources.getString(R.string.catalog_book_distribution), entry.distribution))
    }

    fun configureButtonMargins(
      screenSizeInformation: ScreenSizeInformationType,
      viewGroup: ViewGroup
    ) {

      val marginRight = screenSizeInformation.screenDPToPixels(8).toInt()
      for (i in 0 until viewGroup.childCount) {
        val view = viewGroup.getChildAt(i)
        val layout = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        layout.setMargins(0, 0, marginRight, 0)
        view.layoutParams = layout
      }
    }

    fun configureViewTextFormat(
      resources: Resources,
      entry: FeedEntryOPDS,
      bookHeaderFormat: TextView
    ) {
      when (entry.probableFormat) {
        BOOK_FORMAT_EPUB -> {
          // Not showing the text for epub format books is deliberate!
          bookHeaderFormat.visibility = View.INVISIBLE
          bookHeaderFormat.text = resources.getText(R.string.book_format_epub)
        }
        BOOK_FORMAT_AUDIO -> {
          bookHeaderFormat.visibility = View.VISIBLE
          bookHeaderFormat.text = resources.getText(R.string.book_format_audiobook)
        }
        BOOK_FORMAT_PDF -> {
          bookHeaderFormat.visibility = View.INVISIBLE
        }
        null -> {
          bookHeaderFormat.visibility = View.INVISIBLE
        }
      }
    }
  }
}
