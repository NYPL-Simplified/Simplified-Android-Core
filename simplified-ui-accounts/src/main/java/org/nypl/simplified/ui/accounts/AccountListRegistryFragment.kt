package org.nypl.simplified.ui.accounts

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import io.reactivex.disposables.Disposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryStatus
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.threads.NamedThreadPools
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.images.ImageLoaderType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory

/**
 * A fragment that shows the account registry and allows for account creation.
 */

class AccountListRegistryFragment : Fragment() {

  private var errorDialog: AlertDialog? = null
  private lateinit var backgroundExecutor: ListeningScheduledExecutorService
  private lateinit var accountList: RecyclerView
  private lateinit var accountListAdapter: FilterableAccountListAdapter
  private lateinit var accountRegistry: AccountProviderRegistryType
  private lateinit var buildConfig: BuildConfigurationServiceType
  private lateinit var imageLoader: ImageLoaderType

  @Volatile
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var progress: ContentLoadingProgressBar
  private lateinit var title: TextView
  private lateinit var uiThread: UIThreadServiceType

  private val logger = LoggerFactory.getLogger(AccountListRegistryFragment::class.java)
  private var accountCreationSubscription: Disposable? = null
  private var accountRegistrySubscription: Disposable? = null
  private var reload: MenuItem? = null

  private val navigationController by lazy<AccountNavigationControllerType> {
    NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = AccountNavigationControllerType::class.java
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)

    val services = Services.serviceDirectory()

    this.accountRegistry =
      services.requireService(AccountProviderRegistryType::class.java)
    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
    this.imageLoader =
      services.requireService(ImageLoaderType::class.java)
    this.buildConfig =
      services.requireService(BuildConfigurationServiceType::class.java)

    this.accountListAdapter =
      FilterableAccountListAdapter(
        this.imageLoader,
        this::onAccountClicked
      )
  }

  /**
   * Return a list of the available account providers. An account provider is available
   * if no account already exists for it in the current profile.
   */

  private fun determineAvailableAccountProviderDescriptions(): List<AccountProviderDescription> {
    val usedAccountProviders =
      this.profilesController
        .profileCurrentlyUsedAccountProviders()
        .map { p -> p.toDescription() }

    this.logger.debug("profile is using {} providers", usedAccountProviders.size)

    val availableAccountProviders =
      this.accountRegistry.accountProviderDescriptions()
        .values
        .toMutableList()
    availableAccountProviders.removeAll(usedAccountProviders)

    this.logger.debug("returning {} available providers", availableAccountProviders.size)
    return availableAccountProviders
  }

  @UiThread
  private fun onAccountClicked(account: AccountProviderDescription) {
    this.uiThread.checkIsUIThread()

    this.logger.debug("selected account: {} ({})", account.id, account.title)

    this.accountList.visibility = View.INVISIBLE
    this.title.setText(R.string.accountRegistryCreating)
    this.progress.show()

    this.profilesController.profileAccountCreate(account.id)
  }

  private fun onAccountEvent(event: AccountEvent) {
    return when (event) {
      is AccountEventCreation.AccountEventCreationInProgress ->
        this.uiThread.runOnUIThread(
          Runnable {
            this.accountList.visibility = View.INVISIBLE
            this.progress.show()
            this.title.text = event.message
          }
        )

      is AccountEventCreation.AccountEventCreationSucceeded -> {
        this.uiThread.runOnUIThread {
          this.navigationController.popBackStack()
        }
      }

      is AccountEventCreation.AccountEventCreationFailed ->
        this.uiThread.runOnUIThread(
          Runnable {
            this.showAccountCreationFailedDialog(event)
            this.reconfigureViewForRegistryStatus(this.accountRegistry.status)
          }
        )

      else -> {
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout =
      inflater.inflate(R.layout.account_list_registry, container, false)

    this.title =
      layout.findViewById(R.id.accountRegistryTitle)
    this.progress =
      layout.findViewById(R.id.accountRegistryProgress)
    this.accountList =
      layout.findViewById(R.id.accountRegistryList)

    this.accountList.setHasFixedSize(true)
    this.accountList.layoutManager = LinearLayoutManager(this.context)
    this.accountList.adapter = this.accountListAdapter

    return layout
  }

  override fun onStart() {
    super.onStart()
    this.configureToolbar(this.requireActivity())

    this.backgroundExecutor =
      NamedThreadPools.namedThreadPool(1, "simplified-registry-io", 19)
    this.accountRegistrySubscription =
      this.accountRegistry.events.subscribe(this::onAccountRegistryEvent)

    this.accountCreationSubscription =
      this.profilesController
        .accountEvents()
        .subscribe(this::onAccountEvent)

    this.reconfigureViewForRegistryStatus(this.accountRegistry.status)

    this.backgroundExecutor.execute {
      try {
        this.accountRegistry.refresh(
          includeTestingLibraries = this.profilesController
            .profileCurrent()
            .preferences()
            .showTestingLibraries
        )
      } catch (e: Exception) {
        this.logger.error("failed to refresh registry: ", e)
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
    this.backgroundExecutor.execute {
      try {
        this.accountRegistry.refresh(
          includeTestingLibraries = this.profilesController
            .profileCurrent()
            .preferences()
            .showTestingLibraries
        )
      } catch (e: Exception) {
        this.logger.error("failed to refresh registry: ", e)
      }
    }
  }

  private fun onAccountRegistryEvent(event: AccountProviderRegistryEvent) {
    return when (event) {
      AccountProviderRegistryEvent.StatusChanged -> {
        this.uiThread.runOnUIThread(
          Runnable {
            this.reconfigureViewForRegistryStatus(this.accountRegistry.status)
          }
        )
      }
      is AccountProviderRegistryEvent.Updated -> {
      }
      is AccountProviderRegistryEvent.SourceFailed -> {
      }
    }
  }

  @UiThread
  private fun reconfigureViewForRegistryStatus(status: AccountProviderRegistryStatus) {
    this.uiThread.checkIsUIThread()

    return when (status) {
      AccountProviderRegistryStatus.Idle -> {
        this.title.setText(R.string.accountRegistrySelect)
        this.accountList.visibility = View.VISIBLE
        this.progress.hide()
        this.reload?.isEnabled = true

        val availableDescriptions =
          this.determineAvailableAccountProviderDescriptions()

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

    this.backgroundExecutor.shutdown()
    this.accountCreationSubscription?.dispose()
    this.accountRegistrySubscription?.dispose()
  }

  @UiThread
  private fun showAccountCreationFailedDialog(accountEvent: AccountEventCreation.AccountEventCreationFailed) {
    this.uiThread.checkIsUIThread()

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

  @UiThread
  private fun showErrorPage(accountEvent: AccountEventCreation.AccountEventCreationFailed) {
    this.uiThread.checkIsUIThread()

    val parameters =
      ErrorPageParameters(
        emailAddress = this.buildConfig.supportErrorReportEmailAddress,
        body = "",
        subject = "[simplye-error-report]",
        attributes = accountEvent.attributes.toSortedMap(),
        taskSteps = accountEvent.taskResult.steps
      )

    this.navigationController.openErrorPage(parameters)
  }
}
