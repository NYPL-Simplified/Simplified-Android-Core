package org.nypl.simplified.app;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.drm.core.AdobeVendorID;
import org.nypl.simplified.app.catalog.MainCatalogActivity;
import org.nypl.simplified.books.core.AccountAuthProvider;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.AccountLoginListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.cardcreator.Constants;
import org.nypl.simplified.cardcreator.LocationTracker;
import org.nypl.simplified.cardcreator.fragments.AddressFragment;
import org.nypl.simplified.cardcreator.fragments.AgeFragment;
import org.nypl.simplified.cardcreator.fragments.ConfirmationFragment;
import org.nypl.simplified.cardcreator.fragments.CredentialsFragment;
import org.nypl.simplified.cardcreator.fragments.HomeAddressConfirmFragment;
import org.nypl.simplified.cardcreator.fragments.HomeAddressFragment;
import org.nypl.simplified.cardcreator.fragments.LocationFragment;
import org.nypl.simplified.cardcreator.fragments.NameFragment;
import org.nypl.simplified.cardcreator.fragments.ReviewFragment;
import org.nypl.simplified.cardcreator.fragments.WorkAddressConfirmFragment;
import org.nypl.simplified.cardcreator.fragments.WorkAddressFragment;
import org.nypl.simplified.cardcreator.listener.AccountListenerType;
import org.nypl.simplified.cardcreator.listener.AddressListenerType;
import org.nypl.simplified.cardcreator.listener.InputListenerType;
import org.nypl.simplified.cardcreator.listener.LocationListenerType;
import org.nypl.simplified.cardcreator.listener.UsernameListenerType;
import org.nypl.simplified.cardcreator.model.AddressResponse;
import org.nypl.simplified.cardcreator.model.NewPatronResponse;
import org.nypl.simplified.cardcreator.model.UsernameResponse;
import org.nypl.simplified.cardcreator.validation.AddressValidationTask;
import org.nypl.simplified.cardcreator.validation.CreatePatronTask;
import org.nypl.simplified.cardcreator.validation.UsernameValidationTask;

import java.util.List;


/**
 *
 */
public class CardCreatorActivity extends FragmentActivity implements
  InputListenerType,
  AddressListenerType,
  UsernameListenerType,
  AccountListenerType,
  AccountLoginListenerType,
  LocationListenerType {


  protected static final String TAG = "CardCreatorActivity";
  private Prefs prefs = new Prefs(this);

  /**
   *
   */
  public CardCreatorActivity() {

  }

  @Override
  public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn) {
    final MenuItem item = NullCheck.notNull(item_mn);
    switch (item.getItemId()) {

      case android.R.id.home: {
        finish();
        return true;
      }

      default: {
        return super.onOptionsItemSelected(item);
      }
    }
  }

  @Override
  protected void onCreate(final Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_card_creator);

    final ActionBar bar = this.getActionBar();

    bar.setTitle("Sign Up for a Library Card");

    findViewById(R.id.prev_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_PREV_BUTTON));

    findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));

    final StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

    StrictMode.setThreadPolicy(policy);

    if (findViewById(R.id.fragment_container) != null) {

      // However, if we're being restored from a previous state,
      // then we don't need to do anything and should return or else
      // we could end up with overlapping fragments.
      if (state != null) {
        return;
      }

      // Create a new Fragment to be placed in the activity layout
      final AgeFragment age_fragment = new AgeFragment().newInstance();

      // In case this activity was started with special instructions from an
      // Intent, pass the Intent's extras to the fragment as arguments
      age_fragment.setArguments(getIntent().getExtras());


      // Add the fragment to the 'fragment_container' FrameLayout
      getSupportFragmentManager().beginTransaction()
        .replace(R.id.fragment_container, age_fragment).commit();
    }

  }

  @Override
  protected void onResume() {
    super.onResume();

    if (this.getVisibleFragment() == null) {
      this.prefs.putBoolean(Constants.SHOW_PREV_BUTTON, false);
      findViewById(R.id.prev_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_PREV_BUTTON));
      if (this.prefs.getBoolean(Constants.EQUAL_OR_OLDER_13)) {
        this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
      }
      if (this.prefs.getBoolean(Constants.UNDER_13)) {
        this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
        ((Button) findViewById(R.id.next_button)).setText("Done");
        ((TextView) findViewById(R.id.error)).setText("You are not old enough to sign up for a library card.");
        (findViewById(R.id.error)).setVisibility(View.VISIBLE);

      }
      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));

    } else {

      this.prefs.putBoolean(Constants.SHOW_PREV_BUTTON, true);
      findViewById(R.id.prev_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_PREV_BUTTON));

    }

  }

  @android.support.annotation.Nullable
  private Fragment getVisibleFragment() {
    final FragmentManager fragment_manager = getSupportFragmentManager();
    final List<Fragment> fragments = fragment_manager.getFragments();
    if (fragments != null) {
      for (Fragment fragment : fragments) {
        if (fragment != null && fragment.isVisible()) {
          return fragment;
        }
      }
    }
    return null;
  }

  /**
   * @param view next view
   */

  public void next(final View view) {

    this.hideKeyboard();

    if ("Done".equals(((Button) findViewById(R.id.next_button)).getText())) {
      finish();
      this.prefs.clearAllPreferences();
      return;
    }

    this.prefs.putBoolean(Constants.SHOW_PREV_BUTTON, true);
    findViewById(R.id.prev_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_PREV_BUTTON));

    final Fragment current_fragment = this.getVisibleFragment();
    final Fragment new_fragment;

    if (current_fragment instanceof AgeFragment) {

      // show location fragment
      new_fragment = new LocationFragment().newInstance(this.prefs.getString(Constants.ADDRESS_OUTPUT), this.prefs.getString(Constants.ADDRESS_STATUS));
      this.replace(new_fragment);

    } else if (current_fragment instanceof LocationFragment) {

      new_fragment = new HomeAddressFragment().newInstance();
      this.replace(new_fragment);

    } else if (current_fragment instanceof AddressFragment) {

      ((Button) findViewById(R.id.next_button)).setText("Next");

      new_fragment = new WorkAddressFragment().newInstance();
      this.replace(new_fragment);

    } else if (current_fragment instanceof HomeAddressFragment) {


      new AddressValidationTask(
        this,
        ((HomeAddressFragment) current_fragment).line_1.getText().toString(),
        ((HomeAddressFragment) current_fragment).line_2.getText().toString(),
        ((HomeAddressFragment) current_fragment).city.getText().toString(),
        ((HomeAddressFragment) current_fragment).state.getText().toString(),
        ((HomeAddressFragment) current_fragment).zip.getText().toString(), false, Simplified.getCardCreator()).run();

    } else if (current_fragment instanceof HomeAddressConfirmFragment) {

      if ("Confirm".equals(((Button) findViewById(R.id.next_button)).getText())) {

        ((Button) findViewById(R.id.next_button)).setText("Next");

        if (!"NY".equals(this.prefs.getString(Constants.STATE_H_DATA_KEY))) {

          new_fragment = new AddressFragment().newInstance();
          this.replace(new_fragment);

          if (this.prefs.getBoolean(Constants.LIVE_IN_NY_DATA_KEY)
            || this.prefs.getBoolean(Constants.WORK_IN_NY_DATA_KEY)
            || this.prefs.getBoolean(Constants.SCHOOL_IN_NY_DATA_KEY)) {
            this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
            findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));
          } else {
            this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, false);
            findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));
          }

        } else {
          new_fragment = new NameFragment().newInstance();
          this.replace(new_fragment);
        }
      }

    } else if (current_fragment instanceof WorkAddressFragment) {

      new AddressValidationTask(
        this,
        ((WorkAddressFragment) current_fragment).line_1.getText().toString(),
        ((WorkAddressFragment) current_fragment).line_2.getText().toString(),
        ((WorkAddressFragment) current_fragment).city.getText().toString(),
        ((WorkAddressFragment) current_fragment).state.getText().toString(),
        ((WorkAddressFragment) current_fragment).zip.getText().toString(), true,  Simplified.getCardCreator()).run();

    } else if (current_fragment instanceof WorkAddressConfirmFragment) {
      if ("Confirm".equals(((Button) findViewById(R.id.next_button)).getText())) {
        ((Button) findViewById(R.id.next_button)).setText("Next");

        new_fragment = new NameFragment().newInstance();
        this.replace(new_fragment);
      }

    } else if (current_fragment instanceof NameFragment) {

      new_fragment = new CredentialsFragment().newInstance();
      this.replace(new_fragment);
      this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));

    } else if (current_fragment instanceof CredentialsFragment) {

      new UsernameValidationTask(this,
        ((CredentialsFragment) current_fragment).username.getText().toString(),  Simplified.getCardCreator()).run();

    } else if (current_fragment instanceof ReviewFragment) {

      new CreatePatronTask(this, this.prefs,  Simplified.getCardCreator()).run();

    } else if (current_fragment instanceof ConfirmationFragment) {

      this.openCatalog();

    }

  }

  private void openCatalog() {
    final Intent i = new Intent(this, MainCatalogActivity.class);

    i.putExtra("reload", true);

    this.startActivity(i);
    this.overridePendingTransition(0, 0);
    this.finish();
    this.prefs.clearAllPreferences();

  }

  private void replace(final Fragment new_fragment) {

    final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

    transaction.replace(R.id.fragment_container, new_fragment);
    transaction.addToBackStack(null);

    transaction.commit();

  }

  /**
   * @param view previous view
   */

  public void prev(final View view) {
    this.onBackPressed();
  }

  /**
   * @param view age verfication checkbox
   */

  public void onRadioButtonClicked(final View view) {

    final boolean checked = ((RadioButton) view).isChecked();

    // Check which radio button was clicked
    final int i = view.getId();
    if (i == R.id.under13 && checked) {
        ((Button) findViewById(R.id.next_button)).setText("Done");

        this.prefs.putBoolean(Constants.UNDER_13, true);
        this.prefs.putBoolean(Constants.EQUAL_OR_OLDER_13, false);
        this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
        findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));

        ((TextView) findViewById(R.id.error)).setText("You are not old enough to sign up for a library card.");
        (findViewById(R.id.error)).setVisibility(View.VISIBLE);

    } else if (i == R.id.equalOrOlder && checked) {

        ((Button) findViewById(R.id.next_button)).setText("Next");

        this.prefs.putBoolean(Constants.UNDER_13, false);
        this.prefs.putBoolean(Constants.EQUAL_OR_OLDER_13, true);
        this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
        findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));
        (findViewById(R.id.error)).setVisibility(View.GONE);
    }
  }


  /**
   * @param view check location
   */

  public void checkLocation(final View view) {

    final LocationTracker tracker = new LocationTracker(CardCreatorActivity.this);

    if (tracker.canGetLocation()) {

      if (tracker.isNYS(this)) {

        ((Button) findViewById(R.id.next_button)).setText("Next");

        ((EditText) findViewById(R.id.region)).setText(tracker.getAddressOutput());
        ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageSuccess);
        ((TextView) findViewById(android.R.id.text1)).setText("We have successfully determined that you are in New York!");

      } else {

        ((Button) findViewById(R.id.next_button)).setText("Done");

        ((EditText) findViewById(R.id.region)).setText(tracker.getAddressOutput());
        ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);
        ((TextView) findViewById(android.R.id.text1)).setText("You must be in New York to sign up for a library card. "
          + "Please try to sign up again when you are in another location.");
      }
      this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));

    } else {
      // Can't get location.
      // GPS or network is not enabled.
      // Ask user to enable GPS/network in settings.

      tracker.showSettingsAlert();
    }


  }

  /**
   * @param view address selection
   */

  public void onAddressRadioButtonClicked(final View view) {
    final boolean checked = ((RadioButton) view).isChecked();

    this.prefs.putBoolean(Constants.LIVE_IN_NY_DATA_KEY, false);
    this.prefs.putBoolean(Constants.WORK_IN_NY_DATA_KEY, false);
    this.prefs.putBoolean(Constants.SCHOOL_IN_NY_DATA_KEY, false);

    this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
    findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));

    // Check which radio button was clicked
    final int i = view.getId();
    if (i == R.id.liveInNYC && checked) {
        this.prefs.putBoolean(Constants.LIVE_IN_NY_DATA_KEY, true);
    } else if (i == R.id.workInNYC && checked) {
        this.prefs.putBoolean(Constants.WORK_IN_NY_DATA_KEY, true);
    } else if (i == R.id.goToSchoolInNYC && checked) {
        this.prefs.putBoolean(Constants.SCHOOL_IN_NY_DATA_KEY, true);
    }
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();

    ((Button) findViewById(R.id.next_button)).setText("Next");

    if (this.getVisibleFragment() == null || this.getVisibleFragment() instanceof AgeFragment) {
      this.prefs.putBoolean(Constants.SHOW_PREV_BUTTON, false);
      findViewById(R.id.prev_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_PREV_BUTTON));
      if (this.prefs.getBoolean(Constants.EQUAL_OR_OLDER_13)) {
        this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
      }
      if (this.prefs.getBoolean(Constants.UNDER_13)) {
        this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
      }
      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));
    } else {
      this.prefs.putBoolean(Constants.SHOW_PREV_BUTTON, true);
      findViewById(R.id.prev_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_PREV_BUTTON));
    }

    if (this.getVisibleFragment() instanceof LocationFragment)
    {
      this.checkLocation(null);
    }

    if (this.getVisibleFragment() instanceof AddressFragment
      && (this.prefs.getBoolean(Constants.WORK_IN_NY_DATA_KEY) || this.prefs.getBoolean(Constants.SCHOOL_IN_NY_DATA_KEY))) {
      this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));
    }

    if (this.getVisibleFragment() instanceof WorkAddressConfirmFragment || this.getVisibleFragment() instanceof HomeAddressConfirmFragment) {
      ((Button) findViewById(R.id.next_button)).setText("Confirm");

      this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, false);

      if ((this.prefs.contains(Constants.SELECTED_WORK_ADDRESS) && this.getVisibleFragment() instanceof WorkAddressConfirmFragment)
        || (this.prefs.contains(Constants.SELECTED_ADDRESS) && this.getVisibleFragment() instanceof HomeAddressConfirmFragment)) {
        this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
      }

      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));
    }

  }

  @Override
  protected void onStart() {
    super.onStart();
    this.hideKeyboard();
  }

  @Override
  protected void onStop() {
    super.onStop();
  }

  @Override
  public void onSaveInstanceState(final Bundle state) {
    super.onSaveInstanceState(state);
  }

  @Override
  public void onInputComplete() {

    this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
    findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));
  }

  @Override
  public void onInputInComplete() {

    this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, false);
    findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));

  }

  @Override
  public void onAddressValidationSucceeded(final AddressResponse response) {

    Log.i(TAG, response.message);

    this.prefs.putString(Constants.CARD_TYPE_DATA_KEY, response.message);

    final Fragment current_fragment = this.getVisibleFragment();
    final Fragment new_fragment;
    if (current_fragment instanceof HomeAddressFragment) {


      if (response.addresses != null || response.address != null) {
        ((Button) findViewById(R.id.next_button)).setText("Confirm");
        new_fragment = new HomeAddressConfirmFragment().newInstance(response);
        this.replace(new_fragment);

      } else {
        ((TextView) findViewById(android.R.id.text1)).setText("No valid address found");
        ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);

      }

    } else if (current_fragment instanceof WorkAddressFragment) {

      if (!response.card_type.equals("null")) {
        ((Button) findViewById(R.id.next_button)).setText("Confirm");

        if (response.addresses != null || response.address != null) {


          new_fragment = new WorkAddressConfirmFragment().newInstance(response);
          this.replace(new_fragment);

        }

      } else {
        ((TextView) findViewById(android.R.id.text1)).setText(response.message);
        ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);

      }
    }

    this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
    findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));
  }

  @Override
  public void onAddressValidationFailed(final AddressResponse response) {
    Log.i(TAG, response.message);
    ((TextView) findViewById(android.R.id.text1)).setText(response.message);
    ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);

  }

  @Override
  public void onAddressValidationError(final String message) {
    Log.i(TAG, message);
    ((TextView) findViewById(android.R.id.text1)).setText(message);
    ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);
  }

  @Override
  public void onUsernameValidationSucceeded(final UsernameResponse response) {
    Log.i(TAG, response.message);

    ((Button) findViewById(R.id.next_button)).setText("Create Card");

    final Fragment new_fragment = new ReviewFragment().newInstance();
    this.replace(new_fragment);
    this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
    findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));

  }

  @Override
  public void onUsernameValidationFailed(final UsernameResponse response) {
    Log.i(TAG, response.message);
    ((TextView) findViewById(android.R.id.text1)).setText(response.message);
    ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);
  }

  @Override
  public void onUsernameValidationError(final String message) {
    Log.i(TAG, message);
//    showToast("An Error occurred, please try again later");
    ((TextView) findViewById(android.R.id.text1)).setText(message);
    ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);

  }

  @Override
  public void onAccountCreationSucceeded(final NewPatronResponse response) {

    Log.i(TAG, response.message);

    this.prefs.putString(Constants.BARCODE_DATA_KEY, response.barcode);
    this.prefs.putString(Constants.PIN_DATA_KEY, response.pin);
    this.prefs.putString(Constants.USERNAME_DATA_KEY, response.username);
    this.prefs.putString(Constants.MESSAGE_DATA_KEY, response.message);

    final Fragment new_fragment = new ConfirmationFragment().newInstance();

    this.replace(new_fragment);

    ((Button) findViewById(R.id.next_button)).setText("Open Catalog");

    findViewById(R.id.prev_button).setVisibility(View.GONE);

    this.prefs.putBoolean(Constants.SHOW_NEXT_BUTTON, true);
    findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(Constants.SHOW_NEXT_BUTTON));


    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();


    final Resources rr = NullCheck.notNull(CardCreatorActivity.this.getResources());
    final OptionType<AdobeVendorID> adobe_vendor = Option.some(
      new AdobeVendorID(rr.getString(org.nypl.simplified.app.R.string.feature_adobe_vendor_id)));
    final BooksType books = app.getBooks();

    final AccountBarcode barcode = new AccountBarcode(this.prefs.getString(Constants.USERNAME_DATA_KEY));
    final AccountPIN pin = new AccountPIN(this.prefs.getString(Constants.PIN_DATA_KEY));
    final AccountAuthProvider auth_provider = new AccountAuthProvider(rr.getString(org.nypl.simplified.app.R.string.feature_default_auth_provider_name));

    final AccountCredentials creds =
      new AccountCredentials(adobe_vendor, barcode, pin, Option.some(auth_provider));
    books.accountLogin(creds, CardCreatorActivity.this);


  }

  @Override
  public void onAccountCreationFailed(final NewPatronResponse response) {
    Log.i(TAG, response.message);
//    showToast(response.message);
    ((TextView) findViewById(android.R.id.text1)).setText(response.message);
    ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);

  }

  @Override
  public void onAccountCreationError(final String message) {
    Log.i(TAG, message);
    ((TextView) findViewById(android.R.id.text1)).setText(message);
    ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);
  }

  private void hideKeyboard() {
    // Check if no view has focus:
    final View view = this.getCurrentFocus();
    if (view != null) {
      final InputMethodManager im = (InputMethodManager) this.getSystemService(
        Context.INPUT_METHOD_SERVICE);
      im.hideSoftInputFromWindow(
        view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
  }

  @Override
  public void onAccountLoginFailureCredentialsIncorrect() {
    // Nothing
  }

  @Override
  public void onAccountLoginFailureServerError(final int code) {
    // Nothing
  }

  @Override
  public void onAccountLoginFailureLocalError(final OptionType<Throwable> error, final String message) {
    // Nothing
  }

  @Override
  public void onAccountLoginSuccess(final AccountCredentials credentials) {
    // Nothing
  }

  @Override
  public void onAccountLoginFailureDeviceActivationError(final String message) {
    // Nothing
  }

  @Override
  public void onAccountSyncAuthenticationFailure(final String message) {
    // Nothing
  }

  @Override
  public void onAccountSyncBook(final BookID book) {
    // Nothing
  }

  @Override
  public void onAccountSyncFailure(final OptionType<Throwable> error, final String message) {
    // Nothing
  }

  @Override
  public void onAccountSyncSuccess() {
    // Nothing
  }

  @Override
  public void onAccountSyncBookDeleted(final BookID book) {
    // Nothing
  }

  @Override
  public void onCheckLocation() {
    this.checkLocation(null);
  }
}
