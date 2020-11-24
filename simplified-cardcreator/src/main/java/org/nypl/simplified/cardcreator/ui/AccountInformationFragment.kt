package org.nypl.simplified.cardcreator.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import org.nypl.simplified.cardcreator.databinding.FragmentAccountInformationBinding
import org.nypl.simplified.cardcreator.utils.Cache
import org.nypl.simplified.cardcreator.utils.hideKeyboard
import org.nypl.simplified.cardcreator.viewmodel.UsernameViewModel
import org.slf4j.LoggerFactory

class AccountInformationFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(AccountInformationFragment::class.java)

  private var _binding: FragmentAccountInformationBinding? = null
  private val binding get() = _binding!!

  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections
  private var back = false

  private val minPinChars = 4
  private val usernameMinChars = 5
  private val usernameMaxChars = 25
  private val usernameAvailable = "available-username"

  private val viewModel: UsernameViewModel by viewModels()

  private var dialog: AlertDialog? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = FragmentAccountInformationBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    arguments?.let {
      back = AccountInformationFragmentArgs.fromBundle(it).back
    }

    navController = Navigation.findNavController(requireActivity(), R.id.card_creator_nav_host_fragment)

    binding.pinEt.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
        validateForm()
      }

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    binding.usernameEt.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
        validateForm()
      }

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    // Go to next screen
    binding.nextBtn.setOnClickListener {
      hideKeyboard()
      back = false
      validateUsername()
    }

    // Go to previous screen
    binding.prevBtn.setOnClickListener {
      navController.popBackStack()
    }

    restoreViewData()
  }

  private fun validateUsername() {
    showLoading(true)
    viewModel.validateUsernameResponse.observe(
      viewLifecycleOwner,
      Observer { response ->
        if (!back) {
          showLoading(false)
          if (response.type == usernameAvailable) {
            logger.debug("Username is valid")
            Cache(requireContext()).setAccountInformation(
              binding.usernameEt.text.toString(),
              binding.pinEt.text.toString()
            )
            nextAction = AccountInformationFragmentDirections.actionNext()
            navController.navigate(nextAction)
          } else {
            Toast.makeText(activity, response.message, Toast.LENGTH_SHORT).show()
          }
        }
      }
    )

    viewModel.apiError.observe(
      viewLifecycleOwner,
      Observer {
        showLoading(false)
        var error = getString(R.string.validate_username_general_error)
        if (it != null) {
          error = getString(R.string.validate_username_error, it)
        }
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setMessage(error)
          .setCancelable(false)
          .setPositiveButton(getString(R.string.try_again)) { _, _ ->
            validateUsername()
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
    viewModel.validateUsername(
      binding.usernameEt.text.toString(),
      requireActivity().intent.getStringExtra("username")!!,
      requireActivity().intent.getStringExtra("password")!!
    )
  }

  private fun validateForm() {
    binding.nextBtn.isEnabled = binding.pinEt.text.length ==
      minPinChars &&
      binding.usernameEt.text.length in usernameMinChars..usernameMaxChars
  }

  /**
   * Show loading screen
   */
  private fun showLoading(show: Boolean) {
    logger.debug("Toggling loading screen")
    if (show) {
      binding.loading.visibility = View.VISIBLE
    } else {
      binding.loading.visibility = View.GONE
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  /**
   * Restores cached data
   */
  private fun restoreViewData() {
    val accountInformation = Cache(requireContext()).getAccountInformation()
    binding.pinEt.setText(accountInformation.pin, TextView.BufferType.EDITABLE)
    binding.usernameEt.setText(accountInformation.username, TextView.BufferType.EDITABLE)
  }

  override fun onPause() {
    super.onPause()
    dialog?.dismiss()
  }
}
