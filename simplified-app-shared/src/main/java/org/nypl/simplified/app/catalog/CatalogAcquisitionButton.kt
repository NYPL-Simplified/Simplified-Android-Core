package org.nypl.simplified.app.catalog

import android.content.Context
import android.content.res.Resources
import android.support.v7.widget.AppCompatButton
import android.text.TextUtils
import android.util.TypedValue
import android.view.ViewGroup
import com.google.common.base.Preconditions
import com.io7m.jfunctional.Some
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.app.R
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.book_database.api.BookAcquisitionSelection
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BORROW
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BUY
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_GENERIC
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_OPEN_ACCESS
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_SAMPLE
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_SUBSCRIBE
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory

/**
 * An acquisition button.
 */

class CatalogAcquisitionButton(
  context: Context,
  private val profiles: ProfilesControllerType,
  private val books: BooksControllerType,
  private val account: AccountType,
  bookRegistry: BookRegistryReadableType,
  private val entry: FeedEntryOPDS,
  private val acquisition: OPDSAcquisition,
  private val onWantOpenLoginDialog: () -> Unit)
  : AppCompatButton(context), CatalogBookButtonType {

  private var accountEventSubscription: ObservableSubscriptionType<AccountEvent>? = null

  init {
    val texts =
      buttonTextsFor(this.context.resources, bookRegistry, entry, acquisition)

    this.text = texts.text
    this.ellipsize = TextUtils.TruncateAt.END
    this.maxLines = 1
    this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f)

    this.contentDescription = texts.contentDescription
    this.setOnClickListener { this.onClick() }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    LOG.trace("[{}]: attached", this.entry.bookID.brief())
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    LOG.trace("[{}]: detached", this.entry.bookID.brief())
    this.unsubscribe()
  }

  /**
   * Someone clicked the button.
   *
   * If the user needs to be logged in and isn't, then a dialog will be displayed asking the
   * user to log in. In order to see the results of logging in, the button subscribes to account
   * events and schedules a borrowing operation when it sees that the account has logged in.
   * The button unsubscribes when it is detached from whatever view it is attached to.
   *
   * If the user doesn't need to log in, the borrow proceeds straight away.
   */

  private fun onClick() {
    val loginState = this.account.loginState()
    if (this.account.requiresCredentials() && loginState.credentials == null) {
      this.tryLogin()
    } else {
      this.tryBorrow()
    }
  }

  private fun tryBorrow() {
    this.unsubscribe()
    LOG.debug("[{}]: trying borrow of type {}", this.entry.bookID.brief(), this.acquisition.type)
    this.books.bookBorrow(account, this.entry.bookID, this.acquisition, this.entry.feedEntry)
  }

  private fun unsubscribe() {
    this.accountEventSubscription?.unsubscribe()
  }

  private fun tryLogin() {
    LOG.debug("[{}]: trying login", this.entry.bookID.brief())

    this.accountEventSubscription =
      this.profiles.accountEvents()
        .subscribe { event -> this.onAccountEvent(event) }

    this.onWantOpenLoginDialog.invoke()
  }

  private fun onAccountEvent(event: AccountEvent) {
    if (event is AccountEventLoginStateChanged) {
      if (event.accountID == this.account.id()) {
        return this.configureForState(event.state)
      }
    }
  }

  private fun configureForState(state: AccountLoginState) {
    UIThread.runOnUIThread {
      when (state) {
        is AccountLoginState.AccountLoggingIn,
        is AccountLoginState.AccountLoginFailed -> {

          /*
           * These events can occur multiple times while the user tries and fails to log in. The
           * user may eventually succeed, so we keep listening for events here.
           */

        }

        is AccountLoginState.AccountLogoutFailed,
        is AccountLoginState.AccountLoggingOut,
        AccountLoginState.AccountNotLoggedIn -> {
          LOG.debug("[{}]: user aborted login", this.entry.bookID.brief())
          this.unsubscribe()
        }

        is AccountLoginState.AccountLoggedIn ->
          this.tryBorrow()
      }
    }
  }

  /**
   * The text of a button.
   */

  data class ButtonTexts(
    val text: String,
    val contentDescription: String)

  companion object {

    private val LOG =
      LoggerFactory.getLogger(CatalogAcquisitionButton::class.java)

    /**
     * Construct an acquisition button disguised as a retry button.
     */

    fun retryButton(
      context: Context,
      profiles: ProfilesControllerType,
      books: BooksControllerType,
      account: AccountType,
      bookRegistry: BookRegistryReadableType,
      entry: FeedEntryOPDS,
      acquisition: OPDSAcquisition,
      onWantOpenLoginDialog: () -> Unit): CatalogAcquisitionButton {
      val button =
        CatalogAcquisitionButton(
        context = context,
        profiles = profiles,
        books = books,
        account = account,
        bookRegistry = bookRegistry,
        entry = entry,
        acquisition = acquisition,
        onWantOpenLoginDialog = onWantOpenLoginDialog)

      button.text = context.resources.getString(R.string.catalog_book_error_retry)
      return button
    }

    /**
     * Given a feed entry, add all the required acquisition buttons to the given
     * view group.
     */

    fun addButtonsToViewGroup(
      context: Context,
      viewGroup: ViewGroup,
      books: BooksControllerType,
      profiles: ProfilesControllerType,
      bookRegistry: BookRegistryReadableType,
      account: AccountType,
      entry: FeedEntryOPDS,
      onWantOpenLoginDialog: () -> Unit) {

      Preconditions.checkArgument(
        viewGroup.childCount == 0,
        "View group containing acquisition buttons should be empty")

      val bookID = entry.bookID
      val opdsEntry = entry.feedEntry

      val acquisitionOpt =
        BookAcquisitionSelection.preferredAcquisition(opdsEntry.acquisitions)

      if (acquisitionOpt is Some<OPDSAcquisition>) {
        val acquisition = acquisitionOpt.get()

        val button =
          CatalogAcquisitionButton(
            context = context,
            profiles = profiles,
            books = books,
            account = account,
            bookRegistry = bookRegistry,
            entry = entry,
            acquisition = acquisition,
            onWantOpenLoginDialog = onWantOpenLoginDialog)

        viewGroup.addView(button)
      } else {
        LOG.error("[{}]: no available acquisition for book ({})", bookID.brief(), opdsEntry.title)
      }
    }

    /**
     * Determine the button text/content description for the given data.
     */

    private fun buttonTextsFor(
      resources: Resources,
      bookRegistry: BookRegistryReadableType,
      entry: FeedEntryOPDS,
      acquisition: OPDSAcquisition): ButtonTexts {

      val availability = entry.feedEntry.availability
      return if (bookRegistry.book(entry.bookID).isSome) {
        ButtonTexts(
          text = resources.getString(R.string.catalog_book_download),
          contentDescription = resources.getString(R.string.catalog_accessibility_book_download))
      } else {
        when (acquisition.relation) {
          ACQUISITION_OPEN_ACCESS -> {
            ButtonTexts(
              text = resources.getString(R.string.catalog_book_download),
              contentDescription = resources.getString(R.string.catalog_accessibility_book_download))
          }
          ACQUISITION_BORROW -> {
            if (availability is OPDSAvailabilityHoldable) {
              ButtonTexts(
                text = resources.getString(R.string.catalog_book_reserve),
                contentDescription = resources.getString(R.string.catalog_accessibility_book_reserve))
            } else {
              ButtonTexts(
                text = resources.getString(R.string.catalog_book_borrow),
                contentDescription = resources.getString(R.string.catalog_accessibility_book_borrow))
            }
          }
          ACQUISITION_BUY,
          ACQUISITION_GENERIC,
          ACQUISITION_SAMPLE,
          ACQUISITION_SUBSCRIBE -> {
            ButtonTexts(
              text = resources.getString(R.string.catalog_book_download),
              contentDescription = resources.getString(R.string.catalog_accessibility_book_download))
          }
        }
      }
    }
  }
}
