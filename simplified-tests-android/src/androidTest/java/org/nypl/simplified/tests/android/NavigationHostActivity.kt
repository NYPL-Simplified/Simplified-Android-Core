package org.nypl.simplified.tests.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.librarysimplified.services.api.ServiceDirectoryProviderType
import org.nypl.simplified.tests.MutableServiceDirectory

abstract class NavigationHostActivity<T : Fragment> : AppCompatActivity(), ServiceDirectoryProviderType {

  companion object {
    val services: MutableServiceDirectory = MutableServiceDirectory()
  }

  lateinit var currentFragment: T

  override val serviceDirectory: MutableServiceDirectory = services

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.setTheme(R.style.SimplifiedTheme_ActionBar_DeepPurple)
    this.setContentView(R.layout.navigation_host)

    this.currentFragment = this.createFragment()
    this.supportFragmentManager.beginTransaction()
      .replace(R.id.navigationHostFragmentHolder, this.currentFragment)
      .commit()
  }

  abstract fun createFragment(): T
}
