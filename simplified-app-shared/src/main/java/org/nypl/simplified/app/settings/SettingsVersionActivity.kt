package org.nypl.simplified.app.settings

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import org.nypl.simplified.app.AdobeDRMServices
import org.nypl.simplified.app.BuildConfig
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.profiles.ProfileTimeOutActivity
import org.slf4j.LoggerFactory

class SettingsVersionActivity : ProfileTimeOutActivity() {

  private var buildClicks = 1
  private lateinit var crashButton: Button
  private lateinit var drmTable: TableLayout
  private lateinit var versionText: TextView
  private lateinit var versionTitle: TextView
  private lateinit var buildTitle: TextView
  private lateinit var buildText: TextView
  private lateinit var developerOptions: ViewGroup

  private val logger = LoggerFactory.getLogger(SettingsVersionActivity::class.java)

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)

    this.setTheme(Simplified.getCurrentTheme().themeWithActionBar)
    this.setContentView(R.layout.settings_version)

    this.developerOptions =
      this.findViewById(R.id.settings_version_dev)
    this.crashButton =
      this.developerOptions.findViewById(R.id.settings_version_dev_crash)

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

    try {
      val pkgManager = this.getPackageManager()
      val pkgInfo = pkgManager.getPackageInfo(packageName, 0)
      this.versionText.text = "${pkgInfo.versionName} (${pkgInfo.versionCode})"
    } catch (e: PackageManager.NameNotFoundException) {
      this.versionText.text = "Unavailable"
    }

    this.developerOptions.visibility = View.GONE
    this.buildText.text = BuildConfig.GIT_COMMIT

    this.drmTable.removeAllViews()
    this.drmTable.addView(drmACSSupportRow())
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
    value.text = try {
      AdobeDRMServices.newAdobeDRM(this, AdobeDRMServices.getPackageOverride(this.resources))
      value.setTextColor(ContextCompat.getColor(this, R.color.simplified_material_green_primary))
      "Supported"
    } catch (e: Throwable) {
      this.logger.debug("DRM unsupported: ", e)
      value.setTextColor(ContextCompat.getColor(this, R.color.simplified_material_red_primary))
      "Unsupported"
    }
    return row
  }
}