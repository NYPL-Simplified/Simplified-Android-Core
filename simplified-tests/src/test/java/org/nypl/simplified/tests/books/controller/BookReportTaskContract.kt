package org.nypl.simplified.tests.books.controller

import android.content.Context
import com.io7m.jfunctional.Option
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.joda.time.DateTime
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.vanilla.LSHTTPClients
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.controller.BookReportTask
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess

abstract class BookReportTaskContract {

  private lateinit var http: LSHTTPClientType
  private lateinit var server: MockWebServer

  @Before
  fun setup() {
    this.http =
      LSHTTPClients()
        .create(
          context = Mockito.mock(Context::class.java),
          configuration = LSHTTPClientConfiguration("simplified-test", "0.0.1")
        )

    this.server = MockWebServer()
    this.server.start()
  }

  @After
  fun tearDown() {
    this.server.close()
  }

  /**
   * A feed entry that has no issues URI causes the task to give up.
   */

  @Test
  fun testWithFeedEntryMissingURI() {
    val account = Mockito.mock(AccountType::class.java)
    Mockito.`when`(account.id)
      .thenReturn(AccountID.generate())

    val feedEntry =
      FeedEntry.FeedEntryOPDS(
        accountID = account.id,
        feedEntry = OPDSAcquisitionFeedEntry.newBuilder(
          "x",
          "Title",
          DateTime.now(),
          OPDSAvailabilityOpenAccess.get(Option.none())
        ).build()
      )

    val task = BookReportTask(http, account, feedEntry, "someType")
    task.call()
  }

  /**
   * A feed entry that has an issues URI causes the task to POST to it.
   */

  @Test
  fun testWithFeedEntryWithoutCredentials() {
    val issuesURI =
      this.server.url("issues").toUri()

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("")
    )

    val account =
      Mockito.mock(AccountType::class.java)
    Mockito.`when`(account.id)
      .thenReturn(AccountID.generate())
    Mockito.`when`(account.loginState)
      .thenReturn(AccountLoginState.AccountNotLoggedIn)

    val entryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "x",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      )
    entryBuilder.setIssuesOption(Option.some(issuesURI))

    val feedEntry = FeedEntry.FeedEntryOPDS(
      feedEntry = entryBuilder.build(),
      accountID = account.id
    )
    val task = BookReportTask(http, account, feedEntry, "someType")
    task.call()

    val req0 = this.server.takeRequest()
    Assert.assertEquals(this.server.url("issues"), req0.requestUrl)
    Assert.assertEquals(1, this.server.requestCount)
  }

  /**
   * A feed entry that has an issues URI causes the task to POST to it.
   */

  @Test
  fun testWithFeedEntryWithCredentials() {
    val issuesURI =
      this.server.url("issues").toUri()

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("")
    )

    val account =
      Mockito.mock(AccountType::class.java)
    Mockito.`when`(account.id)
      .thenReturn(AccountID.generate())
    Mockito.`when`(account.loginState)
      .thenReturn(
        AccountLoginState.AccountLoggedIn(
          AccountAuthenticationCredentials.Basic(
            userName = AccountUsername("abcd"),
            password = AccountPassword("1234"),
            adobeCredentials = null,
            authenticationDescription = null
          )
        )
      )

    val entryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "x",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      )
    entryBuilder.setIssuesOption(Option.some(issuesURI))

    val feedEntry = FeedEntry.FeedEntryOPDS(
      feedEntry = entryBuilder.build(),
      accountID = account.id
    )
    val task = BookReportTask(http, account, feedEntry, "someType")
    task.call()

    val req0 = this.server.takeRequest()
    Assert.assertEquals(this.server.url("issues"), req0.requestUrl)
    Assert.assertEquals(1, this.server.requestCount)
  }

  /**
   * If an issues URI returns a failure, it is logged but ignored.
   */

  @Test
  fun testWithFeedEntryIssuesFailure() {
    val issuesURI =
      this.server.url("issues").toUri()

    this.server.enqueue(
      MockResponse()
        .setResponseCode(400)
        .setBody("")
    )

    val account =
      Mockito.mock(AccountType::class.java)
    Mockito.`when`(account.id)
      .thenReturn(AccountID.generate())
    Mockito.`when`(account.loginState)
      .thenReturn(
        AccountLoginState.AccountLoggedIn(
          AccountAuthenticationCredentials.Basic(
            userName = AccountUsername("abcd"),
            password = AccountPassword("1234"),
            adobeCredentials = null,
            authenticationDescription = null
          )
        )
      )

    val entryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "x",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      )
    entryBuilder.setIssuesOption(Option.some(issuesURI))

    val feedEntry = FeedEntry.FeedEntryOPDS(
      feedEntry = entryBuilder.build(),
      accountID = account.id
    )
    val task = BookReportTask(http, account, feedEntry, "someType")
    task.call()

    val req0 = this.server.takeRequest()
    Assert.assertEquals(this.server.url("issues"), req0.requestUrl)
    Assert.assertEquals(1, this.server.requestCount)
  }
}
