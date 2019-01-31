package org.nypl.simplified.tests.books.controller

import com.fasterxml.jackson.databind.ObjectMapper
import junit.framework.Assert
import org.joda.time.LocalDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.nypl.simplified.books.controller.SimplifiedBearerToken
import org.nypl.simplified.books.controller.SimplifiedBearerTokenJSON
import java.io.File
import java.net.URI

open class SimplifiedBearerTokenContract {

  @JvmField
  @Rule
  val expected = ExpectedException.none()

  val objectMapper = ObjectMapper()

  @Test
  fun testSerializeRoundTrip() {
    val baseTime = LocalDateTime.now()

    val expected =
      SimplifiedBearerToken(
        accessToken = "ee22988f-c5a1-4944-8fa4-567e6933fd83",
        expiration = baseTime.plusSeconds(60),
        location = URI.create("http://example.com"))

    val serialized =
      SimplifiedBearerTokenJSON.serializeToText(objectMapper, baseTime, expected)

    val received =
      SimplifiedBearerTokenJSON.deserializeFromText(objectMapper, baseTime, serialized)

    Assert.assertEquals(expected, received)
  }

  @Test
  fun testSerializeRoundTripFile() {
    val file = File.createTempFile("simplified-bearer-token", "txt")

    val baseTime = LocalDateTime.now()

    val expected =
      SimplifiedBearerToken(
        accessToken = "ee22988f-c5a1-4944-8fa4-567e6933fd83",
        expiration = baseTime.plusSeconds(60),
        location = URI.create("http://example.com"))

    SimplifiedBearerTokenJSON.serializeToFile(objectMapper, baseTime, expected, file)

    val received =
      SimplifiedBearerTokenJSON.deserializeFromFile(objectMapper, baseTime, file)

    Assert.assertEquals(expected, received)
  }
}