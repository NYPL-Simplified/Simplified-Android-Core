package org.nypl.simplified.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import io.reactivex.disposables.Disposable
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountEventDeletion
import org.nypl.simplified.accounts.api.AccountEventUpdated
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.toolbar.ToolbarHostType
import org.nypl.simplified.ui.host.HostViewModel
import org.nypl.simplified.ui.host.HostViewModelReadableType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType

/**
 * A fragment that shows the set of accounts in the current profile.
 */

class SettingsFragmentAccounts : Fragment() {

  private lateinit var accountCurrentSubtitle: TextView
  private lateinit var accountCurrentTitle: TextView
  private lateinit var accountCurrentView: ViewGroup
  private lateinit var accountList: RecyclerView
  private lateinit var accountListAdapter: SettingsAccountsAdapter
  private lateinit var accountListData: MutableList<AccountType>
  private lateinit var hostModel: HostViewModelReadableType
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var uiThread: UIThreadServiceType
  private var accountSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.accountListData = mutableListOf()
    this.accountListAdapter =
      SettingsAccountsAdapter(
        accounts = this.accountListData,
        onItemClicked = { account -> this.onAccountClicked(account) },
        onItemLongClicked = { account -> this.onAccountLongClicked(account) })
  }

  @UiThread
  private fun onAccountLongClicked(account: AccountType) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    AlertDialog.Builder(context)
      .setTitle(R.string.settingsAccountDeleteConfirmTitle)
      .setMessage(context.getString(R.string.settingsAccountDeleteConfirm, account.provider.displayName))
      .setPositiveButton(R.string.settingsAccountDelete) { dialog, which ->
        dialog.dismiss()
      }
      .create()
      .show()
  }

  @UiThread
  private fun onAccountClicked(account: AccountType) {
    this.uiThread.checkIsUIThread()
    this.findNavigationController().openSettingsAccount(account.id)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout =
      inflater.inflate(R.layout.settings_accounts, container, false)

    this.accountCurrentView =
      layout.findViewById(R.id.accountCurrent)
    this.accountCurrentTitle =
      this.accountCurrentView.findViewById(R.id.accountCellTitle)
    this.accountCurrentSubtitle =
      this.accountCurrentView.findViewById(R.id.accountCellSubtitle)

    this.accountList =
      layout.findViewById(R.id.accountList)

    this.accountList.setHasFixedSize(true)
    this.accountList.layoutManager = LinearLayoutManager(this.context)
    this.accountList.adapter = this.accountListAdapter
    (this.accountList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    return layout
  }

  override fun onStart() {
    super.onStart()

    this.hostModel =
      ViewModelProviders.of(this.requireActivity())
        .get(HostViewModel::class.java)

    this.configureToolbar()

    this.profilesController =
      this.hostModel.services.requireService(ProfilesControllerType::class.java)
    this.uiThread =
      this.hostModel.services.requireService(UIThreadServiceType::class.java)

    this.accountSubscription =
      this.profilesController.accountEvents()
        .subscribe(this::onAccountEvent)

    this.uiThread.runOnUIThread(Runnable {
      this.reconfigureAccountListUI()
    })
  }

  private fun configureToolbar() {
    val host = this.activity
    if (host is ToolbarHostType) {
      host.toolbarClearMenu()
      host.toolbarSetTitleSubtitle(
        title = this.requireContext().getString(R.string.settingsAccounts),
        subtitle = ""
      )
      host.toolbarSetBackArrowConditionally(
        context = host,
        shouldArrowBePresent = {
          this.findNavigationController().backStackSize() > 1
        },
        onArrowClicked = {
          this.findNavigationController().popBackStack()
        })
    } else {
      throw IllegalStateException("The activity ($host) hosting this fragment must implement ${ToolbarHostType::class.java}")
    }
  }

  private fun onAccountEvent(accountEvent: AccountEvent) {
    return when (accountEvent) {
      is AccountEventCreation.AccountEventCreationSucceeded,
      is AccountEventDeletion.AccountEventDeletionSucceeded,
      is AccountEventUpdated -> {
        this.uiThread.runOnUIThread(Runnable {
          this.reconfigureAccountListUI()
        })
      }
      else -> {

      }
    }
  }

  @UiThread
  private fun reconfigureAccountListUI() {
    this.uiThread.checkIsUIThread()

    val profile =
      this.profilesController.profileCurrent()

    val accountNow =
      profile.accountCurrent()

    val accountList =
      profile
        .accounts()
        .values
        .filter { account -> account.id != accountNow.id }
        .sortedBy { account -> account.provider.displayName }

    this.accountCurrentTitle.text = accountNow.provider.displayName
    this.accountCurrentSubtitle.text = accountNow.provider.subtitle

    this.accountListData.clear()
    this.accountListData.addAll(accountList)
    this.accountListAdapter.notifyDataSetChanged()
  }

  override fun onStop() {
    super.onStop()

    this.accountSubscription?.dispose()
  }

  private fun findNavigationController(): SettingsNavigationControllerType {
    return this.hostModel.navigationController(SettingsNavigationControllerType::class.java)
  }
}
