package org.nypl.simplified.cardcreator.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
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
import org.nypl.simplified.cardcreator.databinding.FragmentReviewBinding
import org.nypl.simplified.cardcreator.models.Patron
import org.nypl.simplified.cardcreator.utils.Cache
import org.nypl.simplified.cardcreator.viewmodels.PatronViewModel
import org.slf4j.LoggerFactory

class ReviewFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(ReviewFragment::class.java)

  private lateinit var sharedPreferences: SharedPreferences

  private var _binding: FragmentReviewBinding? = null
  private val binding get() = _binding!!

  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections

  private lateinit var cache: Cache

  private val NY_STATE = "NY"
  private val EMPTY = ""
  private val CARD_GRANTED = "card-granted"

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

    sharedPreferences = activity!!.getSharedPreferences(Cache.DEFAULT_PREFERENCE_NAME, Context.MODE_PRIVATE)
    cache = Cache(sharedPreferences)
    navController = Navigation.findNavController(activity!!, R.id.card_creator_nav_host_fragment)

    setReviewData()

    // Go to next screen
    binding.nextBtn.setOnClickListener {
      showLoading(true)
      viewModel.createPatron(getPatron())
    }

    // Go to previous screen
    binding.prevBtn.setOnClickListener {
      activity!!.onBackPressed()
    }

    viewModel.createPatronResponse.observe(viewLifecycleOwner, Observer { response ->
      showLoading(false)
      Toast.makeText(activity, response.message, Toast.LENGTH_SHORT).show()
      if (response.type == CARD_GRANTED) {
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
  }

  private fun getPatron(): Patron {
    val homeAddress = cache.getHomeAddress()
    val personalInformation = cache.getPersonalInformation()
    val accountInformation = cache.getAccountInformation()
    val workAddress = cache.getWorkAddress()
    val schoolAddress = cache.getSchoolAddress()

    when {
      cache.getHomeAddress().state == NY_STATE -> {
        return Patron(
          homeAddress,
          personalInformation.email,
          "$personalInformation.firstName $personalInformation.lastName",
          accountInformation.pin,
          accountInformation.username,
          workAddress)
      }
      cache.getSchoolAddress().line_1 == EMPTY -> {
        return Patron(
          homeAddress,
          personalInformation.email,
          "$personalInformation.firstName $personalInformation.lastName",
          accountInformation.pin,
          accountInformation.username,
          workAddress)
      }
      else -> {
        return Patron(
          homeAddress,
          personalInformation.email,
          "$personalInformation.firstName $personalInformation.lastName",
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
    binding.addressHomeTv.text = "${homeAddress.line_1}\n${homeAddress.city}, ${homeAddress.state} ${homeAddress.zip}"
    if (homeAddress.state != NY_STATE) {
      val workAddress = cache.getWorkAddress()
      if (workAddress.line_1 != EMPTY) {
        binding.workAddressData.visibility = View.VISIBLE
        binding.workAddressTv.text = "${workAddress.line_1}\n${workAddress.city}, ${workAddress.state} ${workAddress.zip}"
      } else {
        val schoolAddress = cache.getSchoolAddress()
        binding.schoolAddressData.visibility = View.VISIBLE
        binding.schoolAddressTv.text ="${schoolAddress.line_1}\n${schoolAddress.city}, ${schoolAddress.state} ${schoolAddress.zip}"
      }
    }
    val personalInformation = cache.getPersonalInformation()
    binding.nameTv.text = "$personalInformation.firstName $personalInformation.lastName"
    binding.emailTv.text = personalInformation.email
    val accountInformation = cache.getAccountInformation()
    binding.usernameTv.text = accountInformation.username
    binding.pinTv.text = accountInformation.pin
  }
}
