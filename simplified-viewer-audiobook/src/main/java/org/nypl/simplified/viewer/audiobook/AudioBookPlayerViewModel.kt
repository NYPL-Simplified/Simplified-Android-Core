package org.nypl.simplified.viewer.audiobook

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.books.audio.AudioBookFeedbooksSecretServiceType
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.networkconnectivity.api.NetworkConnectivityType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.viewer.audiobook.ui.navigation.Listener
import org.nypl.simplified.viewer.audiobook.ui.navigation.Screen
import org.nypl.simplified.viewer.audiobook.ui.screens.PlayerScreenState
import org.nypl.simplified.viewer.audiobook.session.SessionBuilder
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.r2.shared.util.Try
import org.slf4j.LoggerFactory

@OptIn(ExperimentalMedia2::class, ExperimentalCoroutinesApi::class)
internal class AudioBookPlayerViewModel private constructor(
  private val application: Application,
  private val parameters: AudioBookPlayerParameters,
  private val sessionBuilder: SessionBuilder
) : ViewModel() {

  private val logger =
    LoggerFactory.getLogger(AudioBookPlayerViewModel::class.java)

  private val serviceBinder: CompletableDeferred<AudioBookPlayerService.Binder> =
    CompletableDeferred()

  init {
    startAudioBookPlayerService()
    tryStartSession()
  }

  private fun startAudioBookPlayerService() {
    val mediaServiceConnection = object : ServiceConnection {

      override fun onServiceConnected(name: ComponentName?, service: IBinder) {
        logger.debug("MediaService bound.")
        serviceBinder.complete(service as AudioBookPlayerService.Binder)
      }

      override fun onServiceDisconnected(name: ComponentName) {
        logger.debug("MediaService disconnected.")
        // Should not happen, do nothing.
      }

      override fun onNullBinding(name: ComponentName) {
        logger.debug("Failed to bind to MediaService.")
        // Should not happen, do nothing.
      }
    }

    // AudioBookPlayerService.onBind requires the intent to have a non-null action.
    val intent = Intent(AudioBookPlayerService.SERVICE_INTERFACE)
      .apply { setClass(application, AudioBookPlayerService::class.java) }
    application.startService(intent)
    application.bindService(intent, mediaServiceConnection, 0)
  }

  private fun tryStartSession() {
    viewModelScope.launch {
      getSession().fold(
        onSuccess = { session ->
          listener.onPlayerReady(
            PlayerScreenState(session.navigator, viewModelScope)
          )
        },
        onFailure = {
          listener.onLoadingException(it)
        }
      )
    }
  }

  private suspend fun getSession(): Try<AudioBookPlayerSessionHolder, Exception> {
    val binder = serviceBinder.await()

    binder.sessionHolder
      ?.takeIf { (it.parameters.bookID == parameters.bookID) }
      ?.let { return Try.success(it) }

    binder.sessionHolder?.close()

    val sessionHolderTry =
      sessionBuilder.open(this.parameters)

    sessionHolderTry.onSuccess { holder ->
      holder.navigator.play()
      serviceBinder.await().bindNavigator(holder)
    }

    return sessionHolderTry
  }

  private val listener: Listener =
    Listener()

  val currentScreen: StateFlow<Screen>
    get() = listener.currentScreen

  fun onBackstackPressed(): Boolean {
    val hasNavigated = listener.onBackstackPressed()
    val shouldFinish = !hasNavigated
    if (shouldFinish) {
      onCloseActivity()
    }
    return shouldFinish
  }

  private fun onCloseActivity() {
    if (serviceBinder.isCompleted)
      serviceBinder.getCompleted().closeSession()
  }

  internal class Factory(
    private val application: Application,
    private val parameters: AudioBookPlayerParameters,
    services: ServiceDirectoryType
  ) : ViewModelProvider.NewInstanceFactory() {

    private val strategies =
      services.requireService(AudioBookManifestStrategiesType::class.java)
    private val networkConnectivity =
      services.requireService(NetworkConnectivityType::class.java)
    private val profiles =
      services.requireService(ProfilesControllerType::class.java)
    private val covers =
        services.requireService(BookCoverProviderType::class.java)
    private val feedbooksConfigService =
      services.optionalService(AudioBookFeedbooksSecretServiceType::class.java)

    private val sessionBuilder : SessionBuilder =
      SessionBuilder(
        application,
        profiles,
        strategies,
        networkConnectivity,
        application.cacheDir,
        feedbooksConfigService
      )

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return when {
        modelClass.isAssignableFrom(AudioBookPlayerViewModel::class.java) ->
          AudioBookPlayerViewModel(application, parameters, sessionBuilder) as T
        else ->
          super.create(modelClass)
      }
    }
  }
}
