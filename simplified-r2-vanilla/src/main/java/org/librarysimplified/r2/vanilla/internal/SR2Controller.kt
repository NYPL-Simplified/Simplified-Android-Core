package org.librarysimplified.r2.vanilla.internal

import android.webkit.WebView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.librarysimplified.r2.api.SR2BookChapter
import org.librarysimplified.r2.api.SR2BookMetadata
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.api.SR2Bookmark.Type.LAST_READ
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerConfiguration
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkCreated
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkDeleted
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarksLoaded
import org.librarysimplified.r2.api.SR2Event.SR2Error.SR2ChapterNonexistent
import org.librarysimplified.r2.api.SR2Event.SR2Error.SR2WebViewInaccessible
import org.librarysimplified.r2.api.SR2Event.SR2OnCenterTapped
import org.librarysimplified.r2.api.SR2Event.SR2ReadingPositionChanged
import org.librarysimplified.r2.api.SR2Locator
import org.librarysimplified.r2.api.SR2Locator.SR2LocatorChapterEnd
import org.librarysimplified.r2.api.SR2Locator.SR2LocatorPercent
import org.librarysimplified.r2.vanilla.internal.SR2CommandInternal.SR2CommandInternalAPI
import org.librarysimplified.r2.vanilla.internal.SR2CommandInternal.SR2CommandInternalDelay
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.streamer.server.Server
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.concurrent.GuardedBy

/**
 * The default R2 controller implementation.
 */

internal class SR2Controller private constructor(
  private val configuration: SR2ControllerConfiguration,
  private val port: Int,
  private val server: Server,
  private val publication: Publication,
  private val epubFileName: String
) : SR2ControllerType {

  companion object {

    private val logger =
      LoggerFactory.getLogger(SR2Controller::class.java)

    /**
     * Find a high-numbered port upon which to run the internal server. Tries up to ten
     * times to find a port and then gives up with an exception if it can't.
     */

    private fun fetchUnusedHTTPPort(): Int {
      for (i in 0 until 10) {
        try {
          val socket = ServerSocket(0)
          val port = socket.localPort
          socket.close()
          return port
        } catch (e: IOException) {
          this.logger.error("failed to open port: ", e)
        }

        try {
          Thread.sleep(1_000L)
        } catch (e: InterruptedException) {
          Thread.currentThread().interrupt()
        }
      }

      throw IOException("Unable to find an unused port for the server")
    }

    /**
     * Create a new controller based on the given configuration.
     */

    fun create(
      configuration: SR2ControllerConfiguration
    ): SR2ControllerType {
      val bookFile = configuration.bookFile
      this.logger.debug("creating controller for {}", bookFile)

      val publication = runBlocking {
        configuration.streamer.open(bookFile, allowUserInteraction = false)
      }.getOrElse {
        throw IOException("Failed to open EPUB", it)
      }

      if (publication.isRestricted) {
        throw IOException("Failed to unlock EPUB", publication.protectionError)
      }

      this.logger.debug("publication title: {}", publication.metadata.title)
      val port = this.fetchUnusedHTTPPort()
      this.logger.debug("server port: {}", port)

      val server = Server(port, configuration.context)
      this.logger.debug("starting server")
      server.start(5_000)

      this.logger.debug("loading epub into server")
      val epubName = "/${bookFile.name}"
      this.logger.debug("publication uri: {}", Publication.localBaseUrlOf(epubName, port))
      server.addEpub(
        publication = publication,
        container = null,
        fileName = epubName,
        userPropertiesPath = null
      )

      this.logger.debug("server ready")
      return SR2Controller(
        configuration = configuration,
        epubFileName = epubName,
        port = port,
        publication = publication,
        server = server
      )
    }
  }

  private val logger =
    LoggerFactory.getLogger(SR2Controller::class.java)

  /*
   * A single threaded command executor. The purpose of this executor is to accept
   * commands from multiple threads and ensure that the commands are executed serially.
   */

  private val queueExecutor =
    Executors.newSingleThreadExecutor { runnable ->
      val thread = Thread(runnable)
      thread.name = "org.librarysimplified.r2.vanilla.commandQueue"
      thread
    }

  private val closed = AtomicBoolean(false)
  private val webViewConnectionLock = Any()
  private val eventSubject: PublishSubject<SR2Event> = PublishSubject.create()

  @GuardedBy("webViewConnectionLock")
  private var webViewConnection: SR2WebViewConnection? = null

  @Volatile
  private var currentChapterIndex = 0

  @Volatile
  private var currentChapterProgress = 0.0

  @Volatile
  private var currentBookProgress = 0.0

  @Volatile
  private var bookmarks = listOf<SR2Bookmark>()

  private fun locationOfSpineItem(
    index: Int
  ): String {
    require(index < this.publication.readingOrder.size) {
      "index must be in [0, ${this.publication.readingOrder.size}]; was $index"
    }

    return buildString {
      this.append(Publication.localBaseUrlOf(this@SR2Controller.epubFileName, port))
      this.append(publication.readingOrder[index].href)
    }
  }

  private fun setCurrentChapter(locator: SR2Locator) {
    require(locator.chapterIndex < this.publication.readingOrder.size) {
      "Chapter index ${locator.chapterIndex} must be in the range [0, ${this.publication.readingOrder.size})"
    }
    this.currentChapterIndex = locator.chapterIndex
    this.currentChapterProgress = when (locator) {
      is SR2LocatorPercent -> locator.chapterProgress
      is SR2LocatorChapterEnd -> 1.0
    }
  }

  private fun updateBookmarkLastRead(
    title: String,
    locator: SR2Locator
  ) {
    val newBookmark = SR2Bookmark(
      date = DateTime.now(),
      type = LAST_READ,
      title = title,
      locator = locator,
      bookProgress = this.currentBookProgress
    )
    val newBookmarks = this.bookmarks.toMutableList()
    newBookmarks.removeAll { bookmark -> bookmark.type == LAST_READ }
    newBookmarks.add(newBookmark)
    this.bookmarks = newBookmarks.toList()
    this.eventSubject.onNext(SR2BookmarkCreated(newBookmark))
  }

  private fun executeInternalCommand(command: SR2CommandInternal) {
    this.logger.debug("executing {}", command)

    if (this.closed.get()) {
      this.logger.debug("executor has been shut down")
      return
    }

    return when (command) {
      is SR2CommandInternalDelay ->
        this.executeCommandInternalDelay(command)
      is SR2CommandInternalAPI ->
        this.executeCommandInternalAPI(command)
    }
  }

  private fun executeCommandInternalDelay(command: SR2CommandInternalDelay) {
    Thread.sleep(command.timeMilliseconds)
  }

  private fun executeCommandInternalAPI(command: SR2CommandInternalAPI) {
    return when (val apiCommand = command.command) {
      is SR2Command.OpenChapter ->
        this.executeCommandOpenChapter(command, apiCommand)
      SR2Command.OpenPageNext ->
        this.executeCommandOpenPageNext(command, apiCommand as SR2Command.OpenPageNext)
      SR2Command.OpenChapterNext ->
        this.executeCommandOpenChapterNext(command, apiCommand as SR2Command.OpenChapterNext)
      SR2Command.OpenPagePrevious ->
        this.executeCommandOpenPagePrevious(command, apiCommand as SR2Command.OpenPagePrevious)
      is SR2Command.OpenChapterPrevious ->
        this.executeCommandOpenChapterPrevious(command, apiCommand)
      is SR2Command.BookmarksLoad ->
        this.executeCommandBookmarksLoad(command, apiCommand)
      SR2Command.Refresh ->
        this.executeCommandRefresh(command, apiCommand as SR2Command.Refresh)
      SR2Command.BookmarkCreate ->
        this.executeCommandBookmarkCreate(command, apiCommand as SR2Command.BookmarkCreate)
      is SR2Command.BookmarkDelete ->
        this.executeCommandBookmarkDelete(command, apiCommand)
    }
  }

  private fun executeCommandBookmarkDelete(
    command: SR2CommandInternalAPI,
    apiCommand: SR2Command.BookmarkDelete
  ) {
    val newBookmarks = this.bookmarks.toMutableList()
    val removed = newBookmarks.remove(apiCommand.bookmark)
    if (removed) {
      this.bookmarks = newBookmarks.toList()
      this.eventSubject.onNext(SR2BookmarkDeleted(apiCommand.bookmark))
    }
  }

  private fun executeCommandBookmarkCreate(
    command: SR2CommandInternalAPI,
    createBookmark: SR2Command.BookmarkCreate
  ) {
    val bookmark =
      SR2Bookmark(
        date = DateTime.now(),
        type = SR2Bookmark.Type.EXPLICIT,
        title = this.makeChapterTitleOf(this.currentChapterIndex),
        locator = SR2LocatorPercent(this.currentChapterIndex, this.currentChapterProgress),
        bookProgress = this.currentBookProgress
      )

    val newBookmarks = this.bookmarks.toMutableList()
    newBookmarks.add(bookmark)
    this.bookmarks = newBookmarks.toList()
    this.eventSubject.onNext(SR2BookmarkCreated(bookmark))
  }

  private fun executeCommandRefresh(
    command: SR2CommandInternalAPI,
    refresh: SR2Command.Refresh
  ) {
    this.openChapterIndex(
      command,
      SR2LocatorPercent(this.currentChapterIndex, this.currentChapterProgress)
    )
  }

  private fun executeCommandBookmarksLoad(
    command: SR2CommandInternalAPI,
    apiCommand: SR2Command.BookmarksLoad
  ) {
    val newBookmarks = this.bookmarks.toMutableList()
    newBookmarks.addAll(apiCommand.bookmarks)
    this.bookmarks = newBookmarks.toList()
    this.eventSubject.onNext(SR2BookmarksLoaded)
  }

  private fun executeCommandOpenChapterPrevious(
    command: SR2CommandInternalAPI,
    apiCommand: SR2Command.OpenChapterPrevious
  ) {
    this.openChapterIndex(
      command,
      SR2LocatorChapterEnd(chapterIndex = Math.max(0, this.currentChapterIndex - 1))
    )
  }

  private fun executeCommandOpenChapterNext(
    command: SR2CommandInternalAPI,
    apiCommand: SR2Command.OpenChapterNext
  ) {
    this.openChapterIndex(
      command,
      SR2LocatorPercent(
        chapterIndex = Math.max(0, this.currentChapterIndex + 1),
        chapterProgress = 0.0
      )
    )
  }

  private fun executeCommandOpenPagePrevious(
    command: SR2CommandInternalAPI,
    apiCommand: SR2Command.OpenPagePrevious
  ) {
    this.executeWithWebView(command) { connection -> connection.jsAPI.openPagePrevious() }
  }

  private fun executeCommandOpenPageNext(
    command: SR2CommandInternalAPI,
    apiCommand: SR2Command.OpenPageNext
  ) {
    this.executeWithWebView(command) { connection -> connection.jsAPI.openPageNext() }
  }

  private fun executeCommandOpenChapter(
    command: SR2CommandInternalAPI,
    apiCommand: SR2Command.OpenChapter
  ) {
    this.openChapterIndex(command, apiCommand.locator)
  }

  private fun executeWithWebView(
    command: SR2CommandInternalAPI,
    exec: (SR2WebViewConnection) -> Unit
  ) {
    val webViewRef = synchronized(this.webViewConnectionLock) { this.webViewConnection }
    if (webViewRef != null) {
      this.configuration.uiExecutor.invoke { exec.invoke(webViewRef) }
    } else {
      /*
       * If the web view isn't connected, submit a delay and then submit a retry of the
       * existing command. Either the web view will be reconnected shortly, or the controller
       * will be shut down entirely.
       */

      this.eventSubject.onNext(SR2WebViewInaccessible("No web view is connected"))
      this.submitCommandActual(SR2CommandInternalDelay(timeMilliseconds = 1_000L))
      this.submitCommandActual(
        command.copy(
          id = UUID.randomUUID(),
          submitted = DateTime.now(),
          isRetryOf = command.id
        )
      )
    }
  }

  private fun openChapterIndex(
    command: SR2CommandInternalAPI,
    locator: SR2Locator
  ) {
    val previousLocator =
      SR2LocatorPercent(this.currentChapterIndex, this.currentChapterProgress)

    try {
      val location = this.locationOfSpineItem(locator.chapterIndex)
      this.logger.debug("openChapterIndex: {}", location)

      // Warning: The current chapter must be set before loading the spine location. The
      // page will send a reading position changed event immediately on load, but before our
      // callback returns. If we don't update the chapter index first we'll report the wrong
      // chapter location in our SR2ReadingPositionChanged event. This is fragile and should
      // be fixed later.
      this.setCurrentChapter(locator)

      this.openURL(
        command = command,
        location = location,
        onLoad = { webViewConnection ->
          when (locator) {
            is SR2LocatorPercent -> {
              webViewConnection.jsAPI.setProgression(locator.chapterProgress)
            }
            is SR2LocatorChapterEnd -> {
              webViewConnection.jsAPI.openPageLast()
            }
          }
        }
      )
    } catch (e: Exception) {
      this.logger.error("unable to open chapter ${locator.chapterIndex}: ", e)
      this.setCurrentChapter(previousLocator)
      this.eventSubject.onNext(
        SR2ChapterNonexistent(
          chapterIndex = locator.chapterIndex,
          message = e.message ?: "Unable to open chapter ${locator.chapterIndex}"
        )
      )
    }
  }

  private fun openURL(
    command: SR2CommandInternalAPI,
    location: String,
    onLoad: (SR2WebViewConnection) -> Unit
  ) {
    this.executeWithWebView(command) { webViewConnection ->
      webViewConnection.openURL(location) {
        onLoad.invoke(webViewConnection)
      }
    }
  }

  private fun getBookProgress(chapterProgress: Double): Double {
    require(chapterProgress < 1 || chapterProgress > 0) {
      "progress must be in [0, 1]; was $chapterProgress"
    }

    val chapterCount = this.publication.readingOrder.size
    val currentIndex = this.currentChapterIndex
    val result = ((currentIndex + 1 * chapterProgress) / chapterCount)
    this.logger.debug("$result = ($currentIndex + 1 * $chapterProgress) / $chapterCount")
    return result
  }

  /**
   * A receiver that accepts calls from the Javascript code running inside the current
   * WebView.
   */

  private inner class JavascriptAPIReceiver : SR2JavascriptAPIReceiverType {

    private val logger =
      LoggerFactory.getLogger(JavascriptAPIReceiver::class.java)

    @android.webkit.JavascriptInterface
    override fun onReadingPositionChanged(
      currentPage: Int,
      pageCount: Int
    ) {
      val chapterIndex =
        this@SR2Controller.currentChapterIndex
      val chapterProgress =
        currentPage.toDouble() / pageCount.toDouble()
      val chapterTitle =
        this@SR2Controller.makeChapterTitleOf(chapterIndex)

      this@SR2Controller.currentBookProgress =
        this@SR2Controller.getBookProgress(chapterProgress)
      this@SR2Controller.currentChapterProgress =
        chapterProgress

      this.logger.debug(
        """onReadingPositionChanged: chapterIndex=$chapterIndex, currentPage=$currentPage, pageCount=$pageCount, chapterProgress=$chapterProgress"""
      )

      /*
       * This is pure paranoia; we only update the last-read location if the new position
       * doesn't appear to point to the very start of the book. This is to defend against
       * any future bugs that might cause a "reading position change" event to be published
       * before the user's _real_ last-read position has been restored using a command or
       * bookmark. If this happened, we'd accidentally overwrite the user's reading position with
       * a pointer to the start of the book, so this check prevents that.
       */

      if (chapterIndex != 0 || chapterProgress > 0.000_001) {
        this@SR2Controller.queueExecutor.execute {
          this@SR2Controller.updateBookmarkLastRead(
            title = chapterTitle,
            locator = SR2LocatorPercent(
              chapterIndex = chapterIndex,
              chapterProgress = chapterProgress
            )
          )
        }
      }

      this@SR2Controller.eventSubject.onNext(
        SR2ReadingPositionChanged(
          chapterIndex = chapterIndex,
          chapterTitle = chapterTitle,
          chapterProgress = chapterProgress,
          currentPage = currentPage,
          pageCount = pageCount,
          bookProgress = this@SR2Controller.currentBookProgress
        )
      )
    }

    @android.webkit.JavascriptInterface
    override fun onCenterTapped() {
      this.logger.debug("onCenterTapped")
      this@SR2Controller.eventSubject.onNext(SR2OnCenterTapped())
    }

    @android.webkit.JavascriptInterface
    override fun onClicked() {
      this.logger.debug("onClicked")
    }

    @android.webkit.JavascriptInterface
    override fun onLeftTapped() {
      this.logger.debug("onLeftTapped")
      this@SR2Controller.submitCommand(SR2Command.OpenPagePrevious)
    }

    @android.webkit.JavascriptInterface
    override fun onRightTapped() {
      this.logger.debug("onRightTapped")
      this@SR2Controller.submitCommand(SR2Command.OpenPageNext)
    }
  }

  private fun submitCommandActual(command: SR2CommandInternal) {
    this.logger.debug("submitCommand (isRetryOf: {}) {}", command.isRetryOf, command)
    this.queueExecutor.execute { this.executeInternalCommand(command) }
  }

  override val bookMetadata: SR2BookMetadata =
    SR2BookMetadata(
      id = this.publication.metadata.identifier!!, // FIXME : identifier is not mandatory in RWPM.
      readingOrder = this.makeReadingOrder()
    )

  private fun makeReadingOrder() =
    this.publication.readingOrder.mapIndexed { index, _ -> this.makeChapter(index) }

  private fun makeChapter(
    index: Int
  ): SR2BookChapter {
    return SR2BookChapter(
      chapterIndex = index,
      title = this.makeChapterTitleOf(index)
    )
  }

  /**
   * Return the title of the given chapter.
   */

  private fun makeChapterTitleOf(index: Int): String {
    val chapter = this.publication.readingOrder[index]

    // The title is actually part of the table of contents; however, there may not be a
    // one-to-one mapping between chapters and table of contents entries. We do a lookup
    // based on the chapter href.
    return this.publication.tableOfContents.firstOrNull { it.href == chapter.href }?.title ?: ""
  }

  override val events: Observable<SR2Event> =
    this.eventSubject

  override fun submitCommand(command: SR2Command) =
    this.submitCommandActual(SR2CommandInternalAPI(command = command))

  override fun bookmarksNow(): List<SR2Bookmark> =
    this.bookmarks

  override fun positionNow(): SR2Locator {
    return SR2LocatorPercent(
      this.currentChapterIndex,
      this.currentChapterProgress
    )
  }

  override fun viewConnect(webView: WebView) {
    synchronized(this.webViewConnectionLock) {
      this.webViewConnection = SR2WebViewConnection.create(
        webView = webView,
        jsReceiver = this.JavascriptAPIReceiver(),
        commandQueue = this
      )
    }
  }

  override fun viewDisconnect() {
    synchronized(this.webViewConnectionLock) {
      this.webViewConnection = null
    }
  }

  override fun close() {
    if (this.closed.compareAndSet(false, true)) {
      try {
        this.viewDisconnect()
      } catch (e: Exception) {
        this.logger.error("could not disconnect view: ", e)
      }

      try {
        this.server.closeAllConnections()
      } catch (e: Exception) {
        this.logger.error("could not close connections: ", e)
      }

      try {
        this.server.stop()
      } catch (e: Exception) {
        this.logger.error("could not stop server: ", e)
      }

      try {
        this.publication.close()
      } catch (e: Exception) {
        this.logger.error("could not close publication: ", e)
      }

      try {
        this.queueExecutor.shutdown()
      } catch (e: Exception) {
        this.logger.error("could not stop command queue: ", e)
      }

      try {
        this.eventSubject.onComplete()
      } catch (e: Exception) {
        this.logger.error("could not complete event stream: ", e)
      }
    }
  }
}
