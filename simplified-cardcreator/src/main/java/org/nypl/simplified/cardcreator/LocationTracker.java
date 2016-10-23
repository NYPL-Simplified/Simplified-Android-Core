package org.nypl.simplified.cardcreator;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by aferditamuriqi on 9/15/16.
 */
public class LocationTracker extends Service implements LocationListener {

  private final Context mContext;

  // Flag for GPS status
  boolean isGPSEnabled = false;

  // Flag for network status
  boolean isNetworkEnabled = false;

  // Flag for GPS status
  boolean canGetLocation = false;

  Location location; // Location
  double latitude; // Latitude
  double longitude; // Longitude

  boolean isNewYork = false;
  String addressOutput;

  // The minimum distance to change Updates in meters
  private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

  // The minimum time between updates in milliseconds
  private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

  // Declaring a Location Manager
  protected LocationManager locationManager;

  public LocationTracker(Context context) {
    this.mContext = context;
    getLocation();
  }

  public Location getLocation() {
    try {
      locationManager = (LocationManager) mContext
        .getSystemService(LOCATION_SERVICE);

      // Getting GPS status
      isGPSEnabled = locationManager
        .isProviderEnabled(LocationManager.GPS_PROVIDER);

      // Getting network status
      isNetworkEnabled = locationManager
        .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

      if (!isGPSEnabled && !isNetworkEnabled) {
        // No network provider is enabled
//        this.canGetLocation = false;
      } else {
        this.canGetLocation = true;
        if (isNetworkEnabled) {
          locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            MIN_TIME_BW_UPDATES,
            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
//          Log.d("Network", "Network");
          if (locationManager != null) {
            location = locationManager
              .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
              latitude = location.getLatitude();
              longitude = location.getLongitude();
            }
          }
        }
        // If GPS enabled, get latitude/longitude using GPS Services
        if (isGPSEnabled) {
          if (location == null) {
            locationManager.requestLocationUpdates(
              LocationManager.GPS_PROVIDER,
              MIN_TIME_BW_UPDATES,
              MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
//            Log.d("GPS Enabled", "GPS Enabled");
            if (locationManager != null) {
              location = locationManager
                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
              if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
              }
            }
          }
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return location;
  }


  /**
   * Stop using GPS listener
   * Calling this function will stop using GPS in your app.
   * */
  public void stopUsingGPS(){
    if(locationManager != null){
      locationManager.removeUpdates(LocationTracker.this);
    }
  }


  /**
   * Function to get latitude
   * */
  public double getLatitude(){
    if(location != null){
      latitude = location.getLatitude();
    }

    // return latitude
    return latitude;
  }
  public String getAddressOutput() {
    return addressOutput;
  }

  public boolean isNYS(Context context) {

    Geocoder geocoder = new Geocoder(context, Locale.getDefault());

    // Address found using the Geocoder.
    List<Address> addresses = null;

    try {
      // Using getFromLocation() returns an array of Addresses for the area immediately
      // surrounding the given latitude and longitude. The results are a best guess and are
      // not guaranteed to be accurate.
      addresses = geocoder.getFromLocation(
        location.getLatitude(),
        location.getLongitude(),
        // In this sample, we get just a single address.
        1);
    } catch (IOException ioException) {
      // Catch network or other I/O problems.
//      errorMessage = getString(R.string.service_not_available);
//      Log.e(TAG, errorMessage, ioException);
    } catch (IllegalArgumentException illegalArgumentException) {
      // Catch invalid latitude or longitude values.
//      errorMessage = getString(R.string.invalid_lat_long_used);
//      Log.e(TAG, errorMessage + ". " +
//        "Latitude = " + location.getLatitude() +
//        ", Longitude = " + location.getLongitude(), illegalArgumentException);
    }

    // Handle case where no address was found.
    if (addresses == null || addresses.size() == 0) {
//      if (errorMessage.isEmpty()) {
//        errorMessage = getString(R.string.no_address_found);
//        Log.e(TAG, errorMessage);
//      }

//      ArrayList<String> error = new ArrayList<String>();
//      error.add(errorMessage);
//      deliverResultToReceiver(Constants.FAILURE_RESULT, error);
    } else {
      Address address = addresses.get(0);
      ArrayList<String> addressFragments = new ArrayList<String>();

      // Fetch the address lines using {@code getAddressLine},
      // join them, and send them to the thread. The {@link android.location.address}
      // class provides other options for fetching address details that you may prefer
      // to use. Here are some examples:
      // getLocality() ("Mountain View", for example)
      // getAdminArea() ("CA", for example)
      // getPostalCode() ("94043", for example)
      // getCountryCode() ("US", for example)
      // getCountryName() ("United States", for example)
//            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
//                addressFragments.add(address.getAddressLine(i));
//            }
      addressFragments.add(address.getAdminArea());
      addressFragments.add(address.getCountryCode());
      addressOutput = address.getAdminArea() + " " + address.getCountryCode();

      if (
        (address.getCountryCode().equals("US") && address.getAdminArea().equals("Connecticut")) ||
          (address.getCountryCode().equals("US") && (address.getAdminArea().equals("New York") || address.getAdminArea().equals("NY")))) {
          isNewYork = true;
      }

//      Log.i(TAG, getString(R.string.address_found));
//      deliverResultToReceiver(Constants.SUCCESS_RESULT,
//        addressFragments);
    }
    return isNewYork;
  }


  /**
   * Function to get longitude
   * */
  public double getLongitude(){
    if(location != null){
      longitude = location.getLongitude();
    }

    // return longitude
    return longitude;
  }

  /**
   * Function to check GPS/Wi-Fi enabled
   * @return boolean
   * */
  public boolean canGetLocation() {
    return this.canGetLocation;
  }


  /**
   * Function to show settings alert dialog.
   * On pressing the Settings button it will launch Settings Options.
   * */
  public void showSettingsAlert(){
    AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

    // Setting Dialog Title
    alertDialog.setTitle("Location settings");

    // Setting Dialog Message
    alertDialog.setMessage("Location is not enabled. Do you want to go to settings menu?");

    // On pressing the Settings button.
    alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        mContext.startActivity(intent);
      }
    });

    // On pressing the cancel button
    alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        dialog.cancel();
      }
    });

    // Showing Alert Message
    alertDialog.show();
  }


  @Override
  public void onLocationChanged(Location location) {
  }


  @Override
  public void onProviderDisabled(String provider) {
  }


  @Override
  public void onProviderEnabled(String provider) {
  }


  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
  }


  @Override
  public IBinder onBind(Intent arg0) {
    return null;
  }
}