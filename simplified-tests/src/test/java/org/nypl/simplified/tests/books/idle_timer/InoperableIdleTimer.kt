package org.nypl.simplified.tests.books.idle_timer

import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimerType

class InoperableIdleTimer : ProfileIdleTimerType {
  override fun start() {
  }

  override fun stop() {
  }

  override fun reset() {
  }

  override fun setWarningIdleSecondsRemaining(time: Int) {
  }

  override fun setMaximumIdleSeconds(time: Int) {
  }

  override fun maximumIdleSeconds(): Int {
    return 60_000
  }

  override fun currentIdleSeconds(): Int {
    return 60_000
  }
}
