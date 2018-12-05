package org.nypl.simplified.app.player

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v4.app.FragmentActivity
import android.widget.ImageView
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.Some
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
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadFailed
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloaded
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloading
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
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
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType
import org.nypl.simplified.app.ThemeMatcher
import org.nypl.simplified.app.utilities.ErrorDialogUtilities
import org.nypl.simplified.app.utilities.NamedThreadPools
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.core.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.core.FeedEntryOPDS
import org.nypl.simplified.downloader.core.DownloadType
import org.nypl.simplified.downloader.core.DownloaderHTTP
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rx.Subscription
import java.io.File
import java.lang.StringBuilder
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * The main activity for playing audio books.
 */

class AudioBookPlayerActivity : FragmentActivity(),
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
  private lateinit var services: SimplifiedCatalogAppServicesType
  private lateinit var loadingFragment: AudioBookLoadingFragment
  private lateinit var sleepTimer: PlayerSleepTimerType
  private lateinit var downloadProvider: PlayerDownloadProviderType
  private lateinit var downloadExecutor: ListeningExecutorService
  private lateinit var downloaderDir: File
  private lateinit var downloader: DownloaderType
  private lateinit var formatHandle: BookDatabaseEntryFormatHandleAudioBook
  private var download: DownloadType? = null
  @ColorInt
  private var primaryTintColor: Int = 0

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
    this.log.debug("account color: {}", this.parameters.accountColor)

    this.setContentView(R.layout.audio_book_player_base)
    this.services = Simplified.getCatalogAppServices()!!
    this.playerScheduledExecutor = Executors.newSingleThreadScheduledExecutor()

    this.primaryTintColor =
      this.resources.getColor(ThemeMatcher.color(this.parameters.accountColor))
    this.log.debug("tint color:    0x{}", Integer.toHexString(this.primaryTintColor))

    this.actionBar.setBackgroundDrawable(ColorDrawable(this.primaryTintColor))
    this.actionBar.setDisplayHomeAsUpEnabled(false)

    this.bookTitle = this.parameters.opdsEntry.title
    this.bookAuthor = this.findBookAuthor(this.parameters.opdsEntry)

    /*
     * Open the database format handle.
     */

    val formatHandleOpt =
      this.services.books.bookGetDatabase()
        .databaseOpenExistingEntry(this.parameters.bookID)
        .entryFindFormatHandle(BookDatabaseEntryFormatHandleAudioBook::class.java)

    if (!(formatHandleOpt is Some<BookDatabaseEntryFormatHandleAudioBook>)) {
      ErrorDialogUtilities.showErrorWithRunnable(
        this,
        this.log,
        this.resources.getString(R.string.audio_book_player_error_book_open),
        null,
        { this.finish() })
      return
    }

    this.formatHandle = formatHandleOpt.get()

    /*
     * Create a new downloader that is solely used to fetch audio book manifests.
     */

    this.downloadExecutor =
      MoreExecutors.listeningDecorator(
        NamedThreadPools.namedThreadPool(1, "audiobook-player", 19))

    this.downloaderDir =
      File(this.filesDir, "audiobook-player-downloads")
    DirectoryUtilities.directoryCreate(this.downloaderDir)
    this.downloader =
      DownloaderHTTP.newDownloader(this.downloadExecutor, this.downloaderDir, this.services.http)
    this.downloadProvider =
      DownloadProvider.create(this.downloadExecutor)

    /*
     * Create a sleep timer.
     */

    this.sleepTimer = PlayerSleepTimer.create()

    /*
     * Show a loading fragment.
     */

    this.loadingFragment = AudioBookLoadingFragment.newInstance(
      AudioBookLoadingFragmentParameters(this.primaryTintColor))

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.audio_book_player_fragment_holder, this.loadingFragment, "LOADING")
      .commit()
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
    return this.services.isNetworkAvailable
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
      this.playerFragment = PlayerFragment.newInstance(
        PlayerFragmentParameters(primaryColor = this.primaryTintColor))

      this.supportFragmentManager
        .beginTransaction()
        .replace(R.id.audio_book_player_fragment_holder, this.playerFragment, "PLAYER")
        .commit()
    }
  }

  private fun restoreSavedPlayerPosition() {
    var restored = false

    try {
      val position = this.formatHandle.loadPlayerPosition()
      if (position is Some<PlayerPosition>) {
        this.player.movePlayheadToLocation(position.get())
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
    if (this.services.isNetworkAvailable) {
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

      is PlayerEventChapterCompleted -> Unit
      is PlayerEventChapterWaiting -> Unit
      is PlayerEventPlaybackRateChanged -> Unit
      is PlayerEventError ->
        onLogPlayerError(event)
    }
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
      val fragment = PlayerPlaybackRateFragment.newInstance(
        PlayerFragmentParameters(primaryColor = this.primaryTintColor))
      fragment.show(this.supportFragmentManager, "PLAYER_RATE")
    }
  }

  override fun onPlayerSleepTimerShouldOpen() {

    /*
     * The player fragment wants us to open the sleep timer.
     */

    UIThread.runOnUIThread {
      val fragment = PlayerSleepTimerFragment.newInstance(
        PlayerFragmentParameters(primaryColor = this.primaryTintColor))
      fragment.show(this.supportFragmentManager, "PLAYER_SLEEP_TIMER")
    }
  }

  override fun onPlayerTOCShouldOpen() {

    /*
     * The player fragment wants us to open the table of contents. Load and display it, and
     * also set the action bar title.
     */

    UIThread.runOnUIThread {
      this.actionBar.setTitle(R.string.audiobook_player_toc_title)

      val fragment =
        PlayerTOCFragment.newInstance(
          PlayerTOCFragmentParameters(primaryColor = this.primaryTintColor))

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
    this.actionBar.setTitle(R.string.audio_book_player)
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

    this.services.coverProvider.loadCoverInto(
      FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(this.parameters.opdsEntry) as FeedEntryOPDS,
      view,
      this.services.screenDPToPixels(300).toInt(),
      this.services.screenDPToPixels(400).toInt())
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
    this.services.accessibility.announce(event.message)
  }
}
