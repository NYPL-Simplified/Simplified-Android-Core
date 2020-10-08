package org.nypl.simplified.tests.books.controller

import com.io7m.jfunctional.Option
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.controller.BookReportTask
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.http.core.HTTPResultType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.tests.http.MockingHTTP
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI

abstract class BookReportTaskContract {

  /**
   * A feed entry that has no issues URI causes the task to give up.
   */

  @Test
  fun testWithFeedEntryMissingURI() {
    val http = MockingHTTP()

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
    val http = MockingHTTP()
    val issuesURI = URI.create("http://www.example.com/issues/")
    http.addResponse(
      uri = issuesURI,
      result = HTTPResultOK(
        "OK",
        200,
        ByteArrayInputStream(ByteArray(0)),
        0L,
        mutableMapOf(),
        0L
      ) as HTTPResultType<InputStream>
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

    Assert.assertTrue(
      "Responses have been consumed",
      http.responsesNow()[issuesURI]!!.isEmpty()
    )
  }

  /**
   * A feed entry that has an issues URI causes the task to POST to it.
   */

  @Test
  fun testWithFeedEntryWithCredentials() {
    val http = MockingHTTP()
    val issuesURI = URI.create("http://www.example.com/issues/")
    http.addResponse(
      uri = issuesURI,
      result = HTTPResultOK(
        "OK",
        200,
        ByteArrayInputStream(ByteArray(0)),
        0L,
        mutableMapOf(),
        0L
      ) as HTTPResultType<InputStream>
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

    Assert.assertTrue(
      "Responses have been consumed",
      http.responsesNow()[issuesURI]!!.isEmpty()
    )
  }

  /**
   * If an issues URI returns a failure, it is logged but ignored.
   */

  @Test
  fun testWithFeedEntryIssuesFailure() {
    val http = MockingHTTP()
    val issuesURI = URI.create("http://www.example.com/issues/")
    http.addResponse(
      uri = issuesURI,
      result = HTTPResultError<InputStream>(
        400,
        "UH OH",
        0L,
        mutableMapOf(),
        0L,
        ByteArrayInputStream(ByteArray(0)),
        Option.none()
      ) as HTTPResultType<InputStream>
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

    Assert.assertTrue(
      "Responses have been consumed",
      http.responsesNow()[issuesURI]!!.isEmpty()
    )
  }
}
