package org.nypl.simplified.app.settings

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FluentFuture
import com.io7m.jfunctional.Unit
import com.squareup.picasso.Picasso
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountEventCreation.AccountCreationFailed
import org.nypl.simplified.accounts.api.AccountEventCreation.AccountCreationSucceeded
import org.nypl.simplified.accounts.api.AccountEventDeletion
import org.nypl.simplified.accounts.api.AccountEventDeletion.AccountDeletionFailed
import org.nypl.simplified.accounts.api.AccountEventDeletion.AccountDeletionSucceeded
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.app.NavigationDrawerActivity
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.images.ImageAccountIcons
import org.nypl.simplified.app.utilities.ErrorDialogUtilities
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.futures.FluentFutureExtensions.onException
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent.ProfileAccountSelectFailed
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent.ProfileAccountSelectSucceeded
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException
import org.nypl.simplified.profiles.api.ProfileNonexistentAccountProviderException
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory
import java.util.ArrayList

/**
 * The activity displaying application accounts.
 */

class SettingsAccountsActivity : NavigationDrawerActivity() {

  private val logger = LoggerFactory.getLogger(SettingsAccountsActivity::class.java)

  private lateinit var adapterAccounts: ArrayAdapter<AccountProviderType>
  private lateinit var adapterAccountsArray: ArrayList<AccountProviderType>
  private lateinit var accountListView: ListView
  private lateinit var accountCurrentView: LinearLayout

  private var accountsSubscription: ObservableSubscriptionType<AccountEvent>? = null
  private var profilesSubscription: ObservableSubscriptionType<ProfileEvent>? = null

  companion object {

    private fun configureAccountListCellViews(
      picasso: Picasso,
      accountProvider: AccountProviderType,
      itemTitleView: TextView,
      itemSubtitleView: TextView,
      iconView: ImageView): Unit {

      itemTitleView.text = accountProvider.displayName

      val subtitle = accountProvider.subtitle
      if (subtitle != null) {
        itemSubtitleView.visibility = View.VISIBLE
        itemSubtitleView.text = subtitle
      } else {
        itemSubtitleView.visibility = View.INVISIBLE
      }

      ImageAccountIcons.loadAccountLogoIntoView(
        loader = picasso,
        account = accountProvider,
        iconView = iconView)

      return Unit.unit()
    }
  }

  override fun navigationDrawerShouldShowIndicator(): Boolean {
    return true
  }

  override fun navigationDrawerGetActivityTitle(resources: Resources): String {
    return resources.getString(R.string.settings)
  }

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val contentArea = this.contentFrame
    val inflater = this.layoutInflater
    val layout = inflater.inflate(R.layout.accounts, contentArea, false) as ViewGroup

    contentArea.addView(layout)
    contentArea.requestLayout()

    this.accountListView =
      this.findViewById(R.id.account_list)
    this.accountCurrentView =
      this.findViewById(R.id.current_account)

    this.adapterAccountsArray = ArrayList()
    this.adapterAccounts =
      AccountsArrayAdapter(
        targetContext = this,
        picasso = Simplified.getLocalImageLoader(),
        adapterAccountsArray = this.adapterAccountsArray,
        inflater = inflater)

    val profiles = Simplified.getProfilesController()
    this.accountListView.adapter = this.adapterAccounts
    this.accountListView.setOnItemClickListener { adapterView, view, position, id ->
      this.onWantShowAccount(position, profiles)
    }

    this.accountListView.setOnItemLongClickListener { _, _, position, _ ->
      this.onWantDeleteAccountByProvider(position, profiles)
      true
    }

    this.populateAccountsArray()

    try {
      this.updateCurrentAccountView(
        this.accountCurrentView,
        profiles
          .profileCurrent()
          .accountCurrent()
          .id())
    } catch (e: ProfileNoneCurrentException) {
      throw IllegalStateException(e)
    }

    this.accountsSubscription =
      profiles
        .accountEvents()
        .subscribe { event -> this.onAccountEvent(event) }

    this.profilesSubscription =
      profiles
        .profileEvents()
        .subscribe { event -> this.onProfileEvent(event) }
  }

  private fun onWantShowAccount(
    position: Int,
    profiles: ProfilesControllerType) {
    try {
      val selectedProvider = this.adapterAccounts.getItem(position)
      this.openAccountSettings(profiles.profileAccountFindByProvider(selectedProvider.id).id())
    } catch (e: ProfileNoneCurrentException) {
      throw IllegalStateException(e)
    } catch (e: AccountsDatabaseNonexistentException) {
      throw IllegalStateException(e)
    }
  }

  private fun onWantDeleteAccountByProvider(
    position: Int,
    profiles: ProfilesControllerType) {
    this.showAccountDeletionDialog(this.adapterAccounts.getItem(position), profiles)
  }

  /**
   * Open a dialog that allows for the deletion of an account associated with an account
   * provider.
   */

  private fun showAccountDeletionDialog(
    accountProvider: AccountProviderType,
    profiles: ProfilesControllerType) {
    val builder =
      AlertDialog.Builder(this)

    builder.setMessage(
      this.resources.getString(R.string.settings_account_delete,
        accountProvider.displayName))

    builder.setPositiveButton(R.string.settings_account_delete_button) { _, _ ->
      val providerId = accountProvider.id
      FluentFuture.from(profiles.profileAccountDeleteByProvider(providerId))
        .onException(java.lang.Exception::class.java) { exception -> AccountDeletionFailed.ofException(exception) }
        .map { event -> this.onAccountDeletionEvent(event) }
    }

    builder.create()
      .show()
  }

  private class AccountsArrayAdapter(
    targetContext: Context,
    private val picasso: Picasso,
    private val adapterAccountsArray: ArrayList<AccountProviderType>,
    private val inflater: LayoutInflater)
    : ArrayAdapter<AccountProviderType>(targetContext, R.layout.account_list_item, adapterAccountsArray) {

    override fun getView(
      position: Int,
      reuse: View?,
      parent: ViewGroup): View {

      val containerView: View
      if (reuse != null) {
        containerView = reuse
      } else {
        containerView = this.inflater.inflate(R.layout.account_list_item, parent, false)
      }

      val accountProvider =
        this.adapterAccountsArray[position]

      val iconView =
        containerView.findViewById<ImageView>(R.id.cellIcon)
      val itemTitleView =
        containerView.findViewById<TextView>(android.R.id.text1)
      val itemSubtitleView =
        containerView.findViewById<TextView>(android.R.id.text2)

      configureAccountListCellViews(
        picasso = this.picasso,
        accountProvider = accountProvider,
        itemTitleView = itemTitleView,
        itemSubtitleView = itemSubtitleView,
        iconView = iconView)

      return containerView
    }
  }

  private fun updateCurrentAccountView(
    currentAccountView: LinearLayout,
    account: AccountID) {

    try {
      UIThread.checkIsUIThread()

      val accountProvider =
        Simplified.getProfilesController()
          .profileCurrent()
          .account(account)
          .provider()

      val titleText =
        currentAccountView.findViewById<TextView>(android.R.id.text1)
      val subtitleText =
        currentAccountView.findViewById<TextView>(android.R.id.text2)
      val iconView =
        currentAccountView.findViewById<ImageView>(R.id.cellIcon)
      val localImageLoader =
        Simplified.getLocalImageLoader()

      configureAccountListCellViews(
        picasso = localImageLoader,
        accountProvider = accountProvider,
        itemTitleView = titleText,
        itemSubtitleView = subtitleText,
        iconView = iconView)

      currentAccountView.setOnClickListener { this.openAccountSettings(account) }
    } catch (e: ProfileNoneCurrentException) {
      throw IllegalStateException(e)
    } catch (e: AccountsDatabaseNonexistentException) {
      throw IllegalStateException(e)
    }
  }

  private fun openAccountSettings(account: AccountID) {
    val parameterBundle = Bundle()
    setActivityArguments(parameterBundle, false)
    parameterBundle.putSerializable(SettingsAccountActivity.ACCOUNT_ID, account)

    val intent = Intent()
    intent.setClass(this, SettingsAccountActivity::class.java)
    intent.putExtras(parameterBundle)
    this.startActivity(intent)
  }

  override fun onStart() {
    super.onStart()
    this.navigationDrawerShowUpIndicatorUnconditionally()
  }

  override fun onDestroy() {
    super.onDestroy()
    this.profilesSubscription?.unsubscribe()
    this.accountsSubscription?.unsubscribe()
  }

  private fun onAccountEvent(event: AccountEvent): Unit {
    this.logger.debug("onAccountEvent: {}", event)

    return when (event) {
      is AccountEventCreation ->
        this.onAccountCreationEvent(event)
      is AccountEventDeletion ->
        this.onAccountDeletionEvent(event)
      else ->
        Unit.unit()
    }
  }

  private fun onAccountCreationEvent(event: AccountEventCreation): Unit {
    return event.matchCreation<Unit, RuntimeException>(
      { this.onAccountCreationSucceeded(it) },
      { this.onAccountCreationFailed(it) })
  }

  private fun onAccountDeletionEvent(event: AccountEventDeletion): Unit {
    return event.matchDeletion<Unit, RuntimeException>(
      { this.onAccountDeletionSucceeded(it) },
      { this.onAccountDeletionFailed(it) })
  }

  private fun onAccountDeletionFailed(event: AccountDeletionFailed): Unit {
    this.logger.debug("onAccountDeletionFailed: {}", event)

    ErrorDialogUtilities.showError(
      this,
      this.logger,
      this.resources.getString(R.string.profiles_account_deletion_error_general), null)

    return Unit.unit()
  }

  private fun onAccountDeletionSucceeded(event: AccountDeletionSucceeded): Unit {
    this.logger.debug("onAccountDeletionSucceeded: {}", event)

    UIThread.runOnUIThread {
      this.populateAccountsArray()
      this.invalidateOptionsMenu()
    }
    return Unit.unit()
  }

  private fun onAccountCreationFailed(event: AccountCreationFailed): Unit {
    this.logger.debug("onAccountCreationFailed: {}", event)

    ErrorDialogUtilities.showError(
      this,
      this.logger,
      this.resources.getString(R.string.profiles_account_creation_error_general), null)

    return Unit.unit()
  }

  private fun onAccountCreationSucceeded(event: AccountCreationSucceeded): Unit {
    this.logger.debug("onAccountCreationSucceeded: {}", event)

    UIThread.runOnUIThread {
      this.populateAccountsArray()
      this.invalidateOptionsMenu()
    }
    return Unit.unit()
  }

  private fun onProfileEvent(event: ProfileEvent): Unit {
    if (event is ProfileAccountSelectEvent) {
      if (event is ProfileAccountSelectSucceeded) {
        return this.onProfileAccountSelectSucceeded(event)
      }
      if (event is ProfileAccountSelectFailed) {
        return this.onProfileAccountSelectFailed(event)
      }
    }
    return Unit.unit()
  }

  private fun onProfileAccountSelectFailed(event: ProfileAccountSelectFailed): Unit {
    this.logger.debug("onProfileAccountSelectFailed: {}", event)

    ErrorDialogUtilities.showError(
      this,
      this.logger,
      this.resources.getString(R.string.profiles_account_selection_error_general), null)

    return Unit.unit()
  }

  private fun onProfileAccountSelectSucceeded(event: ProfileAccountSelectSucceeded): Unit {
    this.logger.debug("onProfileAccountSelectSucceeded: {}", event)

    UIThread.runOnUIThread {
      this.updateCurrentAccountView(this.accountCurrentView, event.accountCurrent())
    }
    return Unit.unit()
  }

  /**
   * Fetch the currently used account providers and insert them all into the list view.
   */

  private fun populateAccountsArray() {
    try {
      UIThread.checkIsUIThread()

      val providers =
        Simplified.getProfilesController()
          .profileCurrentlyUsedAccountProviders()

      this.adapterAccountsArray.clear()
      this.adapterAccountsArray.addAll(providers)
      this.adapterAccountsArray.sort()
      this.adapterAccounts.notifyDataSetChanged()

    } catch (e: ProfileNoneCurrentException) {
      throw IllegalStateException(e)
    } catch (e: ProfileNonexistentAccountProviderException) {
      throw IllegalStateException(e)
    }
  }

  /**
   * Open a dialog that shows a list of available account providers, and offers the
   * ability to create an account for a selected provider.
   */

  private fun openAccountCreationDialog() {
    val availableAccountProviders =
      this.determineAvailableAccountProviders()

    val dialog = Dialog(this)
    dialog.setContentView(R.layout.accounts_picker)

    val adapter =
      AccountsArrayAdapter(
        targetContext = this,
        picasso = Simplified.getLocalImageLoader(),
        adapterAccountsArray = availableAccountProviders,
        inflater = this.layoutInflater)

    val listView = dialog.findViewById<ListView>(R.id.account_list)
    listView.adapter = adapter
    listView.setOnItemClickListener { _, _, position, _ ->
      val accountProvider = availableAccountProviders[position]
      availableAccountProviders.remove(accountProvider)
      adapter.notifyDataSetChanged()
      this.tryCreateAccount(accountProvider)
      dialog.dismiss()
      Unit.unit()
    }

    dialog.show()
  }

  /**
   * Return a list of the available account providers. An account provider is available
   * if no account already exists for it in the current profile.
   */

  private fun determineAvailableAccountProviders(): ArrayList<AccountProviderType> {
    val usedAccountProviders =
      Simplified.getProfilesController()
        .profileCurrentlyUsedAccountProviders()

    val availableAccountProviders =
      ArrayList(Simplified.getAccountProviders().providers().values)

    availableAccountProviders.removeAll(usedAccountProviders)
    availableAccountProviders.sortWith(Comparator { provider0, provider1 ->
      val name0 = provider0.displayName.removePrefix("The ")
      val name1 = provider1.displayName.removePrefix("The ")
      name0.toUpperCase().compareTo(name1.toUpperCase())
    })

    this.logger.debug("returning {} available providers", availableAccountProviders.size)
    return availableAccountProviders
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    try {
      if (item.itemId == R.id.add_account) {
        this.openAccountCreationDialog()
        return true
      }

      if (item.itemId == android.R.id.home) {
        this.onBackPressed()
        return true
      }

      return super.onOptionsItemSelected(item)
    } catch (e: ProfileNoneCurrentException) {
      throw IllegalStateException(e)
    } catch (e: ProfileNonexistentAccountProviderException) {
      throw IllegalStateException(e)
    }
  }

  private fun tryCreateAccount(accountProvider: AccountProviderType): Unit {
    FluentFuture
      .from(Simplified.getProfilesController().profileAccountCreate(accountProvider.id))
      .onException(Exception::class.java) { event -> AccountCreationFailed.of(event) }
      .map { event -> this.onAccountCreationEvent(event) }
    return Unit.unit()
  }

  /**
   * Create an options menu that shows a list of any account providers that are not currently
   * in use by the current profile. If the list would be empty, it isn't shown.
   */

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    try {
      val usedAccountProviders =
        Simplified.getProfilesController().profileCurrentlyUsedAccountProviders()
      val availableAccountProviders =
        ImmutableList.sortedCopyOf(Simplified.getAccountProviders().providers().values)

      if (usedAccountProviders.size != availableAccountProviders.size) {
        val inflater = this.menuInflater
        inflater.inflate(R.menu.add_account, menu)
      }

      return true
    } catch (e: ProfileNoneCurrentException) {
      throw IllegalStateException(e)
    } catch (e: ProfileNonexistentAccountProviderException) {
      throw IllegalStateException(e)
    }
  }
}

