package org.nypl.simplified.cardcreator.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.databinding.FragmentHomeAddressBinding
import org.nypl.simplified.cardcreator.model.Address
import org.nypl.simplified.cardcreator.model.AddressDetails
import org.nypl.simplified.cardcreator.utils.Cache
import org.nypl.simplified.cardcreator.utils.hideKeyboard
import org.nypl.simplified.cardcreator.viewmodel.AddressViewModel
import org.slf4j.LoggerFactory

class HomeAddressFragment : Fragment(), AdapterView.OnItemSelectedListener {

  private val logger = LoggerFactory.getLogger(HomeAddressFragment::class.java)

  private var _binding: FragmentHomeAddressBinding? = null
  private val binding get() = _binding!!

  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections
  private val addressCharsMin = 5
  private val validAddress = "valid-address"
  private val alternateAddress = "alternate-addresses"
  private var dialog: AlertDialog? = null

  private val viewModel: AddressViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = FragmentHomeAddressBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    navController = Navigation.findNavController(requireActivity(), R.id.card_creator_nav_host_fragment)

    binding.spState.onItemSelectedListener = this
    ArrayAdapter.createFromResource(
      requireContext(),
      R.array.states_array,
      android.R.layout.simple_spinner_item
    ).also { adapter ->
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
      binding.spState.adapter = adapter
    }

    binding.etCity.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
        validateForm()
      }

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    binding.etStreet1.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
        validateForm()
      }

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    binding.etZip.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
        validateForm()
      }

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    // Go to next screen
    binding.nextBtn.setOnClickListener {
      validateAddress()
    }

    // Go to previous screen
    binding.prevBtn.setOnClickListener {
      navController.popBackStack()
    }
    restoreViewData()
  }

  /**
   * Validates entered address
   */
  private fun validateAddress() {
    showLoading(true)
    viewModel.validateAddressResponse.observe(
      viewLifecycleOwner,
      Observer { response ->
        showLoading(false)
        if (response.type == validAddress || response.type == alternateAddress) {
          logger.debug("Address is valid")
          Cache(requireContext()).setHomeAddress(
            AddressDetails(
              response.address.line_1,
              response.address.city,
              response.address.state,
              response.address.zip
            )
          )
          nextAction = HomeAddressFragmentDirections.actionNext()

          navController.navigate(nextAction)
        } else {
          Toast.makeText(activity, response.message, Toast.LENGTH_SHORT).show()
        }
      }
    )

    viewModel.apiError.observe(
      viewLifecycleOwner,
      Observer {
        showLoading(false)
        var error = getString(R.string.validate_address_general_error)
        if (it != null) {
          error = getString(R.string.validate_address_error, it)
        }
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setMessage(error)
          .setCancelable(false)
          .setPositiveButton(getString(R.string.try_again)) { _, _ ->
            validateAddress()
          }
          .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.cancel()
          }
        if (dialog == null) {
          dialog = dialogBuilder.create()
        }
        dialog?.show()
      }
    )
    viewModel.validateAddress(
      Address(
        AddressDetails(
          binding.etCity.text.toString(),
          binding.etStreet1.text.toString(),
          getStateAbbreviation(binding.spState.selectedItem.toString()),
          binding.etZip.text.toString()
        ),
        false
      ),
      requireActivity().intent.getStringExtra("username")!!,
      requireActivity().intent.getStringExtra("password")!!
    )
  }

  /**
   * Show loading screen
   */
  private fun showLoading(show: Boolean) {
    hideKeyboard()
    logger.debug("Toggling loading screen")
    if (show) {
      binding.loading.visibility = View.VISIBLE
      binding.form.visibility = View.GONE
      binding.navButtons.visibility = View.GONE
    } else {
      binding.loading.visibility = View.GONE
      binding.form.visibility = View.VISIBLE
      binding.navButtons.visibility = View.VISIBLE
    }
  }

  /**
   * Gets the abbreviation portion from the address state spinner
   */
  private fun getStateAbbreviation(stateListItem: String): String {
    return stateListItem.takeLast(3).dropLast(1)
  }

  /**
   * Checks form to verify that that address entered is legit
   */
  private fun validateForm() {
    binding.nextBtn.isEnabled = (
      binding.spState.selectedItem.toString() != getString(R.string.required) &&
        binding.etZip.text.length == addressCharsMin &&
        binding.etStreet1.text.length >= addressCharsMin &&
        binding.etCity.text.length >= addressCharsMin
      )
  }

  override fun onNothingSelected(parent: AdapterView<*>?) {
    binding.nextBtn.isEnabled = false
  }

  override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    logger.debug(parent!!.getItemAtPosition(position).toString())
    validateForm()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  /**
   * Restores cached data
   */
  private fun restoreViewData() {
    val homeAddress = Cache(requireContext()).getHomeAddress()
    binding.etZip.setText(homeAddress.zip, TextView.BufferType.EDITABLE)
    binding.etStreet1.setText(homeAddress.line_1, TextView.BufferType.EDITABLE)
    binding.etCity.setText(homeAddress.city, TextView.BufferType.EDITABLE)
  }

  override fun onPause() {
    super.onPause()
    dialog?.dismiss()
  }
}
