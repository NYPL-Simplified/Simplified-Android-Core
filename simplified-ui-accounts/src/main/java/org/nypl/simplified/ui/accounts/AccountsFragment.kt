package org.nypl.simplified.ui.accounts

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import io.reactivex.disposables.Disposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountEventDeletion.AccountEventDeletionFailed
import org.nypl.simplified.accounts.api.AccountEventDeletion.AccountEventDeletionSucceeded
import org.nypl.simplified.accounts.api.AccountEventUpdated
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.images.ImageLoaderType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType

/**
 * A fragment that shows the set of accounts in the current profile.
 */

class AccountsFragment : Fragment() {

  private lateinit var accountList: RecyclerView
  private lateinit var accountListAdapter: AccountsAdapter
  private lateinit var accountListData: MutableList<AccountType>
  private lateinit var buildConfig: BuildConfigurationServiceType
  private lateinit var imageLoader: ImageLoaderType
  private lateinit var parameters: AccountsFragmentParameters
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var uiThread: UIThreadServiceType
  private var accountSubscription: Disposable? = null

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.accounts.AccountsFragment.parameters"

    /**
     * Create a new accounts fragment for the given parameters.
     */

    fun create(parameters: AccountsFragmentParameters): AccountsFragment {
      val arguments = Bundle()
      arguments.putSerializable(PARAMETERS_ID, parameters)
      val fragment = AccountsFragment()
      fragment.arguments = arguments
      return fragment
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)

    this.parameters = this.requireArguments()[PARAMETERS_ID] as AccountsFragmentParameters
    this.accountListData = mutableListOf()

    val services = Services.serviceDirectory()

    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.imageLoader =
      services.requireService(ImageLoaderType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
    this.buildConfig =
      services.requireService(BuildConfigurationServiceType::class.java)
  }

  @UiThread
  private fun onAccountLongClicked(account: AccountType) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    AlertDialog.Builder(context)
      .setTitle(R.string.accountsDeleteConfirmTitle)
      .setMessage(
        context.getString(
          R.string.accountsDeleteConfirm,
          account.provider.displayName
        )
      )
      .setPositiveButton(R.string.accountsDelete) { dialog, _ ->
        this.profilesController.profileAccountDeleteByProvider(account.provider.id)
        dialog.dismiss()
      }
      .create()
      .show()
  }

  @UiThread
  private fun onAccountClicked(account: AccountType) {
    this.uiThread.checkIsUIThread()
    this.findNavigationController().openSettingsAccount(
      AccountFragmentParameters(
        accountId = account.id,
        closeOnLoginSuccess = false,
        showPleaseLogInTitle = false
      )
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout =
      inflater.inflate(R.layout.accounts, container, false)

    this.accountList =
      layout.findViewById(R.id.accountList)

    this.accountList.setHasFixedSize(true)
    this.accountList.layoutManager = LinearLayoutManager(this.context)
    (this.accountList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    return layout
  }

  override fun onStart() {
    super.onStart()
    this.configureToolbar(this.requireActivity())

    this.accountSubscription =
      this.profilesController.accountEvents()
        .subscribe(this::onAccountEvent)

    this.accountListAdapter =
      AccountsAdapter(
        accounts = this.accountListData,
        imageLoader = this.imageLoader,
        onItemClicked = this::onAccountClicked,
        onItemLongClicked = this::onAccountLongClicked
      )

    this.accountList.adapter = this.accountListAdapter

    this.uiThread.runOnUIThread(
      Runnable {
        this.reconfigureAccountListUI()
      }
    )
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.accounts, menu)

    val accountAdd = menu.findItem(R.id.accountsMenuActionAccountAdd)
    accountAdd.isVisible = this.parameters.shouldShowLibraryRegistryMenu
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.accountsMenuActionAccountAdd -> {
        this.findNavigationController().openSettingsAccountRegistry()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun configureToolbar(activity: Activity) {
    this.supportActionBar?.apply {
      title = getString(R.string.accounts)
      subtitle = null
    }
  }

  private fun onAccountEvent(accountEvent: AccountEvent) {
    return when (accountEvent) {
      is AccountEventCreation.AccountEventCreationSucceeded,
      is AccountEventDeletionSucceeded,
      is AccountEventUpdated -> {
        this.uiThread.runOnUIThread(
          Runnable {
            this.reconfigureAccountListUI()
          }
        )
      }

      is AccountEventDeletionFailed -> {
        this.uiThread.runOnUIThread(
          Runnable {
            this.showAccountDeletionFailedDialog(accountEvent)
          }
        )
      }

      else -> {
      }
    }
  }

  @UiThread
  private fun showAccountDeletionFailedDialog(accountEvent: AccountEventDeletionFailed) {
    this.uiThread.checkIsUIThread()

    AlertDialog.Builder(this.requireContext())
      .setTitle(R.string.accountsDeletionFailed)
      .setMessage(R.string.accountsDeletionFailedMessage)
      .setPositiveButton(R.string.accountsDetails) { _, _ ->
        showErrorPage(accountEvent)
      }
      .create()
      .show()
  }

  @UiThread
  private fun showErrorPage(accountEvent: AccountEventDeletionFailed) {
    this.uiThread.checkIsUIThread()

    val parameters =
      ErrorPageParameters(
        emailAddress = this.buildConfig.supportErrorReportEmailAddress,
        body = "",
        subject = "[simplye-error-report]",
        attributes = accountEvent.attributes.toSortedMap(),
        taskSteps = accountEvent.taskResult.steps
      )

    this.findNavigationController().openErrorPage(parameters)
  }

  @UiThread
  private fun reconfigureAccountListUI() {
    this.uiThread.checkIsUIThread()

    val profile =
      this.profilesController.profileCurrent()

    val accountList =
      profile
        .accounts()
        .values
        .sortedWith(AccountComparator())

    this.accountListData.clear()
    this.accountListData.addAll(accountList)
    this.accountListAdapter.notifyDataSetChanged()
  }

  override fun onStop() {
    super.onStop()
    this.accountSubscription?.dispose()
  }

  private fun findNavigationController(): AccountNavigationControllerType {
    return NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = AccountNavigationControllerType::class.java
    )
  }
}
