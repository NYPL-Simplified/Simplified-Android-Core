package org.nypl.simplified.app;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.tenmiles.helpstack.HSHelpStack;
import com.tenmiles.helpstack.gears.HSDeskGear;

import org.nypl.simplified.app.login.LoginDialog;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
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
import org.slf4j.LoggerFactory;

/**
 * The activity displaying the settings for the application.
 */

public final class MainSettingsAccountActivity extends SimplifiedActivity implements
  AccountLogoutListenerType,
  AccountGetCachedCredentialsListenerType, AccountSyncListenerType, DeviceActivationListenerType {

  private static final Logger LOG = LoggerFactory.getLogger(MainSettingsActivity.class);

  private Account account;
  @Nullable
  AnnotationsManager annotationsManager;

  private @Nullable
  TextView account_name_text;
  private @Nullable
  TextView account_subtitle_text;
  private @Nullable
  ImageView account_icon;

  private @Nullable
  TextView barcode_text;
  private @Nullable
  TextView pin_text;
  private @Nullable
  TableLayout table_with_code;
  private @Nullable
  TableLayout table_signup;
  private @Nullable
  ImageView barcode_image;
  private @Nullable
  TextView barcode_image_toggle;

  private @Nullable
  Switch sync_switch;
  private @Nullable
  LinearLayout sync_table_row;
  private @Nullable
  TableRow advanced_table_row;

  private @Nullable
  Button login;

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
  public void onAccountIsLoggedIn(final AccountCredentials creds) {
    LOG.debug("account is logged in: {}", creds);

    final SimplifiedCatalogAppServicesType app = Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    final Resources rr =
      NullCheck.notNull(this.getResources());
    final TableLayout in_table_with_code =
      NullCheck.notNull(this.table_with_code);
    final TableLayout in_table_signup =
      NullCheck.notNull(this.table_signup);
    final TextView in_account_name_text =
      NullCheck.notNull(this.account_name_text);
    final TextView in_account_subtitle_text =
      NullCheck.notNull(this.account_subtitle_text);
    final ImageView in_account_icon =
      NullCheck.notNull(this.account_icon);
    final TextView in_barcode_text =
      NullCheck.notNull(this.barcode_text);
    final TextView in_pin_text =
      NullCheck.notNull(this.pin_text);
    final ImageView in_barcode_image =
      NullCheck.notNull(this.barcode_image);
    final TextView in_barcode_image_toggle =
      NullCheck.notNull(this.barcode_image_toggle);
    final Button in_login =
      NullCheck.notNull(this.login);

    UIThread.runOnUIThread(() -> {
      in_table_with_code.setVisibility(View.VISIBLE);
      in_table_signup.setVisibility(View.GONE);
      in_account_name_text.setText(this.account.getName());
      in_account_subtitle_text.setText(this.account.getSubtitle());

      try {
        in_account_icon.setImageBitmap(this.account.getLogoBitmap());
      } catch (IllegalArgumentException e) {
        in_account_icon.setImageResource(R.drawable.librarylogomagic);
      }

      in_barcode_text.setText(creds.getBarcode().toString());
      in_barcode_text.setContentDescription(creds.getBarcode().toString().replaceAll(".(?=.)", "$0,"));
      in_pin_text.setText(creds.getPin().toString());
      in_pin_text.setContentDescription(creds.getPin().toString().replaceAll(".(?=.)", "$0,"));

      if (account.supportsBarcodeDisplay()) {
        Bitmap barcodeBitmap = generateBarcodeImage(creds.getBarcode().toString());
        if (barcodeBitmap != null) {
          in_barcode_image.setImageBitmap(barcodeBitmap);

          in_barcode_image_toggle.setVisibility(View.VISIBLE);
          in_barcode_image_toggle.setOnClickListener(v -> {
              if (in_barcode_image_toggle.getText() == getText(R.string.settings_toggle_barcode_show)) {
                in_barcode_image.setVisibility(View.VISIBLE);
                in_barcode_image_toggle.setText(R.string.settings_toggle_barcode_hide);
              } else {
                in_barcode_image.setVisibility(View.GONE);
                in_barcode_image_toggle.setText(R.string.settings_toggle_barcode_show);
              }
            }
          );
        }
      }

      in_login.setText(rr.getString(R.string.settings_log_out));
      in_login.setOnClickListener(v -> {
        final LogoutDialog d = LogoutDialog.newDialog();
        d.setOnConfirmListener(() -> books.accountLogout(creds, this, this, this));
        final FragmentManager fm =
          this.getFragmentManager();
        d.show(fm, "logout-confirm");
      });
    });
  }

  /**
   * Update the switch to match the server, or what the user has chosen on another device.
   * Disable user interaction with the Switch while network operations are taking place.
   *
   * @param account Library Account
   */
  public void checkServerSyncPermission(final BooksType account) {
    this.sync_switch.setEnabled(false);
    this.annotationsManager.requestServerSyncPermissionStatus(account, (enableSync) -> {
      if (enableSync) {
        final int accountID = this.account.getId();
        Simplified.getSharedPrefs().putBoolean("syncPermissionGranted", accountID, true);
      }
      this.sync_switch.setChecked(enableSync);
      this.sync_switch.setEnabled(true);
      return kotlin.Unit.INSTANCE;
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
    LOG.debug("onAccountLogoutFailure");
    LogUtilities.errorWithOptionalException(LOG, message, error);

    final Resources rr = NullCheck.notNull(this.getResources());
    final MainSettingsAccountActivity ctx = this;
    UIThread.runOnUIThread(() -> {
      final AlertDialog.Builder b = new AlertDialog.Builder(ctx);
      b.setNeutralButton("OK", null);
      b.setMessage(rr.getString(R.string.settings_logout_failed_server));
      b.setTitle(rr.getString(R.string.settings_logout_failed));
      b.setCancelable(true);
    });
  }

  @Override
  public void onAccountLogoutFailureServerError(final int code) {
    LOG.error("onAccountLoginFailureServerError: {}", code);

    final Resources rr = NullCheck.notNull(this.getResources());
    final OptionType<Throwable> none = Option.none();
    this.onAccountLogoutFailure(none, rr.getString(R.string.settings_logout_failed_server));
  }

  @Override
  public void onAccountLogoutSuccess() {
    LOG.debug("onAccountLogoutSuccess");
    this.onAccountIsNotLoggedIn();

    this.annotationsManager = null;

    //if current account ??
    final SimplifiedCatalogAppServicesType app = Simplified.getCatalogAppServices();
    app.getBooks().destroyBookStatusCache();

    Simplified.getCatalogAppServices().reloadCatalog(true, this.account);
    final Resources rr = NullCheck.notNull(this.getResources());
    final Context context = this.getApplicationContext();
    final CharSequence text =
      NullCheck.notNull(rr.getString(R.string.settings_logout_succeeded));
    final int duration = Toast.LENGTH_SHORT;

    final TextView bt = NullCheck.notNull(this.barcode_text);
    final TextView pt = NullCheck.notNull(this.pin_text);

    UIThread.runOnUIThread(() -> {
      bt.setVisibility(View.GONE);
      pt.setVisibility(View.GONE);

      final Toast toast = Toast.makeText(context, text, duration);
      toast.show();
      finish();
      overridePendingTransition(0, 0);
      startActivity(getIntent());
      overridePendingTransition(0, 0);
    });

// logout clever

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

      CookieManager.getInstance().removeAllCookies(null);
      CookieManager.getInstance().flush();
    } else {
      final CookieSyncManager cookie_sync_manager = CookieSyncManager.createInstance(this);
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
        this.findViewById(R.id.settings_reveal_password));

      if (result_code == RESULT_OK) {
        final TextView in_pin_text = NullCheck.notNull(this.findViewById(R.id.settings_pin_text));
        in_pin_text.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
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
      final Intent eula_intent = new Intent(this, MainEULAActivity.class);
      if (this.account.getEula() != null) {
        final Bundle b = new Bundle();
        MainEULAActivity.setActivityArguments(b, this.account.getEula());
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
    } else {
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

    final TextView in_barcode_label =
      NullCheck.notNull(this.findViewById(R.id.settings_barcode_label));
    final TextView in_barcode_text =
      NullCheck.notNull(this.findViewById(R.id.settings_barcode_text));
    final TextView in_pin_label =
      NullCheck.notNull(this.findViewById(R.id.settings_pin_label));
    final ImageView in_barcode_image =
      NullCheck.notNull(this.findViewById(R.id.settings_barcode_image));
    final TextView in_barcode_image_toggle =
      NullCheck.notNull(this.findViewById(R.id.settings_barcode_toggle_barcode));
    final TextView in_pin_text =
      NullCheck.notNull(this.findViewById(R.id.settings_pin_text));
    final CheckBox in_pin_reveal =
      NullCheck.notNull(this.findViewById(R.id.settings_reveal_password));

    if (!this.account.pinRequired()) {
      in_pin_label.setVisibility(View.INVISIBLE);
      in_pin_text.setVisibility(View.INVISIBLE);
      in_pin_reveal.setVisibility(View.INVISIBLE);
    }

    final Button in_login =
      NullCheck.notNull(this.findViewById(R.id.settings_login));
    final Button in_signup =
      NullCheck.notNull(this.findViewById(R.id.settings_signup));

    this.sync_switch = findViewById(R.id.sync_switch);
    this.sync_table_row = findViewById(R.id.sync_table_row);
    this.sync_table_row.setVisibility(View.GONE);
    this.advanced_table_row = findViewById(R.id.link_advanced);
    this.advanced_table_row.setVisibility(View.GONE);

    this.advanced_table_row.setOnClickListener(view -> {
      final FragmentManager mgr = getFragmentManager();
      final FragmentTransaction transaction = mgr.beginTransaction();
      final SettingsAccountAdvancedFragment fragment = new SettingsAccountAdvancedFragment();
      transaction.add(R.id.settings_account_container, fragment).addToBackStack("advanced").commit();
    });

    final TableRow in_privacy =
      findViewById(R.id.link_privacy);
    final TableRow in_license =
      findViewById(R.id.link_license);

    final TextView account_name = NullCheck.notNull(
      this.findViewById(android.R.id.text1));
    final TextView account_subtitle = NullCheck.notNull(
      this.findViewById(android.R.id.text2));

    final ImageView in_account_icon = NullCheck.notNull(
      this.findViewById(R.id.account_icon));

    in_pin_text.setTransformationMethod(
      PasswordTransformationMethod.getInstance());
    if (android.os.Build.VERSION.SDK_INT >= 21) {
      this.handle_pin_reveal(in_pin_text, in_pin_reveal);
    } else {
      in_pin_reveal.setVisibility(View.GONE);
    }

    final TableRow in_report_issue =
      findViewById(R.id.report_issue);

    if (this.account.getSupportEmail() == null) {
      in_report_issue.setVisibility(View.GONE);
    } else {
      in_report_issue.setVisibility(View.VISIBLE);
      in_report_issue.setOnClickListener(view -> {
        final Intent intent = new Intent(this, ReportIssueActivity.class);
        final Bundle b = new Bundle();
        b.putInt("selected_account", this.account.getId());
        intent.putExtras(b);
        startActivity(intent);
      });
    }

    final TableRow in_support_center =
      findViewById(R.id.support_center);
    if (this.account.supportsHelpCenter()) {
      in_support_center.setVisibility(View.VISIBLE);
      in_support_center.setOnClickListener(view -> {
        final HSHelpStack stack = HSHelpStack.getInstance(this);
        final HSDeskGear gear =
          new HSDeskGear("https://nypl.desk.com/", "4GBRmMv8ZKG8fGehhA", null);
        stack.setGear(gear);
        stack.showHelp(this);
      });
    } else {
      in_support_center.setVisibility(View.GONE);
    }

    //Get labels from the current authentication document.
    final AuthenticationDocumentType auth_doc = docs.getAuthenticationDocument();
    in_barcode_label.setText(auth_doc.getLabelLoginUserID());
    in_pin_label.setText(auth_doc.getLabelLoginPassword());

    final TableLayout in_table_with_code =
      NullCheck.notNull(this.findViewById(R.id.settings_login_table_with_code));
    in_table_with_code.setVisibility(View.GONE);
    final TableLayout in_table_signup =
      NullCheck.notNull(this.findViewById(R.id.settings_signup_table));

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
    } else {
      in_table_signup.setVisibility(View.GONE);
    }

    in_login.setOnClickListener(v -> this.onLoginWithBarcode());

    final CheckBox in_age13_checkbox = NullCheck.notNull(this.findViewById(R.id.age13_checkbox));

    if (Simplified.getSharedPrefs().contains("age13")) {
      in_age13_checkbox.setChecked(Simplified.getSharedPrefs().getBoolean("age13"));
    } else if (account.getId() == 2) {
      showAgeGateOptionsDialog(in_age13_checkbox);
    }

    in_age13_checkbox.setOnCheckedChangeListener(this::showAgeChangeConfirmation);

    if (this.account.needsAuth()) {
      in_login.setVisibility(View.VISIBLE);
      in_age13_checkbox.setVisibility(View.GONE);
    } else {
      in_login.setVisibility(View.GONE);
      in_age13_checkbox.setVisibility(View.VISIBLE);
    }

    if (this.account.supportsCardCreator()) {
      in_signup.setOnClickListener(v -> {
        final Intent cardcreator = new Intent(this, CardCreatorActivity.class);
        startActivity(cardcreator);
      });
      in_signup.setText(R.string.need_card_button);

    } else if (this.account.getCardCreatorUrl() != null) {
      in_signup.setOnClickListener(v -> {
        final Intent e_card = new Intent(Intent.ACTION_VIEW);
        e_card.setData(Uri.parse(this.account.getCardCreatorUrl()));
        startActivity(e_card);
      });
      in_signup.setText(R.string.need_card_button);
    }

    final boolean permission = Simplified.getSharedPrefs().getBoolean("syncPermissionGranted", this.account.getId());
    this.sync_switch.setChecked(permission);

    /*
    If switching on, disable user interaction until server has responded.
    If switching off, disable applicable network requests by updating shared prefs flags.
     */
    this.sync_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if (isChecked) {
        buttonView.setEnabled(false);
        annotationsManager.updateServerSyncPermissionStatus(true, (success) -> {
          if (success) {
            Simplified.getSharedPrefs().putBoolean("syncPermissionGranted", this.account.getId(), true);
            this.sync_switch.setChecked(true);
          } else {
            Simplified.getSharedPrefs().putBoolean("syncPermissionGranted", this.account.getId(), false);
            this.sync_switch.setChecked(false);
          }
          this.sync_switch.setEnabled(true);
          return kotlin.Unit.INSTANCE;
        });
      } else {
        Simplified.getSharedPrefs().putBoolean("syncPermissionGranted", this.account.getId(), false);
        this.sync_switch.setChecked(false);
      }
    });

    if (this.account.getPrivacyPolicy() != null) {
      in_privacy.setVisibility(View.VISIBLE);
    } else {
      in_privacy.setVisibility(View.GONE);
    }
    if (this.account.getContentLicense() != null) {
      in_license.setVisibility(View.VISIBLE);
    } else {
      in_license.setVisibility(View.GONE);
    }

    in_license.setOnClickListener(view -> {
      final Intent intent = new Intent(this, WebViewActivity.class);
      final Bundle b = new Bundle();
      WebViewActivity.setActivityArguments(
        b,
        this.account.getContentLicense(),
        "Content Licenses",
        SimplifiedPart.PART_SETTINGS);
      intent.putExtras(b);
      intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
      startActivity(intent);
    });

    in_privacy.setOnClickListener(view -> {
      final Intent intent =
        new Intent(this, WebViewActivity.class);
      final Bundle b = new Bundle();
      WebViewActivity.setActivityArguments(
        b,
        this.account.getPrivacyPolicy(),
        "Privacy Policy",
        SimplifiedPart.PART_SETTINGS);
      intent.putExtras(b);
      intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
      startActivity(intent);
    });

    this.navigationDrawerSetActionBarTitle();

    this.account_name_text = account_name;
    this.account_subtitle_text = account_subtitle;
    this.account_icon = in_account_icon;
    this.barcode_text = in_barcode_text;
    this.pin_text = in_pin_text;
    this.barcode_image_toggle = in_barcode_image_toggle;
    this.barcode_image = in_barcode_image;
    this.login = in_login;
    this.table_with_code = in_table_with_code;
    this.table_signup = in_table_signup;

    final CheckBox in_eula_checkbox =
      NullCheck.notNull(this.findViewById(R.id.eula_checkbox));

    final OptionType<EULAType> eula_opt = docs.getEULA();

    if (eula_opt.isSome()) {
      final Some<EULAType> some_eula = (Some<EULAType>) eula_opt;
      final EULAType eula = some_eula.get();

      in_eula_checkbox.setChecked(eula.eulaHasAgreed());
      in_eula_checkbox.setEnabled(true);
      in_eula_checkbox.setOnCheckedChangeListener((button, checked) -> eula.eulaSetHasAgreed(checked));

      if (eula.eulaHasAgreed()) {
        LOG.debug("EULA: agreed");
      } else {
        LOG.debug("EULA: not agreed");
      }
    } else {
      LOG.debug("EULA: unavailable");
    }

    this.getWindow().setSoftInputMode(
      WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
  }

  private void showAgeGateOptionsDialog(CheckBox age13_box) {
    final AlertDialog.Builder builder = new AlertDialog.Builder(this);

    builder.setTitle(R.string.age_verification_title);
    builder.setMessage(R.string.age_verification_changed);

    // Under 13
    builder.setNeutralButton(R.string.age_verification_13_younger, (dialog, which) -> {
      Simplified.getSharedPrefs().putBoolean("age13", false);
      age13_box.setChecked(false);
      setSimplyCollectionCatalog(true);
    });

    // 13 and Over
    builder.setPositiveButton(R.string.age_verification_13_older, (dialog, which) -> {
      Simplified.getSharedPrefs().putBoolean("age13", true);
      age13_box.setChecked(true);
      setSimplyCollectionCatalog(false);
    });

    AlertDialog alert = builder.show();
    final int resID = ThemeMatcher.Companion.color(this.account.getMainColor());
    final int mainTextColor = ContextCompat.getColor(this, resID);
    alert.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(mainTextColor);
    alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(mainTextColor);
  }

  private void showAgeChangeConfirmation(CompoundButton button, boolean checked) {
    if (checked) {
      Simplified.getSharedPrefs().putBoolean("age13", true);
      setSimplyCollectionCatalog(false);
    } else {
      final AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(R.string.age_verification_confirm_title);
      builder.setMessage(R.string.age_verification_confirm_under13_check);

      builder.setNegativeButton(R.string.catalog_cancel_downloading, (dialog, which) -> {
        Simplified.getSharedPrefs().putBoolean("age13", true);
        button.setChecked(true);
      });

      //Confirm Under 13
      builder.setPositiveButton(R.string.catalog_book_delete, (dialog, which) -> {
        Simplified.getSharedPrefs().putBoolean("age13", false);
        button.setChecked(false);
        setSimplyCollectionCatalog(true);
      });

      AlertDialog alert = builder.show();
      final int resID = ThemeMatcher.Companion.color(this.account.getMainColor());
      final int mainTextColor = ContextCompat.getColor(this, resID);
      alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(mainTextColor);
      alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(mainTextColor);
    }
  }

  private void setSimplyCollectionCatalog(final Boolean deleteBooks) {
    Simplified.getCatalogAppServices().reloadCatalog(deleteBooks, this.account);
    Simplified.getBooks(this.account, this, Simplified.getCatalogAppServices().getAdobeDRMExecutor());
  }

  private boolean syncButtonShouldBeVisible() {
    return (this.account.supportsSimplyESync() && this.account.getId() == Simplified.getCurrentAccount().getId());
  }

  @Override
  public void onBackPressed() {
    //Pop any Fragments if they exist in the navigation stack.
    final FragmentManager manager = getFragmentManager();
    if (manager.getBackStackEntryCount() > 0) {
      manager.popBackStackImmediate();
      return;
    }
    super.onBackPressed();
  }

  /**
   *
   */
  public void onLoginWithBarcode() {

    final LoginListenerType login_listener = new LoginListenerType() {
      @Override
      public void onLoginAborted() {
        LOG.trace("feed auth: aborted login");
//        listener.onAuthenticationNotProvided();
      }

      @Override
      public void onLoginFailure(
        final OptionType<Throwable> error,
        final String message) {
        LogUtilities.errorWithOptionalException(LOG, "failed login", error);
//        listener.onAuthenticationError(error, message);
      }

      @Override
      public void onLoginSuccess(
        final AccountAuthenticationCredentials creds) {
        LOG.trace("feed auth: login supplied new credentials");
//        LoginActivity.this.openCatalog();

        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);
      }
    };


    final FragmentManager fm = this.getFragmentManager();
    UIThread.runOnUIThread(() -> {
      final AccountBarcode barcode = new AccountBarcode("");
      final AccountPIN pin = new AccountPIN("");

      if (Simplified.getCurrentAccount().getId() == this.account.getId()) {
        final LoginDialog df = LoginDialog.newDialog("Login required", barcode, pin);
        df.setLoginListener(login_listener);
        df.show(fm, "login-dialog");
      } else {
        final LoginDialog df = LoginDialog.newDialog("Login required", barcode, pin, this.account);
        df.setLoginListener(login_listener);
        df.show(fm, "login-dialog");
      }
    });
  }

  @TargetApi(21)
  private void handle_pin_reveal(final TextView in_pin_text, final CheckBox in_pin_reveal) {
    /*
     * Add a listener that reveals/hides the password field.
     */
    in_pin_reveal.setOnCheckedChangeListener((view, checked) -> {
      if (checked) {
        final KeyguardManager keyguard_manager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (!keyguard_manager.isKeyguardSecure()) {
          // Show a message that the user hasn't set up a lock screen.
          Toast.makeText(this, R.string.settings_screen_Lock_not_setup,
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
    });
  }

  @Override
  protected void onResume() {
    super.onResume();

    final Resources rr =
      NullCheck.notNull(this.getResources());
    final TableLayout in_table_with_code =
      NullCheck.notNull(this.table_with_code);
    final TableLayout in_table_signup =
      NullCheck.notNull(this.table_signup);
    final TextView in_account_name_text =
      NullCheck.notNull(this.account_name_text);
    final TextView in_account_subtitle_text =
      NullCheck.notNull(this.account_subtitle_text);
    final ImageView in_account_icon =
      NullCheck.notNull(this.account_icon);
    final TextView in_barcode_text =
      NullCheck.notNull(this.barcode_text);
    final TextView in_pin_text =
      NullCheck.notNull(this.pin_text);
    final ImageView in_barcode_image =
      NullCheck.notNull(this.barcode_image);
    final TextView in_barcode_image_toggle =
      NullCheck.notNull(this.barcode_image_toggle);
    final Button in_login =
      NullCheck.notNull(this.login);
    final CheckBox in_eula_checkbox =
      NullCheck.notNull(this.findViewById(R.id.eula_checkbox));

    in_account_name_text.setText(this.account.getName());
    in_account_subtitle_text.setText(this.account.getSubtitle());

    try {
      in_account_icon.setImageBitmap(this.account.getLogoBitmap());
    } catch (IllegalArgumentException e) {
      in_account_icon.setImageResource(R.drawable.librarylogomagic);
    }

    final BooksType userAccount;
    if (this.account == null) {
      userAccount = Simplified.getCatalogAppServices().getBooks();
    } else {
      userAccount = Simplified.getBooks(this.account, this, Simplified.getCatalogAppServices().getAdobeDRMExecutor());
    }

    final AccountsDatabaseType accounts_database = Simplified.getAccountsDatabase(this.account, this);
    if (!accounts_database.accountGetCredentials().isSome()) {
      this.sync_table_row.setVisibility(View.GONE);
      LOG.debug("No user currently signed in, bypassing UI update and Sync Status Initiation.");
      return;
    }

    final AccountCredentials creds = ((Some<AccountCredentials>) accounts_database.accountGetCredentials()).get();

    if (syncButtonShouldBeVisible()) {
      if (this.annotationsManager == null) {
        this.annotationsManager = new AnnotationsManager(this.account, creds, this);
        checkServerSyncPermission(userAccount);
      }
      this.sync_table_row.setVisibility(View.VISIBLE);
      this.advanced_table_row.setVisibility(View.VISIBLE);
    } else {
      this.sync_table_row.setVisibility(View.GONE);
      this.advanced_table_row.setVisibility(View.GONE);
    }

    if (account.supportsBarcodeDisplay()) {
      Bitmap barcodeBitmap = generateBarcodeImage(creds.getBarcode().toString());
      if (barcodeBitmap != null) {
        in_barcode_image.setImageBitmap(barcodeBitmap);

        in_barcode_image_toggle.setVisibility(View.VISIBLE);
        in_barcode_image_toggle.setOnClickListener(view -> {
          if (in_barcode_image_toggle.getText() == getText(R.string.settings_toggle_barcode_show)) {
            in_barcode_image.setVisibility(View.VISIBLE);
            in_barcode_image_toggle.setText(R.string.settings_toggle_barcode_hide);
          } else {
            in_barcode_image.setVisibility(View.GONE);
            in_barcode_image_toggle.setText(R.string.settings_toggle_barcode_show);
          }
        });
      }
    }

    in_table_with_code.setVisibility(View.VISIBLE);
    in_table_signup.setVisibility(View.GONE);

    in_barcode_text.setText(creds.getBarcode().toString());
    in_barcode_text.setContentDescription(creds.getBarcode().toString().replaceAll(".(?=.)", "$0,"));
    in_pin_text.setText(creds.getPin().toString());
    in_pin_text.setContentDescription(creds.getPin().toString().replaceAll(".(?=.)", "$0,"));

    in_eula_checkbox.setEnabled(false);

    in_login.setText(rr.getString(R.string.settings_log_out));
    in_login.setOnClickListener(view -> {
      final LogoutDialog dialog = LogoutDialog.newDialog();
      dialog.setOnConfirmListener(() -> {
        //Delete cache if logging out of current active library account
        userAccount.accountLogout(creds, this, this, this);
        if (this.account == Simplified.getCurrentAccount()) {
          userAccount.destroyBookStatusCache();
        }
      });
      final FragmentManager fm = this.getFragmentManager();
      dialog.show(fm, "logout-confirm");
    });
  }

  private Bitmap generateBarcodeImage(String barcodeString) {
    try {
      MultiFormatWriter multiFormatWriter = new MultiFormatWriter();

      BitMatrix bitMatrix = multiFormatWriter.encode(barcodeString, BarcodeFormat.CODABAR, 2800, 500);
      BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
      Bitmap barcodeBitmap = barcodeEncoder.createBitmap(bitMatrix);

      return barcodeBitmap;
    } catch (Exception e) {
      LOG.error("Error generating barcode image: {}", e.getLocalizedMessage());
      return null;
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
