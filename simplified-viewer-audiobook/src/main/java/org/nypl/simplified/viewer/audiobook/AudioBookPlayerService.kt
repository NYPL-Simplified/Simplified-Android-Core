package org.nypl.simplified.viewer.audiobook

import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.lifecycleScope
import androidx.media2.session.MediaSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import org.librarysimplified.audiobook.api.PlayerPosition
import org.librarysimplified.audiobook.player.api.PlayerType
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalMedia2::class, ExperimentalCoroutinesApi::class)
class AudioBookPlayerService : LifecycleMediaSessionService() {

  private val logger =
    LoggerFactory.getLogger(AudioBookPlayerService::class.java)

  /**
   * The service interface to be used by the app.
   */
  inner class Binder : android.os.Binder() {

    private var saveLocationJob: Job? = null

    var sessionHolder: AudioBookPlayerSessionHolder? = null
      private set

    fun closeNavigator() {
      stopForeground(true)
      saveLocationJob?.cancel()
      saveLocationJob = null
      sessionHolder?.close()
      sessionHolder = null
    }

    @OptIn(FlowPreview::class)
    fun bindNavigator(
      navigator: MediaNavigator,
      player: PlayerType, parameters: AudioBookPlayerParameters,
      formatHandle: BookDatabaseEntryFormatHandleAudioBook
    ): AudioBookPlayerSessionHolder {
      val activityIntent = createSessionActivityIntent(parameters)
      val session = navigator.session(applicationContext, activityIntent)
      addSession(session)

      val sessionHolder = AudioBookPlayerSessionHolder(
        parameters = parameters,
        navigator = navigator,
        session = session,
        player = player,
        formatHandle = formatHandle
      )

      saveLocationJob = navigator.currentLocator
        .sample(3000)
        .onEach {  locator -> savePlayerPosition(formatHandle, locator, navigator.publication) }
        .launchIn(lifecycleScope)

      this.sessionHolder = sessionHolder

      return sessionHolder
    }

    private fun savePlayerPosition(
      formatHandle: BookDatabaseEntryFormatHandleAudioBook,
      locator: Locator,
      publication: Publication
    ) {
      val offset =
        locator.locations.fragments.first()
          .substringAfter("t=").toLong()

      val chapter =
        publication.readingOrder.indexOfFirstWithHref(locator.href)!!

      val position = PlayerPosition(
        title = locator.title,
        part = 0,
        chapter = chapter ,
        offsetMilliseconds = offset
      )
      formatHandle.savePlayerPosition(position)
    }

    private fun createSessionActivityIntent(audioBookPlayerParameters: AudioBookPlayerParameters): PendingIntent {
      // This intent will be triggered when the notification is clicked.
      var flags = PendingIntent.FLAG_UPDATE_CURRENT
      flags = flags or PendingIntent.FLAG_IMMUTABLE

      val intent = AudioBookPlayerContract.createIntent(applicationContext, audioBookPlayerParameters)
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

      return PendingIntent.getActivity(applicationContext, 0, intent, flags)
    }
  }

  private val binder by lazy {
    Binder()
  }

  override fun onCreate() {
    super.onCreate()
    logger.debug("MediaService created.")
  }

  override fun onBind(intent: Intent): IBinder? {
    logger.debug("onBind called with $intent")

    return if (intent.action == SERVICE_INTERFACE) {
      super.onBind(intent)
      // Readium-aware client.
      logger.debug("Returning custom binder.")
      binder
    } else {
      // External controller.
      logger.debug("Returning MediaSessionService binder.")
      super.onBind(intent)
    }
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    return binder.sessionHolder?.session
  }

  override fun onDestroy() {
    super.onDestroy()
    logger.debug("MediaService destroyed.")
  }

  override fun onTaskRemoved(rootIntent: Intent) {
    super.onTaskRemoved(rootIntent)
    logger.debug("Task removed. Stopping session and service.")
    // Close the navigator to allow the service to be stopped.
    binder.closeNavigator()
    stopSelf()
  }

  companion object {
    const val SERVICE_INTERFACE = "org.nypl.simplified.viewer.audiobook.AudioBookPlayerService"
  }
}
