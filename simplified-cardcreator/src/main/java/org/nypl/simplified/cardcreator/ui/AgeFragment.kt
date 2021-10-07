package org.nypl.simplified.cardcreator.ui

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.databinding.FragmentAgeBinding
import org.nypl.simplified.cardcreator.utils.Cache
import org.nypl.simplified.cardcreator.utils.Constants
import org.slf4j.LoggerFactory

/**
 * The Age screens that gates sign up process for people under the age of 13
 */
class AgeFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(AgeFragment::class.java)

  private var _binding: FragmentAgeBinding? = null
  private val binding get() = _binding!!

  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections

  private var isOver13 = false

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentAgeBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    navController = Navigation.findNavController(requireActivity(), R.id.card_creator_nav_host_fragment)

    /**
     * User is already logged in, present with Juvenile Card Creator policy
     */
    if (Cache(requireActivity()).isJuvenileCard!!) {
      nextAction = AgeFragmentDirections.actionJuvenilePolicy()
      navController.navigate(nextAction)
    }

    binding.older13Rb.setOnCheckedChangeListener { _, _ ->
      validateForm()
    }

    binding.under13Rb.setOnCheckedChangeListener { _, _ ->
      validateForm()
    }

    binding.eulaCheckbox.setOnCheckedChangeListener { _, isChecked ->
      validateForm()
      if (isChecked) {
        logger.debug("User confirmed they are over the age of 13")
      } else {
        logger.debug("User indicated they are under the age of 13")
      }
    }

    // Go to next screen
    binding.nextBtn.setOnClickListener {
      if (isOver13) {
        logger.debug("User navigated from Age screen to Location screen")
        nextAction = AgeFragmentDirections.actionNext()
        navController.navigate(nextAction)
      } else {
        requireActivity().setResult(Activity.RESULT_CANCELED)
        requireActivity().finish()
      }
    }

    binding.eula.setOnClickListener {
      nextAction = AgeFragmentDirections.actionEula(Constants.EULA)
      navController.navigate(nextAction)
    }
  }

  /**
   * Validates required user input enabling/disabling forward navigation
   */
  private fun validateForm() {
    if (binding.older13Rb.isChecked) {
      isOver13 = true
      binding.nextBtn.text = getString(R.string.next)
      binding.error.visibility = View.INVISIBLE
      if (binding.eulaCheckbox.isChecked) {
        logger.debug("Age form valid")
        binding.nextBtn.isEnabled = true
      } else {
        binding.nextBtn.isEnabled = false
        logger.debug("Age form invalid, user has not accepted the EULA")
      }
    } else {
      isOver13 = false
      binding.error.visibility = View.VISIBLE
      binding.nextBtn.text = getString(R.string.done)
      logger.debug("Age form invalid, user is under 13")
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
