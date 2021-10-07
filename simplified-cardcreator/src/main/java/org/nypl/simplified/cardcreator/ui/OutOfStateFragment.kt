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
import org.nypl.simplified.cardcreator.databinding.FragmentOutOfStateBinding
import org.nypl.simplified.cardcreator.model.AddressType
import org.slf4j.LoggerFactory

class OutOfStateFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(OutOfStateFragment::class.java)

  private var _binding: FragmentOutOfStateBinding? = null
  private val binding get() = _binding!!

  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentOutOfStateBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    navController = Navigation.findNavController(requireActivity(), R.id.card_creator_nav_host_fragment)

    binding.workRb.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        binding.nextBtn.isEnabled = true
        nextAction = OutOfStateFragmentDirections.actionNext(AddressType.WORK)
      }
    }

    binding.schoolRb.setOnCheckedChangeListener { buttonView, isChecked ->
      if (isChecked) {
        binding.nextBtn.isEnabled = true
        nextAction = OutOfStateFragmentDirections.actionNext(AddressType.SCHOOL)
      }
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
