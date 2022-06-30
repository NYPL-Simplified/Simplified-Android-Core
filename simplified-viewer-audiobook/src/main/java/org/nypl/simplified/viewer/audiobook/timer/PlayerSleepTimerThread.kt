package org.nypl.simplified.viewer.audiobook.timer

/**
 * A sleep timer thread. The purpose of this class is to allow for efficient checks of the form
 * "is the current thread a sleep timer thread?". Specifically, if the current thread is an instance
 * of PlayerSleepTimerThread, then the current thread is a timer thread.
 */

class PlayerSleepTimerThread(runnable: Runnable) : Thread(runnable) {

  init {
    this.name = "org.librarysimplified.audiobook.api:timer:${this.id}"
  }

  companion object {

    fun isSleepTimerThread(): Boolean {
      return Thread.currentThread() is PlayerSleepTimerThread
    }

    fun checkIsSleepTimerThread() {
      if (!isSleepTimerThread()) {
        throw IllegalStateException(
          StringBuilder(128)
            .append("Current thread is not a sleep timer thread!\n")
            .append("  Thread: ")
            .append(Thread.currentThread())
            .append('\n')
            .toString()
        )
      }
    }
  }
}
