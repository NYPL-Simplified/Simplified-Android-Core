package org.nypl.simplified.tests.books.reader.bookmarks

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.Option
import org.hamcrest.core.IsInstanceOf
import org.joda.time.LocalDateTime
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials
import org.nypl.simplified.books.accounts.AccountBarcode
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.accounts.AccountPIN
import org.nypl.simplified.books.reader.ReaderBookLocation
import org.nypl.simplified.books.reader.ReaderBookmark
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkServerCommandQueue
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkServerCommandQueueType
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkHTTPCalls
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkKind.ReaderBookmarkExplicit
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.http.core.HTTPResultType
import org.nypl.simplified.tests.http.MockingHTTP
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

abstract class ReaderBookmarkServerCommandQueueContract {

  @Rule
  @JvmField
  val expectedException = ExpectedException.none()

  private lateinit var objectMapper: ObjectMapper
  private var queue: ReaderBookmarkServerCommandQueueType? = null

  @Before
  fun setup() {
    this.objectMapper = ObjectMapper()
  }

  @After
  fun tearDown() {
    val q = this.queue
    if (q != null && q.isRunning()) {
      q.close()
    }
  }

  /**
   * Sending a bookmark works.
   */

  @Test(timeout = 15_000L)
  fun testSendBookmark() {
    val http = MockingHTTP()
    http.addResponse(
      uri = URI.create("http://example.com/annotations"),
      result = HTTPResultOK(
        "OK",
        200,
        ByteArrayInputStream(ByteArray(1)),
        0L,
        mutableMapOf(),
        0L) as HTTPResultType<InputStream>)

    val httpCalls = ReaderBookmarkHTTPCalls(objectMapper, http)
    val q =
      ReaderBookmarkServerCommandQueue.create(
        httpCalls = httpCalls,
        executors = { MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())})
    this.queue = q

    val credentials =
      AccountAuthenticationCredentials.builder(
        AccountPIN.create("1234"),
        AccountBarcode.create("abcd"))
        .build()

    val bookmark =
      ReaderBookmark(
        opdsId = "opdsid",
        location = ReaderBookLocation.create(Option.none(), "id"),
        time = LocalDateTime.now(),
        kind = ReaderBookmarkExplicit,
        chapterTitle = "A Title",
        chapterProgress = 0.5,
        bookProgress = 0.25,
        uri = null,
        deviceID = "urn:uuid:28cad755-2a0e-48bc-b5c8-1d43d57ac3e9")

    val accountID = AccountID.create(24)
    q.sendBookmark(
      accountID,
      URI.create("http://example.com/annotations"),
      credentials,
      bookmark)
      .get()
  }

  /**
   * Sending a bookmark fails and publishes the correct events.
   */

  @Test
  fun testSendBookmarkFails() {
    val http = MockingHTTP()

    val error =
      HTTPResultError<InputStream>(
      500,
      "OUCH",
      0L,
      mutableMapOf(),
      0L,
      ByteArrayInputStream(ByteArray(0)),
      Option.none()) as HTTPResultType<InputStream>

    http.addResponse(
      uri = URI.create("http://example.com/annotations"),
      result = error)
    http.addResponse(
      uri = URI.create("http://example.com/annotations"),
      result = error)
    http.addResponse(
      uri = URI.create("http://example.com/annotations"),
      result = error)

    val httpCalls = ReaderBookmarkHTTPCalls(objectMapper, http)
    val q =
      ReaderBookmarkServerCommandQueue.create(
        httpCalls = httpCalls,
        executors = { MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())})
    this.queue = q

    val credentials =
      AccountAuthenticationCredentials.builder(
        AccountPIN.create("1234"),
        AccountBarcode.create("abcd"))
        .build()

    val bookmark =
      ReaderBookmark(
        opdsId = "opdsid",
        location = ReaderBookLocation.create(Option.none(), "id"),
        time = LocalDateTime.now(),
        kind = ReaderBookmarkExplicit,
        chapterTitle = "A Title",
        chapterProgress = 0.5,
        bookProgress = 0.25,
        uri = null,
        deviceID = "urn:uuid:28cad755-2a0e-48bc-b5c8-1d43d57ac3e9")

    this.expectedException.expect(ExecutionException::class.java)
    this.expectedException.expectCause(IsInstanceOf.instanceOf(IOException::class.java))

    q.sendBookmark(
      AccountID.create(24),
      URI.create("http://example.com/annotations"),
      credentials,
      bookmark)
      .get()
  }

  /**
   * Interrupting a send by cancelling the future works.
   */

  @Test
  fun testSendBookmarkCancel() {
    val http = MockingHTTP()

    val error =
      HTTPResultError<InputStream>(
        500,
        "OUCH",
        0L,
        mutableMapOf(),
        0L,
        ByteArrayInputStream(ByteArray(0)),
        Option.none()) as HTTPResultType<InputStream>

    http.addResponse(
      uri = URI.create("http://example.com/annotations"),
      result = error)
    http.addResponse(
      uri = URI.create("http://example.com/annotations"),
      result = error)
    http.addResponse(
      uri = URI.create("http://example.com/annotations"),
      result = error)

    val httpCalls = ReaderBookmarkHTTPCalls(objectMapper, http)
    val q =
      ReaderBookmarkServerCommandQueue.create(
        httpCalls = httpCalls,
        executors = { MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())})
    this.queue = q

    val credentials =
      AccountAuthenticationCredentials.builder(
        AccountPIN.create("1234"),
        AccountBarcode.create("abcd"))
        .build()

    val bookmark =
      ReaderBookmark(
        opdsId = "opdsid",
        location = ReaderBookLocation.create(Option.none(), "id"),
        time = LocalDateTime.now(),
        kind = ReaderBookmarkExplicit,
        chapterTitle = "A Title",
        chapterProgress = 0.5,
        bookProgress = 0.25,
        uri = null,
        deviceID = "urn:uuid:28cad755-2a0e-48bc-b5c8-1d43d57ac3e9")

    val future =
      q.sendBookmark(
      AccountID.create(24),
      URI.create("http://example.com/annotations"),
      credentials,
      bookmark)

    future.cancel(true)
    this.expectedException.expect(CancellationException::class.java)
    future.get()
  }
}
