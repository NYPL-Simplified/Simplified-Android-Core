package org.nypl.simplified.cardcreator.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import org.nypl.simplified.android.ktx.viewLifecycleAware
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.databinding.FragmentAccountInformationBinding
import org.nypl.simplified.cardcreator.model.UsernameVerificationResponse
import org.nypl.simplified.cardcreator.model.UsernameVerificationResponse.UsernameVerificationError
import org.nypl.simplified.cardcreator.model.UsernameVerificationResponse.UsernameVerificationException
import org.nypl.simplified.cardcreator.model.UsernameVerificationResponse.UsernameVerificationSuccess
import org.nypl.simplified.cardcreator.utils.Cache
import org.nypl.simplified.cardcreator.utils.hideKeyboard
import org.nypl.simplified.cardcreator.viewmodel.AccountInformationViewModel
import org.slf4j.LoggerFactory

class AccountInformationFragment(
  // Needed to enable isolated fragment testing with an activityViewModel
  activityVMFactory: (() -> ViewModelProvider.Factory)? = null
) : Fragment() {

  private val logger = LoggerFactory.getLogger(AccountInformationFragment::class.java)

  private var binding by viewLifecycleAware<FragmentAccountInformationBinding>()

  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections

  private val viewModel: AccountInformationViewModel by activityViewModels(activityVMFactory)

  private var dialog: AlertDialog? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentAccountInformationBinding.inflate(inflater, container, false)
    binding.lifecycleOwner = viewLifecycleOwner
    binding.viewModel = viewModel
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    navController = findNavController()

    // Go to next screen
    binding.nextBtn.setOnClickListener {
      hideKeyboard()
      verifyUsername()
    }

    // Go to previous screen
    binding.prevBtn.setOnClickListener { navController.popBackStack() }

    viewModel.validateUsernameResponse
      .receive(viewLifecycleOwner, this::handleValidateUsernameResponse)

    restoreViewData()
  }

  private fun verifyUsername() = viewModel.verifyUsername(binding.usernameEt.text.toString())

  private fun handleValidateUsernameResponse(response: UsernameVerificationResponse) {
    when (response) {
      is UsernameVerificationSuccess -> {
        logger.debug("Username is valid")
        Cache(requireContext()).setAccountInformation(
          binding.usernameEt.text.toString(),
          binding.passwordEt.text.toString()
        )
        nextAction = AccountInformationFragmentDirections.actionNext()
        navController.navigate(nextAction)
      }
      is UsernameVerificationError -> {
        if (response.isUnavailableUsername) {
          val message = getString(R.string.unavailable_username)
          Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        } else {
          val message = getString(R.string.validate_username_error, response.status)
          showTryAgainDialog(message)
        }
      }
      is UsernameVerificationException -> {
        val message = getString(R.string.validate_username_general_error)
        showTryAgainDialog(message)
      }
    }
  }

  private fun showTryAgainDialog(message: String) {
    val dialogBuilder = AlertDialog.Builder(requireContext())
    dialogBuilder.setMessage(message)
      .setCancelable(false)
      .setPositiveButton(getString(R.string.try_again)) { _, _ ->
        verifyUsername()
      }
      .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
        dialog.cancel()
      }
    if (dialog == null) {
      dialog = dialogBuilder.create()
    }
    dialog?.show()
  }

  /**
   * Restores cached data
   */
  private fun restoreViewData() {
    val accountInformation = Cache(requireContext()).getAccountInformation()
    binding.passwordEt.setText(accountInformation.pin, TextView.BufferType.EDITABLE)
    binding.usernameEt.setText(accountInformation.username, TextView.BufferType.EDITABLE)
  }

  override fun onPause() {
    super.onPause()
    dialog?.dismiss()
  }
}
