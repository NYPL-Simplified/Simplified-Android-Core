package org.nypl.simplified.ui.announcements

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import org.librarysimplified.services.api.Services

class AnnouncementsDialog : DialogFragment(R.layout.announcements_dialog) {

  private val services = Services.serviceDirectory()

  private val viewModel: AnnouncementsViewModel by viewModels(
    factoryProducer = { AnnouncementsViewModelFactory(services) }
  )

  private lateinit var title: TextView
  private lateinit var content: TextView
  private lateinit var okButton: Button

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (this.viewModel.currentAnnouncement.value == null) {
      this.dismiss()
    }

    this.viewModel.currentAnnouncement.observe(this) { announcement ->
      if (announcement == null) {
        this.dismiss()
      } else {
        this.reconfigureUI(announcement)
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    this.title = view.findViewById(R.id.announcements_title)
    this.okButton = view.findViewById(R.id.announcements_ok)
    this.content = view.findViewById(R.id.announcements_content)
  }

  override fun onStart() {
    super.onStart()
    dialog?.window?.setLayout(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.WRAP_CONTENT
    )
    this.okButton.setOnClickListener {
      this.viewModel.acknowledgeCurrentAnnouncement()
    }
  }

  private fun reconfigureUI(announcementIndex: Int) {
    val title =
      requireContext().getString(
        R.string.announcementTitle,
        this.viewModel.account.provider.displayName,
        announcementIndex + 1,
        this.viewModel.announcements.size
      )
    val announcement =
      this.viewModel.announcements[announcementIndex].content

    this.title.text = title
    this.content.text = announcement
  }
}
