package org.nypl.simplified.ui.settings

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.nypl.simplified.adobe.extensions.AdobeDRMExtensions
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.taskrecorder.api.TaskStepResolution
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.slf4j.LoggerFactory

/**
 * A fragment that shows various debug options for testing app functionality at runtime.
 */

class SettingsFragmentDebug : Fragment(R.layout.settings_debug) {

  private val logger = LoggerFactory.getLogger(SettingsFragmentDebug::class.java)
  private val viewModel: SettingsDebugViewModel by viewModels()

  private lateinit var adobeDRMActivationTable: TableLayout
  private lateinit var cacheButton: Button
  private lateinit var cardCreatorFakeLocation: SwitchCompat
  private lateinit var crashButton: Button
  private lateinit var crashlyticsId: TextView
  private lateinit var customOPDS: Button
  private lateinit var drmTable: TableLayout
  private lateinit var enableR2: SwitchCompat
  private lateinit var failNextBoot: SwitchCompat
  private lateinit var forgetAnnouncementsButton: Button
  private lateinit var hasSeenLibrarySelection: SwitchCompat
  private lateinit var sendAnalyticsButton: Button
  private lateinit var sendReportButton: Button
  private lateinit var showErrorButton: Button
  private lateinit var showOnlySupportedBooks: SwitchCompat
  private lateinit var showTesting: SwitchCompat
  private lateinit var syncAccountsButton: Button

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.crashButton =
      view.findViewById(R.id.settingsVersionDevCrash)
    this.cacheButton =
      view.findViewById(R.id.settingsVersionDevShowCacheDir)
    this.sendReportButton =
      view.findViewById(R.id.settingsVersionDevSendReports)
    this.showErrorButton =
      view.findViewById(R.id.settingsVersionDevShowError)
    this.sendAnalyticsButton =
      view.findViewById(R.id.settingsVersionDevSyncAnalytics)
    this.syncAccountsButton =
      view.findViewById(R.id.settingsVersionDevSyncAccounts)
    this.forgetAnnouncementsButton =
      view.findViewById(R.id.settingsVersionDevUnacknowledgeAnnouncements)
    this.drmTable =
      view.findViewById(R.id.settingsVersionDrmSupport)
    this.adobeDRMActivationTable =
      view.findViewById(R.id.settingsVersionDrmAdobeActivations)
    this.showTesting =
      view.findViewById(R.id.settingsVersionDevProductionLibrariesSwitch)
    this.failNextBoot =
      view.findViewById(R.id.settingsVersionDevFailNextBootSwitch)
    this.hasSeenLibrarySelection =
      view.findViewById(R.id.settingsVersionDevSeenLibrarySelectionScreen)
    this.cardCreatorFakeLocation =
      view.findViewById(R.id.settingsVersionDevCardCreatorLocationSwitch)
    this.enableR2 =
      view.findViewById(R.id.settingsVersionDevEnableR2Switch)
    this.showOnlySupportedBooks =
      view.findViewById(R.id.settingsVersionDevShowOnlySupported)
    this.customOPDS =
      view.findViewById(R.id.settingsVersionDevCustomOPDS)
    this.crashlyticsId =
      view.findViewById(R.id.settingsVersionCrashlyticsID)

    this.drmTable.addView(
      this.createDrmSupportRow("Adobe Acs", this.viewModel.adeptSupported)
    )
    this.drmTable.addView(
      this.createDrmSupportRow("AxisNow", this.viewModel.axisNowSupported)
    )

    this.viewModel.adeptActivations.observe(this.viewLifecycleOwner) { activations ->
      if (activations.isNotEmpty()) {
        this.onAdobeDRMReceivedActivations(activations)
      }
    }

    this.showTesting.isChecked =
      this.viewModel.showTestingLibraries
    this.failNextBoot.isChecked =
      this.viewModel.isBootFailureEnabled
    this.hasSeenLibrarySelection.isChecked =
      this.viewModel.hasSeenLibrarySelection
    this.cardCreatorFakeLocation.isChecked =
      this.viewModel.cardCreatorFakeLocation
    this.enableR2.isChecked =
      this.viewModel.useExperimentalR2
    this.showOnlySupportedBooks.isChecked =
      this.viewModel.showOnlySupportedBooks
    this.crashlyticsId.text =
      this.viewModel.crashlyticsId
  }

  override fun onStart() {
    super.onStart()
    this.configureToolbar(this.requireActivity())

    this.crashButton.setOnClickListener {
      throw OutOfMemoryError("Pretending to have run out of memory!")
    }

    this.cacheButton.setOnClickListener {
      this.showCacheAlert()
    }

    this.sendReportButton.setOnClickListener {
      this.viewModel.sendErrorLogs()
    }

    this.showErrorButton.setOnClickListener {
      this.showErrorPage()
    }

    /*
     * A button that publishes a "sync requested" event to the analytics system. Registered
     * analytics implementations are expected to respond to this event and publish any buffered
     * data that they may have to remote servers.
     */

    this.sendAnalyticsButton.setOnClickListener {
      this.viewModel.sendAnalytics()
      Toast.makeText(
        this.requireContext(),
        "Triggered analytics send",
        Toast.LENGTH_SHORT
      ).show()
    }

    this.syncAccountsButton.setOnClickListener {
      this.viewModel.syncAccounts()
      Toast.makeText(
        this.requireContext(),
        "Triggered sync of all accounts",
        Toast.LENGTH_SHORT
      ).show()
    }

    /*
     * Forget announcements when the button is clicked.
     */

    this.forgetAnnouncementsButton.setOnClickListener {
      this.viewModel.forgetAllAnnouncements()
    }

    /*
    * Update the current profile's preferences whenever the testing switch is changed.
    */

    this.showTesting.setOnCheckedChangeListener { _, checked ->
      this.viewModel.showTestingLibraries = checked
    }

    /*
     * Configure the "fail next boot" switch to enable/disable boot failures.
     */

    this.failNextBoot.setOnCheckedChangeListener { _, checked ->
      this.viewModel.isBootFailureEnabled = checked
    }

    /*
     * Configure the "has seen library selection" switch
     */

    this.hasSeenLibrarySelection.setOnCheckedChangeListener { _, checked ->
      this.viewModel.hasSeenLibrarySelection = checked
    }

    this.cardCreatorFakeLocation.setOnCheckedChangeListener { _, checked ->
      this.viewModel.cardCreatorFakeLocation = checked
    }

    /*
    * Update the current profile's preferences whenever the R2 switch is changed.
    */

    this.enableR2.setOnCheckedChangeListener { _, changed ->
      this.viewModel.useExperimentalR2 = changed
    }

    /*
     * Update the feed loader when filtering options are changed.
     */

    this.showOnlySupportedBooks.setOnCheckedChangeListener { _, isChecked ->
      this.viewModel.showOnlySupportedBooks = isChecked
    }

    /*
     * Configure the custom OPDS button.
     */

    this.customOPDS.setOnClickListener {
      this.findNavigationController().openSettingsCustomOPDS()
    }
  }

  private fun configureToolbar(activity: Activity) {
    this.supportActionBar?.apply {
      title = getString(R.string.settingsVersion)
      subtitle = null
    }
  }

  private fun showErrorPage() {
    val attributes = sortedMapOf(
      Pair("Version", this.viewModel.appVersion)
    )

    val taskSteps =
      mutableListOf<TaskStep>()

    taskSteps.add(
      TaskStep(
        "Opening error page.",
        TaskStepResolution.TaskStepSucceeded("Error page successfully opened.")
      )
    )

    val parameters =
      ErrorPageParameters(
        emailAddress = this.viewModel.supportEmailAddress,
        body = "",
        subject = "[simplye-error-report] ${this.viewModel.appVersion}",
        attributes = attributes,
        taskSteps = taskSteps
      )

    this.findNavigationController().openErrorPage(parameters)
  }

  private fun showCacheAlert() {
    val context = this.requireContext()
    val message = StringBuilder(128)
    message.append("Cache directory is: ")
    message.append(context.cacheDir)
    message.append("\n")
    message.append("\n")
    message.append("Exists: ")
    message.append(context.cacheDir?.isDirectory ?: false)
    message.append("\n")

    AlertDialog.Builder(context)
      .setTitle("Cache Directory")
      .setMessage(message.toString())
      .show()
  }

  private fun createDrmSupportRow(name: String, isSupported: Boolean): TableRow {
    val row =
      this.layoutInflater.inflate(
        R.layout.settings_version_table_item, this.drmTable, false
      ) as TableRow
    val key =
      row.findViewById<TextView>(R.id.key)
    val value =
      row.findViewById<TextView>(R.id.value)

    key.text = name

    if (isSupported) {
      value.setTextColor(Color.GREEN)
      value.text = "Supported"
    } else {
      value.setTextColor(Color.RED)
      value.text = "Unsupported"
    }

    return row
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

  private fun findNavigationController(): SettingsNavigationControllerType {
    return NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = SettingsNavigationControllerType::class.java
    )
  }
}
