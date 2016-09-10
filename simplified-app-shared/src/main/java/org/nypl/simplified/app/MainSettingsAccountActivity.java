package org.nypl.simplified.app;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.AccountAuthProvider;
import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.AccountGetCachedCredentialsListenerType;
import org.nypl.simplified.books.core.AccountLogoutListenerType;
import org.nypl.simplified.books.core.AccountPatron;
import org.nypl.simplified.books.core.AuthenticationDocumentType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.DocumentStoreType;
import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

/**
 * The activity displaying the settings for the application.
 */

public final class MainSettingsAccountActivity extends SimplifiedActivity implements
  AccountLogoutListenerType,
  AccountGetCachedCredentialsListenerType {
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(MainSettingsActivity.class);
  }

  private @Nullable TextView barcode_text;
  private @Nullable TextView pin_text;
  private @Nullable TextView name_text;
  private @Nullable TextView provider_text;
  private @Nullable TextView table_device_title;

  private @Nullable TableLayout table_with_code;
  private @Nullable TableLayout table_with_token;

  private @Nullable Button login;
  private @Nullable Button activate;

  /**
   * Construct an activity.
   */

  public MainSettingsAccountActivity() {

  }

  @Override
  protected SimplifiedPart navigationDrawerGetPart() {
    return SimplifiedPart.PART_ACCOUNT;
  }

  @Override
  protected boolean navigationDrawerShouldShowIndicator() {
    return true;
  }

  @Override
  public void onAccountIsLoggedIn(
    final AccountCredentials creds) {
    MainSettingsAccountActivity.LOG.debug("account is logged in: {}", creds);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    final Resources rr = NullCheck.notNull(this.getResources());

    final TextView in_barcode_text = NullCheck.notNull(this.barcode_text);
    final TextView in_pin_text = NullCheck.notNull(this.pin_text);
    final TextView in_name_text = NullCheck.notNull(this.name_text);
    final TextView in_provider_text = NullCheck.notNull(this.provider_text);

    final TextView in_table_device_title = NullCheck.notNull(this.table_device_title);
    final TableLayout in_table_with_code = NullCheck.notNull(this.table_with_code);
    final TableLayout in_table_with_token = NullCheck.notNull(this.table_with_token);

    final Button in_login = NullCheck.notNull(this.login);
    final Button in_activate = NullCheck.notNull(this.activate);


    UIThread.runOnUIThread(
      new Runnable() {
        @Override
        public void run() {
          
          if (creds.getProvider().isNone())
          {
            creds.setProvider(Option.some(new AccountAuthProvider(rr.getString(R.string.feature_default_auth_provider_name))));
            in_provider_text.setText(rr.getString(R.string.feature_default_auth_provider_name));
          }
          else
          {
            in_provider_text.setText(((Some<AccountAuthProvider>) creds.getProvider()).get().toString());
          }

          if ("Clever".equals(((Some<AccountAuthProvider>) creds.getProvider()).get().toString())) {
            in_table_with_code.setVisibility(View.GONE);
            in_table_with_token.setVisibility(View.VISIBLE);
            try {

              final JSONObject name = new JSONObject(((Some<AccountPatron>) creds.getPatron()).get().toString()).getJSONObject("name");
              in_name_text.setText(name.getString("first") + " " + name.getString("middle") + " " + name.getString("last"));

            } catch (JSONException e) {
              e.printStackTrace();
            }
          } else {
            in_table_with_code.setVisibility(View.VISIBLE);
            in_table_with_token.setVisibility(View.GONE);
            in_barcode_text.setText(creds.getBarcode().toString());
            in_barcode_text.setContentDescription(creds.getBarcode().toString().replaceAll(".(?=.)", "$0,"));
            in_pin_text.setText(creds.getPin().toString());
            in_pin_text.setContentDescription(creds.getPin().toString().replaceAll(".(?=.)", "$0,"));
          }

          in_login.setEnabled(true);
          in_login.setText(rr.getString(R.string.settings_log_out));
          in_login.setOnClickListener(
            new OnClickListener() {
              @Override
              public void onClick(
                final @Nullable View v) {
                final LogoutDialog d = LogoutDialog.newDialog();
                d.setOnConfirmListener(
                  new Runnable() {
                    @Override
                    public void run() {
                      books.accountLogout(creds, MainSettingsAccountActivity.this);
                    }
                  });
                final FragmentManager fm =
                  MainSettingsAccountActivity.this.getFragmentManager();
                d.show(fm, "logout-confirm");
              }
            });


          in_activate.setEnabled(true);
          if (books.accountIsDeviceActive()) {
            in_activate.setText(rr.getString(R.string.settings_deactivate_this_device));
          } else {
            in_activate.setText(rr.getString(R.string.settings_activate_this_device));
          }
          in_activate.setOnClickListener(
            new OnClickListener() {
              @Override
              public void onClick(
                final @Nullable View v) {
                if (books.accountIsDeviceActive()) {
                  //deactivate account with adobe
                  books.accountDeActivateDevice();
                  in_activate.setText(rr.getString(R.string.settings_activate_this_device));

                } else {
                  books.accountActivateDevice();
                  in_activate.setText(rr.getString(R.string.settings_deactivate_this_device));
                  in_activate.setVisibility(View.GONE);
                  in_table_device_title.setVisibility(View.GONE);
                }
              }
            });

        }
      });
  }

  @Override
  public void onAccountIsNotLoggedIn() {
    /*
    do nothing
     */
  }

  @Override
  public void onAccountLogoutFailure(
    final OptionType<Throwable> error,
    final String message) {
    MainSettingsAccountActivity.LOG.debug("onAccountLogoutFailure");
    LogUtilities.errorWithOptionalException(
      MainSettingsAccountActivity.LOG, message, error);

    final Resources rr = NullCheck.notNull(this.getResources());
    final MainSettingsAccountActivity ctx = this;
    UIThread.runOnUIThread(
      new Runnable() {
        @Override
        public void run() {
          final AlertDialog.Builder b = new AlertDialog.Builder(ctx);
          b.setNeutralButton("OK", null);
          b.setMessage(rr.getString(R.string.settings_logout_failed_server));
          b.setTitle(rr.getString(R.string.settings_logout_failed));
          b.setCancelable(true);

          final AlertDialog a = b.create();
          a.setOnDismissListener(
            new OnDismissListener() {
              @Override
              public void onDismiss(
                final @Nullable DialogInterface d) {
                final Button in_login = NullCheck.notNull(MainSettingsAccountActivity.this.login);
                in_login.setEnabled(true);

              }
            });
          a.show();
        }
      });
  }

  @Override
  public void onAccountLogoutFailureServerError(final int code) {
    MainSettingsAccountActivity.LOG.error(
      "onAccountLoginFailureServerError: {}", code);

    final Resources rr = NullCheck.notNull(this.getResources());
    final OptionType<Throwable> none = Option.none();
    this.onAccountLogoutFailure(
      none, rr.getString(R.string.settings_logout_failed_server));

  }

  @Override
  public void onAccountLogoutSuccess() {
    MainSettingsAccountActivity.LOG.debug("onAccountLogoutSuccess");
    this.onAccountIsNotLoggedIn();

    final Resources rr = NullCheck.notNull(this.getResources());
    final Context context = MainSettingsAccountActivity.this.getApplicationContext();
    final CharSequence text =
      NullCheck.notNull(rr.getString(R.string.settings_logout_succeeded));
    final int duration = Toast.LENGTH_SHORT;

    final TextView bt = NullCheck.notNull(this.barcode_text);
    final TextView pt = NullCheck.notNull(this.pin_text);
    final TextView nt = NullCheck.notNull(this.name_text);

    UIThread.runOnUIThread(
      new Runnable() {
        @Override
        public void run() {

          bt.setVisibility(View.GONE);
          pt.setVisibility(View.GONE);
          nt.setVisibility(View.GONE);

          final Toast toast = Toast.makeText(context, text, duration);
          toast.show();
          MainSettingsAccountActivity.this.finish();

        }
      });


// logout clever

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

      CookieManager.getInstance().removeAllCookies(null);
      CookieManager.getInstance().flush();
    } else {
      final CookieSyncManager cookie_sync_manager = CookieSyncManager.createInstance(MainSettingsAccountActivity.this);
      cookie_sync_manager.startSync();
      final CookieManager cookie_manager = CookieManager.getInstance();
      cookie_manager.removeAllCookie();
      cookie_manager.removeSessionCookie();
      cookie_sync_manager.stopSync();
      cookie_sync_manager.sync();
    }


  }

  @Override
  protected void onActivityResult(final int request_code, final int result_code, final Intent data) {
    if (request_code == 1) {
      // Challenge completed, proceed with using cipher
      final CheckBox in_pin_reveal = NullCheck.notNull(
        (CheckBox) this.findViewById(R.id.settings_reveal_password));

      if (result_code == RESULT_OK) {

        final TextView in_pin_text =
          NullCheck.notNull((TextView) this.findViewById(R.id.settings_pin_text));

        in_pin_text.setTransformationMethod(
          HideReturnsTransformationMethod.getInstance());
        in_pin_reveal.setChecked(true);
      } else {
        // The user canceled or didn't complete the lock screen
        // operation. Go to error/cancellation flow.
        in_pin_reveal.setChecked(false);
      }
    }
  }

  @Override
  public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn) {
    final MenuItem item = NullCheck.notNull(item_mn);
    switch (item.getItemId()) {

      case android.R.id.home: {
        onBackPressed();
        return true;
      }

      default: {
        return super.onOptionsItemSelected(item);
      }
    }
  }


  @Override
  protected void onCreate(
    final @Nullable Bundle state) {
    super.onCreate(state);


    final ActionBar bar = this.getActionBar();
    if (android.os.Build.VERSION.SDK_INT < 21) {
      bar.setDisplayHomeAsUpEnabled(false);
      bar.setHomeButtonEnabled(true);
      bar.setIcon(R.drawable.ic_arrow_back);
    } else {
      bar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
      bar.setDisplayHomeAsUpEnabled(true);
      bar.setHomeButtonEnabled(false);
    }


    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final DocumentStoreType docs = app.getDocumentStore();

    final LayoutInflater inflater = NullCheck.notNull(this.getLayoutInflater());

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(R.layout.settings_account, content_area, false));
    content_area.addView(layout);
    content_area.requestLayout();

    final TextView in_barcode_label = NullCheck.notNull(
      (TextView) this.findViewById(R.id.settings_barcode_label));

    final TextView in_barcode_text = NullCheck.notNull(
      (TextView) this.findViewById(R.id.settings_barcode_text));

    final TextView in_pin_label = NullCheck.notNull(
      (TextView) this.findViewById(R.id.settings_pin_label));

    final TextView in_pin_text =
      NullCheck.notNull((TextView) this.findViewById(R.id.settings_pin_text));

    final CheckBox in_pin_reveal = NullCheck.notNull(
      (CheckBox) this.findViewById(R.id.settings_reveal_password));


    final TextView in_name_label = NullCheck.notNull(
      (TextView) this.findViewById(R.id.settings_name_label));

    final TextView in_name_text = NullCheck.notNull(
      (TextView) this.findViewById(R.id.settings_name_text));


    final Button in_login =
      NullCheck.notNull((Button) this.findViewById(R.id.settings_login));

    final TextView in_table_device_title = NullCheck.notNull(
      (TextView) this.findViewById(R.id.settings_device_title));

    final Button in_activate =
      NullCheck.notNull((Button) this.findViewById(R.id.settings_activate_this_device));

    final TextView in_provider_text = NullCheck.notNull(
      (TextView) this.findViewById(R.id.settings_provider_name_text));

    in_pin_text.setTransformationMethod(
      PasswordTransformationMethod.getInstance());
    if (android.os.Build.VERSION.SDK_INT >= 21) {
      this.handle_pin_reveal(in_pin_text, in_pin_reveal);
    } else {
      in_pin_reveal.setVisibility(View.GONE);
    }

    /**
     * Get labels from the current authentication document.
     */

    final AuthenticationDocumentType auth_doc =
      docs.getAuthenticationDocument();
    in_barcode_label.setText(auth_doc.getLabelLoginUserID());
    in_pin_label.setText(auth_doc.getLabelLoginPassword());
    in_name_label.setText(auth_doc.getLabelLoginPatronName());


    final TableLayout in_table_with_code =
      NullCheck.notNull((TableLayout) this.findViewById(R.id.settings_login_table_with_code));
    final TableLayout in_table_with_token =
      NullCheck.notNull((TableLayout) this.findViewById(R.id.settings_login_table_with_token));


    in_table_with_code.setVisibility(View.GONE);
    in_table_with_token.setVisibility(View.GONE);

    in_login.setEnabled(false);
    in_activate.setEnabled(false);


    this.navigationDrawerSetActionBarTitle();
    this.provider_text = in_provider_text;
    this.barcode_text = in_barcode_text;
    this.pin_text = in_pin_text;
    this.name_text = in_name_text;
    this.login = in_login;
    this.activate = in_activate;
    this.table_device_title = in_table_device_title;
    this.table_with_code = in_table_with_code;
    this.table_with_token = in_table_with_token;

    this.getWindow().setSoftInputMode(
      WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
  }

  @TargetApi(21)
  private void handle_pin_reveal(final TextView in_pin_text, final CheckBox in_pin_reveal) {
    /**
     * Add a listener that reveals/hides the password field.
     */
    in_pin_reveal.setOnCheckedChangeListener(
      new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(
          final CompoundButton view,
          final boolean checked) {
          if (checked) {
            final KeyguardManager keyguard_manager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (!keyguard_manager.isKeyguardSecure()) {
              // Show a message that the user hasn't set up a lock screen.
              Toast.makeText(MainSettingsAccountActivity.this, R.string.settings_screen_Lock_not_setup,
                Toast.LENGTH_LONG).show();
              in_pin_reveal.setChecked(false);
            } else {
              final Intent intent = keyguard_manager.createConfirmDeviceCredentialIntent(null, null);
              if (intent != null) {
                startActivityForResult(intent, 1);
              }
            }
          } else {
            in_pin_text.setTransformationMethod(
              PasswordTransformationMethod.getInstance());
          }
        }
      });
  }

  @Override
  protected void onResume() {
    super.onResume();

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    books.accountGetCachedLoginDetails(this);
    final Resources rr = NullCheck.notNull(this.getResources());

    final Button in_activate = NullCheck.notNull(this.activate);
    final TextView in_table_device_title = NullCheck.notNull(this.table_device_title);
    if (books.accountIsDeviceActive()) {
      in_activate.setText(rr.getString(R.string.settings_deactivate_this_device));
      in_activate.setVisibility(View.GONE);
      in_table_device_title.setVisibility(View.GONE);
    } else {
      in_activate.setText(rr.getString(R.string.settings_activate_this_device));
    }
  }
}
