package org.nypl.simplified.tests.sandbox

import android.os.Bundle
import android.widget.Button
import androidx.fragment.app.FragmentActivity

class TestTabbedActivity : FragmentActivity() {

  private lateinit var button: Button

  companion object {
    var on = true
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setContentView(R.layout.host)

    this.button = this.findViewById(R.id.breakButton)
    this.button.setOnClickListener {
      if (on) {
        this.supportFragmentManager.beginTransaction()
          .replace(R.id.rootFragmentHolder, YellowFragment())
          .commit()
      } else {
        this.supportFragmentManager.beginTransaction()
          .replace(R.id.rootFragmentHolder, TestFragment())
          .commit()
      }
      on = !on
    }

    if (savedInstanceState == null) {
      this.supportFragmentManager.beginTransaction()
        .replace(R.id.rootFragmentHolder, TestFragment())
        .commit()
    }
  }

}