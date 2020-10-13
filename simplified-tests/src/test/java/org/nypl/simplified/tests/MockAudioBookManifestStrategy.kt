package org.nypl.simplified.tests

import org.nypl.simplified.books.audio.AudioBookManifestData
import org.nypl.simplified.books.audio.AudioBookManifestStrategyType
import org.nypl.simplified.taskrecorder.api.TaskResult
import rx.Observable
import rx.subjects.PublishSubject

class MockAudioBookManifestStrategy : AudioBookManifestStrategyType {

  var onExecute: () -> TaskResult<AudioBookManifestData> = {
    TaskResult.fail("Failed", "Failed", "failed")
  }

  var eventSubject: PublishSubject<String> =
    PublishSubject.create()

  override val events: Observable<String> =
    this.eventSubject

  override fun execute(): TaskResult<AudioBookManifestData> {
    return this.onExecute.invoke()
  }
}
