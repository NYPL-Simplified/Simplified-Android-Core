package org.nypl.simplified.migration.fake

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.joda.time.LocalDateTime
import org.nypl.simplified.migration.spi.MigrationEvent
import org.nypl.simplified.migration.spi.MigrationEvent.MigrationStepInProgress
import org.nypl.simplified.migration.spi.MigrationEvent.MigrationStepSucceeded
import org.nypl.simplified.migration.spi.MigrationEvent.Subject.ACCOUNT
import org.nypl.simplified.migration.spi.MigrationEvent.Subject.BOOK
import org.nypl.simplified.migration.spi.MigrationEvent.Subject.BOOKMARK
import org.nypl.simplified.migration.spi.MigrationReport
import org.nypl.simplified.migration.spi.MigrationType
import java.io.IOException

/**
 * A fake migration that always runs and takes ten seconds to do nothing.
 */

class FakeMigration : MigrationType {

  private val eventsObservable = PublishSubject.create<MigrationEvent>()
  private val eventLog = mutableListOf<MigrationEvent>()

  init {
    this.eventsObservable.subscribe { event -> this.eventLog.add(event) }
  }

  override val events: Observable<MigrationEvent>
    get() = this.eventsObservable

  override fun needsToRun(): Boolean {
    return true
  }

  private val messages = listOf(
    "Solving the halting problem...",
    "Reticulating splines...",
    "Factoring a 2048-bit prime...",
    "Solving the travelling salesman problem...",
    "Evaluating an infinite loop..."
  )

  override fun run(): MigrationReport {
    for (message in messages) {
      this.eventsObservable.onNext(MigrationStepInProgress(message))
      try {
        Thread.sleep(1000L)
      } catch (e: Exception) {
        Thread.currentThread().interrupt()
      }
    }

    this.eventsObservable.onNext(
      MigrationStepSucceeded(
        message = "Account \"Alexandria Public Library\" was created successfully.",
        subject = ACCOUNT
      )
    )
    this.eventsObservable.onNext(
      MigrationStepSucceeded(
        message = "Book \"How To Kill Insects; A 2000-Page Guide\" was copied successfully.",
        subject = BOOK
      )
    )
    this.eventsObservable.onNext(
      MigrationStepSucceeded(
        message = "Book \"How To Read Books\" was copied successfully.",
        subject = BOOK
      )
    )
    this.eventsObservable.onNext(
      MigrationEvent.MigrationStepError(
        message = "Book \"An Illustrated Guide To Cold, Soggy Things\" caught fire.",
        exception = IOException("Fire!"),
        attributes = mapOf(
          Pair("bookID", "ae35964219d1b24bba10a2fba9551a06219d701a8f3fff6e80750d09f4f3b495"),
          Pair("bookTitle", "An Illustrated Guide To Cold, Soggy Things")
        )
      )
    )
    this.eventsObservable.onNext(
      MigrationStepSucceeded(
        message = "Bookmark 1 was copied successfully.",
        subject = BOOKMARK
      )
    )
    this.eventsObservable.onNext(
      MigrationStepSucceeded(
        message = "Bookmark 2 was copied successfully.",
        subject = BOOKMARK
      )
    )
    this.eventsObservable.onNext(
      MigrationStepSucceeded(
        message = "Book \"Unlikely Mathematics\" was copied successfully.",
        subject = BOOK
      )
    )
    this.eventsObservable.onNext(
      MigrationStepSucceeded(
        message = "Account \"Babel Public Library\" was created successfully.",
        subject = ACCOUNT
      )
    )
    this.eventsObservable.onNext(
      MigrationStepSucceeded(
        message = "Account \"Alexandria Public Library\" was authenticated successfully.",
        subject = ACCOUNT
      )
    )
    this.eventsObservable.onNext(
      MigrationStepSucceeded(
        message = "Account \"Babel Public Library\" was authenticated successfully.",
        subject = ACCOUNT
      )
    )

    return MigrationReport(
      application = "Fake Application",
      migrationService = "FakeMigration",
      timestamp = LocalDateTime.now(),
      events = this.eventLog.toList()
    )
  }
}
