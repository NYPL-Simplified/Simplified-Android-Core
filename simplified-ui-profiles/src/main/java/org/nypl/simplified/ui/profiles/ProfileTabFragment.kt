package org.nypl.simplified.ui.profiles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import org.librarysimplified.services.api.Services
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType

/**
 * A simple profile tab that allows for logging out of a profile.
 */

class ProfileTabFragment : Fragment() {

  private lateinit var loggedInAs: TextView
  private lateinit var logOut: Button
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var uiThread: UIThreadServiceType

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

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
    val context = this.requireContext()

    val layout = inflater.inflate(R.layout.profile_tab, container, false)
    this.logOut = layout.findViewById(R.id.profileLogOut)
    this.logOut.setOnClickListener {
      this.onSelectedSwitchProfile()
    }

    this.loggedInAs = layout.findViewById(R.id.profileLoggedInAs)
    this.loggedInAs.text = context.getString(
      R.string.profileLoggedInAs, this.profilesController.profileCurrent().displayName)
    return layout
  }

  @UiThread
  private fun onSelectedSwitchProfile() {
    this.uiThread.checkIsUIThread()

    val activity = this.requireActivity()
    ProfileDialogs.createSwitchConfirmDialog(
      context = activity,
      onConfirm = {
        NavigationControllers.find(activity, ProfilesNavigationControllerType::class.java)
          .openProfileSelect()
      }
    ).show()
  }

  override fun onStart() {
    super.onStart()
    this.configureToolbar()
  }

  private fun configureToolbar() {
    val host = this.activity
    if (host is ToolbarHostType) {
      host.toolbarClearMenu()
      host.toolbarSetTitleSubtitle(
        title = this.requireContext().getString(R.string.profileTitle),
        subtitle = ""
      )
      host.toolbarUnsetArrow()
    } else {
      throw IllegalStateException("The activity ($host) hosting this fragment must implement ${ToolbarHostType::class.java}")
    }
  }
}
