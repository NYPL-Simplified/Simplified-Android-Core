package org.nypl.simplified.tests.sandbox

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar

class StyledActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val prefs =
      this.getSharedPreferences("org.nypl.simplified.tests.sandbox.StyledActivity", Context.MODE_PRIVATE)
    val theme =
      prefs.getInt("theme", R.style.SimplifiedTheme_ActionBar_Blue)

    this.setTheme(theme)
    this.setContentView(R.layout.empty)

    val actionBar = this.supportActionBar
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(false)
      actionBar.setHomeButtonEnabled(true)
    }

    val layout =
      this.findViewById<ViewGroup>(R.id.empty)

    val button0 = Button(this)
    button0.text = "Enabled"
    button0.setOnClickListener {
      AlertDialog.Builder(this)
        .setMessage("Hello!")
        .setNegativeButton("Negative", { _, _ ->  })
        .setPositiveButton("Positive", { _, _ ->  })
        .create()
        .show()
    }

    val div0 = View(this)
    div0.layoutParams = LinearLayout.LayoutParams(16, 16)

    val button1 = Button(this)
    button1.text = "Disabled"
    button1.isEnabled = false

    val div1 = View(this)
    div1.layoutParams = LinearLayout.LayoutParams(16, 16)

    val button2 = Button(this)
    button2.text = "Randomize"
    button2.setOnClickListener {
      prefs.edit()
        .putInt("theme",
          listOf(
            R.style.SimplifiedTheme_ActionBar_Amber,
            R.style.SimplifiedTheme_ActionBar_Blue,
            R.style.SimplifiedTheme_ActionBar_BlueGrey,
            R.style.SimplifiedTheme_ActionBar_Brown,
            R.style.SimplifiedTheme_ActionBar_Cyan,
            R.style.SimplifiedTheme_ActionBar_DeepOrange,
            R.style.SimplifiedTheme_ActionBar_DeepPurple,
            R.style.SimplifiedTheme_ActionBar_Green,
            R.style.SimplifiedTheme_ActionBar_Grey,
            R.style.SimplifiedTheme_ActionBar_Indigo,
            R.style.SimplifiedTheme_ActionBar_LightBlue,
            R.style.SimplifiedTheme_ActionBar_Orange,
            R.style.SimplifiedTheme_ActionBar_Pink,
            R.style.SimplifiedTheme_ActionBar_Purple,
            R.style.SimplifiedTheme_ActionBar_Red,
            R.style.SimplifiedTheme_ActionBar_Teal
          ).random())
        .commit()

      val intent = Intent(this, StyledActivity::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      this.startActivity(intent)
      this.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
      this.finish()
    }

    val div2 = View(this)
    div2.layoutParams = LinearLayout.LayoutParams(16, 16)

    val progress = ProgressBar(this, null, android.R.attr.progressBarStyleLarge)
    progress.isIndeterminate = true
    progress.layoutParams = LinearLayout.LayoutParams(64, 64)

    layout.addView(button0)
    layout.addView(div0)
    layout.addView(button1)
    layout.addView(div1)
    layout.addView(button2)
    layout.addView(div2)
    layout.addView(progress)
  }

}
