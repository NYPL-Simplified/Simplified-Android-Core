package org.nypl.simplified.app;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.ProcedureType;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.app.testing.OnMultipleClickListener;
import org.nypl.simplified.app.testing.AlternateFeedURIsActivity;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.BooksControllerConfigurationType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.DocumentStoreType;
import org.nypl.simplified.books.core.EULAType;
import org.nypl.simplified.books.core.SyncedDocumentType;

class MainSettingsFragment extends PreferenceFragment implements LoginListenerType {

  /**
   * Construct an Fragment.
   */
  MainSettingsFragment() {

  }

  @Override
  public void onResume() {
    super.onResume();

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();

    final BooksType books = app.getBooks();
    final Resources resources = NullCheck.notNull(this.getResources());

    final boolean clever_enabled = resources.getBoolean(R.bool.feature_auth_provider_clever);


    if (books.accountIsLoggedIn()) {

      final Intent account =
        new Intent(this.getActivity(), MainSettingsAccountActivity.class);
      final Preference preferences = findPreference(resources.getString(R.string.settings_account));
      preferences.setIntent(account);
      preferences.setOnPreferenceClickListener(null);

    } else if (clever_enabled) {

      final Intent account =
        new Intent(this.getActivity(), LoginActivity.class);
      final Preference preferences = findPreference(resources.getString(R.string.settings_account));
      preferences.setIntent(account);
      preferences.setOnPreferenceClickListener(null);

    } else {

      final Preference preferences = findPreference(resources.getString(R.string.settings_account));
      preferences.setIntent(null);
      preferences.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {

          final AccountBarcode barcode = new AccountBarcode("");
          final AccountPIN pin = new AccountPIN("");

          final LoginDialog df =
            LoginDialog.newDialog("Login required", barcode, pin);
          df.setLoginListener(MainSettingsFragment.this);

          final FragmentManager fm = MainSettingsFragment.this.getActivity().getFragmentManager();
          df.show(fm, "login-dialog");

          return false;
        }
      });

    }

    final Preference secret = findPreference(resources.getString(R.string.settings_alt_uris));

    try {
      final PackageInfo p_info = MainSettingsFragment.this.getActivity().getPackageManager().getPackageInfo(MainSettingsFragment.this.getActivity().getPackageName(), 0);
      final String version = p_info.versionName;
      secret.setTitle("Version: " + version);

    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }

    final BooksControllerConfigurationType books_config =
      app.getBooks().booksGetConfiguration();
    if (books_config.getAlternateRootFeedURI() != null) {
      final Bundle b = new Bundle();
      SimplifiedActivity.setActivityArguments(b, false);
      final Intent intent = new Intent();
      intent.setClass(
        MainSettingsFragment.this.getActivity(), AlternateFeedURIsActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
      intent.putExtras(b);

      secret.setTitle(resources.getString(R.string.settings_alt_uris));
      secret.setIntent(intent);
      secret.setOnPreferenceClickListener(null);

    } else {
      secret.setIntent(null);
      secret.setOnPreferenceClickListener(new OnMultipleClickListener() {

        @Override
        public boolean onMultipleClick(final Preference v) {


          final Bundle b = new Bundle();
          SimplifiedActivity.setActivityArguments(b, false);
          final Intent intent = new Intent();
          intent.setClass(
            MainSettingsFragment.this.getActivity(), AlternateFeedURIsActivity.class);
          intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
          intent.putExtras(b);

          secret.setTitle(resources.getString(R.string.settings_alt_uris));
          secret.setIntent(intent);

          final Toast toast = Toast.makeText(MainSettingsFragment.this.getActivity(), resources.getString(R.string.settings_alt_uris) + " activated", Toast.LENGTH_SHORT);
          toast.show();
          return true;
        }
      });

    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final Resources resources = NullCheck.notNull(this.getResources());
    final DocumentStoreType docs = app.getDocumentStore();
    final OptionType<HelpstackType> helpstack = app.getHelpStack();


    if (helpstack.isSome()) {
      final Intent help =
        new Intent(this.getActivity(), HelpActivity.class);
      final Preference preference = findPreference(resources.getString(R.string.help));
      preference.setIntent(help);

    }
    final PreferenceScreen screen = getPreferenceScreen();
    final Preference about_preference = findPreference(resources.getString(R.string.settings_about));
    screen.removePreference(about_preference);

    docs.getAbout().map_(
      new ProcedureType<SyncedDocumentType>() {
        @Override
        public void call(final SyncedDocumentType ack) {

          final Intent intent = new Intent(
            MainSettingsFragment.this.getActivity(), WebViewActivity.class);
          final Bundle b = new Bundle();
          WebViewActivity.setActivityArguments(
            b,
            ack.documentGetReadableURL().toString(),
            resources.getString(R.string.settings_about),
            SimplifiedPart.PART_SETTINGS);
          intent.putExtras(b);

          about_preference.setIntent(intent);
          screen.addPreference(about_preference);
        }

      });


    docs.getEULA().map_(
      new ProcedureType<EULAType>() {
        @Override
        public void call(final EULAType eula) {

          final Intent intent =
            new Intent(MainSettingsFragment.this.getActivity(), WebViewActivity.class);
          final Bundle b = new Bundle();
          WebViewActivity.setActivityArguments(
            b,
            eula.documentGetReadableURL().toString(),
            resources.getString(R.string.settings_eula),
            SimplifiedPart.PART_SETTINGS);
          intent.putExtras(b);


          final Preference preferences = findPreference(resources.getString(R.string.settings_eula));
          preferences.setIntent(intent);

        }
      });

    docs.getPrivacyPolicy().map_(
      new ProcedureType<SyncedDocumentType>() {
        @Override
        public void call(final SyncedDocumentType policy) {

          final Intent intent = new Intent(
            MainSettingsFragment.this.getActivity(), WebViewActivity.class);
          final Bundle b = new Bundle();
          WebViewActivity.setActivityArguments(
            b,
            policy.documentGetReadableURL().toString(),
            resources.getString(R.string.settings_privacy),
            SimplifiedPart.PART_SETTINGS);
          intent.putExtras(b);

          final Preference preferences = findPreference(resources.getString(R.string.settings_privacy));
          preferences.setIntent(intent);

        }
      });


    docs.getAcknowledgements().map_(
      new ProcedureType<SyncedDocumentType>() {
        @Override
        public void call(final SyncedDocumentType ack) {
          final Intent intent = new Intent(
            MainSettingsFragment.this.getActivity(), WebViewActivity.class);
          final Bundle b = new Bundle();
          WebViewActivity.setActivityArguments(
            b,
            ack.documentGetReadableURL().toString(),
            resources.getString(R.string.settings_credits),
            SimplifiedPart.PART_SETTINGS);
          intent.putExtras(b);

          final Preference preferences = findPreference(resources.getString(R.string.settings_credits));
          preferences.setIntent(intent);

        }
      });


  }

  @Override
  public void onLoginAborted() {
    // do nothing
  }

  @Override
  public void onLoginFailure(final OptionType<Throwable> error, final String message) {
    // do nothing
  }

  @Override
  public void onLoginSuccess(final AccountCredentials creds) {
    final Intent account =
      new Intent(this.getActivity(), MainSettingsAccountActivity.class);
    MainSettingsFragment.this.startActivity(account);
  }
}