package org.nypl.simplified.ui.profiles

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.io7m.jfunctional.None
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.profiles.api.ProfileCreationEvent
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileUpdated

class ProfileModificationDefaultFragment : Fragment(R.layout.profile_modification) {

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.profiles.ProfileModificationDefaultFragment.parameters"

    /**
     * Create a login fragment for the given parameters.
     */

    fun create(parameters: ProfileModificationFragmentParameters): ProfileModificationDefaultFragment {
      val fragment = ProfileModificationDefaultFragment()
      fragment.arguments = bundleOf(this.PARAMETERS_ID to parameters)
      return fragment
    }
  }

  private val subscriptions = CompositeDisposable()
  private val viewModel: ProfileModificationDefaultViewModel by viewModels()
  private val listener: FragmentListenerType<ProfileModificationEvent> by fragmentListeners()

  private val parameters: ProfileModificationFragmentParameters by lazy {
    this.requireArguments()[PARAMETERS_ID] as ProfileModificationFragmentParameters
  }

  private lateinit var cancel: Button
  private lateinit var create: Button
  private lateinit var nameField: EditText

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    this.nameField =
      view.findViewById(R.id.profileName)
    this.cancel =
      view.findViewById(R.id.profileButtonCancel)
    this.create =
      view.findViewById(R.id.profileButtonCreate)

    this.cancel.setOnClickListener {
      this.listener.post(ProfileModificationEvent.Cancelled)
    }

    val nameFieldListener = OnTextChangeListener(this::onNameChanged)
    this.nameField.addTextChangedListener(nameFieldListener)

    val profileID = this.parameters.profileID
    val existingProfile = profileID?.let { this.viewModel.findProfileById(profileID) }
    if (profileID != null && existingProfile != null) {
      this.nameField.setText(existingProfile.displayName)
      this.create.setText(R.string.profileModify)
      this.create.setOnClickListener {
        val nameNow = this.nameField.text.trim().toString()
        this.viewModel.renameProfile(profileID, nameNow)
      }
    } else {
      this.create.setOnClickListener {
        val name = this.nameField.text.trim().toString()
        this.viewModel.createProfile(name)
      }
    }
  }

  @Suppress("UNUSED_PARAMETER")
  private fun onNameChanged(
    text: CharSequence,
    start: Int,
    before: Int,
    count: Int
  ) {
    this.updateCreateIsPossible()
  }

  private fun updateCreateIsPossible() {
    val context = this.requireContext()
    val nameText = this.nameField.text.trim()
    val conflicting = this.isNameAlreadyTaken(nameText.toString())

    if (nameField.isFocused) {
      this.nameField.error = when {
        nameText.isBlank() -> context.getString(R.string.profileCreationErrorNameBlank)
        conflicting -> context.getString(R.string.profileCreationErrorNameAlreadyUsed)
        else -> null
      }
    }

    this.create.isEnabled = nameField.text.isNotBlank() && !conflicting
  }

  private fun isNameAlreadyTaken(name: String): Boolean {
    val profile = this.viewModel.findProfileByName(name)
    return (profile != null && profile.id != this.parameters.profileID)
  }

  override fun onStart() {
    super.onStart()
    configureToolbar(this.requireActivity())

    this.viewModel.profileEvents
      .subscribe(this::onProfileEvent)
      .let { subscriptions.add(it) }
  }

  private fun configureToolbar(activity: Activity) {
    this.supportActionBar?.apply {
      title = getString(R.string.profilesTitle)
      subtitle = null
    }
  }

  private fun onProfileEvent(event: ProfileEvent) {
    when (event) {
      is ProfileCreationEvent.ProfileCreationFailed -> {
        this.showProfileCreationError(event)
      }
      is ProfileCreationEvent.ProfileCreationSucceeded,
      is ProfileUpdated.Succeeded -> {
        this.listener.post(ProfileModificationEvent.Succeeded)
      }
      is ProfileUpdated.Failed -> {
        this.showProfileUpdateError(event)
      }
    }
  }

  private fun showProfileCreationError(event: ProfileCreationEvent.ProfileCreationFailed) {
    val context = this.requireContext()
    AlertDialog.Builder(context)
      .setTitle(R.string.profileCreationError)
      .setMessage(
        when (event.errorCode()) {
          ProfileCreationEvent.ProfileCreationFailed.ErrorCode.ERROR_DISPLAY_NAME_ALREADY_USED ->
            context.getString(R.string.profileCreationErrorNameAlreadyUsed)
          null, ProfileCreationEvent.ProfileCreationFailed.ErrorCode.ERROR_GENERAL ->
            context.getString(R.string.profileCreationErrorGeneral, someOrEmpty(event.exception()))
        }
      )
      .setIcon(R.drawable.profile_failure)
      .create()
      .show()
  }

  private fun showProfileUpdateError(event: ProfileUpdated.Failed) {
    val context = this.requireContext()
    AlertDialog.Builder(context)
      .setTitle(R.string.profileUpdateError)
      .setMessage(context.getString(R.string.profileUpdateFailedMessage, event.exception.message))
      .setIcon(R.drawable.profile_failure)
      .create()
      .show()
  }

  private fun someOrEmpty(exception: OptionType<Exception>): String {
    return when (exception) {
      is Some<Exception> -> exception.get().message ?: ""
      is None -> ""
      else -> ""
    }
  }

  override fun onStop() {
    super.onStop()
    subscriptions.clear()
    this.closeKeyboard(this.requireContext(), this.nameField.windowToken)
  }

  private fun closeKeyboard(
    context: Context,
    windowToken: IBinder
  ) {
    val manager =
      context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    manager.hideSoftInputFromWindow(windowToken, 0)
  }
}
