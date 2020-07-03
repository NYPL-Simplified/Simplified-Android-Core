package org.nypl.simplified.cardcreator.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView.BufferType
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.databinding.FragmentJuvenilePolicyBinding
import org.nypl.simplified.cardcreator.utils.Constants
import org.nypl.simplified.cardcreator.utils.getCache
import org.nypl.simplified.cardcreator.viewmodel.PlatformViewModel
import org.nypl.simplified.cardcreator.viewmodel.TokenViewModel
import org.slf4j.LoggerFactory

/**
 * Screen the presents regulations surrounding creating juvenile cards
 */
class JuvenilePolicyFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(JuvenilePolicyFragment::class.java)

  private var _binding: FragmentJuvenilePolicyBinding? = null
  private val binding get() = _binding!!

  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections

  private val viewModel: TokenViewModel by viewModels()
  private val platformViewModel: PlatformViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = FragmentJuvenilePolicyBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    navController = Navigation.findNavController(requireActivity(), R.id.card_creator_nav_host_fragment)

    binding.policyCheckbox.setOnCheckedChangeListener { _, _ ->
      validateForm()
    }

    binding.eulaCheckbox.setOnCheckedChangeListener { _, _ ->
      validateForm()
    }

    val agreement = SpannableString(getString(R.string.legal_disclaimer))

    val eulaLink: ClickableSpan = object : ClickableSpan() {
      override fun onClick(textView: View) {
        nextAction = JuvenilePolicyFragmentDirections.actionEula(Constants.EULA)
        navController.navigate(nextAction)
      }
    }

    val legalLink: ClickableSpan = object : ClickableSpan() {
      override fun onClick(textView: View) {
        nextAction = JuvenilePolicyFragmentDirections.actionEula(Constants.LEGAL_DISCLAIMER)
        navController.navigate(nextAction)
      }
    }

    agreement.setSpan(eulaLink, 29, 55, 0)
    agreement.setSpan(legalLink, 64, 80, 0)
    agreement.setSpan(ForegroundColorSpan(Color.BLUE), 29, 55, 0)
    agreement.setSpan(ForegroundColorSpan(Color.BLUE), 64, 80, 0)

    binding.eula.movementMethod = LinkMovementMethod.getInstance()
    binding.eula.setText(agreement, BufferType.SPANNABLE)
    binding.eula.isSelected = true

    // Go to next screen
    binding.nextBtn.setOnClickListener {
      if (validateForm()) {
        binding.progress.visibility = View.VISIBLE
        getToken()
      } else {
        requireActivity().setResult(Activity.RESULT_CANCELED)
        requireActivity().finish()
      }
    }

    viewModel.issoTokenData.observe(viewLifecycleOwner, Observer {
      getCache().token = it.access_token
      getEligibility()
    })

    platformViewModel.dependentEligibilityData.observe(viewLifecycleOwner, Observer {
      if (it.eligible) {
        nextAction = JuvenilePolicyFragmentDirections.actionLocation()
        navController.navigate(nextAction)
      } else {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setMessage(it.description)
          .setCancelable(false)
          .setNegativeButton(getString(R.string.cancel)) { _, _ ->
            requireActivity().setResult(Activity.RESULT_CANCELED)
            requireActivity().finish()
          }
        val alert = dialogBuilder.create()
        alert.show()
      }
    })

    platformViewModel.apiError.observe(viewLifecycleOwner, Observer {
      binding.progress.visibility = View.GONE
      val dialogBuilder = AlertDialog.Builder(requireContext())
      dialogBuilder.setMessage("$it Error")
        .setCancelable(false)
        .setPositiveButton(getString(R.string.try_again)) { _, _ ->
          getToken()
        }
        .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
          dialog.cancel()
        }
      val alert = dialogBuilder.create()
      alert.show()
    })

    viewModel.apiError.observe(viewLifecycleOwner, Observer {
      binding.progress.visibility = View.GONE
      val dialogBuilder = AlertDialog.Builder(requireContext())
      dialogBuilder.setMessage("$it Error")
        .setCancelable(false)
        .setPositiveButton(getString(R.string.try_again)) { _, _ ->
          getToken()
        }
        .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
          dialog.cancel()
        }
      val alert = dialogBuilder.create()
      alert.show()
    })
  }

  /**
   * Check to see if the user is eligible to create child cards
   */
  private fun getEligibility() {
    platformViewModel.getDependentEligibility(
      requireActivity().intent.extras.getString("userIdentifier"),
      getCache().token.toString()
    )
  }

  /**
   * Get token needed for platform API calls
   */
  private fun getToken() {
    binding.progress.visibility = View.VISIBLE
    viewModel.getToken(
      requireActivity().intent.extras.getString("clientId"),
      requireActivity().intent.extras.getString("clientSecret"))
  }

  /**
   * Validates required user input enabling/disabling forward navigation
   */
  private fun validateForm(): Boolean {
    return if (binding.eulaCheckbox.isChecked && binding.policyCheckbox.isChecked) {
      binding.nextBtn.text = getString(R.string.next); true
    } else {
      binding.nextBtn.text = getString(R.string.cancel); false
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
