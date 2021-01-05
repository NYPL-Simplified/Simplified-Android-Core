package org.nypl.simplified.books.borrowing.internal

import one.irradia.mime.api.MIMECompatibility
import one.irradia.mime.api.MIMEType
import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedUnacceptableMIME
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskFailed
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskFactoryType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskType
import java.net.CookieManager
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI

/**
 * A task that downloads a file for an account that uses SAML authentication. The download is
 * attempted, with any cookies belonging to the account attached to the download requests. If the
 * downloaded file type is the expected content type, the file is saved to the book database.
 * Otherwise, if the downloaded file resembles a login form (the content type is HTML), the
 * borrowing context is notified that authentication is required.
 */

class BorrowSAMLDownload private constructor() : BorrowSubtaskType {
  companion object : BorrowSubtaskFactoryType {
    val loginPageContentType: MIMEType = MIMEType("text", "html", mapOf())

    override val name: String
      get() = "SAML Authenticated Download"

    override fun createSubtask(): BorrowSubtaskType {
      return BorrowSAMLDownload()
    }

    override fun isApplicableFor(
      type: MIMEType,
      target: URI?,
      account: AccountReadableType?
    ): Boolean =
      account?.loginState?.credentials is AccountAuthenticationCredentials.SAML2_0 &&
        BorrowDirectDownload.isApplicableFor(type, target, account)
  }

  override fun execute(
    context: BorrowContextType
  ) {
    context.taskRecorder.beginNewStep("Downloading with SAML auth...")
    context.bookDownloadIsRunning(null, 0L, 0L, "Requesting download...")

    val cookieStore = createAccountCookieStore(context.account)

    BorrowHTTP.download(
      context = context,
      onDownloadFailedUnacceptableMIME = this::onDownloadFailedUnacceptableMIME,
      requestModifier = { properties ->
        val requestCookies = sortedMapOf<String, String>()

        cookieStore.get(properties.target).forEach { cookie ->
          requestCookies.put(cookie.name, cookie.value)
        }

        properties.copy(
          cookies = requestCookies
        )
      }
    )
  }

  /**
   * Create a CookieStore containing the cookies belonging to the given account.
   */

  private fun createAccountCookieStore(
    account: AccountReadableType
  ): CookieStore {
    val credentials = account.loginState.credentials as AccountAuthenticationCredentials.SAML2_0

    val cookieManager = CookieManager()
    val cookieStore = cookieManager.cookieStore

    credentials.cookies.forEach { accountCookie ->
      // HttpCookie.parse allows for multiple name/values in one header string, so it returns a
      // list. In the account, if there is more than one cookie for a URL, each one is stored as
      // a separate AccountCookie, so we can just call first() on the parsed result.

      cookieStore.add(URI(accountCookie.url), HttpCookie.parse(accountCookie.value).first())
    }

    return cookieStore
  }

  private fun onDownloadFailedUnacceptableMIME(
    context: BorrowContextType,
    result: DownloadFailedUnacceptableMIME
  ) {
    val status = result.responseStatus as LSHTTPResponseStatus.Responded.OK
    val receivedType = status.properties.contentType

    if (MIMECompatibility.isCompatibleLax(receivedType, loginPageContentType)) {
      context.bookDownloadIsWaitingForExternalAuthentication()
    } else {
      throw BorrowSubtaskFailed()
    }
  }
}
