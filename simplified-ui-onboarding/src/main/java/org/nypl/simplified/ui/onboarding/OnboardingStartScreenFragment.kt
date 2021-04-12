package org.nypl.simplified.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.nypl.simplified.navigation.api.NavigationControllers

class OnboardingStartScreenFragment : Fragment(R.layout.onboarding_start_screen) {

  private lateinit var selectionAlternateButton: Button
  private lateinit var selectionButton: Button
  private lateinit var selectionImageView: ImageView

  private val viewModel: OnboardingStartScreenViewModel by viewModels()

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
      NavigationControllers
        .find(requireActivity(), OnboardingNavigationController::class.java)
        .openSettingsAccountRegistry()
    }

    this.selectionAlternateButton.setOnClickListener {
      viewModel.setHasSeenOnboarding()
      parentFragmentManager.setFragmentResult("", Bundle())
    }
  }
}
