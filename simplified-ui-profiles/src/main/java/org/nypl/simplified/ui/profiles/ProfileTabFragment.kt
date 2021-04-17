package org.nypl.simplified.ui.profiles

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.librarysimplified.services.api.Services
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType

/**
 * A simple profile tab that allows for logging out of a profile.
 */

class ProfileTabFragment : Fragment(R.layout.profile_tab) {

  private lateinit var loggedInAs: TextView
  private lateinit var logOut: Button
  private lateinit var profilesController: ProfilesControllerType

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val services = Services.serviceDirectory()

    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val context = this.requireContext()

    this.logOut = view.findViewById(R.id.profileLogOut)
    this.logOut.setOnClickListener {
      this.onSelectedSwitchProfile()
    }

    this.loggedInAs = view.findViewById(R.id.profileLoggedInAs)
    this.loggedInAs.text = context.getString(R.string.profileLoggedInAs, this.findDisplayName())
  }

  private fun findDisplayName(): String {
    return try {
      this.profilesController.profileCurrent().displayName
    } catch (e: Exception) {
      "..."
    }
  }

  private fun onSelectedSwitchProfile() {
    val activity = this.requireActivity()
    ProfileDialogs.createSwitchConfirmDialog(
      context = activity,
      onConfirm = {
        this.findNavigationController().openProfileSelect()
      }
    ).show()
  }

  override fun onStart() {
    super.onStart()
    this.configureToolbar(this.requireActivity())
  }

  private fun configureToolbar(activity: Activity) {
    this.supportActionBar?.apply {
      title = getString(R.string.profileTitle)
      subtitle = null
    }
  }

  private fun findNavigationController(): ProfilesNavigationControllerType {
    return NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = ProfilesNavigationControllerType::class.java
    )
  }
}
