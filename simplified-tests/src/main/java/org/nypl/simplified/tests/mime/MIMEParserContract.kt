package org.nypl.simplified.tests.mime

import org.hamcrest.core.StringContains
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.nypl.simplified.mime.MIMEParserType

abstract class MIMEParserContract {

  abstract fun parser(text: String): MIMEParserType

  @JvmField
  @Rule
  val expectedException: ExpectedException = ExpectedException.none()

  @Test
  fun textEmpty()
  {
    expectFailure("End-of-stream", parser(""))
  }

  @Test
  fun testTextPlain0() {
    val result = parser("text/plain").parseExceptionally()
    Assert.assertEquals("text", result.type)
    Assert.assertEquals("plain", result.subtype)
    Assert.assertEquals("text/plain", result.fullType)
    Assert.assertTrue(result.parameters.isEmpty())
  }

  @Test
  fun testTextPlain1() {
    val result = parser("text/plain;").parseExceptionally()
    Assert.assertEquals("text", result.type)
    Assert.assertEquals("plain", result.subtype)
    Assert.assertEquals("text/plain", result.fullType)
    Assert.assertTrue(result.parameters.isEmpty())
  }

  @Test
  fun testOPDS() {
    val result = parser("application/atom+xml;type=entry;profile=opds-catalog").parseExceptionally()
    Assert.assertEquals("application", result.type)
    Assert.assertEquals("atom+xml", result.subtype)
    Assert.assertEquals("application/atom+xml", result.fullType)
    Assert.assertTrue(result.parameters["type"] == "entry")
    Assert.assertTrue(result.parameters["profile"] == "opds-catalog")
    Assert.assertEquals(2, result.parameters.size)
  }

  @Test
  fun testTextProfile() {
    val result = parser("text/html;profile=http://librarysimplified.org/terms/profiles/streaming-media").parseExceptionally()
    Assert.assertEquals("text", result.type)
    Assert.assertEquals("html", result.subtype)
    Assert.assertEquals("text/html", result.fullType)
    Assert.assertTrue(result.parameters["profile"] == "http://librarysimplified.org/terms/profiles/streaming-media")
    Assert.assertEquals(1, result.parameters.size)
  }

  @Test
  fun testTextProfileQuoted() {
    val result = parser("text/html;profile=\"http://librarysimplified.org/terms/profiles/streaming-media\"").parseExceptionally()
    Assert.assertEquals("text", result.type)
    Assert.assertEquals("html", result.subtype)
    Assert.assertEquals("text/html", result.fullType)
    Assert.assertTrue(result.parameters["profile"] == "http://librarysimplified.org/terms/profiles/streaming-media")
    Assert.assertEquals(1, result.parameters.size)
  }

  @Test
  fun testError0()
  {
    expectFailure("semicolon", parser(";;;"))
  }

  @Test
  fun testError1()
  {
    expectFailure("semicolon", parser("text/plain;;"))
  }

  @Test
  fun testError2()
  {
    expectFailure("End-of-stream", parser("text/plain;x="))
  }

  @Test
  fun testError3()
  {
    expectFailure("semicolon", parser("text/plain;x=;"))
  }

  private fun expectFailure(message: String, parser: MIMEParserType) {
    this.expectedException.expect(Exception::class.java)
    this.expectedException.expectMessage(StringContains.containsString(message))
    parser.parseExceptionally()
  }
}
