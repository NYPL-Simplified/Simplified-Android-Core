package org.nypl.simplified.books.audio

import org.nypl.simplified.taskrecorder.api.TaskResult
import rx.Observable

/**
 * A strategy for downloading, parsing, and license-checking an audio book manifest. A given
 * strategy may be executed any number of times and should (in the absence of errors) return
 * the same result each time it is executed.
 */

interface AudioBookManifestStrategyType {

  /**
   * An observable source of events published during fulfillment.
   */

  val events: Observable<String>

  /**
   * Execute the strategy.
   */

  fun execute(): TaskResult<AudioBookManifestData>
}
