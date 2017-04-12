package org.nypl.simplified.cardcreator.validation;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by aferditamuriqi on 9/15/16.
 */

public class LocationTracker extends Service implements LocationListener {

  private final Context context;

  // Flag for GPS status
  private boolean is_gps_enabled;

  // Flag for network status
  private boolean is_network_enabled;

  // Flag for GPS status
  private boolean can_get_location;
  private Location location;
  private double latitude;
  private double longitude;

  private boolean is_new_york;
  private String address_output;

  // The minimum distance to change Updates in meters
  private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;

  // The minimum time between updates in milliseconds
  private static final long MIN_TIME_BW_UPDATES = 1000 * 60;

  // Declaring a Location Manager
  private LocationManager location_manager;

  /**
   * @param in_context context
   */
  public LocationTracker(final Context in_context) {
    this.context = in_context;
    this.getLocation();
  }

  /**
   * @return location
   */
  public Location getLocation() {
    try {
      this.location_manager = (LocationManager) this.context
        .getSystemService(LOCATION_SERVICE);

      // Getting GPS status
      this.is_gps_enabled = this.location_manager
        .isProviderEnabled(LocationManager.GPS_PROVIDER);

      // Getting network status
      this.is_network_enabled = this.location_manager
        .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

      if (!this.is_gps_enabled && !this.is_network_enabled) {
        // No network provider is enabled
        this.can_get_location = false;
      } else {
        this.can_get_location = true;
        if (this.is_network_enabled) {
          this.location_manager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            MIN_TIME_BW_UPDATES,
            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
          if (this.location_manager != null) {
            this.location = this.location_manager
              .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (this.location != null) {
              this.latitude = this.location.getLatitude();
              this.longitude = this.location.getLongitude();
            }
          }
        }
        // If GPS enabled, get latitude/longitude using GPS Services
        if (this.is_gps_enabled) {
          if (this.location == null) {
            this.location_manager.requestLocationUpdates(
              LocationManager.GPS_PROVIDER,
              MIN_TIME_BW_UPDATES,
              MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            if (this.location_manager != null) {
              this.location = this.location_manager
                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
              if (this.location != null) {
                this.latitude = this.location.getLatitude();
                this.longitude = this.location.getLongitude();
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return this.location;
  }


  /**
   * Stop using GPS listener
   * Calling this function will stop using GPS in your app.
   */
  public void stopUsingGPS() {
    if (this.location_manager != null) {
      this.location_manager.removeUpdates(LocationTracker.this);
    }
  }


  /**
   * Function to get latitude
   *
   * @return latitude
   */
  public double getLatitude() {
    if (this.location != null) {
      this.latitude = this.location.getLatitude();
    }

    // return latitude
    return this.latitude;
  }

  /**
   * @return address output
   */
  public String getAddressOutput() {
    return this.address_output;
  }

  /**
   * @param in_context context
   * @return boolean is New York State
   */
  public boolean isNYS(final Context in_context) {

    final Geocoder geocoder = new Geocoder(in_context, Locale.getDefault());

    // Address found using the Geocoder.
    List<Address> addresses = null;

    try {
      // Using getFromLocation() returns an array of Addresses for the area immediately
      // surrounding the given latitude and longitude. The results are a best guess and are
      // not guaranteed to be accurate.
      addresses = geocoder.getFromLocation(
        this.location.getLatitude(),
        this.location.getLongitude(),
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
      final Address address = addresses.get(0);
      final ArrayList<String> address_fragments = new ArrayList<String>();

      address_fragments.add(address.getAdminArea());
      address_fragments.add(address.getCountryCode());
      this.address_output = address.getAdminArea() + " " + address.getCountryCode();

      if ((address.getCountryCode().equals("US") && (address.getAdminArea().equals("New York") || address.getAdminArea().equals("NY")))) {
        this.is_new_york = true;
      }

    }
    return this.is_new_york;
  }


  /**
   * Function to get longitude
   *
   * @return longitude
   */
  public double getLongitude() {
    if (this.location != null) {
      this.longitude = this.location.getLongitude();
    }

    return this.longitude;
  }

  /**
   * Function to check GPS/Wi-Fi enabled
   *
   * @return boolean
   */
  public boolean canGetLocation() {
    return this.can_get_location;
  }


  /**
   * Function to show settings alert dialog.
   * On pressing the Settings button it will launch Settings Options.
   */
  public void showSettingsAlert() {
    final AlertDialog.Builder alert = new AlertDialog.Builder(this.context);

    // Setting Dialog Title
    alert.setTitle("Location settings");

    // Setting Dialog Message
    alert.setMessage("Location is not enabled. Do you want to go to settings menu?");

    // On pressing the Settings button.
    alert.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
      public void onClick(final DialogInterface dialog, final int which) {
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        LocationTracker.this.context.startActivity(intent);
      }
    });

    // On pressing the cancel button
    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      public void onClick(final DialogInterface dialog, final int which) {
        dialog.cancel();
      }
    });

    // Showing Alert Message
    alert.show();
  }


  @Override
  public void onLocationChanged(final Location in_location) {
  }


  @Override
  public void onProviderDisabled(final String in_provider) {
  }


  @Override
  public void onProviderEnabled(final String in_provider) {
  }


  @Override
  public void onStatusChanged(final String in_provider, final int in_status, final Bundle in_extras) {
  }


  @Override
  public IBinder onBind(final Intent arg0) {
    return null;
  }
}
