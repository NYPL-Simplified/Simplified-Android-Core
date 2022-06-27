package org.nypl.simplified.viewer.audiobook.screens

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import org.readium.r2.shared.publication.Link
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalMedia2::class, ExperimentalTime::class)
internal class PlayerScreenState(
  private val mediaNavigator: MediaNavigator,
  private val opdsEntry: OPDSAcquisitionFeedEntry,
  private val navigatorScope: CoroutineScope
) {

  val title: String =
    opdsEntry.title

  val author: String? =
    opdsEntry.authors.firstOrNull()

  val readingOrder: List<Link> =
    mediaNavigator.publication.readingOrder

  val resource: State<MediaNavigator.Playback.Resource>
    get() = resourceMutable

  val paused: State<Boolean>
    get() = pausedMutable

  val error: State<Exception?>
    get() = errorMutable

  private val resourceMutable: MutableState<MediaNavigator.Playback.Resource> =
    mutableStateOf(mediaNavigator.playback.value.resource)

  private val pausedMutable: MutableState<Boolean> =
    mutableStateOf(mediaNavigator.playback.value.state == MediaNavigator.Playback.State.Paused)

  private val errorMutable: MutableState<Exception?> =
    mutableStateOf(null)

  private var preventPlaybackUpdate: Boolean =
    false

  private val commandMutex: Mutex =
    Mutex()

  init {
    mediaNavigator.playback
      .onEach(this::onPlaybackChange)
      .launchIn(navigatorScope)

    play()
  }

  private fun onPlaybackChange(playback: MediaNavigator.Playback) {
    if (playback.state == MediaNavigator.Playback.State.Error) {
      errorMutable.value = Exception("An error occurred in the player.")
    }

    if (!preventPlaybackUpdate) {
      updatePlayback(playback)
    }
  }

  private fun updatePlayback(playback: MediaNavigator.Playback) {
    if (playback.resource != resource.value) {
      resourceMutable.value = playback.resource
    }

    if ((playback.state == MediaNavigator.Playback.State.Paused) != paused.value) {
      pausedMutable.value = playback.state == MediaNavigator.Playback.State.Paused
    }
  }

  private fun executeCommand(block: suspend (MediaNavigator).() -> Unit) = navigatorScope.launch {
   executeCommandAsync(block)
  }

  private suspend fun executeCommandAsync(block: suspend (MediaNavigator).() -> Unit) = commandMutex.withLock {
    preventPlaybackUpdate = true
    mediaNavigator.block()
    preventPlaybackUpdate = false
    onPlaybackChange(mediaNavigator.playback.value)
  }

  fun goPrevious() = executeCommand {
    val currentIndex = resource.value.index
    if (currentIndex > 0) {
      go(mediaNavigator.publication.readingOrder[currentIndex - 1])
    }
  }

  fun goNext() = executeCommand {
    val currentIndex = resource.value.index
    if (currentIndex + 1 < mediaNavigator.publication.readingOrder.size) {
      mediaNavigator.go(mediaNavigator.publication.readingOrder[currentIndex + 1])
    }
  }

  fun go(link: Link) = executeCommand {
    mediaNavigator.go(link)
  }

  fun seek(position: Duration) = executeCommand {
    resourceMutable.value = resource.value.copy(position = position)
    val currentIndex = resource.value.index
    mediaNavigator.seek(currentIndex, position)
  }

  fun play() = executeCommand {
    mediaNavigator.play()
  }

  fun pause() = executeCommand {
    mediaNavigator.pause()
  }

  fun goBackward() = executeCommand {
    mediaNavigator.goBackward()
  }

  fun goForward() = executeCommand {
    mediaNavigator.goForward()
  }
}
