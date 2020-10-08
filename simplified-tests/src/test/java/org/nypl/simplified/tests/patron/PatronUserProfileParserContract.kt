package org.nypl.simplified.tests.patron

import org.hamcrest.core.IsInstanceOf
import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.parser.api.ParseResult.Failure
import org.nypl.simplified.parser.api.ParseResult.Success
import org.nypl.simplified.patron.api.PatronDRMAdobe
import org.nypl.simplified.patron.api.PatronUserProfile
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.slf4j.Logger
import java.io.InputStream
import java.net.URI

abstract class PatronUserProfileParserContract {

  abstract val logger: Logger

  abstract val parsers: PatronUserProfileParsersType

  @Test
  fun testEmpty() {
    val parser =
      this.parsers.createParser(URI.create("urn:x"), resource("empty.json"))

    val result = parser.parse()
    this.dump(result)
    Assert.assertThat(result, IsInstanceOf(Failure::class.java))
  }

  @Test
  fun testEmpty2() {
    val parser =
      this.parsers.createParser(URI.create("urn:x"), resource("empty2.json"))

    val result = parser.parse()
    this.dump(result)
    Assert.assertThat(result, IsInstanceOf(Failure::class.java))
  }

  @Test
  fun testExampleDRMMalformed() {
    val parser =
      this.parsers.createParser(URI.create("urn:x"), resource("example-drm-malformed.json"))

    val result = parser.parse()
    this.dump(result)
    Assert.assertThat(result, IsInstanceOf(Failure::class.java))
  }

  @Test
  fun testExample() {
    val parser =
      this.parsers.createParser(URI.create("urn:x"), resource("example.json"))

    val result = parser.parse()
    this.dump(result)
    Assert.assertThat(result, IsInstanceOf(Success::class.java))

    val success = result as Success
    val profile = success.result

    Assert.assertEquals(
      "6120696828384",
      profile.authorization?.identifier
    )
    Assert.assertEquals(
      "2019-08-02T00:00:00.000Z",
      profile.authorization?.expires.toString()
    )
    Assert.assertEquals(
      true,
      profile.settings.synchronizeAnnotations
    )

    Assert.assertEquals(1, profile.drm.size)

    val drmAdobe = profile.drm.map { a -> a as PatronDRMAdobe }.first()
    Assert.assertEquals("NYPL", drmAdobe.vendor)
    Assert.assertEquals(URI("http://librarysimplified.org/terms/drm/scheme/ACS"), drmAdobe.scheme)
    Assert.assertEquals("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K", drmAdobe.clientToken)
  }

  @Test
  fun testExampleWithDeviceManagement() {
    val parser =
      this.parsers.createParser(URI.create("urn:x"), resource("example-with-device.json"))

    val result = parser.parse()
    this.dump(result)
    Assert.assertThat(result, IsInstanceOf(Success::class.java))

    val success = result as Success
    val profile = success.result

    Assert.assertEquals(
      "6120696828384",
      profile.authorization?.identifier
    )
    Assert.assertEquals(
      "2019-08-02T00:00:00.000Z",
      profile.authorization?.expires.toString()
    )
    Assert.assertEquals(
      true,
      profile.settings.synchronizeAnnotations
    )

    Assert.assertEquals(1, profile.drm.size)

    val drmAdobe = profile.drm.map { a -> a as PatronDRMAdobe }.first()
    Assert.assertEquals("NYPL", drmAdobe.vendor)
    Assert.assertEquals(URI("http://librarysimplified.org/terms/drm/scheme/ACS"), drmAdobe.scheme)
    Assert.assertEquals("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K", drmAdobe.clientToken)
    Assert.assertEquals("https://example.com/devices", drmAdobe.deviceManagerURI?.toString())
  }

  @Test
  fun testExampleUnknownDRM() {
    val parser =
      this.parsers.createParser(URI.create("urn:x"), resource("example-drm-unknown.json"))

    val result = parser.parse()
    this.dump(result)
    Assert.assertThat(result, IsInstanceOf(Success::class.java))

    val success = result as Success
    val profile = success.result

    Assert.assertEquals(
      "6120696828384",
      profile.authorization?.identifier
    )
    Assert.assertEquals(
      "2019-08-02T00:00:00.000Z",
      profile.authorization?.expires.toString()
    )
    Assert.assertEquals(
      true,
      profile.settings.synchronizeAnnotations
    )

    Assert.assertEquals(0, profile.drm.size)
    Assert.assertEquals(1, result.warnings.size)
    Assert.assertTrue(result.warnings[0].message.contains("Unrecognized DRM scheme"))
  }

  @Test
  fun testSimply2126() {
    val parser =
      this.parsers.createParser(URI.create("urn:x"), resource("simply-2126.json"))

    val result = parser.parse()
    this.dump(result)
    Assert.assertThat(result, IsInstanceOf(Success::class.java))

    val success = result as Success
    val profile = success.result

    Assert.assertEquals(
      "1278371823781",
      profile.authorization?.identifier
    )
    Assert.assertEquals(
      "2020-04-13T00:00:00.000Z",
      profile.authorization?.expires.toString()
    )
    Assert.assertEquals(
      false,
      profile.settings.synchronizeAnnotations
    )

    Assert.assertEquals(1, profile.drm.size)
    Assert.assertEquals(0, result.warnings.size)
  }

  private fun resource(file: String): InputStream {
    val path = "/org/nypl/simplified/tests/patron/$file"
    return PatronUserProfileParserContract::class.java.getResourceAsStream(path)!!
  }

  private fun dump(result: ParseResult<PatronUserProfile>) {
    return when (result) {
      is Success ->
        this.logger.debug("success: {}", result.result)
      is Failure ->
        result.errors.forEach { error ->
          this.logger.error("error: {}: ", error, error.exception)
        }
    }
  }
}
