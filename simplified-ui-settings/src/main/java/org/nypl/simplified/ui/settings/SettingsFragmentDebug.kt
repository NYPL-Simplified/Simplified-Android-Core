package org.nypl.simplified.ui.settings

import android.app.Activity
import android.os.Bundle
import android.text.format.Formatter
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import org.librarysimplified.services.api.Services
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.cardcreator.CardCreatorDebugging
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.api.ProfileUpdated.Succeeded
import org.slf4j.LoggerFactory

/**
 * A fragment that shows various debug options for testing app functionality at runtime.
 */

class SettingsFragmentDebug : PreferenceFragmentCompat() {

  private val logger =
    LoggerFactory.getLogger(SettingsFragmentDebug::class.java)

  private val profileViewModel: ProfileViewModel by viewModels()
  private val settingsViewModel: SettingsViewModel by viewModels {
    SettingsViewModel.getFactory(this.requireActivity().application)
  }

  private val navigationController by lazy {
    NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = SettingsNavigationControllerType::class.java
    )
  }

  private lateinit var showErrorPage: Preference
  private lateinit var sendErrorLogs: Preference
  private lateinit var sendAnalytics: Preference
  private lateinit var syncAccounts: Preference
  private lateinit var addOpdsFeed: Preference
  private lateinit var cacheLocation: Preference
  private lateinit var cacheSize: Preference
  private lateinit var showLibrarySelection: SwitchPreference
  private lateinit var showTestingLibraries: SwitchPreference
  private lateinit var locationNyc: SwitchPreference
  private lateinit var enableR2: SwitchPreference
  private lateinit var adobeAcsModule: Preference

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    this.setPreferencesFromResource(R.xml.settings_debug, rootKey)

    // Do not use the shared preference data store; instead, we're using the
    // preference views and manually handling data storage.
    this.preferenceManager.preferenceDataStore = NoOpSettingsDataStore()

    this.showErrorPage = this.findPreference("pref_key_show_error_page")!!
    this.sendErrorLogs = this.findPreference("pref_key_send_error_logs")!!
    this.sendAnalytics = this.findPreference("pref_key_send_analytics")!!
    this.syncAccounts = this.findPreference("pref_key_sync_accounts")!!
    this.addOpdsFeed = this.findPreference("pref_key_add_feed")!!
    this.cacheLocation = this.findPreference("pref_key_cache_location")!!
    this.cacheSize = this.findPreference("pref_key_cache_size")!!
    this.showLibrarySelection = this.findPreference("pref_key_library_selection")!!
    this.showTestingLibraries = this.findPreference("pref_key_testing_libraries")!!
    this.locationNyc = this.findPreference("pref_key_location_nyc")!!
    this.enableR2 = this.findPreference("pref_key_readium2")!!
    this.adobeAcsModule = this.findPreference("pref_key_adobe_acs")!!
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    /** Set initial values */

    this.locationNyc.isChecked = CardCreatorDebugging.fakeNewYorkLocation

    /** Listen for updates from our view models */

    this.profileViewModel.profileEvents.observe(this.viewLifecycleOwner, { event ->
      when (event) {
        is Succeeded -> {
          val old = event.oldDescription.preferences
          val new = event.newDescription.preferences

          // Reset the account registry if the 'showTestingLibraries'
          // preference has changed.
          if (old.showTestingLibraries != new.showTestingLibraries) {
            // this.accountRegistry.clear()
            // this.accountRegistry.refresh(
            //   includeTestingLibraries = new.showTestingLibraries
            // )
            TODO()
          }
        }
      }
    })
    this.profileViewModel.profilePreferences.observe(this.viewLifecycleOwner, { newPreferences ->
      newPreferences?.let {
        this.showLibrarySelection.isChecked = !newPreferences.hasSeenLibrarySelectionScreen
        this.showTestingLibraries.isChecked = newPreferences.showTestingLibraries
        this.enableR2.isChecked = newPreferences.useExperimentalR2
      }
    })
    this.settingsViewModel.cacheDir.observe(this.viewLifecycleOwner, { newFile ->
      this.cacheLocation.summary = newFile.absolutePath
    }
    )
    this.settingsViewModel.cacheSize.observe(this.viewLifecycleOwner, { newSize ->
      this.cacheSize.summary = Formatter.formatFileSize(requireContext(), newSize)
    })

    /** Handle user interactions */

    this.showErrorPage.setOnPreferenceClickListener { TODO() }
    this.sendErrorLogs.setOnPreferenceClickListener { TODO() }
    this.sendAnalytics.setOnPreferenceClickListener { TODO() }
    this.syncAccounts.setOnPreferenceClickListener { TODO() }
    this.addOpdsFeed.setOnPreferenceClickListener { TODO() }
    this.adobeAcsModule.setOnPreferenceClickListener { TODO() }

    this.locationNyc.setOnPreferenceClickListener {
      CardCreatorDebugging.fakeNewYorkLocation = this.locationNyc.isChecked
      true
    }
    this.showLibrarySelection.setOnPreferenceClickListener {
      this.profileViewModel.profileUpdate {
        it.copy(
          preferences = it.preferences.copy(
            hasSeenLibrarySelectionScreen = !this.showLibrarySelection.isChecked
          )
        )
      }
      true
    }
    this.showTestingLibraries.setOnPreferenceClickListener {
      this.profileViewModel.profileUpdate {
        it.copy(
          preferences = it.preferences.copy(
            showTestingLibraries = this.showTestingLibraries.isChecked
          )
        )
      }
      true
    }
    this.enableR2.setOnPreferenceClickListener {
      this.profileViewModel.profileUpdate {
        it.copy(
          preferences = it.preferences.copy(
            useExperimentalR2 = this.enableR2.isChecked
          )
        )
      }
      true
    }
  }

  override fun onStart() {
    super.onStart()
    this.configureToolbar(this.requireActivity())
  }

  private fun configureToolbar(activity: Activity) {
    this.supportActionBar?.apply {
      title = getString(R.string.settingsVersion)
      subtitle = null
    }
  }


  /*
  private var adeptExecutor: AdobeAdeptExecutorType? = null
  private var profileEventSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val services = Services.serviceDirectory()

    this.booksController =
      services.requireService(BooksControllerType::class.java)
    this.analytics =
      services.requireService(AnalyticsType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
    this.buildConfig =
      services.requireService(BuildConfigurationServiceType::class.java)
    this.adeptExecutor =
      services.optionalService(AdobeAdeptExecutorType::class.java)
  }

  override fun onStart() {
    super.onStart()
    this.configureToolbar()

    this.sendReportButton.setOnClickListener {
      Reports.sendReportsDefault(
        context = this.requireContext(),
        address = this.buildConfig.supportErrorReportEmailAddress,
        subject = "[simplye-error-report] ${this.appVersion}",
        body = ""
      )
    }

    /*
     * A button that publishes a "sync requested" event to the analytics system. Registered
     * analytics implementations are expected to respond to this event and publish any buffered
     * data that they may have to remote servers.
     */

    this.sendAnalyticsButton.setOnClickListener {
      this.analytics.publishEvent(
        AnalyticsEvent.SyncRequested(
          timestamp = LocalDateTime.now(),
          credentials = null
        )
      )
      Toast.makeText(
        this.requireContext(),
        "Triggered analytics send",
        Toast.LENGTH_SHORT
      ).show()
    }

    this.showErrorButton.setOnClickListener {
      this.showErrorPage()
    }

    this.syncAccountsButton.setOnClickListener {
      Toast.makeText(
        this.requireContext(),
        "Triggered sync of all accounts",
        Toast.LENGTH_SHORT
      ).show()

      try {
        this.profilesController.profileCurrent()
          .accounts()
          .values
          .forEach { account ->
            this.booksController.booksSync(account)
          }
      } catch (e: Exception) {
        this.logger.error("ouch: ", e)
      }
    }

    this.drmTable.removeAllViews()
    this.drmTable.addView(this.drmACSSupportRow())
    this.adobeDRMActivationTable.removeAllViews()

    this.profileEventSubscription =
      this.profilesController
        .profileEvents()
        .subscribe(this::onProfileEvent)

    /*
     * Configure the custom OPDS button.
     */

    this.customOPDS.setOnClickListener {
      this.navigationController.openSettingsCustomOPDS()
    }
  }

  data class ExampleError(
    override val message: String
  ) : PresentableErrorType

  private fun showErrorPage() {
    val attributes = sortedMapOf(
      Pair("Version", this.appVersion)
    )

    val taskSteps =
      mutableListOf<TaskStep<ExampleError>>()

    taskSteps.add(
      TaskStep(
        "Opening error page.",
        TaskStepResolution.TaskStepSucceeded("Error page successfully opened.")
      )
    )

    val parameters =
      ErrorPageParameters(
        emailAddress = this.buildConfig.supportErrorReportEmailAddress,
        body = "",
        subject = "[simplye-error-report] ${this.appVersion}",
        attributes = attributes,
        taskSteps = taskSteps
      )

    this.navigationController.openErrorPage(parameters)
  }


  private fun onProfileEvent(event: ProfileEvent) {
    if (event is ProfileUpdated) {
      this.uiThread.runOnUIThread(Runnable {
        this.showTesting.isChecked = this.profilesController
          .profileCurrent()
          .preferences()
          .showTestingLibraries
      })

      if (event is Succeeded) {
        val old = event.oldDescription.preferences
        val new = event.newDescription.preferences
        if (old.showTestingLibraries != new.showTestingLibraries) {
          this.accountRegistry.clear()
          this.accountRegistry.refresh(
            includeTestingLibraries = new.showTestingLibraries
          )
        }
      }
    }
  }

  private fun drmACSSupportRow(): TableRow {
    val row =
      this.layoutInflater.inflate(
        R.layout.settings_version_table_item, this.drmTable, false
      ) as TableRow
    val key =
      row.findViewById<TextView>(R.id.key)
    val value =
      row.findViewById<TextView>(R.id.value)

    key.text = "Adobe ACS"

    val executor = this.adeptExecutor
    if (executor == null) {
      value.setTextColor(Color.RED)
      value.text = "Unsupported"
      return row
    }

    /*
     * If we managed to get an executor, then fetch the current activations.
     */

    val adeptFuture =
      AdobeDRMExtensions.getDeviceActivations(
        executor,
        { message -> this.logger.error("DRM: {}", message) },
        { message -> this.logger.debug("DRM: {}", message) })

    adeptFuture.addListener(
      Runnable {
        this.uiThread.runOnUIThread(Runnable {
          try {
            this.onAdobeDRMReceivedActivations(adeptFuture.get())
          } catch (e: Exception) {
            this.onAdobeDRMReceivedActivationsError(e)
          }
        })
      },
      MoreExecutors.directExecutor()
    )

    value.setTextColor(Color.GREEN)
    value.text = "Supported"
    return row
  }

  private fun onAdobeDRMReceivedActivationsError(e: Exception) {
    this.logger.error("could not retrieve activations: ", e)
  }

  private fun onAdobeDRMReceivedActivations(activations: List<AdobeDRMExtensions.Activation>) {
    this.adobeDRMActivationTable.removeAllViews()

    this.run {
      val row =
        this.layoutInflater.inflate(
          R.layout.settings_drm_activation_table_item,
          this.adobeDRMActivationTable,
          false
        ) as TableRow
      val index = row.findViewById<TextView>(R.id.index)
      val vendor = row.findViewById<TextView>(R.id.vendor)
      val device = row.findViewById<TextView>(R.id.device)
      val userName = row.findViewById<TextView>(R.id.userName)
      val userId = row.findViewById<TextView>(R.id.userId)
      val expiry = row.findViewById<TextView>(R.id.expiry)

      index.text = "Index"
      vendor.text = "Vendor"
      device.text = "Device"
      userName.text = "UserName"
      userId.text = "UserID"
      expiry.text = "Expiry"

      this.adobeDRMActivationTable.addView(row)
    }

    for (activation in activations) {
      val row =
        this.layoutInflater.inflate(
          R.layout.settings_drm_activation_table_item,
          this.adobeDRMActivationTable,
          false
        ) as TableRow
      val index = row.findViewById<TextView>(R.id.index)
      val vendor = row.findViewById<TextView>(R.id.vendor)
      val device = row.findViewById<TextView>(R.id.device)
      val userName = row.findViewById<TextView>(R.id.userName)
      val userId = row.findViewById<TextView>(R.id.userId)
      val expiry = row.findViewById<TextView>(R.id.expiry)

      index.text = activation.index.toString()
      vendor.text = activation.vendor.value
      device.text = activation.device.value
      userName.text = activation.userName
      userId.text = activation.userID.value
      expiry.text = activation.expiry ?: "No expiry"

      this.adobeDRMActivationTable.addView(row)
    }
  }
  */
}
