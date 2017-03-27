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
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
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
import org.nypl.simplified.cardcreator.validation.LocationTracker;
import org.nypl.simplified.cardcreator.validation.UsernameValidationTask;
import org.nypl.simplified.prefs.Prefs;

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
  private Prefs prefs = Simplified.getSharedPrefs();

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

    bar.setTitle(R.string.library_signup_title);

    findViewById(R.id.prev_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_PREV_BUTTON)));

    findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));

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

    this.showProgress(false);
    if (this.getVisibleFragment() == null) {
      this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_PREV_BUTTON), false);
      findViewById(R.id.prev_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_PREV_BUTTON)));
      if (this.prefs.getBoolean(this.getResources().getString(R.string.EQUAL_OR_OLDER_13)) && this.prefs.getBoolean(this.getResources().getString(R.string.EULA_ACCEPTED))) {
        this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
      }
      if (this.prefs.getBoolean(this.getResources().getString(R.string.UNDER_13)) && this.prefs.getBoolean(this.getResources().getString(R.string.EULA_ACCEPTED))) {
        this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
        ((Button) findViewById(R.id.next_button)).setText(R.string.nav_done);
        ((TextView) findViewById(R.id.error)).setText(R.string.age_verification_too_young);
        (findViewById(R.id.error)).setVisibility(View.VISIBLE);

      }
      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));

    } else {

      this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_PREV_BUTTON), true);
      findViewById(R.id.prev_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_PREV_BUTTON)));

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

    this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_PREV_BUTTON), true);
    findViewById(R.id.prev_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_PREV_BUTTON)));

    final Fragment current_fragment = this.getVisibleFragment();
    final Fragment new_fragment;

    if (current_fragment instanceof AgeFragment) {

      // show location fragment
      new_fragment = new LocationFragment().newInstance(this.prefs.getString(this.getResources().getString(R.string.ADDRESS_OUTPUT)),
        this.prefs.getString(this.getResources().getString(R.string.ADDRESS_STATUS)));
      this.replace(new_fragment);

    } else if (current_fragment instanceof LocationFragment) {

      new_fragment = new HomeAddressFragment().newInstance();
      this.replace(new_fragment);

    } else if (current_fragment instanceof AddressFragment) {

      ((Button) findViewById(R.id.next_button)).setText(R.string.nav_next);

      new_fragment = new WorkAddressFragment().newInstance();
      this.replace(new_fragment);

    } else if (current_fragment instanceof HomeAddressFragment) {


      this.showProgress(false);
      new AddressValidationTask(
        CardCreatorActivity.this,
        ((HomeAddressFragment) current_fragment).getLine_1().getText().toString(),
        ((HomeAddressFragment) current_fragment).getLine_2().getText().toString(),
        ((HomeAddressFragment) current_fragment).getCity().getText().toString(),
        ((HomeAddressFragment) current_fragment).getState().getText().toString(),
        ((HomeAddressFragment) current_fragment).getZip().getText().toString(), false, Simplified.getCardCreator()).run();

    } else if (current_fragment instanceof HomeAddressConfirmFragment) {

      if (getString(R.string.nav_confirm).equals(((Button) findViewById(R.id.next_button)).getText())) {

        ((Button) findViewById(R.id.next_button)).setText(R.string.nav_next);

        if (!"NY".equals(this.prefs.getString(this.getResources().getString(R.string.STATE_H_DATA_KEY)))) {

          new_fragment = new AddressFragment().newInstance();
          this.replace(new_fragment);

          if (this.prefs.getBoolean(this.getResources().getString(R.string.LIVE_IN_NY_DATA_KEY))
            || this.prefs.getBoolean(this.getResources().getString(R.string.WORK_IN_NY_DATA_KEY))
            || this.prefs.getBoolean(this.getResources().getString(R.string.SCHOOL_IN_NY_DATA_KEY))) {
            this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
            findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));
          } else {
            this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), false);
            findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));
          }

        } else {
          new_fragment = new NameFragment().newInstance();
          this.replace(new_fragment);
        }
      }

    } else if (current_fragment instanceof WorkAddressFragment) {

      this.showProgress(false);
      new AddressValidationTask(
        CardCreatorActivity.this,
        ((WorkAddressFragment) current_fragment).getLine_1().getText().toString(),
        ((WorkAddressFragment) current_fragment).getLine_2().getText().toString(),
        ((WorkAddressFragment) current_fragment).getCity().getText().toString(),
        ((WorkAddressFragment) current_fragment).getState().getText().toString(),
        ((WorkAddressFragment) current_fragment).getZip().getText().toString(), true,  Simplified.getCardCreator()).run();

    } else if (current_fragment instanceof WorkAddressConfirmFragment) {
      if (getString(R.string.nav_confirm).equals(((Button) findViewById(R.id.next_button)).getText())) {
        ((Button) findViewById(R.id.next_button)).setText(R.string.nav_next);

        new_fragment = new NameFragment().newInstance();
        this.replace(new_fragment);
      }

    } else if (current_fragment instanceof NameFragment) {

      new_fragment = new CredentialsFragment().newInstance();
      this.replace(new_fragment);
      this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));

    } else if (current_fragment instanceof CredentialsFragment) {

      this.showProgress(false);
      new UsernameValidationTask(CardCreatorActivity.this,
        ((CredentialsFragment) current_fragment).getUsername().getText().toString(),  Simplified.getCardCreator()).run();

    } else if (current_fragment instanceof ReviewFragment) {

      this.showProgress(false);

      new CreatePatronTask(CardCreatorActivity.this, CardCreatorActivity.this.prefs,  Simplified.getCardCreator()).run();

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

    this.showProgress(false);
    final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

    transaction.replace(R.id.fragment_container, new_fragment);
    transaction.addToBackStack(null);

    transaction.commit();

  }

  private void showProgress(final boolean show) {
    final ViewGroup.LayoutParams params = findViewById(R.id.progress_bar).getLayoutParams();
    if (show) {
      params.height = 100;
      params.width = 100;
      findViewById(R.id.next_button).setEnabled(false);
    }
    else {
      params.height = 0;
      params.width = 0;
    }
    findViewById(R.id.progress_bar).setLayoutParams(params);
    findViewById(R.id.main_frame).invalidate();
    findViewById(R.id.fragment_container).invalidate();
    getWindow().getDecorView().getRootView().invalidate();
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
    final boolean eula_checkbox =  ((CheckBox) findViewById(org.nypl.simplified.cardcreator.R.id.eula_checkbox)).isChecked();

    // Check which radio button was clicked
    final int i = view.getId();
    if (i == R.id.under13 && checked) {
      ((Button) findViewById(R.id.next_button)).setText(R.string.nav_done);

      this.prefs.putBoolean(this.getResources().getString(R.string.UNDER_13), true);
      this.prefs.putBoolean(this.getResources().getString(R.string.EQUAL_OR_OLDER_13), false);
      this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));

      ((TextView) findViewById(R.id.error)).setText(R.string.age_verification_too_young);
      (findViewById(R.id.error)).setVisibility(View.VISIBLE);

    } else {
      if (i == R.id.equalOrOlder && checked && eula_checkbox) {

        ((Button) findViewById(R.id.next_button)).setText(R.string.nav_next);

        this.prefs.putBoolean(this.getResources().getString(R.string.UNDER_13), false);
        this.prefs.putBoolean(this.getResources().getString(R.string.EQUAL_OR_OLDER_13), true);
        this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
        findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));
        (findViewById(R.id.error)).setVisibility(View.GONE);
      }
      else
      {
        ((Button) findViewById(R.id.next_button)).setText(R.string.nav_next);

        this.prefs.putBoolean(this.getResources().getString(R.string.UNDER_13), false);
        this.prefs.putBoolean(this.getResources().getString(R.string.EQUAL_OR_OLDER_13), true);
        this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), false);
        findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));
        (findViewById(R.id.error)).setVisibility(View.GONE);

      }
    }
  }


  /**
   * @param view eula checkbox
   */
  public void onEulaCheckBoxClicked(final View view) {


    final boolean equal_or_older =  ((RadioButton) findViewById(org.nypl.simplified.cardcreator.R.id.equalOrOlder)).isChecked();
    final boolean under_13 =  ((RadioButton) findViewById(org.nypl.simplified.cardcreator.R.id.under13)).isChecked();
    final boolean checked = ((CheckBox) view).isChecked();

    if (checked && equal_or_older)
    {
      ((Button) findViewById(R.id.next_button)).setText(R.string.nav_next);

      this.prefs.putBoolean(this.getResources().getString(R.string.UNDER_13), false);
      this.prefs.putBoolean(this.getResources().getString(R.string.EQUAL_OR_OLDER_13), true);
      this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
      this.prefs.putBoolean(this.getResources().getString(R.string.EULA_ACCEPTED), true);
      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));
      (findViewById(R.id.error)).setVisibility(View.GONE);
    }
    else if (checked && under_13)
    {
      ((Button) findViewById(R.id.next_button)).setText(R.string.nav_done);

      this.prefs.putBoolean(this.getResources().getString(R.string.UNDER_13), true);
      this.prefs.putBoolean(this.getResources().getString(R.string.EQUAL_OR_OLDER_13), false);
      this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
      this.prefs.putBoolean(this.getResources().getString(R.string.EULA_ACCEPTED), true);
      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));
      ((TextView) findViewById(R.id.error)).setText(R.string.age_verification_too_young);
      (findViewById(R.id.error)).setVisibility(View.VISIBLE);
    }
    else
    {
      ((Button) findViewById(R.id.next_button)).setText(R.string.nav_next);

      this.prefs.putBoolean(this.getResources().getString(R.string.UNDER_13), false);
      this.prefs.putBoolean(this.getResources().getString(R.string.EQUAL_OR_OLDER_13), true);
      this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), false);
      this.prefs.putBoolean(this.getResources().getString(R.string.EULA_ACCEPTED), false);
      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));
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
      this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));

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

    this.prefs.putBoolean(this.getResources().getString(R.string.LIVE_IN_NY_DATA_KEY), false);
    this.prefs.putBoolean(this.getResources().getString(R.string.WORK_IN_NY_DATA_KEY), false);
    this.prefs.putBoolean(this.getResources().getString(R.string.SCHOOL_IN_NY_DATA_KEY), false);

    this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
    findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));

    // Check which radio button was clicked
    final int i = view.getId();
    if (i == R.id.liveInNYC && checked) {
      this.prefs.putBoolean(this.getResources().getString(R.string.LIVE_IN_NY_DATA_KEY), true);
    } else if (i == R.id.workInNYC && checked) {
      this.prefs.putBoolean(this.getResources().getString(R.string.WORK_IN_NY_DATA_KEY), true);
    } else if (i == R.id.goToSchoolInNYC && checked) {
      this.prefs.putBoolean(this.getResources().getString(R.string.SCHOOL_IN_NY_DATA_KEY), true);
    }
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();

    this.showProgress(false);
    ((Button) findViewById(R.id.next_button)).setText(R.string.nav_next);

    if (this.getVisibleFragment() == null || this.getVisibleFragment() instanceof AgeFragment) {
      this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_PREV_BUTTON), false);
      findViewById(R.id.prev_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_PREV_BUTTON)));
      if (this.prefs.getBoolean(this.getResources().getString(R.string.EQUAL_OR_OLDER_13)) && this.prefs.getBoolean(this.getResources().getString(R.string.EULA_ACCEPTED))) {
        this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
      }
      if (this.prefs.getBoolean(this.getResources().getString(R.string.UNDER_13)) && this.prefs.getBoolean(this.getResources().getString(R.string.EULA_ACCEPTED))) {
        this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
      }
      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));
    } else {
      this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_PREV_BUTTON), true);
      findViewById(R.id.prev_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_PREV_BUTTON)));
    }

    if (this.getVisibleFragment() instanceof LocationFragment)
    {
      this.checkLocation(null);
    }

    if (this.getVisibleFragment() instanceof AddressFragment
      && (this.prefs.getBoolean(this.getResources().getString(R.string.WORK_IN_NY_DATA_KEY))
      || this.prefs.getBoolean(this.getResources().getString(R.string.SCHOOL_IN_NY_DATA_KEY)))) {
      this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));
    }

    if (this.getVisibleFragment() instanceof WorkAddressConfirmFragment || this.getVisibleFragment() instanceof HomeAddressConfirmFragment) {
      ((Button) findViewById(R.id.next_button)).setText(R.string.nav_confirm);

      this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), false);

      if ((this.prefs.contains(this.getResources().getString(R.string.SELECTED_WORK_ADDRESS)) && this.getVisibleFragment() instanceof WorkAddressConfirmFragment)
        || (this.prefs.contains(this.getResources().getString(R.string.SELECTED_ADDRESS)) && this.getVisibleFragment() instanceof HomeAddressConfirmFragment)) {
        this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
      }

      findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));
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

    this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
    findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));
  }

  @Override
  public void onInputInComplete() {

    this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), false);
    findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));

  }

  @Override
  public void onAddressValidationSucceeded(final AddressResponse response) {

    Log.i(TAG, response.getMessage());

    this.prefs.putString(getResources().getString(R.string.CARD_TYPE_DATA_KEY), response.getMessage());

    final Fragment current_fragment = this.getVisibleFragment();
    final Fragment new_fragment;
    if (current_fragment instanceof HomeAddressFragment) {


      if (response.getAddresses() != null || response.getAddress() != null) {
        ((Button) findViewById(R.id.next_button)).setText(R.string.nav_confirm);
        new_fragment = new HomeAddressConfirmFragment().newInstance(response);
        this.replace(new_fragment);

      } else {
        ((TextView) findViewById(android.R.id.text1)).setText(R.string.location_no_valid_address);
        ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);

      }

    } else if (current_fragment instanceof WorkAddressFragment) {

      if (!"null".equals(response.getCard_type())) {
        ((Button) findViewById(R.id.next_button)).setText(R.string.nav_confirm);

        if (response.getAddresses() != null || response.getAddress() != null) {


          new_fragment = new WorkAddressConfirmFragment().newInstance(response);
          this.replace(new_fragment);

        }

      } else {
        ((TextView) findViewById(android.R.id.text1)).setText(response.getMessage());
        ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);

      }
    }

    this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
    findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));
  }

  @Override
  public void onAddressValidationFailed(final AddressResponse response) {
    Log.i(TAG, response.getMessage());
    ((TextView) findViewById(android.R.id.text1)).setText(response.getMessage());
    ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);
    this.showProgress(false);

  }

  @Override
  public void onAddressValidationError(final String message) {
    Log.i(TAG, message);
    ((TextView) findViewById(android.R.id.text1)).setText(message);
    ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);
    this.showProgress(false);
  }

  @Override
  public void onUsernameValidationSucceeded(final UsernameResponse response) {
    Log.i(TAG, response.getMessage());

    ((Button) findViewById(R.id.next_button)).setText(R.string.need_card_create);

    final Fragment new_fragment = new ReviewFragment().newInstance();
    this.replace(new_fragment);
    this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
    findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));
    this.showProgress(false);

  }

  @Override
  public void onUsernameValidationFailed(final UsernameResponse response) {
    Log.i(TAG, response.getMessage());
    ((TextView) findViewById(android.R.id.text1)).setText(response.getMessage());
    ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);
    this.showProgress(false);
  }

  @Override
  public void onUsernameValidationError(final String message) {
    Log.i(TAG, message);
//    showToast("An Error occurred, please try again later");
    ((TextView) findViewById(android.R.id.text1)).setText(message);
    ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);
    this.showProgress(false);

  }

  @Override
  public void onAccountCreationSucceeded(final NewPatronResponse response) {

    Log.i(TAG, response.getMessage());

    this.prefs.putString(this.getResources().getString(R.string.BARCODE_DATA_KEY), response.getBarcode());
    this.prefs.putString(this.getResources().getString(R.string.PIN_DATA_KEY), response.getPin());
    this.prefs.putString(this.getResources().getString(R.string.USERNAME_DATA_KEY), response.getUsername());
    this.prefs.putString(this.getResources().getString(R.string.MESSAGE_DATA_KEY), response.getMessage());

    final Fragment new_fragment = new ConfirmationFragment().newInstance();

    this.replace(new_fragment);

    ((Button) findViewById(R.id.next_button)).setText("Open Catalog");

    findViewById(R.id.prev_button).setVisibility(View.GONE);

    this.prefs.putBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON), true);
    findViewById(R.id.next_button).setEnabled(this.prefs.getBoolean(this.getResources().getString(R.string.SHOW_NEXT_BUTTON)));


    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();


    final Resources rr = NullCheck.notNull(CardCreatorActivity.this.getResources());
    final OptionType<AdobeVendorID> adobe_vendor = Option.some(
      new AdobeVendorID(rr.getString(org.nypl.simplified.app.R.string.feature_adobe_vendor_id)));
    final BooksType books = app.getBooks();

    final AccountBarcode barcode = new AccountBarcode(this.prefs.getString(this.getResources().getString(R.string.USERNAME_DATA_KEY)));
    final AccountPIN pin = new AccountPIN(this.prefs.getString(this.getResources().getString(R.string.PIN_DATA_KEY)));
    final AccountAuthProvider auth_provider = new AccountAuthProvider(rr.getString(org.nypl.simplified.app.R.string.feature_default_auth_provider_name));

    final AccountCredentials creds =
      new AccountCredentials(adobe_vendor, barcode, pin, Option.some(auth_provider));
    books.accountLogin(creds, CardCreatorActivity.this);

    this.showProgress(false);

  }

  @Override
  public void onAccountCreationFailed(final NewPatronResponse response) {
    Log.i(TAG, response.getMessage());
    ((TextView) findViewById(android.R.id.text1)).setText(response.getMessage());
    ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);
    this.showProgress(false);

  }

  @Override
  public void onAccountCreationError(final String message) {
    Log.i(TAG, message);
    ((TextView) findViewById(android.R.id.text1)).setText(message);
    ((TextView) findViewById(android.R.id.text1)).setTextAppearance(getApplicationContext(), R.style.WizardPageError);
    this.showProgress(false);
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
