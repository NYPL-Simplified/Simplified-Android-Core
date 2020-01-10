package org.nypl.simplified.tests.sandbox

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentActivity
import org.librarysimplified.services.api.ServiceDirectory
import org.librarysimplified.services.api.ServiceDirectoryType
import org.librarysimplified.services.api.Services
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.tests.MockProfilesController
import org.nypl.simplified.ui.profiles.ProfileModificationDefaultFragment
import org.nypl.simplified.ui.profiles.ProfileModificationFragmentParameters
import org.nypl.simplified.ui.profiles.ProfileSelectionFragment
import org.nypl.simplified.ui.profiles.ProfilesNavigationControllerType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType

class ProfileActivity : FragmentActivity(), ToolbarHostType {

  private lateinit var toolbar: Toolbar

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setContentView(R.layout.profile_host)

    this.toolbar = this.findViewById(R.id.toolbar)
    this.toolbar.title = "Profiles"

    if (savedInstanceState == null) {
      Services.initialize(createServices())

      NavigationControllers.findDirectory(this)
        .updateNavigationController(
          ProfilesNavigationControllerType::class.java,
          object : ProfilesNavigationControllerType {
            override fun openProfileSelect() {
            }

            override fun openMain() {
            }

            override fun openProfileModify(id: ProfileID) {
              val parameters = ProfileModificationFragmentParameters(id)

              supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentHolder, ProfileModificationDefaultFragment.create(parameters))
                .addToBackStack(null)
                .commit()
            }

            override fun openProfileCreate() {
              val parameters = ProfileModificationFragmentParameters(null)

              supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentHolder, ProfileModificationDefaultFragment.create(parameters))
                .addToBackStack(null)
                .commit()
            }

            override fun popBackStack(): Boolean {
              supportFragmentManager.popBackStack()
              return true
            }

            override fun backStackSize(): Int {
              return 1
            }
          })

      this.supportFragmentManager.beginTransaction()
        .replace(R.id.fragmentHolder, ProfileSelectionFragment())
        .commit()
    }
  }

  private fun createServices(): ServiceDirectoryType {
    val services = ServiceDirectory.builder()
    services.addService(UIThreadServiceType::class.java, object : UIThreadServiceType {})
    services.addService(ProfilesControllerType::class.java, MockProfilesController(16, 3))
    return services.build()
  }

  override fun findToolbar(): Toolbar {
    return this.toolbar
  }
}
