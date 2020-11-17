package org.nypl.simplified.books.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.io7m.jfunctional.Some
import one.irradia.mime.api.MIMEType
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.api.LSHTTPRequestBuilderType
import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.feeds.api.FeedEntry
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Callable

class BookReportTask(
  private val http: LSHTTPClientType,
  private val account: AccountType,
  private val feedEntry: FeedEntry.FeedEntryOPDS,
  private val reportType: String
) : Callable<Unit> {

  private val logger = LoggerFactory.getLogger(BookReportTask::class.java)

  override fun call() {
    return try {
      this.logger.debug(
        "[{}]: running {} for {}",
        this.account.id.uuid,
        this.reportType,
        this.feedEntry.bookID.brief()
      )

      val issuesURIOpt = this.feedEntry.feedEntry.issues
      if (!(issuesURIOpt is Some<URI>)) {
        this.logger.debug(
          "[{}]: no issues URI for {}, giving up",
          this.account.id.uuid,
          this.feedEntry.bookID.brief()
        )
        return
      }

      val credentials =
        this.account.loginState.credentials
      val issuesURI =
        issuesURIOpt.get()

      val post =
        LSHTTPRequestBuilderType.Method.Post(
          serializeProblem(),
          MIMEType("application", "problem+json", mapOf())
        )

      val request =
        this.http.newRequest(issuesURI)
          .setAuthorization(AccountAuthenticatedHTTP.createAuthorizationIfPresent(credentials))
          .setMethod(post)
          .build()

      val response = request.execute()
      when (val status = response.status) {
        is LSHTTPResponseStatus.Responded.OK -> {
          this.logger.debug(
            "[{}]: succeeded for {}",
            this.account.id.uuid,
            this.feedEntry.bookID.brief()
          )
        }
        is LSHTTPResponseStatus.Responded.Error -> {
          this.logger.error(
            "[{}]: http error for {}: ",
            this.account.id.uuid,
            this.feedEntry.bookID.brief(),
            status.status
          )
        }
        is LSHTTPResponseStatus.Failed -> {
          this.logger.error(
            "[{}]: http exception for {}: ",
            this.account.id.uuid,
            this.feedEntry.bookID.brief(),
            status.exception
          )
        }
      }
    } catch (e: Exception) {
      this.logger.error(
        "[{}]: failed for {}: ",
        this.account.id.uuid,
        this.feedEntry.bookID.brief(),
        e
      )
      throw e
    }
  }

  private fun serializeProblem(): ByteArray {
    val mapper = ObjectMapper()
    val document = mapper.createObjectNode()
    document.put("type", reportType)
    val bytes = mapper.writeValueAsBytes(document)
    return bytes
  }
}
