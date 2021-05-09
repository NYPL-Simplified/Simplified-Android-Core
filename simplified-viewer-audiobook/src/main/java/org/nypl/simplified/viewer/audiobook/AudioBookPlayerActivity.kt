package org.nypl.simplified.viewer.audiobook

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerAudioEngineRequest
import org.librarysimplified.audiobook.api.PlayerAudioEngines
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerEvent
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventError
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventPlaybackRateChanged
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventChapterCompleted
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventChapterWaiting
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackBuffering
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackPaused
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackProgressUpdate
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackStarted
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackStopped
import org.librarysimplified.audiobook.api.PlayerPosition
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.api.PlayerSleepTimer
import org.librarysimplified.audiobook.api.PlayerSleepTimerType
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadExpired
import org.librarysimplified.audiobook.api.PlayerType
import org.librarysimplified.audiobook.api.PlayerUserAgent
import org.librarysimplified.audiobook.api.extensions.PlayerExtensionType
import org.librarysimplified.audiobook.downloads.DownloadProvider
import org.librarysimplified.audiobook.feedbooks.FeedbooksPlayerExtension
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.views.PlayerAccessibilityEvent
import org.librarysimplified.audiobook.views.PlayerFragment
import org.librarysimplified.audiobook.views.PlayerFragmentListenerType
import org.librarysimplified.audiobook.views.PlayerFragmentParameters
import org.librarysimplified.audiobook.views.PlayerPlaybackRateFragment
import org.librarysimplified.audiobook.views.PlayerSleepTimerFragment
import org.librarysimplified.audiobook.views.PlayerTOCFragment
import org.librarysimplified.audiobook.views.PlayerTOCFragmentParameters
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.services.api.ServiceDirectoryType
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.books.audio.AudioBookFeedbooksSecretServiceType
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.networkconnectivity.api.NetworkConnectivityType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.threads.NamedThreadPools
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.theme.ThemeServiceType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rx.Subscription
import java.io.IOException
import java.util.ServiceLoader
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The main activity for playing audio books.
 */

class AudioBookPlayerActivity :
  AppCompatActivity(),
  AudioBookLoadingFragmentListenerType,
  PlayerFragmentListenerType {

  private val log: Logger = LoggerFactory.getLogger(AudioBookPlayerActivity::class.java)

  companion object {

    private const val PARAMETER_ID =
      "org.nypl.simplified.viewer.audiobook.AudioBookPlayerActivity.parameters"

    /**
     * Start a new player for the given book.
     *
     * @param from The parent activity
     * @param parameters The player parameters
     */

    fun startActivity(
      from: Activity,
      parameters: AudioBookPlayerParameters
    ) {
      val b = Bundle()
      b.putSerializable(this.PARAMETER_ID, parameters)
      val i = Intent(from, AudioBookPlayerActivity::class.java)
      i.putExtras(b)
      from.startActivity(i)
    }
  }

  @Volatile
  private var playerLastPosition: PlayerPosition? = null
  private lateinit var book: PlayerAudioBookType
  private lateinit var bookAuthor: String
  private lateinit var books: BooksControllerType
  private lateinit var bookSubscription: Subscription
  private lateinit var bookTitle: String
  private lateinit var covers: BookCoverProviderType
  private lateinit var downloadExecutor: ListeningExecutorService
  private lateinit var downloadProvider: PlayerDownloadProviderType
  private lateinit var formatHandle: BookDatabaseEntryFormatHandleAudioBook
  private lateinit var http: LSHTTPClientType
  private lateinit var loadingFragment: AudioBookLoadingFragment
  private lateinit var networkConnectivity: NetworkConnectivityType
  private lateinit var parameters: AudioBookPlayerParameters
  private lateinit var player: PlayerType
  private lateinit var playerFragment: PlayerFragment
  private lateinit var playerScheduledExecutor: ScheduledExecutorService
  private lateinit var playerSubscription: Subscription
  private lateinit var profiles: ProfilesControllerType
  private lateinit var screenSize: ScreenSizeInformationType
  private lateinit var sleepTimer: PlayerSleepTimerType
  private lateinit var strategies: AudioBookManifestStrategiesType
  private lateinit var uiThread: UIThreadServiceType
  private var playerInitialized: Boolean = false
  private val reloadingManifest = AtomicBoolean(false)

  @Volatile
  private var destroying: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    this.log.debug("onCreate")
    super.onCreate(null)

    val i = this.intent!!
    val a = i.extras!!

    this.parameters = a.getSerializable(PARAMETER_ID) as AudioBookPlayerParameters

    this.log.debug("manifest file: {}", this.parameters.manifestFile)
    this.log.debug("manifest uri:  {}", this.parameters.manifestURI)
    this.log.debug("book id:       {}", this.parameters.bookID)
    this.log.debug("entry id:      {}", this.parameters.opdsEntry.id)

    this.setTheme(
      Services.serviceDirectory()
        .requireService(ThemeServiceType::class.java)
        .findCurrentTheme()
        .themeWithActionBar
    )
    this.setContentView(R.layout.audio_book_player_base)
    this.playerScheduledExecutor = Executors.newSingleThreadScheduledExecutor()

    this.supportActionBar?.setDisplayHomeAsUpEnabled(false)

    this.bookTitle = this.parameters.opdsEntry.title
    this.bookAuthor = this.findBookAuthor(this.parameters.opdsEntry)

    val services = Services.serviceDirectory()

    this.profiles =
      services.requireService(ProfilesControllerType::class.java)
    this.http =
      services.requireService(LSHTTPClientType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
    this.screenSize =
      services.requireService(ScreenSizeInformationType::class.java)
    this.books =
      services.requireService(BooksControllerType::class.java)
    this.covers =
      services.requireService(BookCoverProviderType::class.java)
    this.networkConnectivity =
      services.requireService(NetworkConnectivityType::class.java)
    this.strategies =
      services.requireService(AudioBookManifestStrategiesType::class.java)

    /*
     * Open the database format handle.
     */

    val formatHandleOpt =
      this.profiles.profileAccountForBook(this.parameters.bookID)
        .bookDatabase
        .entry(this.parameters.bookID)
        .findFormatHandle(BookDatabaseEntryFormatHandleAudioBook::class.java)

    if (formatHandleOpt == null) {
      val title =
        this.resources.getString(R.string.audio_book_player_error_book_open)
      this.showErrorWithRunnable(
        context = this,
        title = title,
        failure = IllegalStateException(title),
        execute = this::finish
      )
      return
    }

    this.formatHandle = formatHandleOpt

    /*
     * Create a new downloader that is solely used to fetch audio book manifests.
     */

    this.downloadExecutor =
      MoreExecutors.listeningDecorator(
        NamedThreadPools.namedThreadPool(1, "audiobook-player", 19)
      )
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
      if (this.supportFragmentManager.backStackEntryCount == 0) {
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
     * there may be scheduled tasks that try to execute after the activity has stopped. This
     * flag allows them to gracefully avoid running.
     */

    this.destroying = true

    /*
     * Cancel downloads, shut down the player, and close the book.
     */

    if (this.playerInitialized) {
      this.savePlayerPosition()
      this.cancelAllDownloads()

      try {
        this.player.close()
      } catch (e: Exception) {
        this.log.error("error closing player: ", e)
      }

      this.bookSubscription.unsubscribe()
      this.playerSubscription.unsubscribe()

      try {
        this.book.close()
      } catch (e: Exception) {
        this.log.error("error closing book: ", e)
      }
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

  override fun onLoadingFragmentWantsIOExecutor(): ListeningExecutorService {
    return this.downloadExecutor
  }

  override fun onLoadingFragmentIsNetworkConnectivityAvailable(): Boolean {
    return this.networkConnectivity.isNetworkAvailable
  }

  override fun onLoadingFragmentWantsAudioBookParameters(): AudioBookPlayerParameters {
    return this.parameters
  }

  override fun onLoadingFragmentLoadingFailed(exception: Exception) {
    this.showErrorWithRunnable(
      context = this,
      title = exception.message ?: "",
      failure = exception,
      execute = this::finish
    )
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
        downloadProvider = DownloadProvider.create(this.downloadExecutor),
        userAgent = PlayerUserAgent(this.parameters.userAgent)
      )
    )

    if (engine == null) {
      val title =
        this.resources.getString(R.string.audio_book_player_error_engine_open)
      this.showErrorWithRunnable(
        context = this,
        title = title,
        failure = IllegalStateException(title),
        execute = this::finish
      )
      return
    }

    this.log.debug(
      "selected audio engine: {} {}",
      engine.engineProvider.name(),
      engine.engineProvider.version()
    )

    /*
     * Load extensions.
     */

    val extensions =
      this.loadAndConfigureExtensions()

    /*
     * Create the audio book.
     */

    val bookResult =
      engine.bookProvider.create(
        context = this,
        extensions = extensions
      )

    if (bookResult is PlayerResult.Failure) {
      val title =
        this.resources.getString(R.string.audio_book_player_error_book_open)
      this.showErrorWithRunnable(
        context = this,
        title = title,
        failure = bookResult.failure,
        execute = this::finish
      )
      return
    }

    this.book = (bookResult as PlayerResult.Success).result
    this.player = this.book.createPlayer()

    this.bookSubscription =
      this.book.spineElementDownloadStatus.ofType(PlayerSpineElementDownloadExpired::class.java)
        .subscribe(this::onDownloadExpired)
    this.playerSubscription =
      this.player.events.subscribe(this::onPlayerEvent)

    this.playerInitialized = true

    this.restoreSavedPlayerPosition()
    this.startAllPartsDownloading()

    /*
     * Create and load the main player fragment into the holder view declared in the activity.
     */

    this.uiThread.runOnUIThread {
      // Sanity check; Verify the state of the lifecycle before continuing as it's possible the
      // activity could be finishing.
      if (!this.isFinishing && !this.supportFragmentManager.isDestroyed) {
        this.playerFragment = PlayerFragment.newInstance(PlayerFragmentParameters())

        this.supportFragmentManager
          .beginTransaction()
          .replace(R.id.audio_book_player_fragment_holder, this.playerFragment, "PLAYER")
          .commitAllowingStateLoss()
      }
    }
  }

  private fun downloadAndSaveManifest(
    credentials: AccountAuthenticationCredentials?
  ): PlayerManifest {
    this.log.debug("downloading and saving manifest")
    val strategy =
      this.parameters.toManifestStrategy(
        strategies = this.strategies,
        isNetworkAvailable = { this.networkConnectivity.isNetworkAvailable },
        credentials = credentials,
        cacheDirectory = this.cacheDir
      )
    return when (val strategyResult = strategy.execute()) {
      is TaskResult.Success -> {
        AudioBookHelpers.saveManifest(
          profiles = this.profiles,
          bookId = this.parameters.bookID,
          manifestURI = this.parameters.manifestURI,
          manifest = strategyResult.result.fulfilled
        )
        strategyResult.result.manifest
      }
      is TaskResult.Failure ->
        throw IOException(strategyResult.message)
    }
  }

  private fun onDownloadExpired(event: PlayerSpineElementDownloadExpired) {
    this.log.debug("onDownloadExpired: ", event.exception)

    if (this.reloadingManifest.compareAndSet(false, true)) {
      this.log.debug("attempting to download fresh manifest due to expired links")
      this.downloadExecutor.execute {
        try {
          this.book.replaceManifest(
            this.downloadAndSaveManifest(
              this.profiles.profileAccountForBook(this.parameters.bookID)
                .loginState
                .credentials
            )
          )
        } catch (e: Exception) {
          this.log.error("onDownloadExpired: failed to download/replace manifest: ", e)
        } finally {
          this.reloadingManifest.set(false)
        }
      }
    }
  }

  private fun loadAndConfigureExtensions(): List<PlayerExtensionType> {
    val extensions =
      ServiceLoader.load(PlayerExtensionType::class.java)
        .toList()

    val services = Services.serviceDirectory()
    this.loadAndConfigureFeedbooks(services, extensions)
    return extensions
  }

  private fun loadAndConfigureFeedbooks(
    services: ServiceDirectoryType,
    extensions: List<PlayerExtensionType>
  ) {
    val feedbooksConfigService =
      services.optionalService(AudioBookFeedbooksSecretServiceType::class.java)

    if (feedbooksConfigService != null) {
      this.log.debug("feedbooks configuration service is available; configuring extension")
      val extension =
        extensions.filterIsInstance<FeedbooksPlayerExtension>()
          .firstOrNull()
      if (extension != null) {
        this.log.debug("feedbooks extension is available")
        extension.configuration = feedbooksConfigService.configuration
      } else {
        this.log.debug("feedbooks extension is not available")
      }
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
    if (this.networkConnectivity.isNetworkAvailable) {
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
        this.onLogPlayerError(event)

      PlayerEvent.PlayerEventManifestUpdated ->
        Unit
    }
  }

  private fun onPlayerChapterCompleted(event: PlayerEventChapterCompleted) {
    if (event.spineElement.next == null) {
      this.log.debug("book has finished")

      /*
       * Wait a few seconds before displaying the dialog asking if the user wants
       * to return the book.
       */

      this.playerScheduledExecutor.schedule(
        {
          if (!this.destroying) {
            this.uiThread.runOnUIThread { this.loanReturnShowDialog() }
          }
        },
        5L, TimeUnit.SECONDS
      )
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

    try {
      this.books.bookRevoke(this.parameters.accountID, this.parameters.bookID)
    } catch (e: Exception) {
      this.log.error("could not execute revocation: ", e)
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

    this.uiThread.runOnUIThread {
      val fragment =
        PlayerPlaybackRateFragment.newInstance(PlayerFragmentParameters())
      fragment.show(this.supportFragmentManager, "PLAYER_RATE")
    }
  }

  override fun onPlayerSleepTimerShouldOpen() {
    /*
     * The player fragment wants us to open the sleep timer.
     */

    this.uiThread.runOnUIThread {
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

    this.uiThread.runOnUIThread {
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

    this.covers.loadCoverInto(
      FeedEntry.FeedEntryOPDS(this.parameters.accountID, this.parameters.opdsEntry),
      view,
      this.screenSize.dpToPixels(300).toInt(),
      this.screenSize.dpToPixels(400).toInt()
    )
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

  private fun showErrorWithRunnable(
    context: Context,
    title: String,
    failure: Exception,
    execute: () -> Unit
  ) {
    this.log.error("error: {}: ", title, failure)

    this.uiThread.runOnUIThread {
      AlertDialog.Builder(context)
        .setTitle(title)
        .setMessage(failure.localizedMessage)
        .setOnDismissListener {
          execute.invoke()
        }
        .show()
    }
  }
}
