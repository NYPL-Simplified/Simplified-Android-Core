package org.nypl.simplified.cardcreator.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.databinding.FragmentConfirmAlternateAddressBinding
import org.nypl.simplified.cardcreator.model.AddressType
import org.nypl.simplified.cardcreator.utils.Cache
import org.slf4j.LoggerFactory

class ConfirmAlternateAddressFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(ConfirmAlternateAddressFragment::class.java)
  private val bundle by lazy { ConfirmAlternateAddressFragmentArgs.fromBundle(requireArguments()) }

  private var _binding: FragmentConfirmAlternateAddressBinding? = null
  private val binding get() = _binding!!

  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentConfirmAlternateAddressBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    navController = Navigation.findNavController(requireActivity(), R.id.card_creator_nav_host_fragment)

    val address =
      if (bundle.addressType == AddressType.SCHOOL) {
        binding.headerErrorTv.text = getString(R.string.confirm_school_address)
        Cache(requireContext()).getSchoolAddress()
      } else {
        Cache(requireContext()).getWorkAddress()
      }

    binding.addressRb.text = """
      ${address.line1}
      ${address.city}
      ${address.state} ${address.zip}
    """.trimIndent()

    nextAction = ConfirmAlternateAddressFragmentDirections.actionNext()

    // Go to next screen
    binding.nextBtn.setOnClickListener {
      logger.debug("User navigated to the next screen")
      navController.navigate(nextAction)
    }

    // Go to previous screen
    binding.prevBtn.setOnClickListener {
      navController.popBackStack()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
