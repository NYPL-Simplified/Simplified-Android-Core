package org.nypl.simplified.ui.settings

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.disposables.Disposable
import org.librarysimplified.services.api.Services
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.simplified.adobe.extensions.AdobeDRMExtensions
import org.nypl.simplified.boot.api.BootFailureTesting
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reports.Reports
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.taskrecorder.api.TaskStepResolution
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType
import org.slf4j.LoggerFactory

/**
 * A fragment that shows the set of accounts in the current profile.
 */

class SettingsFragmentVersion : Fragment() {

  private val logger =
    LoggerFactory.getLogger(SettingsFragmentVersion::class.java)

  private lateinit var adobeDRMActivationTable: TableLayout
  private lateinit var buildConfig: BuildConfigurationServiceType
  private lateinit var buildText: TextView
  private lateinit var buildTitle: TextView
  private lateinit var cacheButton: Button
  private lateinit var crashButton: Button
  private lateinit var customOPDS: Button
  private lateinit var developerOptions: ViewGroup
  private lateinit var drmTable: TableLayout
  private lateinit var failNextBoot: Switch
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var sendReportButton: Button
  private lateinit var showErrorButton: Button
  private lateinit var showTesting: Switch
  private lateinit var toolbar: Toolbar
  private lateinit var uiThread: UIThreadServiceType
  private lateinit var versionText: TextView
  private lateinit var versionTitle: TextView
  private var adeptExecutor: AdobeAdeptExecutorType? = null
  private var buildClicks = 1
  private var profileEventSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val services = Services.serviceDirectory()

    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
    this.buildConfig =
      services.requireService(BuildConfigurationServiceType::class.java)
    this.adeptExecutor =
      services.optionalService(AdobeAdeptExecutorType::class.java)
  }
  
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout =
      inflater.inflate(R.layout.settings_version, container, false)

    this.developerOptions =
      layout.findViewById(R.id.settingsVersionDev)
    this.crashButton =
      this.developerOptions.findViewById(R.id.settingsVersionDevCrash)
    this.cacheButton =
      this.developerOptions.findViewById(R.id.settingsVersionDevShowCacheDir)
    this.sendReportButton =
      this.developerOptions.findViewById(R.id.settingsVersionDevSendReports)
    this.showErrorButton =
      this.developerOptions.findViewById(R.id.settingsVersionDevShowError)

    this.buildTitle =
      layout.findViewById(R.id.settingsVersionBuildTitle)
    this.buildText =
      layout.findViewById(R.id.settingsVersionBuild)
    this.versionTitle =
      layout.findViewById(R.id.settingsVersionVersionTitle)
    this.versionText =
      layout.findViewById(R.id.settingsVersionVersion)
    this.drmTable =
      layout.findViewById(R.id.settingsVersionDrmSupport)
    this.adobeDRMActivationTable =
      layout.findViewById(R.id.settingsVersionDrmAdobeActivations)
    this.showTesting =
      layout.findViewById(R.id.settingsVersionDevProductionLibrariesSwitch)
    this.failNextBoot =
      layout.findViewById(R.id.settingsVersionDevFailNextBootSwitch)
    this.customOPDS =
      layout.findViewById(R.id.settingsVersionDevCustomOPDS)

    return layout
  }

  override fun onStart() {
    super.onStart()

    this.configureToolbar()

    this.buildTitle.setOnClickListener {
      if (this.buildClicks >= 7) {
        this.developerOptions.visibility = View.VISIBLE
      }
      ++this.buildClicks
    }

    if (this.buildClicks >= 7) {
      this.developerOptions.visibility = View.VISIBLE
    }

    this.crashButton.setOnClickListener {
      throw OutOfMemoryError("Pretending to have run out of memory!")
    }

    this.cacheButton.setOnClickListener {
      val context = this.requireContext()
      val message = StringBuilder(128)
      message.append("Cache directory is: ")
      message.append(context.externalCacheDir)
      message.append("\n")
      message.append("\n")
      message.append("Exists: ")
      message.append(context.externalCacheDir?.isDirectory ?: false)
      message.append("\n")

      AlertDialog.Builder(context)
        .setTitle("Cache Directory")
        .setMessage(message.toString())
        .show()
    }

    try {
      val context = this.requireContext()
      val pkgManager = context.getPackageManager()
      val pkgInfo = pkgManager.getPackageInfo(context.packageName, 0)
      this.versionText.text = "${pkgInfo.versionName} (${pkgInfo.versionCode})"
    } catch (e: PackageManager.NameNotFoundException) {
      this.versionText.text = "Unavailable"
    }

    this.sendReportButton.setOnClickListener {
      Reports.sendReportsDefault(
        context = this.requireContext(),
        address = this.buildConfig.errorReportEmail,
        subject = "[simplye-error-report] ${this.versionText.text}",
        body = "")
    }

    this.showErrorButton.setOnClickListener {
      this.showErrorPage()
    }

    this.developerOptions.visibility = View.GONE
    this.buildText.text = this.buildConfig.vcsCommit

    this.drmTable.removeAllViews()
    this.drmTable.addView(this.drmACSSupportRow())
    this.adobeDRMActivationTable.removeAllViews()

    this.profileEventSubscription =
      this.profilesController
        .profileEvents()
        .subscribe(this::onProfileEvent)

    this.showTesting.isChecked =
      this.profilesController
        .profileCurrent()
        .preferences()
        .showTestingLibraries

    /*
     * Configure the "fail next boot" switch to enable/disable boot failures.
     */

    this.failNextBoot.isChecked = isBootFailureEnabled()
    this.failNextBoot.setOnCheckedChangeListener { _, checked ->
      enableBootFailures(checked)
    }

    /*
     * Configure the custom OPDS button.
     */

    this.customOPDS.setOnClickListener {
      this.findNavigationController().openSettingsCustomOPDS()
    }

    /*
     * Update the current profile's preferences whenever the testing switch is changed.
     */

    this.showTesting.setOnClickListener {
      val show = this.showTesting.isChecked
      this.profilesController.profileUpdate { description ->
        description.copy(preferences = description.preferences.copy(showTestingLibraries = show))
      }
    }
  }

  private fun configureToolbar() {
    val host = this.activity
    if (host is ToolbarHostType) {
      host.toolbarClearMenu()
      host.toolbarSetTitleSubtitle(
        title = this.requireContext().getString(R.string.settingsVersion),
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

  data class ExampleError(
    override val message: String
  ) : PresentableErrorType

  private fun showErrorPage() {
    val attributes = sortedMapOf(
      Pair("Version", "${this.versionText.text}")
    )

    val taskSteps =
      mutableListOf<TaskStep<ExampleError>>()

    taskSteps.add(
      TaskStep(
        "Opening error page.",
        TaskStepResolution.TaskStepSucceeded("Error page successfully opened.")))

    val parameters =
      ErrorPageParameters(
        emailAddress = this.buildConfig.errorReportEmail,
        body = "",
        subject = "[simplye-error-report] ${this.versionText.text}",
        attributes = attributes,
        taskSteps = taskSteps)

    this.findNavigationController().openErrorPage(parameters)
  }

  private fun enableBootFailures(enabled: Boolean) {
    BootFailureTesting.enableBootFailures(
      context = this.requireContext(),
      enabled = enabled
    )
  }

  private fun isBootFailureEnabled(): Boolean {
    return BootFailureTesting.isBootFailureEnabled(this.requireContext())
  }

  private fun onProfileEvent(event: ProfileEvent) {
    if (event is ProfileUpdated) {
      this.uiThread.runOnUIThread(Runnable {
        this.showTesting.isChecked =
          this.profilesController
            .profileCurrent()
            .preferences()
            .showTestingLibraries
      })
    }
  }

  private fun drmACSSupportRow(): TableRow {
    val row =
      this.layoutInflater.inflate(
        R.layout.settings_version_table_item, this.drmTable, false) as TableRow
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
      MoreExecutors.directExecutor())

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

  override fun onStop() {
    super.onStop()

    this.profileEventSubscription?.dispose()

    this.buildTitle.setOnClickListener(null)
    this.cacheButton.setOnClickListener(null)
    this.crashButton.setOnClickListener(null)
    this.customOPDS.setOnClickListener(null)
    this.failNextBoot.setOnCheckedChangeListener(null)
    this.sendReportButton.setOnClickListener(null)
    this.showErrorButton.setOnClickListener(null)
    this.showTesting.setOnClickListener(null)
  }

  private fun findNavigationController(): SettingsNavigationControllerType {
    return NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = SettingsNavigationControllerType::class.java
    )
  }
}
