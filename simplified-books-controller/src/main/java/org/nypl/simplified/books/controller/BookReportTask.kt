package org.nypl.simplified.books.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.Some
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.http.core.HTTPProblemReportLogging
import org.nypl.simplified.http.core.HTTPType
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Callable

class BookReportTask(
  private val http: HTTPType,
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

      val credentials = this.account.loginState.credentials
      val authenticatedHTTP =
        if (credentials != null) {
          Option.some(AccountAuthenticatedHTTP.createAuthenticatedHTTP(credentials))
        } else {
          Option.none()
        }

      val issuesURI = issuesURIOpt.get()
      val result =
        this.http.post(
          authenticatedHTTP,
          issuesURI,
          serializeProblem(),
          "application/problem+json"
        )

      result.match<Unit, Exception>(
        { error ->
          HTTPProblemReportLogging.logError(
            this.logger,
            issuesURI,
            error.message,
            error.status,
            error.problemReport
          )
          Unit
        },
        { error ->
          this.logger.error(
            "[{}]: http exception for {}: ",
            this.account.id.uuid,
            this.feedEntry.bookID.brief(),
            error.error
          )
          Unit
        },
        { ok ->
          this.logger.debug(
            "[{}]: succeeded for {} ({} {})",
            this.account.id.uuid,
            this.feedEntry.bookID.brief(),
            ok.status,
            ok.message
          )
          Unit
        }
      )
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
