package org.nypl.simplified.ui.accounts

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.isVisible
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryStatus
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.images.ImageLoaderType
import org.slf4j.LoggerFactory

/**
 * A fragment that shows the account registry and allows for account creation.
 */

class AccountListRegistryFragment : Fragment(R.layout.account_list_registry) {

  private val logger = LoggerFactory.getLogger(AccountListRegistryFragment::class.java)
  private val subscriptions = CompositeDisposable()
  private val listener: FragmentListenerType<AccountListRegistryEvent> by fragmentListeners()

  private val requestLocationPermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission(),
    ::getLocation
  )
  private val viewModel: AccountListRegistryViewModel by assistedViewModels {
    val locationManager =
      requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    AccountListRegistryViewModel(locationManager)
  }

  private lateinit var accountList: RecyclerView
  private lateinit var accountListAdapter: FilterableAccountListAdapter
  private lateinit var imageLoader: ImageLoaderType
  private lateinit var progress: ContentLoadingProgressBar
  private lateinit var title: TextView
  private lateinit var noLocation: TextView
  private var reload: MenuItem? = null
  private var errorDialog: AlertDialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)

    val services =
      Services.serviceDirectory()

    this.imageLoader =
      services.requireService(ImageLoaderType::class.java)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.title =
      view.findViewById(R.id.accountRegistryTitle)
    this.progress =
      view.findViewById(R.id.accountRegistryProgress)
    this.accountList =
      view.findViewById(R.id.accountRegistryList)
    this.noLocation =
      view.findViewById(R.id.accountRegistryNoLocation)

    this.accountListAdapter =
      FilterableAccountListAdapter(
        this.imageLoader,
        this::onAccountClicked
      )

    with(this.accountList) {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(this.context)
      adapter = this@AccountListRegistryFragment.accountListAdapter
      addItemDecoration(SpaceItemDecoration(RecyclerView.VERTICAL, requireContext()))
    }
    requestLocationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
  }

  private fun onAccountClicked(account: AccountProviderDescription) {
    this.logger.debug("selected account: {} ({})", account.id, account.title)

    this.accountList.visibility = View.INVISIBLE
    this.title.setText(R.string.accountRegistryCreating)
    this.progress.show()

    this.viewModel.createAccount(account.id)
  }

  override fun onStart() {
    super.onStart()
    this.configureToolbar(this.requireActivity())

    this.viewModel.accountRegistryEvents
      .subscribe(this::onAccountRegistryEvent)
      .let { subscriptions.add(it) }

    this.viewModel.accountCreationEvents
      .subscribe(this::onAccountEvent)
      .let { subscriptions.add(it) }

    this.viewModel.displayNoLocationMessageEvents
      .subscribe { this.noLocation.isVisible = it }
      .let(this.subscriptions::add)

    this.reconfigureViewForRegistryStatus(this.viewModel.accountRegistryStatus)
  }

  private fun onAccountEvent(event: AccountEvent) {
    when (event) {
      is AccountEventCreation.AccountEventCreationInProgress -> {
        this.accountList.visibility = View.INVISIBLE
        this.progress.show()
        this.title.text = event.message
      }

      is AccountEventCreation.AccountEventCreationSucceeded -> {
        this.listener.post(AccountListRegistryEvent.AccountCreated)
      }

      is AccountEventCreation.AccountEventCreationFailed -> {
        this.showAccountCreationFailedDialog(event)
        this.reconfigureViewForRegistryStatus(this.viewModel.accountRegistryStatus)
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.account_list_registry, menu)
    this.reload = menu.findItem(R.id.accountMenuActionReload)

    val search = menu.findItem(R.id.accountMenuActionSearch)
    val searchView = search.actionView as SearchView

    searchView.imeOptions = EditorInfo.IME_ACTION_DONE
    searchView.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    searchView.queryHint = getString(R.string.accountSearchHint)

    searchView.setOnQueryTextListener(object : OnQueryTextListener {
      override fun onQueryTextSubmit(query: String): Boolean {
        search.collapseActionView()
        return true
      }

      override fun onQueryTextChange(newText: String): Boolean {
        when {
          newText.isEmpty() -> {
            this@AccountListRegistryFragment.accountListAdapter.resetFilter()
          }
          newText.equals("NYPL", ignoreCase = true) -> {
            this@AccountListRegistryFragment.accountListAdapter.filterList { account ->
              account.title.contains("New York Public Library", ignoreCase = true)
            }
          }
          else -> {
            this@AccountListRegistryFragment.accountListAdapter.filterList { account ->
              account.title.contains(newText, ignoreCase = true)
            }
          }
        }
        return true
      }
    })

    search.setOnActionExpandListener(object : OnActionExpandListener {
      override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
        // Do nothing
        return true
      }

      override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
        this@AccountListRegistryFragment.accountListAdapter.resetFilter()
        return true
      }
    })
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.accountMenuActionReload -> {
        this.reload()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun configureToolbar(activity: Activity) {
    this.supportActionBar?.apply {
      title = getString(R.string.accountAdd)
      subtitle = null
    }
  }

  private fun reload() {
    this.reload?.isEnabled = false
    this.viewModel.refreshAccountRegistry()
  }

  private fun onAccountRegistryEvent(event: AccountProviderRegistryEvent) {
    return when (event) {
      AccountProviderRegistryEvent.StatusChanged -> {
        this.reconfigureViewForRegistryStatus(this.viewModel.accountRegistryStatus)
      }
      is AccountProviderRegistryEvent.Updated -> {
      }
      is AccountProviderRegistryEvent.SourceFailed -> {
      }
    }
  }

  private fun reconfigureViewForRegistryStatus(status: AccountProviderRegistryStatus) {
    return when (status) {
      AccountProviderRegistryStatus.Idle -> {
        this.title.setText(R.string.accountRegistrySelect)
        this.accountList.visibility = View.VISIBLE
        this.progress.hide()
        this.reload?.isEnabled = true

        val availableDescriptions =
          this.viewModel.determineAvailableAccountProviderDescriptions()

        if (availableDescriptions.isEmpty()) {
          this.title.setText(R.string.accountRegistryEmpty)
        } else {
          this.title.setText(R.string.accountRegistrySelect)
        }
        this.accountListAdapter.submitList(availableDescriptions)
      }

      AccountProviderRegistryStatus.Refreshing -> {
        this.title.setText(R.string.accountRegistrySelect)
        this.accountList.visibility = View.INVISIBLE
        this.progress.show()
        this.title.setText(R.string.accountRegistryRetrieving)
        this.reload?.isEnabled = false
      }
    }
  }

  override fun onStop() {
    super.onStop()
    this.subscriptions.clear()
  }

  private fun showAccountCreationFailedDialog(accountEvent: AccountEventCreation.AccountEventCreationFailed) {
    this.logger.debug("showAccountCreationFailedDialog")

    if (this.errorDialog == null) {
      val newDialog =
        AlertDialog.Builder(this.requireActivity())
          .setTitle(R.string.accountCreationFailed)
          .setMessage(R.string.accountCreationFailedMessage)
          .setPositiveButton(R.string.accountsDetails) { dialog, _ ->
            this.errorDialog = null
            this.showErrorPage(accountEvent)
            dialog.dismiss()
          }.create()
      this.errorDialog = newDialog
      newDialog.show()
    }
  }

  private fun showErrorPage(accountEvent: AccountEventCreation.AccountEventCreationFailed) {
    val parameters =
      ErrorPageParameters(
        emailAddress = this.viewModel.supportEmailAddress,
        body = "",
        subject = "[simplye-error-report]",
        attributes = accountEvent.attributes.toSortedMap(),
        taskSteps = accountEvent.taskResult.steps
      )

    this.listener.post(AccountListRegistryEvent.OpenErrorPage(parameters))
  }

  @SuppressLint("MissingPermission")
  private fun getLocation(isPermissionGranted: Boolean) = viewModel.getLocation(isPermissionGranted)
}
