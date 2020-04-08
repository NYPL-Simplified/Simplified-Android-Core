package org.nypl.simplified.cardcreator.ui

import android.content.Context
import android.content.SharedPreferences
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
import org.nypl.simplified.cardcreator.databinding.FragmentPersonalInformationBinding
import org.nypl.simplified.cardcreator.models.PersonalInformation
import org.nypl.simplified.cardcreator.utils.Cache
import org.nypl.simplified.cardcreator.utils.hideKeyboard
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class PersonalInformationFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(PersonalInformationFragment::class.java)

  private var _binding: FragmentPersonalInformationBinding? = null
  private val binding get() = _binding!!

  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections
  private val nameMinChars = 3

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = FragmentPersonalInformationBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    navController = Navigation.findNavController(activity!!, R.id.card_creator_nav_host_fragment)
    nextAction = PersonalInformationFragmentDirections.actionNext()
    sharedPreferences = activity!!.getSharedPreferences(Cache.DEFAULT_PREFERENCE_NAME, Context.MODE_PRIVATE)

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

    binding.emailEt.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
        validateForm()
      }

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    // Go to next screen
    binding.nextBtn.setOnClickListener {
      logger.debug("User navigated to the next screen")
      Cache(sharedPreferences).setPersonalInformation(PersonalInformation(
        binding.firstNameEt.text.toString(),
        binding.middleNameEt.text.toString(),
        binding.lastNameEt.text.toString(),
        binding.emailEt.text.toString()
      ))
      hideKeyboard()
      navController.navigate(nextAction)
    }

    // Go to previous screen
    binding.prevBtn.setOnClickListener {
      activity!!.onBackPressed()
    }
  }

  private fun validateForm() {
    binding.nextBtn.isEnabled = binding.firstNameEt.text.length > nameMinChars &&
      binding.lastNameEt.text.length > nameMinChars &&
      isValidEmailAddress()
  }

  private fun isValidEmailAddress(): Boolean {
    val p = Pattern.compile(".+@.+\\.[a-z]+")
    val m = p.matcher(binding.emailEt.text)
    return m.matches()
  }
}
