package org.nypl.simplified.app.settings

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.profiles.ProfileTimeOutActivity

class SettingsVersionActivity : ProfileTimeOutActivity() {

  companion object {
    const val developerKey = "org.nypl.simplified.app.settings.SettingsVersionActivity.developer"
  }

  private var buildClicks = 0
  private lateinit var crashButton: Button
  private lateinit var versionText: TextView
  private lateinit var versionTitle: TextView
  private lateinit var buildTitle: TextView
  private lateinit var buildText: TextView
  private lateinit var developerOptions: ViewGroup

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

    this.buildTitle.setOnClickListener {
      if (this.buildClicks >= 7) {
        this.developerOptions.visibility = View.VISIBLE
      }
      ++this.buildClicks
    }

    this.crashButton.setOnClickListener {
      throw OutOfMemoryError("Pretending to have run out of memory!")
    }
  }

  override fun onStart() {
    super.onStart()

    this.developerOptions.visibility = View.GONE
  }
}