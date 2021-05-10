package org.nypl.simplified.ui.profiles

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.profiles.api.ProfileCreationEvent
import org.nypl.simplified.profiles.api.ProfileDeletionEvent
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.api.ProfileSelection
import org.nypl.simplified.profiles.api.ProfileUpdated

/**
 * A fragment that displays a profile selection screen.
 */

class ProfileSelectionFragment : Fragment(R.layout.profile_selection) {

  private val subscriptions = CompositeDisposable()
  private val viewModel: ProfileSelectionViewModel by viewModels()
  private val listener: FragmentListenerType<ProfileSelectionEvent> by fragmentListeners()

  private lateinit var create: Button
  private lateinit var list: RecyclerView
  private lateinit var listAdapter: ProfileAdapter

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.list =
      view.findViewById(R.id.profileList)
    this.create =
      view.findViewById(R.id.profileCreate)
    this.create.setOnClickListener {
      onProfileCreateRequested()
    }

    this.listAdapter =
      ProfileAdapter(
        onProfileSelected = this::onProfileSelected,
        onProfileModifyRequested = this::onProfileModifyRequested,
        onProfileDeleteRequested = this::onProfileRemoveRequested
      )

    this.updateProfileList()

    with(this.list) {
      adapter = this@ProfileSelectionFragment.listAdapter
      setHasFixedSize(true)
      setItemViewCacheSize(32)
      layoutManager = LinearLayoutManager(this.context)
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    }
  }

  private fun onProfileCreateRequested() {
    this.listener.post(ProfileSelectionEvent.OpenProfileCreation)
  }

  private fun onProfileRemoveRequested(profile: ProfileReadableType) {
    val context = this.requireContext()
    AlertDialog.Builder(context)
      .setTitle(R.string.profileDeleteAsk)
      .setMessage(context.getString(R.string.profileDeleteAskMessage, profile.displayName))
      .setPositiveButton(R.string.profileDelete) { _, _ ->
        this.viewModel.deleteProfile(profile.id)
      }
      .create()
      .show()
  }

  private fun onProfileModifyRequested(profile: ProfileReadableType) {
    this.listener.post(ProfileSelectionEvent.OpenProfileModification(profile.id))
  }

  private fun onProfileSelected(profile: ProfileReadableType) {
    this.viewModel.selectProfile(profile.id)
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

  private fun updateProfileList() {
    val profiles = this.viewModel.profiles.sortedBy(ProfileReadableType::displayName)
    this.listAdapter.submitList(profiles)
  }

  private fun onProfileEvent(event: ProfileEvent) {
    when (event) {
      is ProfileSelection.ProfileSelectionCompleted ->
        this.listener.post(ProfileSelectionEvent.ProfileSelected)
      is ProfileDeletionEvent.ProfileDeletionFailed ->
        this.onProfileDeletionFailed(event)
      is ProfileCreationEvent.ProfileCreationSucceeded,
      is ProfileUpdated.Succeeded,
      is ProfileDeletionEvent.ProfileDeletionSucceeded -> {
        this.updateProfileList()
      }
    }
  }

  private fun onProfileDeletionFailed(event: ProfileDeletionEvent.ProfileDeletionFailed) {
    val context = this.requireContext()
    AlertDialog.Builder(context)
      .setTitle(R.string.profileDeletionError)
      .setMessage(context.getString(R.string.profileDeletionFailedMessage, event.exception.message))
      .create()
      .show()
  }

  override fun onStop() {
    super.onStop()
    this.subscriptions.clear()
  }
}
