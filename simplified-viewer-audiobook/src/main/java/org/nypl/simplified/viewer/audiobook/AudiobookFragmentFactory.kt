package org.nypl.simplified.viewer.audiobook

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerSleepTimerType
import org.librarysimplified.audiobook.api.PlayerType
import org.librarysimplified.audiobook.views.PlayerFragment
import org.librarysimplified.audiobook.views.PlayerFragmentListenerType
import org.librarysimplified.audiobook.views.PlayerPlaybackRateFragment
import org.librarysimplified.audiobook.views.PlayerSleepTimerFragment
import org.librarysimplified.audiobook.views.PlayerTOCFragment
import java.util.concurrent.ScheduledExecutorService

class AudiobookFragmentFactory(
  private val player: PlayerType,
  private val book: PlayerAudioBookType,
  private val listener: PlayerFragmentListenerType,
  private val scheduledExecutorService: ScheduledExecutorService,
  private val sleepTimer: PlayerSleepTimerType
) : FragmentFactory() {
  override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
    return when (className) {
      AudioBookLoadingFragment::class.java.name -> AudioBookLoadingFragment()
      PlayerFragment::class.java.name -> {
        PlayerFragment(listener, player, book, scheduledExecutorService, sleepTimer)
      }
      PlayerPlaybackRateFragment::class.java.name ->
        PlayerPlaybackRateFragment(listener, player)
      PlayerSleepTimerFragment::class.java.name ->
        PlayerSleepTimerFragment(listener, sleepTimer)
      PlayerTOCFragment::class.java.name ->
        PlayerTOCFragment(listener, book, player)
      else -> super.instantiate(classLoader, className)
    }
  }
}
