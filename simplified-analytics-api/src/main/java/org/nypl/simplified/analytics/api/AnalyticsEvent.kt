package org.nypl.simplified.analytics.api

import org.joda.time.LocalDateTime
import java.net.URI
import java.util.UUID

/**
 * The type of analytics events.
 */

sealed class AnalyticsEvent {

  /**
   * The timestamp of the event.
   */

  abstract val timestamp: LocalDateTime

  /**
   * A user logged in to their profile.
   */

  data class ApplicationOpened(
    override val timestamp: LocalDateTime = LocalDateTime.now(),

    /**
     * The name of the application package.
     */

    val packageName: String,

    /**
     * The application version string.
     */

    val packageVersion: String,

    /**
     * The application version code.
     */

    val packageVersionCode: Int)
    : AnalyticsEvent()

  /**
   * A user logged in to their profile.
   */

  data class ProfileLoggedIn(
    val profileUUID: UUID,
    override val timestamp: LocalDateTime = LocalDateTime.now(),

    /**
     * The current display name of the profile.
     */

    val displayName: String?,

    /**
     * The gender of the profile owner.
     */

    val gender: String?,

    /**
     * The birth date of the profile owner.
     */

    val birthDate: String?)
    : AnalyticsEvent()

  /**
   * A user logged out of their profile.
   */

  data class ProfileLoggedOut(
    val profileUUID: UUID,
    override val timestamp: LocalDateTime = LocalDateTime.now(),

    /**
     * The current display name of the profile.
     */

    val displayName: String)
    : AnalyticsEvent()

  /**
   * The user searched for something in the catalog.
   */

  data class CatalogSearched(
    val profileUUID: UUID,
    override val timestamp: LocalDateTime = LocalDateTime.now(),

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
    val profileUUID: UUID,
    override val timestamp: LocalDateTime = LocalDateTime.now(),

    /**
     * The display name of the profile
     */

    val profileDisplayName: String,

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
    val profileUUID: UUID,
    override val timestamp: LocalDateTime = LocalDateTime.now(),

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

    val bookPage: Int,

    /**
     * The number of pages in the book.
     */

    val bookPagesTotal: Int,

    /**
     * The title of the page.
     */

    val bookPageTitle: String)
    : AnalyticsEvent()

  /**
   * The user closed a book.
   */

  data class BookClosed(
    val profileUUID: UUID,
    override val timestamp: LocalDateTime = LocalDateTime.now(),

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
