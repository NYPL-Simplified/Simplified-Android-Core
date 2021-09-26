package org.nypl.simplified.cardcreator.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.nypl.simplified.cardcreator.utils.Cache
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.databinding.FragmentConfirmHomeAddressBinding
import org.slf4j.LoggerFactory

class ConfirmHomeAddressFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(ConfirmHomeAddressFragment::class.java)

  private var _binding: FragmentConfirmHomeAddressBinding? = null
  private val binding get() = _binding!!

  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections
  private val nyState = "NY"

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentConfirmHomeAddressBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    navController = Navigation.findNavController(requireActivity(), R.id.card_creator_nav_host_fragment)

    val address = Cache(requireContext()).getHomeAddress()

    binding.addressRb.text = """
      ${address.line1}
      ${address.city}
      ${address.state} ${address.zip}
    """.trimIndent()

    nextAction = if (address.state == nyState) {
      ConfirmHomeAddressFragmentDirections.actionPersonalInformation()
    } else {
      ConfirmHomeAddressFragmentDirections.actionOutOfState()
    }

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
