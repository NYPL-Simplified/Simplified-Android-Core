package org.nypl.simplified.tests.oauth

import android.content.Intent
import android.net.Uri
import com.io7m.junreachable.UnreachableCodeException
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import org.nypl.simplified.oauth.OAuthCallbackIntentParsing
import org.nypl.simplified.oauth.OAuthParseResult.Failed
import org.nypl.simplified.oauth.OAuthParseResult.Success
import java.io.IOException
import java.util.UUID

open class OAuthIntentParsingContract {

  val failIfCalled: (Any) -> Uri = {
    throw UnreachableCodeException()
  }

  @Test
  fun intentDataIsMissingFails() {
    val intent = Mockito.mock(Intent::class.java)
    Mockito.`when`(intent.data).thenReturn(null)

    val result = OAuthCallbackIntentParsing.processIntent(intent, "oauth", failIfCalled) as Failed
    Assert.assertTrue(result.message.contains("No data provided"))
  }

  @Test
  fun missingURISchemeFails() {
    val uri = Mockito.mock(Uri::class.java)
    Mockito.`when`(uri.scheme).thenReturn(null)

    val result = OAuthCallbackIntentParsing.processUri(uri, "oauth", failIfCalled) as Failed
    Assert.assertTrue(result.message.contains("Unrecognized URI"))
  }

  @Test
  fun unrecognizedURISchemeFails() {
    val accountId = UUID.randomUUID()
    val uri = Mockito.mock(Uri::class.java)
    Mockito.`when`(uri.userInfo).thenReturn(accountId.toString())
    Mockito.`when`(uri.scheme).thenReturn("http")

    val result = OAuthCallbackIntentParsing.processUri(uri, "oauth", failIfCalled) as Failed
    Assert.assertTrue(result.message.contains("Unrecognized URI"))
  }

  @Test
  fun missingAccountFails() {
    val uri = Mockito.mock(Uri::class.java)
    Mockito.`when`(uri.scheme).thenReturn("oauth")
    Mockito.`when`(uri.userInfo).thenReturn(null)
    Mockito.`when`(uri.encodedFragment).thenReturn("missing_things")

    val fakeUri = Mockito.mock(Uri::class.java)
    Mockito.`when`(fakeUri.getQueryParameter("access_token"))
      .thenReturn(null)

    val result =
      OAuthCallbackIntentParsing.processUri(uri, "oauth") { fakeUri } as Failed
    Assert.assertTrue(result.message.contains("No user info in intent"))
  }

  @Test
  fun missingParameterFails() {
    val accountId = UUID.randomUUID()
    val uri = Mockito.mock(Uri::class.java)
    Mockito.`when`(uri.scheme).thenReturn("oauth")
    Mockito.`when`(uri.userInfo).thenReturn(accountId.toString())
    Mockito.`when`(uri.encodedFragment).thenReturn("missing_things")

    val fakeUri = Mockito.mock(Uri::class.java)
    Mockito.`when`(fakeUri.getQueryParameter("access_token"))
      .thenReturn(null)

    val result =
      OAuthCallbackIntentParsing.processUri(uri, "oauth") { fakeUri } as Failed
    Assert.assertTrue(result.message.contains("Response did not contain an access_token parameter."))
  }

  @Test
  fun parseExceptionFails() {
    val accountId = UUID.randomUUID()

    val uri = Mockito.mock(Uri::class.java)
    Mockito.`when`(uri.scheme).thenReturn("oauth")
    Mockito.`when`(uri.userInfo).thenReturn(accountId.toString())
    Mockito.`when`(uri.encodedFragment).thenReturn("missing_things")

    val result =
      OAuthCallbackIntentParsing.processUri(uri, "oauth") { throw IOException("Failed!") } as Failed
    Assert.assertTrue(result.message.contains("Failed!"))
  }

  @Test
  fun correctURISucceeds() {
    val accountId = UUID.randomUUID()

    val uri = Mockito.mock(Uri::class.java)
    Mockito.`when`(uri.scheme).thenReturn("oauth")
    Mockito.`when`(uri.userInfo).thenReturn(accountId.toString())
    Mockito.`when`(uri.encodedFragment).thenReturn("access_token=smile")

    val fakeUri = Mockito.mock(Uri::class.java)
    Mockito.`when`(fakeUri.getQueryParameter("access_token"))
      .thenReturn("smile")

    val result = OAuthCallbackIntentParsing.processUri(uri, "oauth") { text ->
      Assert.assertEquals(
        "oauth://$accountId@authenticated?access_token=smile",
        text
      )
      fakeUri
    } as Success
    Assert.assertEquals("smile", result.token)
    Assert.assertEquals(accountId, result.accountId)
  }

  @Test
  fun correctIntentSucceeds() {
    val accountId = UUID.randomUUID()

    val uri = Mockito.mock(Uri::class.java)
    Mockito.`when`(uri.scheme).thenReturn("oauth")
    Mockito.`when`(uri.userInfo).thenReturn(accountId.toString())
    Mockito.`when`(uri.encodedFragment).thenReturn("access_token=smile")

    val fakeUri = Mockito.mock(Uri::class.java)
    Mockito.`when`(fakeUri.getQueryParameter("access_token"))
      .thenReturn("smile")

    val intent = Mockito.mock(Intent::class.java)
    Mockito.`when`(intent.data).thenReturn(uri)

    val result = OAuthCallbackIntentParsing.processIntent(intent, "oauth") { text ->
      Assert.assertEquals(
        "oauth://$accountId@authenticated?access_token=smile",
        text
      )
      fakeUri
    } as Success
    Assert.assertEquals("smile", result.token)
    Assert.assertEquals(accountId, result.accountId)
  }
}
