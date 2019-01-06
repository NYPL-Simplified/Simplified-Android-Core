package org.nypl.simplified.app;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.View;
import android.widget.Toast;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.tenmiles.helpstack.HSHelpStack;
import com.tenmiles.helpstack.gears.HSDeskGear;

import org.nypl.simplified.app.testing.AlternateFeedURIsActivity;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.core.BooksControllerConfigurationType;
import org.nypl.simplified.books.core.DocumentStoreType;

public class MainSettingsFragment extends PreferenceFragmentCompat implements LoginListenerType {

  public MainSettingsFragment() {
    // Required empty public constructor
  }

  @Override
  public void onResume() {
    super.onResume();

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();

    final Resources resources = NullCheck.notNull(this.getResources());
    final Preference secret = findPreference(resources.getString(R.string.settings_alt_uris));

    try {
      final PackageInfo p_info = MainSettingsFragment.this.getActivity().getPackageManager().getPackageInfo(MainSettingsFragment.this.getActivity().getPackageName(), 0);
      final String version = p_info.versionName;
      secret.setTitle("Version: " + version + " (" + p_info.versionCode + ")");

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
  public void onViewCreated(final View view, final Bundle state) {
    super.onViewCreated(view, state);
    view.setBackgroundColor(getResources().getColor(R.color.light_background));
  }

  @Override
  public void onCreate(final Bundle saved_instance_state) {
    super.onCreate(saved_instance_state);

    addPreferencesFromResource(R.xml.preferences);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final Resources resources = NullCheck.notNull(this.getResources());
    final DocumentStoreType docs = app.getDocumentStore();
    final OptionType<HelpstackType> helpstack = app.getHelpStack();

    {
      final Preference preferences = findPreference(resources.getString(R.string.settings_accounts));
      preferences.setIntent(null);
      preferences.setOnPreferenceClickListener(preference -> {
        final Bundle b = new Bundle();
        SimplifiedActivity.setActivityArguments(b, false);
        final Intent intent = new Intent();
        intent.setClass(MainSettingsFragment.this.getActivity(), MainSettingsAccountsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtras(b);
        preferences.setIntent(intent);
        return false;
      });
    }

    {
      if (helpstack.isSome()) {
        final Preference preference = findPreference(resources.getString(R.string.help));
        preference.setIntent(null);
        preference.setOnPreferenceClickListener(pref -> {
          final HSHelpStack stack = HSHelpStack.getInstance(getActivity());
          final HSDeskGear gear =
            new HSDeskGear("https://nypl.desk.com/", "4GBRmMv8ZKG8fGehhA", "12060");
          stack.setGear(gear);
          stack.showHelp(getActivity());
          return false;
        });
      }
    }

    {
      final Intent intent = new Intent(
        MainSettingsFragment.this.getActivity(), WebViewActivity.class);
      final Bundle b = new Bundle();
      WebViewActivity.setActivityArguments(
        b,
        "http://www.librarysimplified.org/acknowledgments.html",
        resources.getString(R.string.settings_about),
        SimplifiedPart.PART_SETTINGS);
      intent.putExtras(b);
      intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

      final Preference preferences = findPreference(resources.getString(R.string.settings_about));
      preferences.setIntent(intent);
    }

    {
      final Intent intent =
        new Intent(MainSettingsFragment.this.getActivity(), WebViewActivity.class);
      final Bundle b = new Bundle();
      WebViewActivity.setActivityArguments(
        b,
        "http://www.librarysimplified.org/EULA.html",
        resources.getString(R.string.settings_eula),
        SimplifiedPart.PART_SETTINGS);
      intent.putExtras(b);
      intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

      final Preference preferences = findPreference(resources.getString(R.string.settings_eula));
      preferences.setIntent(intent);
    }

    {
      docs.getLicenses().map_(licenses -> {
          final Intent intent = new Intent(
            MainSettingsFragment.this.getActivity(), WebViewActivity.class);
          final Bundle b = new Bundle();
          WebViewActivity.setActivityArguments(
            b,
            licenses.documentGetReadableURL().toString(),
            resources.getString(R.string.settings_licence_software),
            SimplifiedPart.PART_SETTINGS);
          intent.putExtras(b);
          intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

          final Preference preferences = findPreference(resources.getString(R.string.settings_licence_software));
          preferences.setIntent(intent);
        });
    }
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
  public void onLoginSuccess(final AccountAuthenticationCredentials creds) {
    final Intent account =
      new Intent(this.getActivity(), MainSettingsAccountActivity.class);
    MainSettingsFragment.this.startActivity(account);
  }
}
