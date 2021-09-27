package org.nypl.simplified.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners

class OnboardingStartScreenFragment : Fragment(R.layout.onboarding_start_screen) {

  private lateinit var selectionAlternateButton: Button
  private lateinit var selectionButton: Button
  private lateinit var selectionImageView: ImageView

  private val viewModel: OnboardingStartScreenViewModel by viewModels()
  private val listener: FragmentListenerType<OnboardingStartScreenEvent> by fragmentListeners()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.selectionButton = view.findViewById(R.id.selectionButton)
    this.selectionAlternateButton = view.findViewById(R.id.selectionAlternateButton)
    this.selectionImageView = view.findViewById(R.id.selectionImage)
    this.selectionImageView.setImageResource(viewModel.imageTitleResource)
  }

  override fun onStart() {
    super.onStart()

    this.selectionButton.setOnClickListener {
      this.listener.post(OnboardingStartScreenEvent.FindLibrary)
    }

    this.selectionAlternateButton.setOnClickListener {
      viewModel.setHasSeenOnboarding()
      this.listener.post(OnboardingStartScreenEvent.AddLibraryLater)
    }
  }
}
