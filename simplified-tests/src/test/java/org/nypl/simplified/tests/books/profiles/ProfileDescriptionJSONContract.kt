package org.nypl.simplified.tests.books.profiles

import com.fasterxml.jackson.databind.ObjectMapper
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeZone
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.nypl.simplified.profiles.ProfileDescriptionJSON
import org.nypl.simplified.profiles.api.ProfileAttributes
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfilePreferences
import org.nypl.simplified.reader.api.ReaderPreferences
import org.slf4j.LoggerFactory

abstract class ProfileDescriptionJSONContract {

  private val logger = LoggerFactory.getLogger(ProfileDescriptionJSONContract::class.java)
  private val currentDateTimeZoneSystem = DateTimeZone.getDefault()

  @Before
  fun setUp() {
    DateTimeUtils.setCurrentMillisFixed(0L)
    DateTimeZone.setDefault(DateTimeZone.UTC)
  }

  @After
  fun tearDown() {
    DateTimeUtils.setCurrentMillisSystem()
    DateTimeZone.setDefault(currentDateTimeZoneSystem)
  }

  @Test
  fun testRoundTrip() {
    val mapper = ObjectMapper()

    val dateTime =
      DateTime.parse("2010-01-01T00:00:00Z")

    val description_0 =
      ProfileDescription(
        displayName = "Kermit",
        preferences = ProfilePreferences(
          ProfileDateOfBirth(dateTime, true),
          showTestingLibraries = false,
          hasSeenLibrarySelectionScreen = false,
          readerPreferences = ReaderPreferences.builder().build(),
          mostRecentAccount = null
        ),
        attributes = ProfileAttributes(
          sortedMapOf(
            Pair("a", "b"),
            Pair("c", "d"),
            Pair("e", "f")
          )
        )
      )

    val node =
      ProfileDescriptionJSON.serializeToJSON(mapper, description_0)
    val description_1 =
      ProfileDescriptionJSON.deserializeFromJSON(mapper, node)

    this.logger.debug("{}", ProfileDescriptionJSON.serializeToString(ObjectMapper(), description_1))
    Assert.assertEquals(description_0, description_1)
  }

  @Test
  fun testLFA_0() {
    val mapper = ObjectMapper()

    val description =
      ProfileDescriptionJSON.deserializeFromText(mapper, this.ofResource("profile-lfa-0.json"))

    this.logger.debug("{}", ProfileDescriptionJSON.serializeToString(ObjectMapper(), description))
    Assert.assertEquals("Eggbert", description.displayName)
    Assert.assertEquals("developer", description.attributes.role)
  }

  @Test
  fun testLFA_1() {
    val mapper = ObjectMapper()

    val description =
      ProfileDescriptionJSON.deserializeFromText(mapper, this.ofResource("profile-lfa-1.json"))

    this.logger.debug("{}", ProfileDescriptionJSON.serializeToString(ObjectMapper(), description))
    Assert.assertEquals("Newbert", description.displayName)
    Assert.assertEquals("male", description.attributes.gender)
    Assert.assertEquals("student", description.attributes.role)
    Assert.assertEquals("ຊັ້ນ 8", description.attributes.grade)
    Assert.assertEquals("ສົ້ນຂົວ", description.attributes.school)
  }

  @Test
  fun testNYPL_0() {
    val mapper = ObjectMapper()

    val description =
      ProfileDescriptionJSON.deserializeFromText(mapper, this.ofResource("profile-nypl-0.json"))

    this.logger.debug("{}", ProfileDescriptionJSON.serializeToString(ObjectMapper(), description))
    Assert.assertEquals("", description.displayName)
  }

  private fun ofResource(name: String): String {
    val bytes =
      ProfileDescriptionJSONContract::class.java.getResourceAsStream(
        "/org/nypl/simplified/tests/books/$name"
      )!!.readBytes()

    val text = String(bytes)
    this.logger.debug("{}: {}", name, text)
    return text
  }
}
