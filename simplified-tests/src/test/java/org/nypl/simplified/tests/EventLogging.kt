package org.nypl.simplified.tests

import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.slf4j.Logger
import java.util.concurrent.CountDownLatch

class EventLogging<T>(
  val logger: Logger,
  val events: Subject<T>,
  val latch: CountDownLatch,
  val eventLog: MutableList<T>
) {

  init {
    this.events.subscribe { event ->
      this.logger.debug("event: {}", event)
      this.eventLog.add(event)
      this.latch.countDown()
    }
  }

  companion object {

    fun <T> create(logger: Logger, requiredEventCount: Int): EventLogging<T> {
      return EventLogging(
        logger = logger,
        events = PublishSubject.create(),
        latch = CountDownLatch(requiredEventCount),
        eventLog = mutableListOf()
      )
    }
  }
}
