package org.nypl.simplified.cardcreator.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.location.Geocoder
import android.location.Address
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.databinding.FragmentLocationBinding
import org.slf4j.LoggerFactory
import java.util.Locale

/**
 * The Location screens that gates users who are not in New York
 */
class LocationFragment : Fragment(), LocationListener {

  private val logger = LoggerFactory.getLogger(LocationFragment::class.java)

  private var _binding: FragmentLocationBinding? = null
  private val binding get() = _binding!!

  private lateinit var navController: NavController
  private lateinit var nextAction: NavDirections
  private var isNewYork = false

  private val LOCATION_REQUEST_CODE = 102

  // Minimum distance between location updates, in meters
  private val MIN_DISTANCE_UPDATES: Float = 10f

  // Minimum time interval between location updates, in milliseconds
  private val MIN_TIME_UPDATES = 1000 * 60.toLong()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = FragmentLocationBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    navController = Navigation.findNavController(activity!!, R.id.card_creator_nav_host_fragment)
    isInNewYorkState()

    binding.checkLocationBtn.setOnClickListener {
      isInNewYorkState()
    }

    // Go to next screen
    binding.nextBtn.setOnClickListener {
      if (isNewYork) {
        logger.debug("User navigated to the next screen")
        nextAction = LocationFragmentDirections.actionNext()
        navController.navigate(nextAction)
      } else {
        val data = Intent()
        activity!!.setResult(Activity.RESULT_CANCELED, data)
        activity!!.finish()
      }
    }

    // Go to previous screen
    binding.prevBtn.setOnClickListener {
      activity!!.onBackPressed()
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
   * Checks to see if the user has granted required location permissions
   */
  private fun getLocation(): Location? {
    var location: Location? = null
    logger.debug("Checking for location permission")
    if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_COARSE_LOCATION)
      != PackageManager.PERMISSION_GRANTED
    ) {
      logger.debug("Requesting location permission")
      ActivityCompat.requestPermissions(
        activity!!,
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
        LOCATION_REQUEST_CODE
      )
    } else {
      logger.debug("Location permission granted")
      try {
        logger.debug("Getting current location")
        val locationManager = activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isNetworkLocationEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (isNetworkLocationEnabled) {
          locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_UPDATES, MIN_DISTANCE_UPDATES, this)
          location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }

      } catch (e: Exception) {
        logger.debug("Error getting current location")
        e.printStackTrace()
      }
    }
    return location
  }

  /**
   * Checks to see if a give location is in the state of New York
   */
  // TODO: This function is doing to much, break it up into smaller pieces
  private fun isInNewYorkState(): Boolean {
    logger.debug("Checking to see if user is in New York")
    showLoading(true)
    val locationManager = activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    binding.nextBtn.isEnabled = false
    val MAX_RESULTS = 1
    val location = getLocation()
    val geocoder = Geocoder(activity!!, Locale.getDefault())

    // Address found using the Geocoder.
    var address: Address? = null

    try {
      if (location != null) {
        address = geocoder.getFromLocation(location.latitude, location.longitude, MAX_RESULTS)[0]
        logger.debug( "Region is: ${address.adminArea} ${address.countryCode} ")
        binding.regionEt.setText("${address.adminArea} ${address.countryCode}", TextView.BufferType.EDITABLE)

        if (address.countryCode == "US" && (address.adminArea == "New York" || address.adminArea == "NY")) {
          logger.debug("User is in New York")
          isNewYork = true
        }
      }
    } catch (e: Exception) {
      logger.debug("Error checking to see if user is in New York")
      e.printStackTrace()
    }
    showLoading(false)
    // Show error if user is not in New York
    if (!isNewYork) {
      logger.debug("User is NOT in New York")
      binding.nextBtn.isEnabled = true
      binding.nextBtn.text = getString(R.string.done)
      binding.headerStatusDescTv.text = getString(R.string.location_error)
      binding.headerStatusDescTv.setTextColor(ContextCompat.getColor(activity!!, R.color.red))
    } else {
      binding.nextBtn.isEnabled = true
      binding.nextBtn.text = getString(R.string.next)
      binding.headerStatusDescTv.text = getString(R.string.new_york_success)
      binding.headerStatusDescTv.setTextColor(ContextCompat.getColor(activity!!, R.color.trans_black))
    }
    logger.debug("Stopping location updates")
    locationManager.removeUpdates(this)
    return isNewYork
  }

  /**
   * Listen for result from location permission request, this method is a callback provided by
   * Android for the requestPermissions() method
   *
   * @param requestCode - String user defined request code to identify the request
   * @param permissions - String Array of permissions requested
   * @param grantResults - Integer array of what the user has granted/denied
   */
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    when (requestCode) {
      LOCATION_REQUEST_CODE -> {
        // If request is cancelled, the result arrays are empty.
        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
          logger.debug("Location permission granted")

        } else {
          logger.debug("Location permission NOT granted")
        }
        return
      }
    }
  }

  override fun onLocationChanged(location: Location?) {
    logger.debug("Location has changed")
    val locationManager = activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (location != null) {
      logger.debug("Checking to see if user is in New York")
      showLoading(true)
      binding.nextBtn.isEnabled = false
      val MAX_RESULTS = 1
      val geocoder = Geocoder(activity!!, Locale.getDefault())

      // Address found using the Geocoder.
      var address: Address? = null

      try {
          address = geocoder.getFromLocation(location.latitude, location.longitude, MAX_RESULTS)[0]
          logger.debug( "Region is: ${address.adminArea} ${address.countryCode} ")
          binding.regionEt.setText("${address.adminArea} ${address.countryCode}", TextView.BufferType.EDITABLE)

          if (address.countryCode == "US" && (address.adminArea == "New York" || address.adminArea == "NY")) {
            logger.debug("User is in New York")
            isNewYork = true
          }
      } catch (e: Exception) {
        logger.debug("Error checking to see if user is in New York")
        e.printStackTrace()
      }
      showLoading(false)
      // Show error if user is not in New York
      if (!isNewYork) {
        logger.debug("User is NOT in New York")
        binding.nextBtn.isEnabled = true
        binding.nextBtn.text = getString(R.string.done)
        binding.headerStatusDescTv.text = getString(R.string.location_error)
        binding.headerStatusDescTv.setTextColor(ContextCompat.getColor(activity!!, R.color.red))
      } else {
        binding.nextBtn.isEnabled = true
        binding.nextBtn.text = getString(R.string.next)
        binding.headerStatusDescTv.text = getString(R.string.new_york_success)
        binding.headerStatusDescTv.setTextColor(ContextCompat.getColor(activity!!, R.color.trans_black))
      }
    }
    logger.debug("Stopping location updates")
    locationManager.removeUpdates(this)
  }

  override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    TODO("Not yet implemented")
  }

  override fun onProviderEnabled(provider: String?) {
    TODO("Not yet implemented")
  }

  override fun onProviderDisabled(provider: String?) {
    TODO("Not yet implemented")
  }

}
