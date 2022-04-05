package org.nypl.simplified.viewer.audiobook

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerSleepTimerType
import org.librarysimplified.audiobook.api.PlayerType
import org.librarysimplified.audiobook.views.*
import java.util.concurrent.ScheduledExecutorService

class AudiobookFragmentFactory(
  private val playerDelegate: Lazy<PlayerType>,
  private val bookDelegate: Lazy<PlayerAudioBookType>,
  private val listener: PlayerFragmentListenerType,
  private val scheduledExecutorService: ScheduledExecutorService,
  private val sleepTimer: PlayerSleepTimerType
) : FragmentFactory() {
  override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
    return when (className) {
      AudioBookLoadingFragment::class.java.name -> AudioBookLoadingFragment()
      PlayerFragment::class.java.name ->
        PlayerFragment(listener, playerDelegate.value, bookDelegate.value, scheduledExecutorService, sleepTimer)
      PlayerPlaybackRateFragment::class.java.name ->
        PlayerPlaybackRateFragment(listener, playerDelegate.value)
      PlayerSleepTimerFragment::class.java.name ->
        PlayerSleepTimerFragment(listener, playerDelegate.value, sleepTimer)
      PlayerTOCFragment::class.java.name ->
        PlayerTOCFragment(listener, bookDelegate.value, playerDelegate.value)
      else -> super.instantiate(classLoader, className)
    }
  }
}
