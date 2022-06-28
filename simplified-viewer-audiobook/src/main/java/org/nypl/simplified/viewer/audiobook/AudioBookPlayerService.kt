package org.nypl.simplified.viewer.audiobook

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
import org.nypl.simplified.viewer.audiobook.session.LifecycleMediaSessionService
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.r2.shared.publication.Locator
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

    internal var sessionHolder: AudioBookPlayerSessionHolder? = null
      private set

    fun closeSession() {
      sessionHolder?.let { holder ->
        savePlayerPosition(holder.navigator.currentLocator.value, holder)
      }

      stopForeground(true)
      saveLocationJob?.cancel()
      saveLocationJob = null
      sessionHolder?.close()
      sessionHolder = null
    }

    @OptIn(FlowPreview::class)
    fun bindNavigator(
      sessionHolder: AudioBookPlayerSessionHolder
    ) {
      addSession(sessionHolder.mediaSession)

      saveLocationJob = sessionHolder.navigator.currentLocator
        .sample(3000)
        .onEach {  locator -> savePlayerPosition(locator, sessionHolder) }
        .launchIn(lifecycleScope)

      this.sessionHolder = sessionHolder
    }

    private fun savePlayerPosition(
      locator: Locator,
      sessionHolder: AudioBookPlayerSessionHolder
    ) {
      val offset =
        locator.locations.fragments.first()
          .substringAfter("t=").toLong()

      val chapter =
        sessionHolder.navigator.publication.readingOrder.indexOfFirstWithHref(locator.href)!!

      val position = PlayerPosition(
        title = locator.title,
        part = 0,
        chapter = chapter ,
        offsetMilliseconds = offset
      )

      sessionHolder.formatHandle.savePlayerPosition(position)
      logger.debug("Position $position saved to the database.")
    }
  }

  private val binder by lazy {
    Binder()
  }

  override fun onCreate() {
    super.onCreate()
    logger.debug("AudioBookPlayerService created.")
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
    return binder.sessionHolder?.mediaSession
  }

  override fun onDestroy() {
    super.onDestroy()
    logger.debug("MediaService destroyed.")
  }

  override fun onTaskRemoved(rootIntent: Intent) {
    super.onTaskRemoved(rootIntent)
    logger.debug("Task removed. Stopping session and service.")
    // Close the navigator to allow the service to be stopped.
    binder.closeSession()
    stopSelf()
  }

  companion object {
    const val SERVICE_INTERFACE = "org.nypl.simplified.viewer.audiobook.service.AudioBookPlayerService"
  }
}
