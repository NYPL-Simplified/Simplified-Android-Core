package org.nypl.simplified.analytics.api

import org.joda.time.LocalDateTime
import java.net.URI
import java.util.UUID

/**
 * The type of analytics events.
 */

sealed class AnalyticsEvent {

  /**
   * The UUID of the profile that triggered the event.
   */

  abstract val profileUUID: UUID

  /**
   * The timestamp of the event.
   */

  abstract val timestamp: LocalDateTime

  /**
   * A user logged in to their profile.
   */

  data class ProfileLoggedIn(
    override val profileUUID: UUID,
    override val timestamp: LocalDateTime,

    /**
     * The current display name of the profile.
     */

    val displayName: String)
    : AnalyticsEvent()

  /**
   * A user logged out of their profile.
   */

  data class ProfileLoggedOut(
    override val profileUUID: UUID,
    override val timestamp: LocalDateTime,

    /**
     * The current display name of the profile.
     */

    val displayName: String)
    : AnalyticsEvent()

  /**
   * The user searched for something in the catalog.
   */

  data class CatalogSearched(
    override val profileUUID: UUID,
    override val timestamp: LocalDateTime,

    /**
     * The URI of the account provider (typically a UUID).
     */

    val accountProvider: URI,

    /**
     * The UUID of the account.
     */

    val accountUUID: UUID,

    /**
     * The search string query used.
     */

    val searchQuery: String)
    : AnalyticsEvent()

  /**
   * The user opened a book.
   */

  data class BookOpened(
    override val profileUUID: UUID,
    override val timestamp: LocalDateTime,

    /**
     * The URI of the account provider (typically a UUID).
     */

    val accountProvider: URI,

    /**
     * The UUID of the account.
     */

    val accountUUID: UUID,

    /**
     * The OPDS ID of the book.
     */

    val bookOPDSId: String,

    /**
     * The title of the book.
     */

    val bookTitle: String)
    : AnalyticsEvent()

  /**
   * The user turned a page in a book.
   */

  data class BookPageTurned(
    override val profileUUID: UUID,
    override val timestamp: LocalDateTime,

    /**
     * The URI of the account provider (typically a UUID).
     */

    val accountProvider: URI,

    /**
     * The UUID of the account.
     */

    val accountUUID: UUID,

    /**
     * The OPDS ID of the book.
     */

    val bookOPDSId: String,

    /**
     * The title of the book.
     */

    val bookTitle: String,

    /**
     * The current page of the book.
     */

    val bookPage: Int)
    : AnalyticsEvent()

  /**
   * The user closed a book.
   */

  data class BookClosed(
    override val profileUUID: UUID,
    override val timestamp: LocalDateTime,

    /**
     * The URI of the account provider (typically a UUID).
     */

    val accountProvider: URI,

    /**
     * The UUID of the account.
     */

    val accountUUID: UUID,

    /**
     * The OPDS ID of the book.
     */

    val bookOPDSId: String,

    /**
     * The title of the book.
     */

    val bookTitle: String)
    : AnalyticsEvent()

}
