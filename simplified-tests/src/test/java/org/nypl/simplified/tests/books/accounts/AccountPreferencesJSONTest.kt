package org.nypl.simplified.tests.books.accounts

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.nypl.simplified.accounts.api.AccountPreferences
import org.nypl.simplified.accounts.json.AccountPreferencesJSON
import java.net.URI
import java.util.UUID

/**
 * @see AccountPreferencesJSON
 */

class AccountPreferencesJSONTest {

  private lateinit var mapper: ObjectMapper

  @JvmField
  @Rule
  var expected = ExpectedException.none()

  @Before
  fun setup() {
    this.mapper = ObjectMapper()
  }

  @Test
  @Throws(Exception::class)
  fun testRoundTrip0() {
    val prefs0 =
      AccountPreferences(
        bookmarkSyncingPermitted = false,
        catalogURIOverride = null,
        announcementsAcknowledged = listOf()
      )

    val prefs1 =
      AccountPreferencesJSON.deserializeFromJSON(
        AccountPreferencesJSON.serializeToJSON(mapper, prefs0)
      )
    Assert.assertEquals(prefs0, prefs1)
  }

  @Test
  @Throws(Exception::class)
  fun testRoundTrip1() {
    val prefs0 =
      AccountPreferences(
        bookmarkSyncingPermitted = false,
        catalogURIOverride = URI.create("https://www.example.com/"),
        announcementsAcknowledged = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
      )

    val prefs1 =
      AccountPreferencesJSON.deserializeFromJSON(
        AccountPreferencesJSON.serializeToJSON(mapper, prefs0)
      )
    Assert.assertEquals(prefs0, prefs1)
  }
}
