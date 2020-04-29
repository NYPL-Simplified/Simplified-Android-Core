package org.nypl.simplified.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import io.reactivex.disposables.Disposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryEvent
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryStatus
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.api.ProfilePreferences
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.taskrecorder.api.TaskStepResolution
import org.nypl.simplified.threads.NamedThreadPools
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.images.ImageLoaderType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType
import org.slf4j.LoggerFactory

/**
 * A fragment that shows the account registry and allows for account creation.
 */

class SettingsFragmentAccountRegistry : Fragment() {

  private lateinit var backgroundExecutor: ListeningScheduledExecutorService
  private lateinit var accountList: RecyclerView
  private lateinit var accountListAdapter: SettingsAccountProviderDescriptionAdapter
  private lateinit var accountListData: MutableList<AccountProviderDescription>
  private lateinit var accountRegistry: AccountProviderRegistryType
  private lateinit var buildConfig: BuildConfigurationServiceType
  private lateinit var imageLoader: ImageLoaderType

  @Volatile
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var progress: ProgressBar
  private lateinit var progressText: TextView
  private lateinit var refresh: Button
  private lateinit var title: TextView
  private lateinit var uiThread: UIThreadServiceType
  private val logger = LoggerFactory.getLogger(SettingsFragmentAccountRegistry::class.java)
  private var accountCreationSubscription: Disposable? = null
  private var accountRegistrySubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

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

    this.accountListData = mutableListOf()
    this.accountListAdapter =
      SettingsAccountProviderDescriptionAdapter(
        this.accountListData,
        this.imageLoader,
        this::onAccountClicked
      )
  }

  /**
   * Return a list of the available account providers. An account provider is available
   * if no account already exists for it in the current profile.
   */

  private fun determineAvailableAccountProviderDescriptions(): List<AccountProviderDescription> {

    val profileCurrent =
      this.profilesController.profileCurrent()
    val preferences =
      profileCurrent.preferences()

    val usedAccountProviders =
      this.profilesController
        .profileCurrentlyUsedAccountProviders()
        .map { p -> p.toDescription() }

    this.logger.debug("should show testing providers: {}", preferences.showTestingLibraries)
    this.logger.debug("profile is using {} providers", usedAccountProviders.size)

    val availableAccountProviders =
      this.accountRegistry.accountProviderDescriptions()
        .values
        .filter { provider -> shouldShowProvider(provider, preferences) }
        .toMutableList()

    availableAccountProviders.removeAll(usedAccountProviders)
    availableAccountProviders.sortWith(Comparator { provider0, provider1 ->
      val name0 = provider0.title.removePrefix("The ")
      val name1 = provider1.title.removePrefix("The ")
      name0.toUpperCase().compareTo(name1.toUpperCase())
    })

    this.logger.debug("returning {} available providers", availableAccountProviders.size)
    return availableAccountProviders
  }

  private fun shouldShowProvider(
    provider: AccountProviderDescription,
    preferences: ProfilePreferences
  ) =
    provider.isProduction || preferences.showTestingLibraries

  @UiThread
  private fun onAccountClicked(account: AccountProviderDescription) {
    this.uiThread.checkIsUIThread()

    this.logger.debug("selected account: {} ({})", account.id, account.title)

    this.refresh.isEnabled = false
    this.accountList.visibility = View.INVISIBLE
    this.title.setText(R.string.settingsAccountRegistryCreating)
    this.progressText.text = ""
    this.progress.visibility = View.VISIBLE

    this.profilesController.profileAccountCreate(account.id)
  }

  private fun onAccountEvent(event: AccountEvent) {
    return when (event) {
      is AccountEventCreation.AccountEventCreationInProgress ->
        this.uiThread.runOnUIThread(Runnable {
          this.accountList.visibility = View.INVISIBLE
          this.progress.visibility = View.VISIBLE
          this.progressText.text = event.message
        })

      is AccountEventCreation.AccountEventCreationSucceeded -> {
        this.findNavigationController().popBackStack()
        Unit
      }

      is AccountEventCreation.AccountEventCreationFailed ->
        this.uiThread.runOnUIThread(Runnable {
          this.showAccountCreationFailedDialog(event)
          this.reconfigureViewForRegistryStatus(this.accountRegistry.status)
        })

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
      inflater.inflate(R.layout.settings_account_registry, container, false)

    this.refresh =
      layout.findViewById(R.id.accountRegistryRefreshButton)
    this.title =
      layout.findViewById(R.id.accountRegistryTitle)
    this.progress =
      layout.findViewById(R.id.accountRegistryProgress)
    this.progressText =
      layout.findViewById(R.id.accountRegistryProgressText)
    this.accountList =
      layout.findViewById(R.id.accountRegistryList)

    this.accountList.setHasFixedSize(true)
    this.accountList.layoutManager = LinearLayoutManager(this.context)
    this.accountList.adapter = this.accountListAdapter
    (this.accountList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    this.refresh.setOnClickListener {
      this.refresh.isEnabled = false

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

    return layout
  }

  override fun onStart() {
    super.onStart()

    this.configureToolbar()

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

  private fun onAccountRegistryEvent(event: AccountProviderRegistryEvent) {
    return when (event) {
      AccountProviderRegistryEvent.StatusChanged -> {
        this.uiThread.runOnUIThread(Runnable {
          this.reconfigureViewForRegistryStatus(this.accountRegistry.status)
        })
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
        this.title.setText(R.string.settingsAccountRegistrySelect)
        this.accountList.visibility = View.VISIBLE
        this.progress.visibility = View.INVISIBLE
        this.refresh.isEnabled = true

        val availableDescriptions =
          this.determineAvailableAccountProviderDescriptions()

        if (availableDescriptions.isEmpty()) {
          this.progressText.visibility = View.VISIBLE
          this.progressText.setText(R.string.settingsAccountRegistryEmpty)
        } else {
          this.progressText.visibility = View.INVISIBLE
        }

        this.accountListData.clear()
        this.accountListData.addAll(availableDescriptions)
        this.accountListAdapter.notifyDataSetChanged()
      }

      AccountProviderRegistryStatus.Refreshing -> {
        this.title.setText(R.string.settingsAccountRegistrySelect)
        this.accountList.visibility = View.INVISIBLE
        this.progress.visibility = View.VISIBLE
        this.progressText.visibility = View.VISIBLE
        this.progressText.setText(R.string.settingsAccountRegistryRetrieving)
        this.refresh.isEnabled = false
      }
    }
  }

  override fun onStop() {
    super.onStop()

    this.backgroundExecutor.shutdown()
    this.accountCreationSubscription?.dispose()
    this.accountRegistrySubscription?.dispose()
  }

  private fun findNavigationController(): SettingsNavigationControllerType {
    return NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = SettingsNavigationControllerType::class.java
    )
  }

  @UiThread
  private fun showAccountCreationFailedDialog(accountEvent: AccountEventCreation.AccountEventCreationFailed) {
    this.uiThread.checkIsUIThread()

    AlertDialog.Builder(this.requireContext())
      .setTitle(R.string.settingsAccountCreationFailed)
      .setMessage(R.string.settingsAccountCreationFailedMessage)
      .setPositiveButton(R.string.settingsDetails) { _, _ ->
        showErrorPage(accountEvent)
      }
      .create()
      .show()
  }

  @UiThread
  private fun showErrorPage(accountEvent: AccountEventCreation.AccountEventCreationFailed) {
    this.uiThread.checkIsUIThread()

    val taskSteps =
      mutableListOf<TaskStep<SettingsFragmentVersion.ExampleError>>()

    taskSteps.add(
      TaskStep(
        "Opening error page.",
        TaskStepResolution.TaskStepSucceeded("Error page successfully opened.")
      )
    )

    val parameters =
      ErrorPageParameters(
        emailAddress = this.buildConfig.errorReportEmail,
        body = "",
        subject = "[simplye-error-report]",
        attributes = accountEvent.attributes.toSortedMap(),
        taskSteps = taskSteps
      )

    this.findNavigationController().openErrorPage(parameters)
  }
}
