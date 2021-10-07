package org.nypl.simplified.cardcreator.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.nypl.simplified.cardcreator.databinding.FragmentEulaBinding
import org.nypl.simplified.webview.WebViewUtilities

/**
 * Screen for viewing the End User License Agreement
 */
class EULAFragment : Fragment() {

  private var _binding: FragmentEulaBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentEulaBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    WebViewUtilities.setForcedDark(binding.webview.settings, resources.configuration)

    val url = EULAFragmentArgs.fromBundle(requireArguments()).url
    binding.webview.loadUrl(url)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
