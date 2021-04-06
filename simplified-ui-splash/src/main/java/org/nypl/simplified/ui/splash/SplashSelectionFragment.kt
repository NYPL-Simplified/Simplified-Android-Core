package org.nypl.simplified.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.nypl.simplified.ui.branding.BrandingSplashServiceType
import java.util.*

class SplashSelectionFragment : Fragment() {

  private lateinit var listener: SplashListenerType
  private lateinit var selectionAlternateButton: Button
  private lateinit var selectionButton: Button
  private lateinit var selectionImageView: ImageView
  private lateinit var viewModel: SplashSelectionFragmentViewModel

  companion object {
    private const val parametersKey = "org.nypl.simplified.splash.parameters.selection"

    fun newInstance(): SplashSelectionFragment {
      val args = Bundle()
      val fragment = SplashSelectionFragment()
      fragment.arguments = args
      return fragment
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.viewModel =
      ViewModelProvider(this)
        .get(SplashSelectionFragmentViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val main =
      inflater.inflate(R.layout.splash_selection, container, false) as ViewGroup
    this.selectionButton =
      main.findViewById(R.id.selectionButton)
    this.selectionAlternateButton =
      main.findViewById(R.id.selectionAlternateButton)
    this.selectionImageView =
      main.findViewById(R.id.selectionImage)

    return main
  }

  override fun onStart() {
    super.onStart()

    this.listener = this.activity as SplashListenerType
    val brandingService = getBrandingService()
    this.selectionImageView.setImageResource(brandingService.splashImageTitleResource())

    this.selectionButton.setOnClickListener {
      this.listener.onSplashLibrarySelectionWanted()
    }
    this.selectionAlternateButton.setOnClickListener {

      /*
       * Store the fact that we've seen the selection screen.
       */

      viewModel.profilesController.profileUpdate { profileDescription ->
        profileDescription.copy(
          preferences = profileDescription.preferences.copy(hasSeenLibrarySelectionScreen = true)
        )
      }

      this.listener.onSplashLibrarySelectionNotWanted()
    }
  }

  override fun onStop() {
    super.onStop()

    this.selectionButton.setOnClickListener(null)
    this.selectionAlternateButton.setOnClickListener(null)
  }

  private fun getBrandingService(): BrandingSplashServiceType {
    return ServiceLoader
      .load(BrandingSplashServiceType::class.java)
      .firstOrNull()
      ?: throw IllegalStateException(
        "No available services of type ${BrandingSplashServiceType::class.java.canonicalName}"
      )
  }
}
