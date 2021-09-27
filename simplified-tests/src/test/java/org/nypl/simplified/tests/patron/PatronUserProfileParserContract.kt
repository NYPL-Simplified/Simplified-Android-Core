package org.nypl.simplified.tests.patron

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.parser.api.ParseResult.Failure
import org.nypl.simplified.parser.api.ParseResult.Success
import org.nypl.simplified.patron.api.PatronDRMAdobe
import org.nypl.simplified.patron.api.PatronUserProfile
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.nypl.simplified.tests.ExtraAssertions
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
    ExtraAssertions.assertInstanceOf(result, Failure::class.java)
  }

  @Test
  fun testEmpty2() {
    val parser =
      this.parsers.createParser(URI.create("urn:x"), resource("empty2.json"))

    val result = parser.parse()
    this.dump(result)
    ExtraAssertions.assertInstanceOf(result, Failure::class.java)
  }

  @Test
  fun testExampleDRMMalformed() {
    val parser =
      this.parsers.createParser(URI.create("urn:x"), resource("example-drm-malformed.json"))

    val result = parser.parse()
    this.dump(result)
    ExtraAssertions.assertInstanceOf(result, Failure::class.java)
  }

  @Test
  fun testExample() {
    val parser =
      this.parsers.createParser(URI.create("urn:x"), resource("example.json"))

    val result = parser.parse()
    this.dump(result)
    ExtraAssertions.assertInstanceOf(result, Success::class.java)

    val success = result as Success
    val profile = success.result

    Assertions.assertEquals("6120696828384", profile.authorization?.identifier)
    Assertions.assertEquals("2019-08-02T00:00:00.000Z", profile.authorization?.expires.toString())
    Assertions.assertEquals(true, profile.settings.synchronizeAnnotations)

    Assertions.assertEquals(1, profile.drm.size)

    val drmAdobe = profile.drm.map { a -> a as PatronDRMAdobe }.first()
    Assertions.assertEquals("NYPL", drmAdobe.vendor)
    Assertions.assertEquals(URI("http://librarysimplified.org/terms/drm/scheme/ACS"), drmAdobe.scheme)
    Assertions.assertEquals("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K", drmAdobe.clientToken)
  }

  @Test
  fun testExampleWithDeviceManagement() {
    val parser =
      this.parsers.createParser(URI.create("urn:x"), resource("example-with-device.json"))

    val result = parser.parse()
    this.dump(result)
    ExtraAssertions.assertInstanceOf(result, Success::class.java)

    val success = result as Success
    val profile = success.result

    Assertions.assertEquals("6120696828384", profile.authorization?.identifier)
    Assertions.assertEquals("2019-08-02T00:00:00.000Z", profile.authorization?.expires.toString())
    Assertions.assertEquals(true, profile.settings.synchronizeAnnotations)

    Assertions.assertEquals(1, profile.drm.size)

    val drmAdobe = profile.drm.map { a -> a as PatronDRMAdobe }.first()
    Assertions.assertEquals("NYPL", drmAdobe.vendor)
    Assertions.assertEquals(URI("http://librarysimplified.org/terms/drm/scheme/ACS"), drmAdobe.scheme)
    Assertions.assertEquals("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K", drmAdobe.clientToken)
    Assertions.assertEquals("https://example.com/devices", drmAdobe.deviceManagerURI?.toString())
  }

  @Test
  fun testExampleWithDeviceManagement2() {
    val parser =
      this.parsers.createParser(URI.create("urn:x"), resource("example-with-device-20210512.json"))

    val result = parser.parse()
    this.dump(result)
    ExtraAssertions.assertInstanceOf(result, Success::class.java)

    val success = result as Success
    val profile = success.result

    Assertions.assertEquals("123123", profile.authorization?.identifier)
    Assertions.assertEquals("2022-08-06T00:00:00.000Z", profile.authorization?.expires.toString())
    Assertions.assertEquals(true, profile.settings.synchronizeAnnotations)

    Assertions.assertEquals(1, profile.drm.size)

    val drmAdobe = profile.drm.map { a -> a as PatronDRMAdobe }.first()
    Assertions.assertEquals("NYPL", drmAdobe.vendor)
    Assertions.assertEquals(URI("http://librarysimplified.org/terms/drm/scheme/ACS"), drmAdobe.scheme)
    Assertions.assertEquals("NYNYPL|123|asds|asdasd", drmAdobe.clientToken)
    Assertions.assertEquals("https://example.com/devices", drmAdobe.deviceManagerURI?.toString())
  }

  @Test
  fun testExampleUnknownDRM() {
    val parser =
      this.parsers.createParser(URI.create("urn:x"), resource("example-drm-unknown.json"))

    val result = parser.parse()
    this.dump(result)
    ExtraAssertions.assertInstanceOf(result, Success::class.java)

    val success = result as Success
    val profile = success.result

    Assertions.assertEquals("6120696828384", profile.authorization?.identifier)
    Assertions.assertEquals("2019-08-02T00:00:00.000Z", profile.authorization?.expires.toString())
    Assertions.assertEquals(true, profile.settings.synchronizeAnnotations)

    Assertions.assertEquals(0, profile.drm.size)
    Assertions.assertEquals(1, result.warnings.size)
    Assertions.assertTrue(result.warnings[0].message.contains("Unrecognized DRM scheme"))
  }

  @Test
  fun testSimply2126() {
    val parser =
      this.parsers.createParser(URI.create("urn:x"), resource("simply-2126.json"))

    val result = parser.parse()
    this.dump(result)
    ExtraAssertions.assertInstanceOf(result, Success::class.java)

    val success = result as Success
    val profile = success.result

    Assertions.assertEquals("1278371823781", profile.authorization?.identifier)
    Assertions.assertEquals("2020-04-13T00:00:00.000Z", profile.authorization?.expires.toString())
    Assertions.assertEquals(false, profile.settings.synchronizeAnnotations)

    Assertions.assertEquals(1, profile.drm.size)
    Assertions.assertEquals(0, result.warnings.size)
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
