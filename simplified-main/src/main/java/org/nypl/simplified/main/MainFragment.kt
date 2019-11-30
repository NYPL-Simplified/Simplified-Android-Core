package org.nypl.simplified.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.librarysimplified.services.api.Services
import org.nypl.simplified.navigation.api.NavigationControllerDirectoryType
import org.nypl.simplified.navigation.api.NavigationControllerType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.catalog.CatalogNavigationControllerType
import org.nypl.simplified.ui.navigation.tabs.TabbedNavigationController
import org.nypl.simplified.ui.settings.SettingsNavigationControllerType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType

class MainFragment : Fragment() {

  private lateinit var bottomNavigator: TabbedNavigationController
  private lateinit var bottomView: BottomNavigationView
  private lateinit var navigationControllerDirectory: NavigationControllerDirectoryType
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var uiThread: UIThreadServiceType

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.navigationControllerDirectory =
      NavigationControllers.findDirectory(this.requireActivity())

    val services = Services.serviceDirectory()

    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout =
      inflater.inflate(R.layout.main_tabbed_host, container, false)

    this.bottomView =
      layout.findViewById(R.id.bottomNavigator)

    /*
     * This extremely unfortunate workaround (delaying the creation of the navigator by scheduling
     * the creation on the UI thread) is necessary because the bottom navigator
     * eagerly instantiates fragments and there's nothing we can do to stop it doing so.
     * The actual issue this avoids is documented here:
     *
     * https://github.com/PandoraMedia/BottomNavigator/issues/13
     *
     * In other words, the current onStart method is currently executing in the middle of
     * a fragment transaction, and the bottom navigator will _immediately_ try to start
     * executing more transactions (leading to an exception). By deferring creation of
     * the navigator here, we avoid this issue, but this does mean that code executing
     * in the onStart() methods of fragments within the tabs will not have guaranteed
     * access to a navigation controller.
     */

    this.uiThread.runOnUIThread {
      this.bottomNavigator =
        TabbedNavigationController.create(
          activity = this.requireActivity(),
          profilesController = this.profilesController,
          fragmentContainerId = R.id.tabbedFragmentHolder,
          navigationView = this.bottomView
        )
    }

    return layout
  }

  override fun onStart() {
    super.onStart()

    val toolbar = (this.requireActivity() as ToolbarHostType).findToolbar()
    toolbar.visibility = View.VISIBLE

    this.uiThread.runOnUIThread {
      this.navigationControllerDirectory.updateNavigationController(
        CatalogNavigationControllerType::class.java, this.bottomNavigator)
      this.navigationControllerDirectory.updateNavigationController(
        SettingsNavigationControllerType::class.java, this.bottomNavigator)
      this.navigationControllerDirectory.updateNavigationController(
        NavigationControllerType::class.java, this.bottomNavigator)
    }
  }

  override fun onStop() {
    super.onStop()

    this.navigationControllerDirectory.removeNavigationController(
      CatalogNavigationControllerType::class.java)
    this.navigationControllerDirectory.removeNavigationController(
      SettingsNavigationControllerType::class.java)
    this.navigationControllerDirectory.removeNavigationController(
      NavigationControllerType::class.java)
  }
}
