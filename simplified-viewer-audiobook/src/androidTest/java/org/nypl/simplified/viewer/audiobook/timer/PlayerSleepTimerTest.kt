package org.nypl.simplified.viewer.audiobook.timer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.runner.RunWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlayerSleepTimerTest : PlayerSleepTimerContract() {
  override fun create(): PlayerSleepTimerType {
    return PlayerSleepTimer.create()
  }

  override fun logger(): Logger {
    return LoggerFactory.getLogger(PlayerSleepTimerTest::class.java)
  }
}
