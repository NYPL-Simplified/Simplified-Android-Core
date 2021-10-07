package org.nypl.simplified.cardcreator.ui

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.databinding.FragmentReviewBinding
import org.nypl.simplified.cardcreator.model.CreatePatronResponse
import org.nypl.simplified.cardcreator.model.IdentifierParent
import org.nypl.simplified.cardcreator.model.JuvenilePatronResponse
import org.nypl.simplified.cardcreator.model.Patron
import org.nypl.simplified.cardcreator.utils.Cache
import org.nypl.simplified.cardcreator.utils.getCache
import org.nypl.simplified.cardcreator.utils.isBarcode
import org.nypl.simplified.cardcreator.viewmodel.PatronViewModel
import org.slf4j.LoggerFactory

class ReviewFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(ReviewFragment::class.java)

  private var _binding: FragmentReviewBinding? = null
  private val binding get() = _binding!!

  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections

  private lateinit var cache: Cache

  private val nyState = "NY"
  private val policyTypeDefault = "simplye"

  private val viewModel: PatronViewModel by activityViewModels()

  private var dialog: AlertDialog? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentReviewBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    navController = Navigation.findNavController(requireActivity(), R.id.card_creator_nav_host_fragment)
    cache = Cache(requireContext())

    if (getCache().isJuvenileCard!!) {
      setJuvenileReviewData()
    } else {
      setReviewData()
    }

    // Go to next screen
    binding.nextBtn.setOnClickListener {
      if (getCache().isJuvenileCard!!) {
        createJuvenilePatron()
      } else {
        createPatron()
      }
    }

    // Go to previous screen
    binding.prevBtn.setOnClickListener {
      goBack()
    }

    viewModel.createPatronResponse
      .receive(viewLifecycleOwner, this::handleCreatePatronResponse)

    viewModel.juvenilePatronResponse
      .receive(viewLifecycleOwner, this::handleJuvenilePatronResponse)

    val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
      goBack()
    }
    callback.isEnabled = true

    viewModel.pendingRequest.observe(viewLifecycleOwner, this::showLoading)
  }

  private fun handleCreatePatronResponse(response: CreatePatronResponse) {
    when (response) {
      is CreatePatronResponse.CreatePatronData -> {
        logger.debug("User navigated to the next screen")
        logger.debug("Card granted")
        Toast.makeText(activity, getString(R.string.card_granted), Toast.LENGTH_SHORT).show()
        nextAction = ReviewFragmentDirections.actionNext(
          response.username,
          response.barcode,
          response.password,
          getString(R.string.card_created),
          "${cache.getPersonalInformation().firstName} ${cache.getPersonalInformation().lastName}"
        )
        navController.navigate(nextAction)
      }
      is CreatePatronResponse.CreatePatronHttpError -> {
        val error = getString(R.string.create_card_error, response.status)
        showCreatePatronErrorDialog(error)
      }
      is CreatePatronResponse.CreatePatronException -> {
        val error = response.exception.message
          ?.takeUnless(String::isBlank)
          ?: getString(R.string.create_card_general_error)
        showCreatePatronErrorDialog(error)
      }
    }
  }

  private fun showCreatePatronErrorDialog(error: String) {
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
    if (dialog == null) {
      dialog = dialogBuilder.create()
    }
    dialog?.show()
  }

  private fun goBack() {
    navController.popBackStack()
  }

  private fun createPatron() {
    viewModel.createPatron(getPatron())
  }

  private fun createJuvenilePatron() {
    val identifierParent =
      if (isBarcode(requireActivity().intent.getStringExtra("userIdentifier")!!)) {
        IdentifierParent.BarcodeParent(
          requireActivity().intent.getStringExtra("userIdentifier")!!,
          getCache().getPersonalInformation().firstName,
          getCache().getAccountInformation().username,
          getCache().getAccountInformation().pin
        )
      } else {
        IdentifierParent.UsernameParent(
          getCache().getPersonalInformation().firstName,
          requireActivity().intent.getStringExtra("userIdentifier")!!,
          getCache().getAccountInformation().username,
          getCache().getAccountInformation().pin
        )
      }
    viewModel.createJuvenileCard(identifierParent)
  }

  private fun handleJuvenilePatronResponse(response: JuvenilePatronResponse) {
    when (response) {
      is JuvenilePatronResponse.JuvenilePatronData -> {
        Toast.makeText(activity, R.string.card_created, Toast.LENGTH_SHORT).show()
        logger.debug("User navigated to the next screen")
        logger.debug("Card granted")
        nextAction = ReviewFragmentDirections.actionNext(
          response.data.dependent.username,
          response.data.dependent.barcode,
          response.data.dependent.password,
          getString(R.string.card_created),
          "${cache.getPersonalInformation().firstName} ${cache.getPersonalInformation().lastName}"
        )
        navController.navigate(nextAction)
      }
      is JuvenilePatronResponse.JuvenilePatronError -> {
        val message = getString(R.string.create_card_error, response.status)
        showCreatePatronErrorDialog(message)
      }
      is JuvenilePatronResponse.JuvenilePatronException -> {
        val message = getString(R.string.create_card_general_error)
        showCreatePatronErrorDialog(message)
      }
    }
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
          usernameHasBeenValidated = true,
          acceptTerms = true,
          work_or_school_address = null
        )
      }
      cache.getSchoolAddress().line1.isEmpty() -> {
        return Patron(
          policyTypeDefault,
          homeAddress,
          personalInformation.email,
          "${personalInformation.firstName} ${personalInformation.lastName}",
          personalInformation.birthDate,
          accountInformation.pin,
          accountInformation.username,
          usernameHasBeenValidated = true,
          acceptTerms = true,
          work_or_school_address = workAddress
        )
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
          usernameHasBeenValidated = true,
          acceptTerms = true,
          work_or_school_address = schoolAddress
        )
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
    binding.addressHomeTv1.text = homeAddress.line1
    binding.addressHomeTv2.text = "${homeAddress.city}, ${homeAddress.state} ${homeAddress.zip}"
    if (homeAddress.state != nyState) {
      val workAddress = cache.getWorkAddress()
      if (workAddress.line1.isNotEmpty()) {
        binding.workAddressData.visibility = View.VISIBLE
        binding.workAddressTv1.text = workAddress.line1
        binding.workAddressTv2.text = "${workAddress.city}, ${workAddress.state} ${workAddress.zip}"
      } else {
        val schoolAddress = cache.getSchoolAddress()
        binding.schoolAddressData.visibility = View.VISIBLE
        binding.schoolAddressTv1.text = schoolAddress.line1
        binding.schoolAddressTv1.text = "${schoolAddress.city}, ${schoolAddress.state} ${schoolAddress.zip}"
      }
    }
    val personalInformation = cache.getPersonalInformation()
    binding.emailTv.text = personalInformation.email
    setAccountInfo()
  }

  private fun setJuvenileReviewData() {
    binding.emailLabelTv.visibility = View.GONE
    binding.emailTv.visibility = View.GONE
    binding.schoolAddressData.visibility = View.GONE
    binding.workAddressData.visibility = View.GONE
    binding.addressHomeLabelTv.visibility = View.GONE
    binding.addressHomeTv1.visibility = View.GONE
    binding.addressHomeTv2.visibility = View.GONE
    setAccountInfo()
  }

  private fun setAccountInfo() {
    val personalInformation = cache.getPersonalInformation()
    binding.nameTv.text = "${personalInformation.firstName} ${personalInformation.lastName}"
    val accountInformation = cache.getAccountInformation()
    binding.usernameTv.text = accountInformation.username
    binding.pinTv.text = accountInformation.pin
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onPause() {
    super.onPause()
    dialog?.dismiss()
  }
}
