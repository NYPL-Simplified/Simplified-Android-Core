package org.nypl.simplified.cardcreator.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import org.joda.time.format.DateTimeFormat
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.databinding.FragmentPersonalInformationBinding
import org.nypl.simplified.cardcreator.model.PersonalInformation
import org.nypl.simplified.cardcreator.utils.Cache
import org.nypl.simplified.cardcreator.utils.hideKeyboard
import org.slf4j.LoggerFactory
import java.util.Calendar
import java.util.regex.Pattern

class PersonalInformationFragment : Fragment(), DatePickerDialog.OnDateSetListener {

  private val logger = LoggerFactory.getLogger(PersonalInformationFragment::class.java)

  private var _binding: FragmentPersonalInformationBinding? = null
  private val binding get() = _binding!!

  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections

  private var birthDate: String? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentPersonalInformationBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    navController = Navigation.findNavController(requireActivity(), R.id.card_creator_nav_host_fragment)
    nextAction = PersonalInformationFragmentDirections.actionNext()
    birthDate = Cache(requireContext()).getPersonalInformation().birthDate

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
      if (isOver13()) {
        logger.debug("User navigated to the next screen")
        Cache(requireContext()).setPersonalInformation(
          PersonalInformation(
            binding.firstNameEt.text.toString(),
            binding.middleNameEt.text.toString(),
            binding.lastNameEt.text.toString(),
            birthDate!!,
            binding.emailEt.text.toString()
          )
        )
        hideKeyboard()
        navController.navigate(nextAction)
      }
    }

    // Go to previous screen
    binding.prevBtn.setOnClickListener {
      navController.popBackStack()
    }

    // Either label or EditText can launch birthday picker
    binding.birthDateEt.setOnClickListener { showDatePickerDialog() }
    binding.birthDateLabel.setOnClickListener { showDatePickerDialog() }

    restoreViewData()
  }

  private fun validateForm() {
    binding.nextBtn.isEnabled =
      binding.firstNameEt.text.trim().isNotEmpty() &&
      binding.lastNameEt.text.trim().isNotEmpty() &&
      isValidEmailAddress()
  }

  private fun isValidEmailAddress(): Boolean {
    val p = Pattern.compile(".+@.+\\.[a-z]+")
    val m = p.matcher(binding.emailEt.text)
    return m.matches()
  }

  /**
   * Check to see if date is over 13 years
   */
  private fun isOver13(): Boolean {
    return if (!birthDate.isNullOrEmpty()) {
      val formatter = DateTimeFormat.forPattern("MM/dd/yy")
      val birthDateObj = formatter.parseDateTime(birthDate)
      if (birthDateObj.plusYears(13).isAfterNow) {
        Toast.makeText(activity, getString(R.string.under_13_warning), Toast.LENGTH_SHORT).show()
        false
      } else {
        true
      }
    } else {
      Toast.makeText(activity, getString(R.string.enter_birth_date_warning), Toast.LENGTH_SHORT).show()
      false
    }
  }

  /**
   * Shows date picker spinner
   */
  private fun showDatePickerDialog() {
    val c = Calendar.getInstance()
    val year = c.get(Calendar.YEAR) - 13
    val month = c.get(Calendar.MONTH)
    val day = c.get(Calendar.DAY_OF_MONTH)
    val datePickerSpinner = DatePickerDialog(
      requireContext(),
      R.style.DatePickerSpinnerDialog,
      this,
      year,
      month,
      day
    )
    datePickerSpinner.show()
  }

  override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
    birthDate = "${month + 1}/$dayOfMonth/$year"
    binding.birthDateEt.setText(birthDate, TextView.BufferType.EDITABLE)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  /**
   * Restores cached data
   */
  private fun restoreViewData() {
    val personalInformation = Cache(requireContext()).getPersonalInformation()
    binding.firstNameEt.setText(personalInformation.firstName, TextView.BufferType.EDITABLE)
    binding.middleNameEt.setText(personalInformation.middleName, TextView.BufferType.EDITABLE)
    binding.lastNameEt.setText(personalInformation.lastName, TextView.BufferType.EDITABLE)
    binding.emailEt.setText(personalInformation.email, TextView.BufferType.EDITABLE)
    binding.birthDateEt.setText(personalInformation.birthDate, TextView.BufferType.EDITABLE)
  }
}
