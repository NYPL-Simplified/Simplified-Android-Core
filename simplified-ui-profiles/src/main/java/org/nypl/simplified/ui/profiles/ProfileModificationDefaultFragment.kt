package org.nypl.simplified.ui.profiles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import io.reactivex.disposables.Disposable
import org.joda.time.LocalDate
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.api.ProfileCreationEvent
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType

class ProfileModificationDefaultFragment : Fragment() {

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.profiles.ProfileModificationDefaultFragment.parameters"

    /**
     * Create a login fragment for the given parameters.
     */

    fun create(parameters: ProfileModificationFragmentParameters): ProfileModificationDefaultFragment {
      val arguments = Bundle()
      arguments.putSerializable(this.PARAMETERS_ID, parameters)
      val fragment = ProfileModificationDefaultFragment()
      fragment.arguments = arguments
      return fragment
    }
  }

  @UiThread
  private fun onNameChanged(
    text: CharSequence,
    start: Int,
    before: Int,
    count: Int
  ) {
    this.uiThread.checkIsUIThread()
    this.create.isEnabled = this.determineCreateIsOK()
  }

  private fun determineCreateIsOK(): Boolean {
    var ok = true
    val context = this.requireContext()

    this.nameField.error = null

    val nameText = this.nameField.text.trim()
    if (nameText.isBlank()) {
      this.nameField.error = context.getString(R.string.profileCreationErrorNameBlank)
      ok = false
    }

    val conflicting = this.findConflictingProfile(nameText)
    if (conflicting != null) {
      this.nameField.error = context.getString(R.string.profileCreationErrorNameAlreadyUsed)
      ok = false
    }

    return ok
  }

  private fun findConflictingProfile(name: CharSequence): ProfileReadableType? {
    return this.profilesController.profiles()
      .values
      .find { profile ->
        profile.displayName == name && profile.id != this.parameters.profileID
      }
  }

  private fun profileCreate(name: String) {
    this.profilesController.profileCreate(
      accountProvider = this.accountProviderRegistry.defaultProvider,
      displayName = name,
      gender = "",
      date = LocalDate.now()
    )
  }

  private fun profileModify(profileID: ProfileID) {

  }

  private fun onProfileEvent(event: ProfileEvent) {
    return when (event) {
      is ProfileCreationEvent.ProfileCreationFailed -> {
        this.uiThread.runOnUIThread {
          this.showProfileCreationError(event)
        }
      }
      is ProfileCreationEvent.ProfileCreationSucceeded -> {
        this.uiThread.runOnUIThread {
          NavigationControllers.find(this.requireActivity(), ProfilesNavigationControllerType::class.java)
            .popBackStack()
        }
      }
      else -> {

      }
    }
  }

  @UiThread
  private fun showProfileCreationError(event: ProfileCreationEvent.ProfileCreationFailed) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    AlertDialog.Builder(context)
      .setTitle(R.string.profileCreationError)
      .setMessage(when (event.errorCode()) {
        ProfileCreationEvent.ProfileCreationFailed.ErrorCode.ERROR_DISPLAY_NAME_ALREADY_USED ->
          R.string.profileCreationErrorNameAlreadyUsed
        null, ProfileCreationEvent.ProfileCreationFailed.ErrorCode.ERROR_GENERAL ->
          R.string.profileCreationErrorGeneral
      })
      .setIcon(R.drawable.profile_failure)
      .create()
      .show()
  }

  private lateinit var accountProviderRegistry: AccountProviderRegistryType
  private lateinit var cancel: Button
  private lateinit var create: Button
  private lateinit var nameField: EditText
  private lateinit var nameFieldListener: OnTextChangeListener
  private lateinit var parameters: ProfileModificationFragmentParameters
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var uiThread: UIThreadServiceType
  private val parametersId = PARAMETERS_ID
  private var profileSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.parameters = this.arguments!![this.parametersId] as ProfileModificationFragmentParameters

    val services =
      Services.serviceDirectory()

    this.accountProviderRegistry =
      services.requireService(AccountProviderRegistryType::class.java)
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
      inflater.inflate(R.layout.profile_modification, container, false)

    this.nameField =
      layout.findViewById(R.id.profileName)
    this.cancel =
      layout.findViewById(R.id.profileButtonCancel)
    this.create =
      layout.findViewById(R.id.profileButtonCreate)

    this.cancel.setOnClickListener {
      NavigationControllers.find(this.requireActivity(), ProfilesNavigationControllerType::class.java)
        .popBackStack()
    }

    val profileID = this.parameters.profileID
    if (profileID != null) {
      this.create.setText(R.string.profileModify)
      this.create.setOnClickListener {
        this.profileModify(profileID)
      }
    } else {
      this.create.setOnClickListener {
        this.profileCreate(this.nameField.text.trim().toString())
      }
    }

    this.nameFieldListener = OnTextChangeListener(this::onNameChanged)
    this.nameField.addTextChangedListener(this.nameFieldListener)
    return layout
  }

  override fun onStart() {
    super.onStart()

    val fragmentActivity = this.requireActivity()
    val toolbarHost = fragmentActivity as ToolbarHostType
    val toolbar = toolbarHost.findToolbar()
    toolbar.visibility = View.VISIBLE
    toolbar.setTitle(R.string.profilesTitle)
    toolbar.subtitle = ""

    toolbarHost.toolbarSetBackArrowConditionally(
      context = fragmentActivity,
      onArrowClicked = {
        NavigationControllers.find(fragmentActivity, ProfilesNavigationControllerType::class.java)
          .popBackStack()
      },
      shouldArrowBePresent = {
        true
      })

    this.profileSubscription =
      this.profilesController.profileEvents()
        .subscribe(this::onProfileEvent)

    val existingProfileId = this.parameters.profileID
    if (existingProfileId != null) {
      val existingProfile =
        this.profilesController.profiles()[existingProfileId]
      if (existingProfile != null) {
        this.nameField.setText(existingProfile.displayName)
      }
    }
  }

  override fun onStop() {
    super.onStop()

    this.profileSubscription?.dispose()
    this.create.setOnClickListener(null)
    this.cancel.setOnClickListener(null)
  }
}
