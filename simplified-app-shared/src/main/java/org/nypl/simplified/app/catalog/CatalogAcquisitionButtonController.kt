package org.nypl.simplified.app.catalog

import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast

import com.google.common.util.concurrent.ListeningExecutorService
import com.io7m.jfunctional.None
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.OptionVisitorType
import com.io7m.jfunctional.Some
import com.io7m.jnull.Nullable

import org.nypl.simplified.app.NetworkConnectivityType
import org.nypl.simplified.app.R
import org.nypl.simplified.app.login.LoginCancelledType
import org.nypl.simplified.app.login.LoginDialog
import org.nypl.simplified.app.login.LoginFailedType
import org.nypl.simplified.app.login.LoginSucceededType
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.accounts.AccountsDatabaseNonexistentException
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.controller.BooksControllerType
import org.nypl.simplified.books.controller.ProfilesControllerType
import org.nypl.simplified.books.document_store.DocumentStoreType
import org.nypl.simplified.books.feeds.FeedEntry
import org.nypl.simplified.books.logging.LogUtilities
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.slf4j.LoggerFactory

/**
 * A controller for an acquisition button.
 *
 * This is responsible for logging in, if necessary, and then starting the
 * borrow of a given book.
 */

class CatalogAcquisitionButtonController(
  private val acquisition: OPDSAcquisition,
  private val activity: AppCompatActivity,
  private val books: BooksControllerType,
  private val entry: FeedEntry.FeedEntryOPDS,
  private val id: BookID,
  private val profiles: ProfilesControllerType,
  private val bookRegistry: BookRegistryReadableType,
  private val networkConnectivity: NetworkConnectivityType,
  private val backgroundExecutor: ListeningExecutorService,
  private val documents: DocumentStoreType) : OnClickListener {

  override fun onClick(@Nullable v: View) {

    if (!this.networkConnectivity.isNetworkAvailable) {
      Toast.makeText(
        this.activity.applicationContext,
        this.activity.resources.getString(R.string.catalog_added_for_later_download),
        Toast.LENGTH_LONG)
        .show()
    }

    val account = accountForBook(this.profiles, this.bookRegistry, this.id)

    val authentication_required = account.provider().authentication().isSome && this.acquisition.relation !== OPDSAcquisition.Relation.ACQUISITION_OPEN_ACCESS

    val authentication_provided = account.credentials().isSome
    if (authentication_required && !authentication_provided) {
      this.tryLogin(account)
      return
    }

    this.tryBorrow(account)
  }

  private fun tryBorrow(account: AccountType) {
    LOG.debug("trying borrow of type {}", this.acquisition.type)
    this.books.bookBorrow(account, this.id, this.acquisition, this.entry.feedEntry)
  }

  private fun tryLogin(account: AccountType) {
    LOG.debug("trying login")

    // XXX: Untranslated string!
    val dialog = LoginDialog.newDialog(
      this.profiles,
      this.backgroundExecutor,
      this.documents,
      "Login Required",
      account,
      LoginSucceededType { creds -> this.onLoginSuccess(account) },
      LoginCancelledType { this.onLoginCancelled() },
      LoginFailedType { error, message -> this.onLoginFailure(error, message) })

    dialog.show(this.activity.supportFragmentManager, "login-dialog")
  }

  private fun onLoginFailure(error: OptionType<Exception>, message: String) {
    LogUtilities.errorWithOptionalException(LOG, message, error)
  }

  private fun onLoginCancelled() {
    LOG.debug("login cancelled")
  }

  private fun onLoginSuccess(
    account: AccountType) {

    LOG.debug("login succeeded")
    this.tryBorrow(account)
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(CatalogAcquisitionButtonController::class.java)

    private fun accountForBook(
      profiles: ProfilesControllerType,
      book_registry: BookRegistryReadableType,
      book_id: BookID): AccountType {

      return Option.of<BookWithStatus>(book_registry.books()[book_id]).accept(
        object : OptionVisitorType<BookWithStatus, AccountType> {
          override fun none(none: None<BookWithStatus>): AccountType {
            try {
              return profiles.profileAccountCurrent()
            } catch (e: ProfileNoneCurrentException) {
              throw IllegalStateException(e)
            }

          }

          override fun some(some: Some<BookWithStatus>): AccountType {
            try {
              val profile = profiles.profileCurrent()
              val account_id = some.get().book().account
              return profile.account(account_id)
            } catch (e: ProfileNoneCurrentException) {
              throw IllegalStateException(e)
            } catch (e: AccountsDatabaseNonexistentException) {
              throw IllegalStateException(e)
            }

          }
        })
    }
  }
}
