package org.nypl.simplified.app.settings

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.google.common.util.concurrent.MoreExecutors
import org.nypl.simplified.app.BuildConfig
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.profiles.ProfileTimeOutActivity
import org.nypl.simplified.app.services.BootTesting
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.controller.AdobeDRMExtensions
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfilePreferencesChanged
import org.nypl.simplified.reports.Reports
import org.slf4j.LoggerFactory
import java.lang.StringBuilder

class SettingsVersionActivity : ProfileTimeOutActivity() {

  private var profileEventSubscription: ObservableSubscriptionType<ProfileEvent>? = null
  private var buildClicks = 1

  private lateinit var customOPDS: Button
  private lateinit var cacheButton: Button
  private lateinit var crashButton: Button
  private lateinit var sendReportButton: Button
  private lateinit var drmTable: TableLayout
  private lateinit var versionText: TextView
  private lateinit var versionTitle: TextView
  private lateinit var buildTitle: TextView
  private lateinit var buildText: TextView
  private lateinit var developerOptions: ViewGroup
  private lateinit var adobeDRMActivationTable: TableLayout
  private lateinit var showTesting: Switch
  private lateinit var failNextBoot: Switch

  private val logger = LoggerFactory.getLogger(SettingsVersionActivity::class.java)

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)

    this.setTheme(Simplified.application.services().currentTheme.themeWithActionBar)
    this.setContentView(R.layout.settings_version)

    this.developerOptions =
      this.findViewById(R.id.settings_version_dev)
    this.crashButton =
      this.developerOptions.findViewById(R.id.settings_version_dev_crash)
    this.cacheButton =
      this.developerOptions.findViewById(R.id.settings_version_dev_show_cache_dir)
    this.sendReportButton =
      this.developerOptions.findViewById(R.id.settings_version_dev_send_reports)

    this.buildTitle =
      this.findViewById(R.id.settings_version_build_title)
    this.buildText =
      this.findViewById(R.id.settings_version_build)
    this.versionTitle =
      this.findViewById(R.id.settings_version_version_title)
    this.versionText =
      this.findViewById(R.id.settings_version_version)
    this.drmTable =
      this.findViewById(R.id.settings_version_drm_support)
    this.adobeDRMActivationTable =
      this.findViewById(R.id.settings_version_drm_adobe_activations)
    this.showTesting =
      this.findViewById(R.id.settings_version_dev_production_libraries_switch)
    this.failNextBoot =
      this.findViewById(R.id.settings_version_dev_fail_next_boot_switch)
    this.customOPDS =
      this.findViewById(R.id.settings_version_dev_custom_opds)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        this.finish()
        true
      }
      else ->
        super.onOptionsItemSelected(item)
    }
  }

  override fun onStart() {
    super.onStart()

    val bar = this.supportActionBar
    if (bar != null) {
      bar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp)
      bar.setDisplayHomeAsUpEnabled(true)
      bar.setHomeButtonEnabled(false)
    }

    this.buildTitle.setOnClickListener {
      if (this.buildClicks >= 7) {
        this.developerOptions.visibility = View.VISIBLE
      }
      ++this.buildClicks
    }

    this.crashButton.setOnClickListener {
      throw OutOfMemoryError("Pretending to have run out of memory!")
    }

    this.cacheButton.setOnClickListener {
      val message = StringBuilder(128)
      message.append("Cache directory is: ")
      message.append(this.externalCacheDir)
      message.append("\n")
      message.append("\n")
      message.append("Exists: ")
      message.append(this.externalCacheDir?.isDirectory ?: false)
      message.append("\n")

      AlertDialog.Builder(this)
        .setTitle("Cache Directory")
        .setMessage(message.toString())
        .show()
    }

    try {
      val pkgManager = this.getPackageManager()
      val pkgInfo = pkgManager.getPackageInfo(packageName, 0)
      this.versionText.text = "${pkgInfo.versionName} (${pkgInfo.versionCode})"
    } catch (e: PackageManager.NameNotFoundException) {
      this.versionText.text = "Unavailable"
    }

    val email = this.resources.getString(R.string.feature_migration_report_email)
    this.sendReportButton.setOnClickListener {
      Reports.sendReportsDefault(
        context = this,
        address = email,
        subject = "[simplye-error-report] ${this.versionText.text}",
        body = "")
    }

    this.developerOptions.visibility = View.GONE
    this.buildText.text = BuildConfig.GIT_COMMIT

    this.drmTable.removeAllViews()
    this.drmTable.addView(drmACSSupportRow())
    this.adobeDRMActivationTable.removeAllViews()

    val profilesController =
      Simplified.application.services().profilesController

    this.profileEventSubscription =
      profilesController
        .profileEvents()
        .subscribe(this::onProfileEvent)

    this.showTesting.isChecked =
      profilesController
        .profileCurrent()
        .preferences()
        .showTestingLibraries()

    /*
     * Configure the "fail next boot" switch to enable/disable boot failures.
     */

    this.failNextBoot.isChecked = BootTesting.isBootFailureEnabled(this)
    this.failNextBoot.setOnClickListener {
      BootTesting.enableBootFailures(this, !BootTesting.isBootFailureEnabled(this))
    }

    /*
     * Configure the custom OPDS button.
     */

    this.customOPDS.setOnClickListener {
      val intent = Intent()
      intent.setClass(this, SettingsCustomOPDSActivity::class.java)
      this.startActivity(intent)
    }

    /*
     * Update the current profile's preferences whenever the testing switch is changed.
     */

    this.showTesting.setOnClickListener {
      profilesController
        .profilePreferencesUpdate(
          profilesController.profileCurrent()
            .preferences()
            .toBuilder()
            .setShowTestingLibraries(this.showTesting.isChecked)
            .build())
    }
  }

  override fun onStop() {
    super.onStop()
    this.profileEventSubscription?.unsubscribe()
  }

  private fun onProfileEvent(event: ProfileEvent) {
    if (event is ProfilePreferencesChanged) {
      UIThread.runOnUIThread {
        this.showTesting.isChecked =
          Simplified.application.services()
            .profilesController
            .profileCurrent()
            .preferences()
            .showTestingLibraries()
      }
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

    val executor =
      Simplified.application.services()
        .adobeExecutor

    if (executor == null) {
      value.setTextColor(ContextCompat.getColor(this, R.color.simplified_material_red_primary))
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
        UIThread.runOnUIThread {
          try {
            this.onAdobeDRMReceivedActivations(adeptFuture.get())
          } catch (e: Exception) {
            this.onAdobeDRMReceivedActivationsError(e)
          }
        }
      },
      MoreExecutors.directExecutor())

    value.setTextColor(ContextCompat.getColor(this, R.color.simplified_material_green_primary))
    value.text = "Supported"
    return row
  }

  private fun onAdobeDRMReceivedActivationsError(e: Exception) {
    this.logger.error("could not retrieve activations: ", e)
  }

  private fun onAdobeDRMReceivedActivations(activations: List<AdobeDRMExtensions.Activation>) {
    this.adobeDRMActivationTable.removeAllViews()

    run {
      val row =
        this.layoutInflater.inflate(
          R.layout.settings_drm_activation_table_item, this.adobeDRMActivationTable, false) as TableRow
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
          R.layout.settings_drm_activation_table_item, this.adobeDRMActivationTable, false) as TableRow
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
}
