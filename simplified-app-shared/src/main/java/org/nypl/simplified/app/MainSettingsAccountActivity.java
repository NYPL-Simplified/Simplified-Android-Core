package org.nypl.simplified.app;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.tenmiles.helpstack.HSHelpStack;
import com.tenmiles.helpstack.gears.HSDeskGear;

import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.AccountGetCachedCredentialsListenerType;
import org.nypl.simplified.books.core.AccountLogoutListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.AccountSyncListenerType;
import org.nypl.simplified.books.core.AccountsDatabaseType;
import org.nypl.simplified.books.core.AuthenticationDocumentType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.DeviceActivationListenerType;
import org.nypl.simplified.books.core.DocumentStoreType;
import org.nypl.simplified.books.core.EULAType;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.multilibrary.Account;
import org.nypl.simplified.multilibrary.AccountsRegistry;
import org.slf4j.Logger;

/**
 * The activity displaying the settings for the application.
 */

public final class MainSettingsAccountActivity extends SimplifiedActivity implements
  AccountLogoutListenerType,
  AccountGetCachedCredentialsListenerType, AccountSyncListenerType, DeviceActivationListenerType {
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(MainSettingsActivity.class);
  }

  private @Nullable TextView account_name_text;
  private @Nullable TextView account_subtitle_text;
  private @Nullable ImageView account_icon;

  private @Nullable TextView barcode_text;
  private @Nullable TextView pin_text;
  private @Nullable TableLayout table_with_code;
  private @Nullable TableLayout table_signup;

  private @Nullable Button login;

  private Account account;

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


    final TableLayout in_table_with_code = NullCheck.notNull(this.table_with_code);
    final TableLayout in_table_signup = NullCheck.notNull(this.table_signup);

    final TextView in_account_name_text = NullCheck.notNull(this.account_name_text);
    final TextView in_account_subtitle_text = NullCheck.notNull(this.account_subtitle_text);
    final ImageView in_account_icon = NullCheck.notNull(this.account_icon);

    final TextView in_barcode_text = NullCheck.notNull(this.barcode_text);
    final TextView in_pin_text = NullCheck.notNull(this.pin_text);
    final Button in_login = NullCheck.notNull(this.login);

    UIThread.runOnUIThread(
      new Runnable() {
        @Override
        public void run() {

          in_table_with_code.setVisibility(View.VISIBLE);
          in_table_signup.setVisibility(View.GONE);
          in_account_name_text.setText(MainSettingsAccountActivity.this.account.getName());
          in_account_subtitle_text.setText(MainSettingsAccountActivity.this.account.getSubtitle());
          if (MainSettingsAccountActivity.this.account.getId() == 0) {
            in_account_icon.setImageResource(R.drawable.account_logo_nypl);
          } else if (MainSettingsAccountActivity.this.account.getId() == 1) {
            in_account_icon.setImageResource(R.drawable.account_logo_bpl);
          } else if (MainSettingsAccountActivity.this.account.getId() == 2) {
            in_account_icon.setImageResource(R.drawable.account_logo_instant);
          } else if (MainSettingsAccountActivity.this.account.getId() == 7) {
            in_account_icon.setImageResource(R.drawable.account_logo_alameda);
          } else if (MainSettingsAccountActivity.this.account.getId() == 8) {
            in_account_icon.setImageResource(R.drawable.account_logo_hcls);
          } else if (MainSettingsAccountActivity.this.account.getId() == 9) {
            in_account_icon.setImageResource(R.drawable.account_logo_mcpl);
          } else if (MainSettingsAccountActivity.this.account.getId() == 10) {
            in_account_icon.setImageResource(R.drawable.account_logo_fcpl);
          } else if (MainSettingsAccountActivity.this.account.getId() == 11) {
            in_account_icon.setImageResource(R.drawable.account_logo_anne_arundel);
          } else if (MainSettingsAccountActivity.this.account.getId() == 12) {
            in_account_icon.setImageResource(R.drawable.account_logo_bgc);
          } else if (MainSettingsAccountActivity.this.account.getId() == 13) {
            in_account_icon.setImageResource(R.drawable.account_logo_smcl);
          } else if (MainSettingsAccountActivity.this.account.getId() == 14) {
            in_account_icon.setImageResource(R.drawable.account_logo_cl);
          } else if (MainSettingsAccountActivity.this.account.getId() == 15) {
            in_account_icon.setImageResource(R.drawable.account_logo_ccpl);
          } else if (MainSettingsAccountActivity.this.account.getId() == 16) {
            in_account_icon.setImageResource(R.drawable.account_logo_ccl);
          } else if (MainSettingsAccountActivity.this.account.getId() == 17) {
            in_account_icon.setImageResource(R.drawable.account_logo_bcl);
          } else if (MainSettingsAccountActivity.this.account.getId() == 18) {
            in_account_icon.setImageResource(R.drawable.account_logo_lapl);
          } else if (MainSettingsAccountActivity.this.account.getId() == 19) {
            in_account_icon.setImageResource(R.drawable.account_logo_pcl);
          } else if (MainSettingsAccountActivity.this.account.getId() == 20) {
            in_account_icon.setImageResource(R.drawable.account_logo_sccl);
          } else if (MainSettingsAccountActivity.this.account.getId() == 21) {
            in_account_icon.setImageResource(R.drawable.account_logo_acls);
          } else if (MainSettingsAccountActivity.this.account.getId() == 22) {
            in_account_icon.setImageResource(R.drawable.account_logo_rel);
          } else if (MainSettingsAccountActivity.this.account.getId() == 23) {
            in_account_icon.setImageResource(R.drawable.account_logo_wcfl);
          }

          in_barcode_text.setText(creds.getBarcode().toString());
          in_barcode_text.setContentDescription(creds.getBarcode().toString().replaceAll(".(?=.)", "$0,"));
          in_pin_text.setText(creds.getPin().toString());
          in_pin_text.setContentDescription(creds.getPin().toString().replaceAll(".(?=.)", "$0,"));

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
                      books.accountLogout(creds, MainSettingsAccountActivity.this, MainSettingsAccountActivity.this, MainSettingsAccountActivity.this);
                    }
                  });
                final FragmentManager fm =
                  MainSettingsAccountActivity.this.getFragmentManager();
                d.show(fm, "logout-confirm");
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

    //if current account ??
      final SimplifiedCatalogAppServicesType app =
        Simplified.getCatalogAppServices();

      app.getBooks().destroyBookStatusCache();

    Simplified.getCatalogAppServices().reloadCatalog(true, MainSettingsAccountActivity.this.account);
    final Resources rr = NullCheck.notNull(this.getResources());
    final Context context = MainSettingsAccountActivity.this.getApplicationContext();
    final CharSequence text =
      NullCheck.notNull(rr.getString(R.string.settings_logout_succeeded));
    final int duration = Toast.LENGTH_SHORT;

    final TextView bt = NullCheck.notNull(this.barcode_text);
    final TextView pt = NullCheck.notNull(this.pin_text);

    UIThread.runOnUIThread(
      new Runnable() {
        @Override
        public void run() {

          bt.setVisibility(View.GONE);
          pt.setVisibility(View.GONE);

          final Toast toast = Toast.makeText(context, text, duration);
          toast.show();
          finish();
          overridePendingTransition(0, 0);
          startActivity(getIntent());
          overridePendingTransition(0, 0);

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

    if (item.getItemId() == R.id.show_eula) {

      final Intent eula_intent = new Intent(MainSettingsAccountActivity.this, MainEULAActivity.class);

      if (this.account.getEula() != null) {
        final Bundle b = new Bundle();
        MainEULAActivity.setActivityArguments(
          b,
          this.account.getEula());
        eula_intent.putExtras(b);
      }
      eula_intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
      startActivity(eula_intent);

      return true;
    }

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


    final Bundle extras = getIntent().getExtras();
    if (extras != null) {
      this.account = new AccountsRegistry(this).getAccount(extras.getInt("selected_account"));
    }
    else
    {
      this.account = Simplified.getCurrentAccount();
    }


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


    final DocumentStoreType docs = Simplified.getDocumentStore(this.account, getResources());

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

    final Button in_login =
      NullCheck.notNull((Button) this.findViewById(R.id.settings_login));

    final Button in_signup =
      NullCheck.notNull((Button) this.findViewById(R.id.settings_signup));


    final TableRow in_privacy =
      (TableRow) findViewById(R.id.link_privacy);
    final TableRow in_license =
      (TableRow) findViewById(R.id.link_license);

    final TextView account_name = NullCheck.notNull(
      (TextView) this.findViewById(android.R.id.text1));
    final TextView account_subtitle = NullCheck.notNull(
      (TextView) this.findViewById(android.R.id.text2));

    final ImageView in_account_icon = NullCheck.notNull(
      (ImageView) this.findViewById(R.id.account_icon));

    in_pin_text.setTransformationMethod(
      PasswordTransformationMethod.getInstance());
    if (android.os.Build.VERSION.SDK_INT >= 21) {
      this.handle_pin_reveal(in_pin_text, in_pin_reveal);
    } else {
      in_pin_reveal.setVisibility(View.GONE);
    }

    final TableRow in_report_issue =
      (TableRow) findViewById(R.id.report_issue);

    if (this.account.getSupportEmail() == null)
    {

      in_report_issue.setVisibility(View.GONE);

    }
    else
    {
      in_report_issue.setVisibility(View.VISIBLE);
      in_report_issue.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(final View view) {

          final Intent intent =
            new Intent(MainSettingsAccountActivity.this, ReportIssueActivity.class);
          final Bundle b = new Bundle();
          b.putInt("selected_account", MainSettingsAccountActivity.this.account.getId());
          intent.putExtras(b);
          startActivity(intent);

        }
      });

    }

    final TableRow in_support_center =
      (TableRow) findViewById(R.id.support_center);
    if (this.account.supportsHelpCenter())
    {
      in_support_center.setVisibility(View.VISIBLE);
      in_support_center.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(final View view) {

          final HSHelpStack stack = HSHelpStack.getInstance(MainSettingsAccountActivity.this);

          final HSDeskGear gear =
            new HSDeskGear(" ", " ", null);
          stack.setGear(gear);

          stack.showHelp(MainSettingsAccountActivity.this);

        }
      });
    }
    else
    {
      in_support_center.setVisibility(View.GONE);
    }

     //Get labels from the current authentication document.
    final AuthenticationDocumentType auth_doc =
      docs.getAuthenticationDocument();
    in_barcode_label.setText(auth_doc.getLabelLoginUserID());
    in_pin_label.setText(auth_doc.getLabelLoginPassword());


    final TableLayout in_table_with_code =
      NullCheck.notNull((TableLayout) this.findViewById(R.id.settings_login_table_with_code));
    in_table_with_code.setVisibility(View.GONE);
    final TableLayout in_table_signup =
      NullCheck.notNull((TableLayout) this.findViewById(R.id.settings_signup_table));


//    boolean locationpermission = false;
//    if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//      locationpermission = true;
//    }
//    else
//    {
//      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//    }

    if (this.account.supportsCardCreator() || this.account.getCardCreatorUrl() != null) {
      in_table_signup.setVisibility(View.VISIBLE);
    }
    else {
      in_table_signup.setVisibility(View.GONE);
    }

    in_login.setOnClickListener(
      new OnClickListener() {
        @Override
        public void onClick(
          final @Nullable View v) {

          MainSettingsAccountActivity.this.onLoginWithBarcode();

        }
      });

    final CheckBox in_age13_checkbox =
      NullCheck.notNull((CheckBox) this.findViewById(R.id.age13_checkbox));

    // check if key exists, if doesn't ask user how old they are, move this to catalog activity

    if (Simplified.getSharedPrefs().contains("age13")) {
      in_age13_checkbox.setChecked(Simplified.getSharedPrefs().getBoolean("age13"));
    }

    in_age13_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(final CompoundButton button, final boolean checked) {

        if (checked)
        {
            Simplified.getSharedPrefs().putBoolean("age13", true);
            Simplified.getCatalogAppServices().reloadCatalog(false, MainSettingsAccountActivity.this.account);
        }
        else {
          UIThread.runOnUIThread(
            new Runnable() {
              @Override
              public void run() {

                final AlertDialog.Builder alert = new AlertDialog.Builder(MainSettingsAccountActivity.this);

                // Setting Dialog Title
                alert.setTitle(R.string.age_verification_title);

                // Setting Dialog Message
                alert.setMessage(R.string.age_verification_changed);

                // On pressing the under 13 button.
                alert.setNeutralButton(R.string.age_verification_13_younger, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int which) {
                      Simplified.getSharedPrefs().putBoolean("age13", false);
                      Simplified.getCatalogAppServices().reloadCatalog(true, MainSettingsAccountActivity.this.account);
                    }
                  }
                );

                // On pressing the 13 and over button
                alert.setPositiveButton(R.string.age_verification_13_older, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int which) {
                      Simplified.getSharedPrefs().putBoolean("age13", true);
                      in_age13_checkbox.setChecked(Simplified.getSharedPrefs().getBoolean("age13"));
                    }
                  }
                );

                // Showing Alert Message
                alert.show();

              }
            });
        }
      }
    });


    if (this.account.needsAuth()) {
      in_login.setVisibility(View.VISIBLE);
      in_age13_checkbox.setVisibility(View.GONE);
    }
    else {
      in_login.setVisibility(View.GONE);
      // show age checkbox
      in_age13_checkbox.setVisibility(View.VISIBLE);
    }

    if (this.account.supportsCardCreator()) {

      in_signup.setOnClickListener(
        new OnClickListener() {
          @Override
          public void onClick(
            final @Nullable View v) {
            final Intent cardcreator = new Intent(MainSettingsAccountActivity.this, CardCreatorActivity.class);
            startActivity(cardcreator);
          }
        });
      in_signup.setText(R.string.need_card_button);

    }
    else if (this.account.getCardCreatorUrl() != null)
    {

      in_signup.setOnClickListener(
        new OnClickListener() {
          @Override
          public void onClick(
            final @Nullable View v) {

            final Intent  e_card = new Intent(Intent.ACTION_VIEW);
            e_card.setData(Uri.parse(MainSettingsAccountActivity.this.account.getCardCreatorUrl()));
            startActivity(e_card);

          }
        });
      in_signup.setText(R.string.need_card_button);

    }

    if (this.account.getPrivacyPolicy() != null) {
      in_privacy.setVisibility(View.VISIBLE);
    }
    else {
      in_privacy.setVisibility(View.GONE);
    }
    if (this.account.getContentLicense() != null) {
      in_license.setVisibility(View.VISIBLE);
    }
    else {
      in_license.setVisibility(View.GONE);
    }

    in_license.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(final View view) {

        final Intent intent =
          new Intent(MainSettingsAccountActivity.this, WebViewActivity.class);
        final Bundle b = new Bundle();
        WebViewActivity.setActivityArguments(
          b,
          MainSettingsAccountActivity.this.account.getContentLicense(),
          "Content Licenses",
          SimplifiedPart.PART_SETTINGS);
        intent.putExtras(b);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);

      }
    });

    in_privacy.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(final View view) {

        final Intent intent =
          new Intent(MainSettingsAccountActivity.this, WebViewActivity.class);
        final Bundle b = new Bundle();
        WebViewActivity.setActivityArguments(
          b,
          MainSettingsAccountActivity.this.account.getPrivacyPolicy(),
          "Privacy Policy",
          SimplifiedPart.PART_SETTINGS);
        intent.putExtras(b);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);

      }
    });




    this.navigationDrawerSetActionBarTitle();

    this.account_name_text = account_name;
    this.account_subtitle_text = account_subtitle;
    this.account_icon = in_account_icon;
    this.barcode_text = in_barcode_text;
    this.pin_text = in_pin_text;
    this.login = in_login;
    this.table_with_code = in_table_with_code;
    this.table_signup = in_table_signup;

    final CheckBox in_eula_checkbox =
      NullCheck.notNull((CheckBox) this.findViewById(R.id.eula_checkbox));


    final OptionType<EULAType> eula_opt = docs.getEULA();

    if (eula_opt.isSome()) {
      final Some<EULAType> some_eula = (Some<EULAType>) eula_opt;
      final EULAType eula = some_eula.get();


      in_eula_checkbox.setChecked(eula.eulaHasAgreed());
      in_eula_checkbox.setEnabled(true);

      in_eula_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final CompoundButton button, final boolean checked) {

          eula.eulaSetHasAgreed(checked);

        }
      });

      if (eula.eulaHasAgreed()) {
        MainSettingsAccountActivity.LOG.debug("EULA: agreed");

      } else {
        MainSettingsAccountActivity.LOG.debug("EULA: not agreed");

      }
    } else {
      MainSettingsAccountActivity.LOG.debug("EULA: unavailable");
    }




    this.getWindow().setSoftInputMode(
      WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
  }

  /**
   *
   */
  public void onLoginWithBarcode() {


    final LoginListenerType login_listener = new LoginListenerType() {
      @Override
      public void onLoginAborted() {
        MainSettingsAccountActivity.LOG.trace("feed auth: aborted login");
//        listener.onAuthenticationNotProvided();
      }

      @Override
      public void onLoginFailure(
        final OptionType<Throwable> error,
        final String message) {
        LogUtilities.errorWithOptionalException(
          MainSettingsAccountActivity.LOG, "failed login", error);
//        listener.onAuthenticationError(error, message);
      }

      @Override
      public void onLoginSuccess(
        final AccountCredentials creds) {
        MainSettingsAccountActivity.LOG.trace(
          "feed auth: login supplied new credentials");
//        LoginActivity.this.openCatalog();

        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);

      }
    };


    final FragmentManager fm = this.getFragmentManager();
    UIThread.runOnUIThread(
      new Runnable() {
        @Override
        public void run() {
          final AccountBarcode barcode = new AccountBarcode("");
          final AccountPIN pin = new AccountPIN("");

          if (Simplified.getCurrentAccount().getId() == MainSettingsAccountActivity.this.account.getId())
          {
            final LoginDialog df =
              LoginDialog.newDialog("Login required", barcode, pin);
            df.setLoginListener(login_listener);
            df.show(fm, "login-dialog");
          }
          else {
            final LoginDialog df =
              LoginDialog.newDialog("Login required", barcode, pin, MainSettingsAccountActivity.this.account);
            df.setLoginListener(login_listener);
            df.show(fm, "login-dialog");
          }
        }
      });

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


    BooksType books = app.getBooks();
    if (this.account != null)
    {
      books = Simplified.getBooks(this.account, this, Simplified.getCatalogAppServices().getAdobeDRMExecutor());
    }

    final Resources rr = NullCheck.notNull(this.getResources());
    final TableLayout in_table_with_code = NullCheck.notNull(this.table_with_code);
    final TableLayout in_table_signup = NullCheck.notNull(this.table_signup);

    final TextView in_account_name_text = NullCheck.notNull(this.account_name_text);
    final TextView in_account_subtitle_text = NullCheck.notNull(this.account_subtitle_text);
    final ImageView in_account_icon = NullCheck.notNull(this.account_icon);

    final TextView in_barcode_text = NullCheck.notNull(this.barcode_text);
    final TextView in_pin_text = NullCheck.notNull(this.pin_text);
    final Button in_login = NullCheck.notNull(this.login);
    final CheckBox in_eula_checkbox =
      NullCheck.notNull((CheckBox) this.findViewById(R.id.eula_checkbox));

    in_account_name_text.setText(MainSettingsAccountActivity.this.account.getName());
    in_account_subtitle_text.setText(MainSettingsAccountActivity.this.account.getSubtitle());

    if (MainSettingsAccountActivity.this.account.getId() == 0) {
      in_account_icon.setImageResource(R.drawable.account_logo_nypl);
    } else if (MainSettingsAccountActivity.this.account.getId() == 1) {
      in_account_icon.setImageResource(R.drawable.account_logo_bpl);
    } else if (MainSettingsAccountActivity.this.account.getId() == 2) {
      in_account_icon.setImageResource(R.drawable.account_logo_instant);
    } else if (MainSettingsAccountActivity.this.account.getId() == 7) {
      in_account_icon.setImageResource(R.drawable.account_logo_alameda);
    } else if (MainSettingsAccountActivity.this.account.getId() == 8) {
      in_account_icon.setImageResource(R.drawable.account_logo_hcls);
    } else if (MainSettingsAccountActivity.this.account.getId() == 9) {
      in_account_icon.setImageResource(R.drawable.account_logo_mcpl);
    } else if (MainSettingsAccountActivity.this.account.getId() == 10) {
      in_account_icon.setImageResource(R.drawable.account_logo_fcpl);
    } else if (MainSettingsAccountActivity.this.account.getId() == 11) {
      in_account_icon.setImageResource(R.drawable.account_logo_anne_arundel);
    } else if (MainSettingsAccountActivity.this.account.getId() == 12) {
      in_account_icon.setImageResource(R.drawable.account_logo_bgc);
    } else if (MainSettingsAccountActivity.this.account.getId() == 13) {
      in_account_icon.setImageResource(R.drawable.account_logo_smcl);
    } else if (MainSettingsAccountActivity.this.account.getId() == 14) {
      in_account_icon.setImageResource(R.drawable.account_logo_cl);
    } else if (MainSettingsAccountActivity.this.account.getId() == 15) {
      in_account_icon.setImageResource(R.drawable.account_logo_ccpl);
    } else if (MainSettingsAccountActivity.this.account.getId() == 16) {
      in_account_icon.setImageResource(R.drawable.account_logo_ccl);
    } else if (MainSettingsAccountActivity.this.account.getId() == 17) {
      in_account_icon.setImageResource(R.drawable.account_logo_bcl);
    } else if (MainSettingsAccountActivity.this.account.getId() == 18) {
      in_account_icon.setImageResource(R.drawable.account_logo_lapl);
    } else if (MainSettingsAccountActivity.this.account.getId() == 19) {
      in_account_icon.setImageResource(R.drawable.account_logo_pcl);
    } else if (MainSettingsAccountActivity.this.account.getId() == 20) {
      in_account_icon.setImageResource(R.drawable.account_logo_sccl);
    } else if (MainSettingsAccountActivity.this.account.getId() == 21) {
      in_account_icon.setImageResource(R.drawable.account_logo_acls);
    } else if (MainSettingsAccountActivity.this.account.getId() == 22) {
      in_account_icon.setImageResource(R.drawable.account_logo_rel);
    } else if (MainSettingsAccountActivity.this.account.getId() == 23) {
      in_account_icon.setImageResource(R.drawable.account_logo_wcfl);
    }


    final AccountsDatabaseType accounts_database  = Simplified.getAccountsDatabase(this.account, this);
    if (accounts_database.accountGetCredentials().isSome()) {
      final AccountCredentials creds = ((Some<AccountCredentials>) accounts_database.accountGetCredentials()).get();

      final BooksType final_books = books;
      UIThread.runOnUIThread(
        new Runnable() {
          @Override
          public void run() {

            in_table_with_code.setVisibility(View.VISIBLE);
            in_table_signup.setVisibility(View.GONE);

            in_barcode_text.setText(creds.getBarcode().toString());
            in_barcode_text.setContentDescription(creds.getBarcode().toString().replaceAll(".(?=.)", "$0,"));
            in_pin_text.setText(creds.getPin().toString());
            in_pin_text.setContentDescription(creds.getPin().toString().replaceAll(".(?=.)", "$0,"));

            in_eula_checkbox.setEnabled(false);

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
                        //if current account
                        final_books.accountLogout(creds, MainSettingsAccountActivity.this, MainSettingsAccountActivity.this, MainSettingsAccountActivity.this);
                        if (MainSettingsAccountActivity.this.account == Simplified.getCurrentAccount()) {
                          final_books.destroyBookStatusCache();
                        }
                      }
                    });
                  final FragmentManager fm =
                    MainSettingsAccountActivity.this.getFragmentManager();
                  d.show(fm, "logout-confirm");
                }
              });

          }
        });
    }

  }


  @Override
  public boolean onCreateOptionsMenu(
    final @Nullable Menu in_menu) {

    final Menu menu_nn = NullCheck.notNull(in_menu);
    final MenuInflater inflater = this.getMenuInflater();
    inflater.inflate(R.menu.eula, menu_nn);

    return true;
  }

  @Override
  public void onAccountSyncAuthenticationFailure(final String message) {

  }

  @Override
  public void onAccountSyncBook(final BookID book) {

  }

  @Override
  public void onAccountSyncFailure(final OptionType<Throwable> error, final String message) {

  }

  @Override
  public void onAccountSyncSuccess() {

  }

  @Override
  public void onAccountSyncBookDeleted(final BookID book) {

  }

  @Override
  public void onDeviceActivationFailure(final String message) {
    // do nothing
  }

  @Override
  public void onDeviceActivationSuccess() {
    // do nothing
  }
}
