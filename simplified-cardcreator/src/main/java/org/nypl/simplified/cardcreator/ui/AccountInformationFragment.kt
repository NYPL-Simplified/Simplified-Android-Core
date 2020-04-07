package org.nypl.simplified.cardcreator.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import org.nypl.simplified.cardcreator.viewmodels.UsernameViewModel
import org.slf4j.LoggerFactory

class AccountInformationFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(AccountInformationFragment::class.java)

  private var _binding: FragmentAccountInformationBinding? = null
  private val binding get() = _binding!!

  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections

  private lateinit var sharedPreferences: SharedPreferences

  private val MIN_PIN_CHARS = 4
  private val USERNAME_MIN_CHARS = 5
  private val USERNAME_MAX_CHARS = 25
  private val USERNAME_AVAILABLE = "available-username"

  private val viewModel: UsernameViewModel by viewModels()

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

    navController = Navigation.findNavController(activity!!, R.id.card_creator_nav_host_fragment)

    sharedPreferences = activity!!.getSharedPreferences(Cache.DEFAULT_PREFERENCE_NAME, Context.MODE_PRIVATE)

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
      showLoading(true)
      viewModel.validateUsername(
        binding.usernameEt.text.toString(),
        activity!!.intent.extras.getString("username"),
        activity!!.intent.extras.getString("password"))
    }

    // Go to previous screen
    binding.prevBtn.setOnClickListener {
      activity!!.onBackPressed()
    }

    viewModel.validateUsernameResponse.observe(viewLifecycleOwner, Observer { response ->
      showLoading(false)
      if (response.type == USERNAME_AVAILABLE) {
        logger.debug("Username is valid")
        Cache(sharedPreferences).setAccountInformation(binding.usernameEt.text.toString(),
          binding.pinEt.text.toString())
        nextAction = AccountInformationFragmentDirections.actionNext()
        hideKeyboard()
        navController.navigate(nextAction)
      } else {
        Toast.makeText(activity, response.message, Toast.LENGTH_SHORT).show()
      }
    })
  }

  private fun validateForm() {
    binding.nextBtn.isEnabled = binding.pinEt.text.length ==
      MIN_PIN_CHARS &&
      binding.usernameEt.text.length in USERNAME_MIN_CHARS..USERNAME_MAX_CHARS
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
}
