package org.nypl.simplified.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment

class SplashSelectionFragment : Fragment() {

  private lateinit var parameters: SplashParameters
  private lateinit var listener: SplashListenerType
  private lateinit var selectionAlternateButton: Button
  private lateinit var selectionButton: Button
  private lateinit var selectionImageView: ImageView

  companion object {
    private const val parametersKey = "org.nypl.simplified.splash.parameters.selection"

    fun newInstance(parameters: SplashParameters): SplashSelectionFragment {
      val args = Bundle()
      args.putSerializable(this.parametersKey, parameters)
      val fragment = SplashSelectionFragment()
      fragment.arguments = args
      return fragment
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.parameters = this.arguments!!.getSerializable(parametersKey) as SplashParameters
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
    this.selectionImageView.setImageResource(this.parameters.splashImageTitleResource)

    this.selectionButton.setOnClickListener {
      this.listener.onSplashLibrarySelectionWanted()
    }
    this.selectionAlternateButton.setOnClickListener {
      this.listener.onSplashLibrarySelectionNotWanted()
    }
  }

  override fun onStop() {
    super.onStop()

    this.selectionButton.setOnClickListener(null)
    this.selectionAlternateButton.setOnClickListener(null)
  }
}
