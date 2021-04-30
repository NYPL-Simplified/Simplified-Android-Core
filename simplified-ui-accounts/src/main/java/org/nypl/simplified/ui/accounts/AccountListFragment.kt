package org.nypl.simplified.ui.accounts

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountEventDeletion
import org.nypl.simplified.accounts.api.AccountEventDeletion.AccountEventDeletionFailed
import org.nypl.simplified.accounts.api.AccountEventUpdated
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.images.ImageLoaderType

/**
 * A fragment that shows the set of accounts in the current profile.
 */

class AccountListFragment : Fragment(R.layout.account_list) {

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.accounts.AccountsFragment.parameters"

    /**
     * Create a new accounts fragment for the given parameters.
     */

    fun create(parameters: AccountListFragmentParameters): AccountListFragment {
      val fragment = AccountListFragment()
      fragment.arguments = bundleOf(PARAMETERS_ID to parameters)
      return fragment
    }
  }

  private val subscriptions = CompositeDisposable()
  private val viewModel: AccountListViewModel by viewModels()
  private val listener: FragmentListenerType<AccountListEvent> by fragmentListeners()

  private val parameters: AccountListFragmentParameters by lazy {
    this.requireArguments()[PARAMETERS_ID] as AccountListFragmentParameters
  }

  private lateinit var accountList: RecyclerView
  private lateinit var accountListAdapter: AccountListAdapter
  private lateinit var imageLoader: ImageLoaderType

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)

    val services = Services.serviceDirectory()

    this.imageLoader =
      services.requireService(ImageLoaderType::class.java)
  }

  private fun onAccountDeleteClicked(account: AccountType) {
    val context = this.requireContext()
    AlertDialog.Builder(context)
      .setTitle(R.string.accountsDeleteConfirmTitle)
      .setMessage(
        context.getString(
          R.string.accountsDeleteConfirm,
          account.provider.displayName
        )
      )
      .setNegativeButton(R.string.cancel) { dialog, _ ->
        dialog.dismiss()
      }
      .setPositiveButton(R.string.accountsDelete) { dialog, _ ->
        this.viewModel.deleteAccountByProvider(account.provider.id)
        dialog.dismiss()
      }
      .create()
      .show()
  }

  private fun onAccountClicked(account: AccountType) {
    this.listener.post(AccountListEvent.AccountSelected(account.id))
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.accountList =
      view.findViewById(R.id.accountList)

    this.accountListAdapter =
      AccountListAdapter(
        imageLoader = this.imageLoader,
        onItemClicked = this::onAccountClicked,
        onItemDeleteClicked = this::onAccountDeleteClicked
      )

    this.updateAccountList()

    with(this.accountList) {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(this.context)
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      adapter = this@AccountListFragment.accountListAdapter
    }
  }

  override fun onStart() {
    super.onStart()
    this.configureToolbar(this.requireActivity())

    this.viewModel.accountEvents
      .subscribe(this::onAccountEvent)
      .let { subscriptions.add(it) }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.account_list, menu)

    val accountAdd = menu.findItem(R.id.accountsMenuActionAccountAdd)
    accountAdd.isVisible = this.parameters.shouldShowLibraryRegistryMenu
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.accountsMenuActionAccountAdd -> {
        this.listener.post(AccountListEvent.AddAccount)
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

  private fun updateAccountList() {
    val accounts = this.viewModel.accounts.sortedWith(AccountComparator())
    this.accountListAdapter.submitList(accounts)
  }

  private fun onAccountEvent(accountEvent: AccountEvent) {
    when (accountEvent) {
      is AccountEventDeletionFailed -> {
        this.showAccountDeletionFailedDialog(accountEvent)
      }
      is AccountEventCreation.AccountEventCreationSucceeded,
      is AccountEventDeletion.AccountEventDeletionSucceeded,
      is AccountEventUpdated -> {
        this.updateAccountList()
      }
    }
  }

  private fun showAccountDeletionFailedDialog(accountEvent: AccountEventDeletionFailed) {
    AlertDialog.Builder(this.requireContext())
      .setTitle(R.string.accountsDeletionFailed)
      .setMessage(R.string.accountsDeletionFailedMessage)
      .setPositiveButton(R.string.accountsDetails) { _, _ ->
        showErrorPage(accountEvent)
      }
      .create()
      .show()
  }

  private fun showErrorPage(accountEvent: AccountEventDeletionFailed) {
    val parameters =
      ErrorPageParameters(
        emailAddress = this.viewModel.supportEmailAddress,
        body = "",
        subject = "[simplye-error-report]",
        attributes = accountEvent.attributes.toSortedMap(),
        taskSteps = accountEvent.taskResult.steps
      )

    this.listener.post(AccountListEvent.OpenErrorPage(parameters))
  }

  override fun onStop() {
    super.onStop()
    this.subscriptions.clear()
  }
}
