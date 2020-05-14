package org.nypl.simplified.cardcreator.ui

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.databinding.FragmentReviewBinding
import org.nypl.simplified.cardcreator.models.Patron
import org.nypl.simplified.cardcreator.utils.Cache
import org.nypl.simplified.cardcreator.viewmodels.PatronViewModel
import org.slf4j.LoggerFactory

class ReviewFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(ReviewFragment::class.java)

  private var _binding: FragmentReviewBinding? = null
  private val binding get() = _binding!!

  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections

  private lateinit var cache: Cache

  private val nyState = "NY"
  private val cardGranted = "card-granted"
  private val policyTypeDefault = "web_applicant"

  private val viewModel: PatronViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = FragmentReviewBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    navController = Navigation.findNavController(requireActivity(), R.id.card_creator_nav_host_fragment)
    cache = Cache(requireContext())

    setReviewData()

    // Go to next screen
    binding.nextBtn.setOnClickListener {
      createPatron()
    }

    // Go to previous screen
    binding.prevBtn.setOnClickListener {
      navController.popBackStack()
    }

    viewModel.createPatronResponse.observe(viewLifecycleOwner, Observer { response ->
      showLoading(false)
      Toast.makeText(activity, response.message, Toast.LENGTH_SHORT).show()
      if (response.type == cardGranted) {
        logger.debug("User navigated to the next screen")
        logger.debug("Card granted")
        nextAction = ReviewFragmentDirections.actionNext(
          response.username,
          response.barcode,
          response.pin,
          response.type,
          response.temporary,
          response.message,
          "${cache.getPersonalInformation().firstName} ${cache.getPersonalInformation().lastName}")
        navController.navigate(nextAction)
      }
    })

    viewModel.apiError.observe(viewLifecycleOwner, Observer {
      showLoading(false)
      var error = getString(R.string.create_card_general_error)
      if (it != null) {
        error = getString(R.string.create_card_error, it)
      }
      val dialogBuilder = AlertDialog.Builder(requireContext())
      dialogBuilder.setMessage(error)
        .setCancelable(false)
        .setPositiveButton(getString(R.string.try_again)) { _, _ ->
          createPatron()
        }
        .setNegativeButton(getString(R.string.quit)) { _, _ ->
          Cache(requireContext()).clear()
          requireActivity().setResult(Activity.RESULT_CANCELED)
          requireActivity().finish()
        }
      val alert = dialogBuilder.create()
      alert.show()
    })
  }

  private fun createPatron() {
    showLoading(true)
    viewModel.createPatron(getPatron(),
      requireActivity().intent.extras.getString("username"),
      requireActivity().intent.extras.getString("password"))
  }

  private fun getPatron(): Patron {
    val homeAddress = cache.getHomeAddress()
    val personalInformation = cache.getPersonalInformation()
    val accountInformation = cache.getAccountInformation()
    val workAddress = cache.getWorkAddress()
    val schoolAddress = cache.getSchoolAddress()

    when {
      cache.getHomeAddress().state == nyState -> {
        return Patron(
          policyTypeDefault,
          homeAddress,
          personalInformation.email,
          "${personalInformation.firstName} ${personalInformation.lastName}",
          personalInformation.birthDate,
          accountInformation.pin,
          accountInformation.username,
          null)
      }
      cache.getSchoolAddress().line_1.isEmpty() -> {
        return Patron(
          policyTypeDefault,
          homeAddress,
          personalInformation.email,
          "${personalInformation.firstName} ${personalInformation.lastName}",
          personalInformation.birthDate,
          accountInformation.pin,
          accountInformation.username,
          workAddress)
      }
      else -> {
        return Patron(
          policyTypeDefault,
          homeAddress,
          personalInformation.email,
          "${personalInformation.firstName} ${personalInformation.lastName}",
          personalInformation.birthDate,
          accountInformation.pin,
          accountInformation.username,
          schoolAddress)
      }
    }
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

  /**
   * Sets user data from cache
   */
  private fun setReviewData() {
    val homeAddress = cache.getHomeAddress()
    binding.addressHomeTv1.text = homeAddress.line_1
    binding.addressHomeTv2.text = "${homeAddress.city}, ${homeAddress.state} ${homeAddress.zip}"
    if (homeAddress.state != nyState) {
      val workAddress = cache.getWorkAddress()
      if (workAddress.line_1.isNotEmpty()) {
        binding.workAddressData.visibility = View.VISIBLE
        binding.workAddressTv1.text = workAddress.line_1
        binding.workAddressTv2.text = "${workAddress.city}, ${workAddress.state} ${workAddress.zip}"
      } else {
        val schoolAddress = cache.getSchoolAddress()
        binding.schoolAddressData.visibility = View.VISIBLE
        binding.schoolAddressTv1.text = schoolAddress.line_1
        binding.schoolAddressTv1.text = "${schoolAddress.city}, ${schoolAddress.state} ${schoolAddress.zip}"
      }
    }
    val personalInformation = cache.getPersonalInformation()
    binding.nameTv.text = "${personalInformation.firstName} ${personalInformation.lastName}"
    binding.emailTv.text = personalInformation.email
    val accountInformation = cache.getAccountInformation()
    binding.usernameTv.text = accountInformation.username
    binding.pinTv.text = accountInformation.pin
  }
}
