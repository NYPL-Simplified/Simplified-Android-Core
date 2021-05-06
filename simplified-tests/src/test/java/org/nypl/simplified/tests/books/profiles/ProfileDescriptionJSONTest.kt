package org.nypl.simplified.tests.books.profiles

import com.fasterxml.jackson.databind.ObjectMapper
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.nypl.simplified.profiles.ProfileDescriptionJSON
import org.nypl.simplified.profiles.api.ProfileAttributes
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfilePreferences
import org.nypl.simplified.reader.api.ReaderColorScheme
import org.nypl.simplified.reader.api.ReaderFontSelection
import org.nypl.simplified.reader.api.ReaderPreferences
import org.slf4j.LoggerFactory

class ProfileDescriptionJSONTest {

  private val logger = LoggerFactory.getLogger(ProfileDescriptionJSONTest::class.java)
  private val currentDateTimeZoneSystem = DateTimeZone.getDefault()

  @BeforeEach
  fun setUp() {
    DateTimeUtils.setCurrentMillisFixed(0L)
    DateTimeZone.setDefault(DateTimeZone.UTC)
  }

  @AfterEach
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
    assertEquals(description_0, description_1)
  }

  @Test
  fun testLFA_0() {
    val mapper = ObjectMapper()

    val description =
      ProfileDescriptionJSON.deserializeFromText(mapper, this.ofResource("profile-lfa-0.json"))

    this.logger.debug("{}", ProfileDescriptionJSON.serializeToString(ObjectMapper(), description))
    assertEquals("Eggbert", description.displayName)
    assertEquals("developer", description.attributes.role)
  }

  @Test
  fun testLFA_1() {
    val mapper = ObjectMapper()

    val description =
      ProfileDescriptionJSON.deserializeFromText(mapper, this.ofResource("profile-lfa-1.json"))

    this.logger.debug("{}", ProfileDescriptionJSON.serializeToString(ObjectMapper(), description))
    assertEquals("Newbert", description.displayName)
    assertEquals("male", description.attributes.gender)
    assertEquals("student", description.attributes.role)
    assertEquals("ຊັ້ນ 8", description.attributes.grade)
    assertEquals("ສົ້ນຂົວ", description.attributes.school)
  }

  @Test
  fun testNYPL_0() {
    val mapper = ObjectMapper()

    val description =
      ProfileDescriptionJSON.deserializeFromText(mapper, this.ofResource("profile-nypl-0.json"))

    this.logger.debug("{}", ProfileDescriptionJSON.serializeToString(ObjectMapper(), description))
    assertEquals("", description.displayName)
  }

  @Test
  fun testSMA92() {
    val mapper = ObjectMapper()
    val description =
      ProfileDescriptionJSON.deserializeFromText(mapper, this.ofResource("profile-sma-92.json"))

    assertEquals("", description.displayName)
    assertEquals(1.0, description.preferences.readerPreferences.brightness())
    assertEquals(100.0, description.preferences.readerPreferences.fontScale())
    assertEquals(ReaderFontSelection.READER_FONT_OPEN_DYSLEXIC, description.preferences.readerPreferences.fontFamily())
    assertEquals(ReaderColorScheme.SCHEME_WHITE_ON_BLACK, description.preferences.readerPreferences.colorScheme())
  }

  private fun ofResource(name: String): String {
    val bytes =
      ProfileDescriptionJSONTest::class.java.getResourceAsStream(
        "/org/nypl/simplified/tests/books/$name"
      )!!.readBytes()

    val text = String(bytes)
    this.logger.debug("{}: {}", name, text)
    return text
  }
}
