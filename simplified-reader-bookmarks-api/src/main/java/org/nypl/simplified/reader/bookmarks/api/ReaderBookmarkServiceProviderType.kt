package org.nypl.simplified.reader.bookmarks.api

import io.reactivex.subjects.Subject
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType

/**
 * A provider of bookmark services.
 */

interface ReaderBookmarkServiceProviderType {

  /**
   * The various instances that bookmark services require.
   */

  data class Requirements(

    /**
     * A function that, given a runnable, will create a new thread.
     */

    val threads: (Runnable) -> Thread,

    /**
     * An observable value to which reader bookmark events can be published.
     */

    val events: Subject<ReaderBookmarkEvent>,

    /**
     * An interface through which to make HTTP calls to manage remote bookmarks.
     */

    val httpCalls: ReaderBookmarkHTTPCallsType,

    /**
     * A profile controller.
     */

    val profilesController: ProfilesControllerType
  )

  /**
   * Create a new bookmark service.
   */

  fun createService(
    requirements: Requirements
  ): ReaderBookmarkServiceType
}
