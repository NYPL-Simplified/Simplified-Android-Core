package org.nypl.simplified.app.player

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerAudioEngineRequest
import org.nypl.audiobook.android.api.PlayerAudioEngines
import org.nypl.audiobook.android.api.PlayerDownloadProviderType
import org.nypl.audiobook.android.api.PlayerEvent
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventError
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventPlaybackRateChanged
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventChapterCompleted
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventChapterWaiting
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackBuffering
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackPaused
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackProgressUpdate
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackStarted
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackStopped
import org.nypl.audiobook.android.api.PlayerManifest
import org.nypl.audiobook.android.api.PlayerPosition
import org.nypl.audiobook.android.api.PlayerResult
import org.nypl.audiobook.android.api.PlayerSleepTimer
import org.nypl.audiobook.android.api.PlayerSleepTimerType
import org.nypl.audiobook.android.api.PlayerType
import org.nypl.audiobook.android.downloads.DownloadProvider
import org.nypl.audiobook.android.views.PlayerAccessibilityEvent
import org.nypl.audiobook.android.views.PlayerFragment
import org.nypl.audiobook.android.views.PlayerFragmentListenerType
import org.nypl.audiobook.android.views.PlayerFragmentParameters
import org.nypl.audiobook.android.views.PlayerPlaybackRateFragment
import org.nypl.audiobook.android.views.PlayerSleepTimerFragment
import org.nypl.audiobook.android.views.PlayerTOCFragment
import org.nypl.audiobook.android.views.PlayerTOCFragmentParameters
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.utilities.ErrorDialogUtilities
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.downloader.core.DownloadType
import org.nypl.simplified.downloader.core.DownloaderHTTP
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rx.Subscription
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * The main activity for playing audio books.
 */

class AudioBookPlayerActivity : AppCompatActivity(),
  AudioBookLoadingFragmentListenerType, PlayerFragmentListenerType {

  private val log: Logger = LoggerFactory.getLogger(AudioBookPlayerActivity::class.java)

  companion object {

    private const val PARAMETER_ID =
      "org.nypl.simplified.app.player.AudioBookPlayerActivity.parameters"

    /**
     * Start a new player for the given book.
     *
     * @param from The parent activity
     * @param parameters The player parameters
     */

    fun startActivity(
      from: Activity,
      parameters: AudioBookPlayerParameters) {

      val b = Bundle()
      b.putSerializable(this.PARAMETER_ID, parameters)
      val i = Intent(from, AudioBookPlayerActivity::class.java)
      i.putExtras(b)
      from.startActivity(i)
    }
  }

  private lateinit var book: PlayerAudioBookType
  private lateinit var bookTitle: String
  private lateinit var bookAuthor: String
  private lateinit var playerScheduledExecutor: ScheduledExecutorService
  private lateinit var player: PlayerType
  private var playerInitialized: Boolean = false
  private lateinit var playerSubscription: Subscription
  private lateinit var playerFragment: PlayerFragment

  @Volatile
  private var playerLastPosition: PlayerPosition? = null

  private lateinit var parameters: AudioBookPlayerParameters
  private lateinit var loadingFragment: AudioBookLoadingFragment
  private lateinit var sleepTimer: PlayerSleepTimerType
  private lateinit var downloadProvider: PlayerDownloadProviderType
  private lateinit var downloadExecutor: ListeningExecutorService
  private lateinit var downloaderDir: File
  private lateinit var downloader: DownloaderType
  private lateinit var formatHandle: BookDatabaseEntryFormatHandleAudioBook
  private var download: DownloadType? = null

  @Volatile
  private var destroying: Boolean = false

  override fun onCreate(state: Bundle?) {
    this.log.debug("onCreate")
    super.onCreate(null)

    val i = this.intent!!
    val a = i.extras

    this.parameters = a.getSerializable(PARAMETER_ID) as AudioBookPlayerParameters

    this.log.debug("manifest file: {}", this.parameters.manifestFile)
    this.log.debug("manifest uri:  {}", this.parameters.manifestURI)
    this.log.debug("book id:       {}", this.parameters.bookID)
    this.log.debug("entry id:      {}", this.parameters.opdsEntry.id)

    this.setTheme(this.parameters.theme)
    this.setContentView(R.layout.audio_book_player_base)
    this.playerScheduledExecutor = Executors.newSingleThreadScheduledExecutor()

    val actionBar = this.supportActionBar
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(false)
    }

    this.bookTitle = this.parameters.opdsEntry.title
    this.bookAuthor = this.findBookAuthor(this.parameters.opdsEntry)

    /*
     * Open the database format handle.
     */

    val formatHandleOpt =
      Simplified.application.services()
        .profilesController
        .profileAccountForBook(this.parameters.bookID)
        .bookDatabase
        .entry(this.parameters.bookID)
        .findFormatHandle(BookDatabaseEntryFormatHandleAudioBook::class.java)

    if (formatHandleOpt == null) {
      ErrorDialogUtilities.showErrorWithRunnable(
        this,
        this.log,
        this.resources.getString(R.string.audio_book_player_error_book_open),
        null,
        { this.finish() })
      return
    }

    this.formatHandle = formatHandleOpt

    /*
     * Create a new downloader that is solely used to fetch audio book manifests.
     */

    this.downloadExecutor =
      MoreExecutors.listeningDecorator(
        org.nypl.simplified.threads.NamedThreadPools.namedThreadPool(1, "audiobook-player", 19))

    this.downloaderDir =
      File(this.filesDir, "audiobook-player-downloads")
    DirectoryUtilities.directoryCreate(this.downloaderDir)
    this.downloader =
      DownloaderHTTP.newDownloader(
        this.downloadExecutor,
        this.downloaderDir,
        Simplified.application.services().http)
    this.downloadProvider =
      DownloadProvider.create(this.downloadExecutor)

    /*
     * Create a sleep timer.
     */

    this.sleepTimer = PlayerSleepTimer.create()

    /*
     * Show a loading fragment.
     */

    this.loadingFragment =
      AudioBookLoadingFragment.newInstance(AudioBookLoadingFragmentParameters())

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.audio_book_player_fragment_holder, this.loadingFragment, "LOADING")
      .commit()

    /*
     * Restore the activity title when the back stack is empty.
     */

    this.supportFragmentManager.addOnBackStackChangedListener {
      if (supportFragmentManager.backStackEntryCount == 0) {
        this.restoreActionBarTitle()
      }
    }
  }

  private fun findBookAuthor(entry: OPDSAcquisitionFeedEntry): String {
    if (entry.authors.isEmpty()) {
      return ""
    }
    return entry.authors.first()
  }

  override fun onDestroy() {
    this.log.debug("onDestroy")
    super.onDestroy()

    /*
     * We set a flag to indicate that the activity is currently being destroyed because
     * there may be scheduled tasks that try to execute afte the activity has stopped. This
     * flag allows them to gracefully avoid running.
     */

    this.destroying = true

    /*
     * Cancel the manifest download if one is still happening.
     */

    val down = this.download
    if (down != null) {
      down.cancel()
    }

    /*
     * Cancel downloads and shut down the player.
     */

    if (this.playerInitialized) {
      this.savePlayerPosition()
      this.cancelAllDownloads()
      this.player.close()
      this.playerSubscription.unsubscribe()
    }

    this.downloadExecutor.shutdown()
    this.playerScheduledExecutor.shutdown()
  }

  private fun savePlayerPosition() {
    val position = this.playerLastPosition
    if (position != null) {
      try {
        this.formatHandle.savePlayerPosition(position)
      } catch (e: Exception) {
        this.log.error("could not save player position: ", e)
      }
    }
  }

  override fun onLoadingFragmentWantsDownloader(): DownloaderType {
    return this.downloader
  }

  override fun onLoadingFragmentIsNetworkConnectivityAvailable(): Boolean {
    return Simplified.application.services().networkConnectivity.isNetworkAvailable
  }

  override fun onLoadingFragmentWantsAudioBookParameters(): AudioBookPlayerParameters {
    return this.parameters
  }

  override fun onLoadingFragmentLoadingFailed(exception: Exception) {
    ErrorDialogUtilities.showErrorWithRunnable(this, this.log, exception.message, exception, {
      this.finish()
    })
  }

  override fun onLoadingFragmentLoadingFinished(manifest: PlayerManifest) {
    this.log.debug("finished loading")

    /*
     * Ask the API for the best audio engine available that can handle the given manifest.
     */

    val engine = PlayerAudioEngines.findBestFor(
      PlayerAudioEngineRequest(
        manifest = manifest,
        filter = { true },
        downloadProvider = DownloadProvider.create(this.downloadExecutor)))

    if (engine == null) {
      ErrorDialogUtilities.showErrorWithRunnable(
        this,
        this.log,
        this.resources.getString(R.string.audio_book_player_error_engine_open),
        null,
        { this.finish() })
      return
    }

    this.log.debug(
      "selected audio engine: {} {}",
      engine.engineProvider.name(),
      engine.engineProvider.version())

    /*
     * Create the audio book.
     */

    val bookResult = engine.bookProvider.create(this)
    if (bookResult is PlayerResult.Failure) {
      ErrorDialogUtilities.showErrorWithRunnable(
        this,
        this.log,
        this.resources.getString(R.string.audio_book_player_error_book_open),
        bookResult.failure,
        { this.finish() })
      return
    }

    this.book = (bookResult as PlayerResult.Success).result
    this.player = this.book.createPlayer()
    this.playerSubscription = this.player.events.subscribe { event -> this.onPlayerEvent(event) }
    this.playerInitialized = true

    this.restoreSavedPlayerPosition()
    this.startAllPartsDownloading();

    /*
     * Create and load the main player fragment into the holder view declared in the activity.
     */

    UIThread.runOnUIThread {
      this.playerFragment = PlayerFragment.newInstance(PlayerFragmentParameters())

      this.supportFragmentManager
        .beginTransaction()
        .replace(R.id.audio_book_player_fragment_holder, this.playerFragment, "PLAYER")
        .commit()
    }
  }

  private fun restoreSavedPlayerPosition() {
    var restored = false

    try {
      val position = this.formatHandle.format.position
      if (position != null) {
        this.player.movePlayheadToLocation(position)
        restored = true
      }
    } catch (e: Exception) {
      this.log.error("unable to load saved player position: ", e)
    }

    /*
     * Explicitly wind back to the start of the book if there isn't a suitable position saved.
     */

    if (!restored) {
      this.player.movePlayheadToLocation(this.book.spine[0].position)
    }
  }

  private fun startAllPartsDownloading() {
    if (Simplified.application.services().networkConnectivity.isNetworkAvailable) {
      this.book.wholeBookDownloadTask.fetch()
    }
  }

  private fun cancelAllDownloads() {
    this.book.wholeBookDownloadTask.cancel()
  }

  private fun onPlayerEvent(event: PlayerEvent) {
    return when (event) {
      is PlayerEventPlaybackStarted ->
        this.playerLastPosition =
          event.spineElement.position.copy(offsetMilliseconds = event.offsetMilliseconds)
      is PlayerEventPlaybackBuffering ->
        this.playerLastPosition =
          event.spineElement.position.copy(offsetMilliseconds = event.offsetMilliseconds)
      is PlayerEventPlaybackProgressUpdate ->
        this.playerLastPosition =
          event.spineElement.position.copy(offsetMilliseconds = event.offsetMilliseconds)
      is PlayerEventPlaybackPaused ->
        this.playerLastPosition =
          event.spineElement.position.copy(offsetMilliseconds = event.offsetMilliseconds)
      is PlayerEventPlaybackStopped ->
        this.playerLastPosition =
          event.spineElement.position.copy(offsetMilliseconds = event.offsetMilliseconds)

      is PlayerEventChapterCompleted ->
        this.onPlayerChapterCompleted(event)

      is PlayerEventChapterWaiting -> Unit
      is PlayerEventPlaybackRateChanged -> Unit
      is PlayerEventError ->
        onLogPlayerError(event)
    }
  }

  private fun onPlayerChapterCompleted(event: PlayerEventChapterCompleted) {
    if (event.spineElement.next == null) {
      this.log.debug("book has finished")

      /*
       * Wait a few seconds before displaying the dialog asking if the user wants
       * to return the book.
       */

      this.playerScheduledExecutor.schedule({
        if (!this.destroying) {
          UIThread.runOnUIThread { this.loanReturnShowDialog() }
        }
      }, 5L, TimeUnit.SECONDS)
    }
  }

  private fun loanReturnShowDialog() {
    val alert = AlertDialog.Builder(this)
    alert.setTitle(R.string.audio_book_player_return_title)
    alert.setMessage(R.string.audio_book_player_return_question)
    alert.setNegativeButton(R.string.audio_book_player_do_keep) { dialog, _ ->
      dialog.dismiss()
    }
    alert.setPositiveButton(R.string.audio_book_player_do_return) { _, _ ->
      this.loanReturnPerform()
      this.finish()
    }
    alert.show()
  }

  private fun loanReturnPerform() {
    this.log.debug("returning loan")

    /*
     * We don't care if the return fails. The user can retry when they get back to their
     * book list, if necessary.
     */

    Simplified.application.services()
      .booksController
      .bookRevoke(
        Simplified.application.services()
          .profilesController
          .profileAccountCurrent(),
        this.parameters.bookID)
  }

  private fun onLogPlayerError(event: PlayerEventError) {
    val builder = StringBuilder(128)
    builder.append("Playback error:")
    builder.append('\n')
    builder.append("  Error Code:    ")
    builder.append(event.errorCode)
    builder.append('\n')
    builder.append("  Spine Element: ")
    builder.append(event.spineElement)
    builder.append('\n')
    builder.append("  Offset:        ")
    builder.append(event.offsetMilliseconds)
    builder.append('\n')
    builder.append("  Book Title:    ")
    builder.append(this.parameters.opdsEntry.title)
    builder.append('\n')
    builder.append("  Book OPDS ID:  ")
    builder.append(this.parameters.opdsEntry.id)
    builder.append('\n')
    builder.append("  Stacktrace:")
    builder.append('\n')
    this.log.error("{}", builder.toString(), event.exception)
  }

  override fun onPlayerPlaybackRateShouldOpen() {

    /*
     * The player fragment wants us to open the playback rate selection dialog.
     */

    UIThread.runOnUIThread {
      val fragment =
        PlayerPlaybackRateFragment.newInstance(PlayerFragmentParameters())
      fragment.show(this.supportFragmentManager, "PLAYER_RATE")
    }
  }

  override fun onPlayerSleepTimerShouldOpen() {

    /*
     * The player fragment wants us to open the sleep timer.
     */

    UIThread.runOnUIThread {
      val fragment =
        PlayerSleepTimerFragment.newInstance(PlayerFragmentParameters())
      fragment.show(this.supportFragmentManager, "PLAYER_SLEEP_TIMER")
    }
  }

  override fun onPlayerTOCShouldOpen() {

    /*
     * The player fragment wants us to open the table of contents. Load and display it, and
     * also set the action bar title.
     */

    UIThread.runOnUIThread {
      this.supportActionBar?.setTitle(R.string.audiobook_player_toc_title)

      val fragment = PlayerTOCFragment.newInstance(PlayerTOCFragmentParameters())

      this.supportFragmentManager
        .beginTransaction()
        .replace(R.id.audio_book_player_fragment_holder, fragment, "PLAYER_TOC")
        .addToBackStack(null)
        .commit()
    }
  }

  override fun onPlayerTOCWantsBook(): PlayerAudioBookType {
    return this.book
  }

  override fun onPlayerTOCWantsClose() {

    /*
     * The player fragment wants to close the table of contents dialog. Pop it from the back
     * stack and set the action bar title back to the original title.
     */

    this.supportFragmentManager.popBackStack()
    this.restoreActionBarTitle()
  }

  private fun restoreActionBarTitle() {
    this.supportActionBar?.setTitle(R.string.audio_book_player)
  }

  override fun onPlayerWantsAuthor(): String {
    return this.bookAuthor
  }

  override fun onPlayerWantsCoverImage(view: ImageView) {

    /*
     * Use the cover provider to load a cover image into the image view. The width and height
     * are essentially hints; the target image view almost certainly won't have a usable size
     * before this method is called, so we pass in a width/height hint that should give something
     * reasonably close to the expected 3:4 cover image size ratio.
     */

    val screen = Simplified.application.services().screenSize
    Simplified.application.services()
      .bookCovers
      .loadCoverInto(
        FeedEntry.FeedEntryOPDS(this.parameters.opdsEntry),
        view,
        screen.screenDPToPixels(300).toInt(),
        screen.screenDPToPixels(400).toInt())
  }

  override fun onPlayerWantsPlayer(): PlayerType {
    return this.player
  }

  override fun onPlayerWantsSleepTimer(): PlayerSleepTimerType {
    return this.sleepTimer
  }

  override fun onPlayerWantsTitle(): String {
    return this.parameters.opdsEntry.title
  }

  override fun onPlayerWantsScheduledExecutor(): ScheduledExecutorService {
    return this.playerScheduledExecutor
  }

  override fun onPlayerAccessibilityEvent(event: PlayerAccessibilityEvent) {

  }
}
