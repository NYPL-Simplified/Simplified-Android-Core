package org.nypl.simplified.viewer.audiobook.timer

import io.reactivex.Observable
import org.joda.time.Duration
import javax.annotation.concurrent.ThreadSafe

/**
 * The interface exposed by sleep timer implementations.
 *
 * Implementations of this interface are required to be thread-safe. That is, methods and properties
 * may be safely called/accessed from any thread.
 */

@ThreadSafe
interface PlayerSleepTimerType : AutoCloseable {

  /**
   * Start the timer. If a duration is given, the timer will count down over the given duration
   * and will periodically publish events giving the remaining time. If no duration is given, the
   * timer will wait indefinitely for a call to {@link #finish()}. If the timer is paused, the
   * timer will be unpaused.
   *
   * @param time The total duration for which the timer will run
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  @Throws(java.lang.IllegalStateException::class)
  fun start(time: Duration?)

  /**
   * Cancel the timer. The timer will stop and will publish an event indicating the current
   * state.
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  @Throws(java.lang.IllegalStateException::class)
  fun cancel()

  /**
   * Pause the timer. The timer will pause and will publish an event indicating the current
   * state.
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  @Throws(java.lang.IllegalStateException::class)
  fun pause()

  /**
   * Unpause the timer. The timer will unpause and will publish an event indicating the current
   * state.
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  @Throws(java.lang.IllegalStateException::class)
  fun unpause()

  /**
   * Finish the timer. This makes the timer behave exactly as if a duration had been given to
   * start and the duration has elapsed. If the timer is paused, the timer will be unpaused.
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  @Throws(java.lang.IllegalStateException::class)
  fun finish()

  /**
   * An observable indicating the current state of the timer. The observable is buffered such
   * that each new subscription will receive the most recently published status event, and will
   * then receive new status events as they are published.
   */

  val status: Observable<PlayerSleepTimerEvent>

  /**
   * Close the timer. After this method is called, it is an error to call any of the other methods
   * in the interface.
   */

  override fun close()

  /**
   * @return `true` if the timer has been closed.
   */

  val isClosed: Boolean

  /**
   * A type indicating that the timer is currently running.
   */

  data class Running(
    val paused: Boolean,
    val duration: Duration?
  )

  /**
   * A non-null value of type {@link Running} if the timer is running, and null if it is not.
   */

  val isRunning: Running?
}
