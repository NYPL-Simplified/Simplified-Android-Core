package org.nypl.simplified.ui.profiles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import io.reactivex.disposables.Disposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.api.ProfileDeletionEvent
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.api.ProfileSelection
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType

/**
 * A fragment that displays a profile selection screen.
 */

class ProfileSelectionFragment : Fragment() {

  private lateinit var create: Button
  private lateinit var list: RecyclerView
  private lateinit var listAdapter: ProfileAdapter
  private lateinit var profiles: MutableList<ProfileReadableType>
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var uiThread: UIThreadServiceType
  private var profilesSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val services =
      Services.serviceDirectory()

    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)

    this.profiles = mutableListOf()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout =
      inflater.inflate(R.layout.profile_selection, container, false)

    this.list =
      layout.findViewById(R.id.profileList)
    this.create =
      layout.findViewById(R.id.profileCreate)
    this.create.setOnClickListener {
      onProfileCreateRequested()
    }

    this.listAdapter =
      ProfileAdapter(
        profiles = this.profiles,
        onProfileSelected = this::onProfileSelected,
        onProfileModifyRequested = this::onProfileModifyRequested,
        onProfileDeleteRequested = this::onProfileRemoveRequested
      )

    this.list.adapter = this.listAdapter
    this.list.setHasFixedSize(true)
    this.list.setItemViewCacheSize(32)
    this.list.layoutManager = LinearLayoutManager(this.context)
    (this.list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    return layout
  }

  private fun onProfileCreateRequested() {
    NavigationControllers.find(this.requireActivity(), ProfilesNavigationControllerType::class.java)
      .openProfileCreate()
  }

  private fun onProfileRemoveRequested(profile: ProfileReadableType) {
    val context = this.requireContext()
    AlertDialog.Builder(context)
      .setTitle(R.string.profileDeleteAsk)
      .setMessage(context.getString(R.string.profileDeleteAskMessage, profile.displayName))
      .setPositiveButton(R.string.profileDelete) { _, _ ->
        this.profilesController.profileDelete(profile.id)
      }
      .create()
      .show()
  }

  private fun onProfileModifyRequested(profile: ProfileReadableType) {
    NavigationControllers.find(this.requireActivity(), ProfilesNavigationControllerType::class.java)
      .openProfileModify(profile.id)
  }

  private fun onProfileSelected(profile: ProfileReadableType) {
    this.profilesController.profileSelect(profile.id)
  }

  override fun onStart() {
    super.onStart()

    val toolbarHost = this.requireActivity() as ToolbarHostType
    val toolbar = toolbarHost.findToolbar()
    toolbarHost.toolbarClearMenu()
    toolbarHost.toolbarUnsetArrow()
    toolbar.visibility = View.VISIBLE
    toolbar.setTitle(R.string.profilesTitle)
    toolbar.subtitle = ""

    this.profilesSubscription =
      this.profilesController.profileEvents()
        .subscribe(this::onProfileEvent)

    this.updateProfilesList()
  }

  private fun onProfileEvent(event: ProfileEvent) {
    when (event) {
      is ProfileSelection.ProfileSelectionCompleted ->
        this.uiThread.runOnUIThread {
          NavigationControllers.find(this.requireActivity(), ProfilesNavigationControllerType::class.java)
            .openMain()
        }
      is ProfileDeletionEvent.ProfileDeletionSucceeded ->
        this.uiThread.runOnUIThread {
          this.updateProfilesList()
        }
      is ProfileDeletionEvent.ProfileDeletionFailed ->
        this.uiThread.runOnUIThread {
          this.onProfileDeletionFailed(event)
        }
    }
  }

  @UiThread
  private fun onProfileDeletionFailed(event: ProfileDeletionEvent.ProfileDeletionFailed) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    AlertDialog.Builder(context)
      .setTitle(R.string.profileDeletionError)
      .setMessage(context.getString(R.string.profileDeletionFailedMessage, event.exception.message))
      .create()
      .show()
  }

  @UiThread
  private fun updateProfilesList() {
    this.uiThread.checkIsUIThread()

    this.profiles.clear()
    this.profiles.addAll(
      this.profilesController.profiles()
        .values
        .sortedBy(ProfileReadableType::displayName)
    )
    this.listAdapter.notifyDataSetChanged()
  }

  override fun onStop() {
    super.onStop()

    this.profilesSubscription?.dispose()
    this.create.setOnClickListener(null)
  }
}
