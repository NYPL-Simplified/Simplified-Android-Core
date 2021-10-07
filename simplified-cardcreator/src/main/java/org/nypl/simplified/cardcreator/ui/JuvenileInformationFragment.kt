package org.nypl.simplified.cardcreator.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.databinding.FragmentJuvenileInformationBinding
import org.nypl.simplified.cardcreator.model.PersonalInformation
import org.nypl.simplified.cardcreator.utils.Cache
import org.nypl.simplified.cardcreator.utils.hideKeyboard
import org.slf4j.LoggerFactory

class JuvenileInformationFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(JuvenileInformationFragment::class.java)

  private var _binding: FragmentJuvenileInformationBinding? = null
  private val binding get() = _binding!!

  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentJuvenileInformationBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    navController = Navigation.findNavController(requireActivity(), R.id.card_creator_nav_host_fragment)
    nextAction = PersonalInformationFragmentDirections.actionNext()

    binding.firstNameEt.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
        validateForm()
      }

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    binding.lastNameEt.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
        validateForm()
      }

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    // Go to next screen
    binding.nextBtn.setOnClickListener {
      logger.debug("User navigated to the next screen")
      var lastName = binding.lastNameEt.text.trim().toString()
      if (lastName.isEmpty()) {
        lastName = Cache(requireContext()).getPersonalInformation().lastName
      }
      Cache(requireContext()).setPersonalInformation(
        PersonalInformation(
          binding.firstNameEt.text.toString(),
          "",
          lastName,
          Cache(requireContext()).getPersonalInformation().birthDate,
          Cache(requireContext()).getPersonalInformation().email
        )
      )
      hideKeyboard()
      navController.navigate(nextAction)
    }

    // Go to previous screen
    binding.prevBtn.setOnClickListener {
      navController.popBackStack()
    }
  }

  private fun validateForm() {
    binding.nextBtn.isEnabled = binding.firstNameEt.text.trim().isNotEmpty()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
