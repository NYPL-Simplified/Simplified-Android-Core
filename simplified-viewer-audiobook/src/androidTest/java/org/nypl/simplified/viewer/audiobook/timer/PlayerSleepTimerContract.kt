package org.nypl.simplified.viewer.audiobook.timer

import org.joda.time.Duration
import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.viewer.audiobook.timer.PlayerSleepTimerEvent.PlayerSleepTimerCancelled
import org.nypl.simplified.viewer.audiobook.timer.PlayerSleepTimerEvent.PlayerSleepTimerFinished
import org.nypl.simplified.viewer.audiobook.timer.PlayerSleepTimerEvent.PlayerSleepTimerRunning
import org.nypl.simplified.viewer.audiobook.timer.PlayerSleepTimerEvent.PlayerSleepTimerStopped
import org.slf4j.Logger
import java.util.concurrent.CountDownLatch

/**
 * Test contract for the {@link org.librarysimplified.audiobook.api.PlayerSleepTimerType} interface.
 */

abstract class PlayerSleepTimerContract {

  abstract fun create(): PlayerSleepTimerType

  abstract fun logger(): Logger

  /**
   * Opening a timer and then closing it works. Closing it multiple times isn't an issue.
   */

  @Test
  fun testOpenClose() {
    val timer = this.create()
    Assert.assertFalse("Timer not closed", timer.isClosed)
    timer.close()
    Assert.assertTrue("Timer is closed", timer.isClosed)
    timer.close()
    Assert.assertTrue("Timer is closed", timer.isClosed)
  }

  /**
   * Opening a timer, starting it, and letting it count down to completion works.
   */

  @Test(timeout = 10_000L)
  fun testCountdown() {
    val logger = this.logger()
    val timer = this.create()

    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()

    timer.status.subscribe(
      { event ->
        logger.debug("event: {}", event)
        events.add(
          when (event) {
            PlayerSleepTimerStopped -> "stopped"
            is PlayerSleepTimerRunning -> "running"
            is PlayerSleepTimerCancelled -> "cancelled"
            PlayerSleepTimerFinished -> "finished"
          }
        )
      },
      { waitLatch.countDown() },
      { waitLatch.countDown() }
    )

    logger.debug("starting timer")
    timer.start(Duration.millis(3000L))

    logger.debug("waiting for timer")
    Thread.sleep(1000L)
    Assert.assertNotNull(timer.isRunning)
    Thread.sleep(1000L)
    Thread.sleep(1000L)
    Thread.sleep(1000L)

    logger.debug("closing timer")
    timer.close()

    waitLatch.await()

    logger.debug("events: {}", events)
    Assert.assertEquals(7, events.size)
    Assert.assertEquals("stopped", events[0])
    Assert.assertEquals("running", events[1])
    Assert.assertEquals("running", events[2])
    Assert.assertEquals("running", events[3])
    Assert.assertEquals("running", events[4])
    Assert.assertEquals("finished", events[5])
    Assert.assertEquals("stopped", events[6])
  }

  /**
   * Opening a timer, starting it, and then cancelling it, works.
   */

  @Test(timeout = 10_000L)
  fun testCancel() {

    val logger = this.logger()
    val timer = this.create()

    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()

    timer.status.subscribe(
      { event ->
        logger.debug("event: {}", event)
        events.add(
          when (event) {
            PlayerSleepTimerStopped -> "stopped"
            is PlayerSleepTimerRunning -> "running"
            is PlayerSleepTimerCancelled -> "cancelled"
            PlayerSleepTimerFinished -> "finished"
          }
        )
      },
      { waitLatch.countDown() },
      { waitLatch.countDown() }
    )

    logger.debug("starting timer")
    timer.start(Duration.millis(3000L))

    logger.debug("waiting for timer")
    Thread.sleep(1000L)
    Assert.assertNotNull(timer.isRunning)

    logger.debug("cancelling timer")
    timer.cancel()

    logger.debug("waiting for timer")
    Thread.sleep(1000L)
    Assert.assertNull(timer.isRunning)

    logger.debug("closing timer")
    timer.close()

    waitLatch.await()

    logger.debug("events: {}", events)
    Assert.assertTrue("Must receive at least 4 events", events.size >= 4)
    Assert.assertEquals("stopped", events.first())
    Assert.assertTrue("Received at least a cancelled event", events.contains("cancelled"))
    Assert.assertTrue("Received at least a running event", events.contains("running"))
    Assert.assertEquals("stopped", events.last())
  }

  /**
   * Opening a timer, starting it, and then cancelling it, works.
   */

  @Test(timeout = 10_000L)
  fun testCancelImmediate() {
    val logger = this.logger()
    val timer = this.create()

    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()

    timer.status.subscribe(
      { event ->
        logger.debug("event: {}", event)
        events.add(
          when (event) {
            PlayerSleepTimerStopped -> "stopped"
            is PlayerSleepTimerRunning -> "running"
            is PlayerSleepTimerCancelled -> "cancelled"
            PlayerSleepTimerFinished -> "finished"
          }
        )
      },
      { waitLatch.countDown() },
      { waitLatch.countDown() }
    )

    logger.debug("starting timer")
    timer.start(Duration.millis(3000L))

    logger.debug("cancelling timer")
    timer.cancel()
    Thread.sleep(250L)
    Assert.assertNull(timer.isRunning)

    logger.debug("closing timer")
    timer.close()

    waitLatch.await()

    logger.debug("events: {}", events)
    Assert.assertTrue("Must have received at least one events", events.size >= 1)
    Assert.assertEquals("stopped", events.first())

    /*
     * This is timing sensitive. We may not receive a cancelled event if the timer doesn't even
     * have time to start.
     */

    if (events.size >= 4) {
      Assert.assertTrue("Received at least a running event", events.contains("running"))
    }
    if (events.size >= 3) {
      Assert.assertTrue("Received at least a cancelled event", events.contains("cancelled"))
    }

    Assert.assertEquals("stopped", events.last())
  }

  /**
   * Opening a timer, starting it, and then restarting it with a new time, works.
   */

  @Test(timeout = 10_000L)
  fun testRestart() {
    val events = ArrayList<String>()

    val logger = this.logger()
    val timer = this.create()

    timer.status.subscribe { event ->
      logger.debug("event: {}", event)

      events.add(
        when (event) {
          PlayerSleepTimerStopped -> "stopped"
          is PlayerSleepTimerRunning -> "running " + event.remaining
          is PlayerSleepTimerCancelled -> "cancelled"
          PlayerSleepTimerFinished -> "finished"
        }
      )
    }

    logger.debug("starting timer")
    timer.start(Duration.millis(4000L))

    logger.debug("waiting for timer")
    Thread.sleep(1000L)

    logger.debug("restarting timer")
    timer.start(Duration.millis(6000L))

    logger.debug("waiting for timer")
    Thread.sleep(1000L)
    Assert.assertNotNull(timer.isRunning)

    logger.debug("closing timer")
    timer.close()
    Thread.sleep(1000L)

    logger.debug("events: {}", events)
    Assert.assertTrue("Must have received at least 4 events", events.size >= 4)
    Assert.assertEquals("stopped", events.first())
    Assert.assertTrue(events.contains("running PT4S"))
    Assert.assertTrue(events.contains("running PT6S"))
    Assert.assertEquals("stopped", events.last())
  }

  /**
   * Running the timer to completion repeatedly, works.
   */

  @Test(timeout = 10_000L)
  fun testCompletionRepeated() {
    val logger = this.logger()
    val timer = this.create()

    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()

    timer.status.subscribe(
      { event ->
        logger.debug("event: {}", event)
        events.add(
          when (event) {
            PlayerSleepTimerStopped -> "stopped"
            is PlayerSleepTimerRunning -> "running " + event.remaining
            is PlayerSleepTimerCancelled -> "cancelled"
            PlayerSleepTimerFinished -> "finished"
          }
        )
      },
      { waitLatch.countDown() },
      { waitLatch.countDown() }
    )

    logger.debug("starting timer")
    timer.start(Duration.millis(1000L))

    logger.debug("waiting for timer")
    Thread.sleep(2000L)

    logger.debug("restarting timer")
    timer.start(Duration.millis(1000L))

    logger.debug("waiting for timer")
    Thread.sleep(2000L)

    logger.debug("closing timer")
    timer.close()

    waitLatch.await()

    logger.debug("events: {}", events)
    Assert.assertEquals("Must have received 8 events", 8, events.size)
    Assert.assertEquals("stopped", events[0])
    Assert.assertEquals("running PT1S", events[1])
    Assert.assertEquals("running PT0S", events[2])
    Assert.assertEquals("finished", events[3])
    Assert.assertEquals("running PT1S", events[4])
    Assert.assertEquals("running PT0S", events[5])
    Assert.assertEquals("finished", events[6])
    Assert.assertEquals("stopped", events[7])
  }

  /**
   * Explicit completion works.
   */

  @Test(timeout = 10_000L)
  fun testCompletionIndefinite() {
    val logger = this.logger()
    val timer = this.create()

    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()

    timer.status.subscribe(
      { event ->
        logger.debug("event: {}", event)
        events.add(
          when (event) {
            PlayerSleepTimerStopped -> "stopped"
            is PlayerSleepTimerRunning -> "running " + event.remaining
            is PlayerSleepTimerCancelled -> "cancelled"
            PlayerSleepTimerFinished -> "finished"
          }
        )
      },
      { waitLatch.countDown() },
      { waitLatch.countDown() }
    )

    logger.debug("starting timer")
    timer.start(null)

    logger.debug("waiting for timer")
    Thread.sleep(1000L)

    logger.debug("finishing timer")
    timer.finish()

    logger.debug("waiting for timer")
    Thread.sleep(1000L)

    logger.debug("finishing timer")
    timer.finish()

    logger.debug("waiting for timer")
    Thread.sleep(1000L)
    Assert.assertNull(timer.isRunning)

    timer.close()

    waitLatch.await()

    logger.debug("events: {}", events)
    Assert.assertEquals("Must have received 4 events", 4, events.size)
    Assert.assertEquals("stopped", events[0])
    Assert.assertEquals("running null", events[1])
    Assert.assertEquals("finished", events[2])
    Assert.assertEquals("stopped", events[3])
  }

  /**
   * Explicit completion works.
   */

  @Test(timeout = 10_000L)
  fun testCompletionTimed() {
    val logger = this.logger()
    val timer = this.create()

    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()

    timer.status.subscribe(
      { event ->
        logger.debug("event: {}", event)
        events.add(
          when (event) {
            PlayerSleepTimerStopped -> "stopped"
            is PlayerSleepTimerRunning -> "running " + event.remaining
            is PlayerSleepTimerCancelled -> "cancelled"
            PlayerSleepTimerFinished -> "finished"
          }
        )
      },
      { waitLatch.countDown() },
      { waitLatch.countDown() }
    )

    logger.debug("starting timer")
    timer.start(Duration.standardSeconds(2L))

    logger.debug("waiting for timer")
    Thread.sleep(500L)

    logger.debug("finishing timer")
    timer.finish()

    logger.debug("waiting for timer")
    Thread.sleep(1000L)

    timer.close()

    waitLatch.await()

    logger.debug("events: {}", events)
    Assert.assertEquals("Must have received 4 events", 4, events.size)
    Assert.assertEquals("stopped", events[0])
    Assert.assertEquals("running PT2S", events[1])
    Assert.assertEquals("finished", events[2])
    Assert.assertEquals("stopped", events[3])
  }

  /**
   * Pausing a timer works.
   */

  @Test(timeout = 10_000L)
  fun testPause() {
    val logger = this.logger()
    val timer = this.create()

    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()

    timer.status.subscribe(
      { event ->
        logger.debug("event: {}", event)
        events.add(
          when (event) {
            PlayerSleepTimerStopped -> "stopped"
            is PlayerSleepTimerRunning -> "running" + (if (event.paused) " paused" else "")
            is PlayerSleepTimerCancelled -> "cancelled"
            PlayerSleepTimerFinished -> "finished"
          }
        )
      },
      { waitLatch.countDown() },
      { waitLatch.countDown() }
    )

    logger.debug("starting timer")
    timer.start(Duration.millis(3000L))

    logger.debug("waiting for timer")
    Thread.sleep(1000L)

    timer.pause()
    Thread.sleep(1000L)
    val running = timer.isRunning!!
    Assert.assertTrue("Is paused", running.paused)

    Thread.sleep(1000L)
    Thread.sleep(1000L)

    logger.debug("closing timer")
    timer.close()

    waitLatch.await()

    logger.debug("events: {}", events)
    val distinctEvents = withoutSuccessiveDuplicates(events)
    logger.debug("distinctEvents: {}", distinctEvents)

    logger.debug("events: {}", events)
    Assert.assertEquals(4, distinctEvents.size)
    Assert.assertEquals("stopped", distinctEvents[0])
    Assert.assertEquals("running", distinctEvents[1])
    Assert.assertEquals("running paused", distinctEvents[2])
    Assert.assertEquals("stopped", distinctEvents[3])
  }

  /**
   * Pausing and unpausing a timer works.
   */

  @Test(timeout = 10_000L)
  fun testUnpause() {
    val logger = this.logger()
    val timer = this.create()

    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()

    timer.status.subscribe(
      { event ->
        logger.debug("event: {}", event)
        events.add(
          when (event) {
            PlayerSleepTimerStopped -> "stopped"
            is PlayerSleepTimerRunning -> "running" + (if (event.paused) " paused" else "")
            is PlayerSleepTimerCancelled -> "cancelled"
            PlayerSleepTimerFinished -> "finished"
          }
        )
      },
      { waitLatch.countDown() },
      { waitLatch.countDown() }
    )

    logger.debug("starting timer")
    timer.start(Duration.millis(3000L))

    logger.debug("waiting for timer")
    timer.pause()

    Thread.sleep(1000L)
    val running = timer.isRunning!!
    Assert.assertTrue("Is paused", running.paused)

    Thread.sleep(1000L)

    timer.unpause()
    Thread.sleep(1000L)
    val stillRunning = timer.isRunning!!
    Assert.assertFalse("Is not paused", stillRunning.paused)

    Thread.sleep(1000L)
    Thread.sleep(1000L)
    Thread.sleep(1000L)
    Thread.sleep(1000L)

    logger.debug("closing timer")
    timer.close()

    waitLatch.await()

    logger.debug("events: {}", events)
    val distinctEvents = withoutSuccessiveDuplicates(events)
    logger.debug("distinctEvents: {}", distinctEvents)

    Assert.assertEquals(6, distinctEvents.size)
    Assert.assertEquals("stopped", distinctEvents[0])
    Assert.assertEquals("running", distinctEvents[1])
    Assert.assertEquals("running paused", distinctEvents[2])
    Assert.assertEquals("running", distinctEvents[3])
    Assert.assertEquals("finished", distinctEvents[4])
    Assert.assertEquals("stopped", distinctEvents[5])
  }

  /**
   * Sending unpause requests to an unpaused timer is redundant.
   */

  @Test(timeout = 10_000L)
  fun testUnpauseRedundant() {
    val logger = this.logger()
    val timer = this.create()

    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()

    timer.status.subscribe(
      { event ->
        logger.debug("event: {}", event)
        events.add(
          when (event) {
            PlayerSleepTimerStopped -> "stopped"
            is PlayerSleepTimerRunning -> "running" + (if (event.paused) " paused" else "")
            is PlayerSleepTimerCancelled -> "cancelled"
            PlayerSleepTimerFinished -> "finished"
          }
        )
      },
      { waitLatch.countDown() },
      { waitLatch.countDown() }
    )

    logger.debug("starting timer")
    timer.start(Duration.millis(3000L))

    logger.debug("waiting for timer")
    Thread.sleep(1000L)

    Thread.sleep(1000L)
    val running = timer.isRunning!!
    Assert.assertFalse("Is paused", running.paused)

    timer.unpause()
    Thread.sleep(1000L)
    val stillRunning = timer.isRunning!!
    Assert.assertFalse("Is not paused", stillRunning.paused)

    Thread.sleep(1000L)
    Thread.sleep(1000L)
    Thread.sleep(1000L)

    logger.debug("closing timer")
    timer.close()

    waitLatch.await()

    logger.debug("events: {}", events)
    val distinctEvents = withoutSuccessiveDuplicates(events)
    logger.debug("distinctEvents: {}", distinctEvents)

    Assert.assertEquals(4, distinctEvents.size)
    Assert.assertEquals("stopped", distinctEvents[0])
    Assert.assertEquals("running", distinctEvents[1])
    Assert.assertEquals("finished", distinctEvents[2])
    Assert.assertEquals("stopped", distinctEvents[3])
  }

  private fun <T> withoutSuccessiveDuplicates(values: List<T>): List<T> {
    var current: T? = null
    val results = ArrayList<T>()
    for (x in values) {
      if (x != current) {
        results.add(x)
        current = x
      }
    }
    return results
  }
}
