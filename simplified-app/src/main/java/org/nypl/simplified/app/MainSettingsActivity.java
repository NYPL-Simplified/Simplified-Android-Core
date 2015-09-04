package org.nypl.simplified.app;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.ProcedureType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.testing.AlternateFeedURIsActivity;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountGetCachedCredentialsListenerType;
import org.nypl.simplified.books.core.AccountLoginListenerType;
import org.nypl.simplified.books.core.AccountLogoutListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.AccountSyncListenerType;
import org.nypl.simplified.books.core.AuthenticationDocumentType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.DocumentStoreType;
import org.nypl.simplified.books.core.EULAType;
import org.nypl.simplified.books.core.SyncedDocumentType;
import org.slf4j.Logger;

/**
 * The activity displaying the settings for the application.
 */

public final class MainSettingsActivity extends SimplifiedActivity implements
  AccountLogoutListenerType,
  AccountLoginListenerType,
  AccountGetCachedCredentialsListenerType,
  AccountSyncListenerType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(MainSettingsActivity.class);
  }

  private @Nullable EditText barcode_edit;
  private @Nullable Button   login;
  private @Nullable EditText pin_edit;

  /**
   * Construct an activity.
   */

  public MainSettingsActivity()
  {

  }

  private static void editableDisable(
    final EditText e)
  {
    e.setEnabled(false);
    e.setClickable(false);
    e.setCursorVisible(false);
    e.setFocusable(false);
    e.setFocusableInTouchMode(false);
  }

  private static void editableEnable(
    final EditText e)
  {
    e.setEnabled(true);
    e.setClickable(true);
    e.setCursorVisible(true);
    e.setFocusable(true);
    e.setFocusableInTouchMode(true);
  }

  @Override protected SimplifiedPart navigationDrawerGetPart()
  {
    return SimplifiedPart.PART_SETTINGS;
  }

  @Override protected boolean navigationDrawerShouldShowIndicator()
  {
    return true;
  }

  @Override public void onAccountIsLoggedIn(
    final AccountBarcode barcode,
    final AccountPIN pin)
  {
    MainSettingsActivity.LOG.debug("account is logged in: {}", barcode);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    final Resources rr = NullCheck.notNull(this.getResources());
    final EditText in_barcode_edit = NullCheck.notNull(this.barcode_edit);
    final EditText in_pin_edit = NullCheck.notNull(this.pin_edit);
    final Button in_login = NullCheck.notNull(this.login);

    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          in_pin_edit.setText(pin.toString());
          in_barcode_edit.setText(barcode.toString());
          MainSettingsActivity.editableDisable(in_barcode_edit);
          MainSettingsActivity.editableDisable(in_pin_edit);

          in_login.setEnabled(true);
          in_login.setText(rr.getString(R.string.settings_log_out));
          in_login.setOnClickListener(
            new OnClickListener()
            {
              @Override public void onClick(
                final @Nullable View v)
              {
                final LogoutDialog d = LogoutDialog.newDialog();
                d.setOnConfirmListener(
                  new Runnable()
                  {
                    @Override public void run()
                    {
                      in_login.setEnabled(false);
                      MainSettingsActivity.editableDisable(in_pin_edit);
                      MainSettingsActivity.editableDisable(in_barcode_edit);
                      books.accountLogout(MainSettingsActivity.this);
                    }
                  });
                final FragmentManager fm =
                  MainSettingsActivity.this.getFragmentManager();
                d.show(fm, "logout-confirm");
              }
            });
        }
      });
  }

  @Override public void onAccountIsNotLoggedIn()
  {
    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    final Resources rr = NullCheck.notNull(this.getResources());
    final EditText in_barcode_edit = NullCheck.notNull(this.barcode_edit);
    final EditText in_pin_edit = NullCheck.notNull(this.pin_edit);
    final Button in_login = NullCheck.notNull(this.login);

    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          MainSettingsActivity.editableEnable(in_barcode_edit);
          MainSettingsActivity.editableEnable(in_pin_edit);

          MainSettingsActivity.this.enableLoginIfFieldsNonEmpty(
            in_login, in_pin_edit, in_barcode_edit);

          in_login.setText(rr.getString(R.string.settings_log_in));
          in_login.setOnClickListener(
            new OnClickListener()
            {
              @Override public void onClick(
                final @Nullable View v)
              {
                in_login.setEnabled(false);
                MainSettingsActivity.editableDisable(in_pin_edit);
                MainSettingsActivity.editableDisable(in_barcode_edit);

                final Editable barcode_text = in_barcode_edit.getText();
                final AccountBarcode barcode = new AccountBarcode(
                  NullCheck.notNull(
                    barcode_text.toString()));
                final Editable pin_text = in_pin_edit.getText();
                final AccountPIN pin =
                  new AccountPIN(NullCheck.notNull(pin_text.toString()));
                books.accountLogin(barcode, pin, MainSettingsActivity.this);
              }
            });
        }
      });
  }

  private void onAccountLoginFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    MainSettingsActivity.LOG.debug("onAccountLoginFailure");
    LogUtilities.errorWithOptionalException(
      MainSettingsActivity.LOG, message, error);

    final Resources rr = NullCheck.notNull(this.getResources());
    final MainSettingsActivity ctx = this;
    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          final AlertDialog.Builder b = new AlertDialog.Builder(ctx);
          b.setNeutralButton("OK", null);
          b.setMessage(message);
          b.setTitle(rr.getString(R.string.settings_login_failed));
          b.setCancelable(true);

          final AlertDialog a = b.create();
          a.setOnDismissListener(
            new OnDismissListener()
            {
              @Override public void onDismiss(
                final @Nullable DialogInterface d)
              {
                MainSettingsActivity.this.onAccountIsNotLoggedIn();
              }
            });
          a.show();
        }
      });
  }

  @Override public void onAccountLoginFailureCredentialsIncorrect()
  {
    MainSettingsActivity.LOG.error("onAccountLoginFailureCredentialsIncorrect");

    final Resources rr = NullCheck.notNull(this.getResources());
    final OptionType<Throwable> none = Option.none();
    this.onAccountLoginFailure(
      none, rr.getString(R.string.settings_login_failed_credentials));
  }

  @Override public void onAccountLoginFailureServerError(final int code)
  {
    MainSettingsActivity.LOG.error(
      "onAccountLoginFailureServerError: {}", Integer.valueOf(code));

    final Resources rr = NullCheck.notNull(this.getResources());
    final OptionType<Throwable> none = Option.none();
    this.onAccountLoginFailure(
      none, rr.getString(R.string.settings_login_failed_server));
  }

  @Override public void onAccountLoginFailureLocalError(
    final OptionType<Throwable> error,
    final String message)
  {
    MainSettingsActivity.LOG.error(
      "onAccountLoginFailureLocalError: {}", message);

    final Resources rr = NullCheck.notNull(this.getResources());
    this.onAccountLoginFailure(
      error, rr.getString(R.string.settings_login_failed_server));
  }

  @Override public void onAccountLoginSuccess(
    final AccountBarcode barcode,
    final AccountPIN pin)
  {
    MainSettingsActivity.LOG.debug("account login succeeded: {}", barcode);
    this.onAccountIsLoggedIn(barcode, pin);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    final Resources rr = NullCheck.notNull(this.getResources());
    final Context context = MainSettingsActivity.this.getApplicationContext();
    final CharSequence text =
      NullCheck.notNull(rr.getString(R.string.settings_login_succeeded));
    final int duration = Toast.LENGTH_SHORT;

    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          final Toast toast = Toast.makeText(context, text, duration);
          toast.show();
          books.accountSync(MainSettingsActivity.this);
        }
      });
  }

  @Override
  public void onAccountLoginFailureDeviceActivationError(final String message)
  {
    MainSettingsActivity.LOG.error(
      "onAccountLoginFailureDeviceActivationError: {}", message);

    final Resources rr = NullCheck.notNull(this.getResources());
    final OptionType<Throwable> none = Option.none();
    this.onAccountLoginFailure(
      none, LoginDialog.getDeviceActivationErrorMessage(rr, message));
  }

  @Override public void onAccountLogoutFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    MainSettingsActivity.LOG.debug("onAccountLogoutFailure");
    LogUtilities.errorWithOptionalException(
      MainSettingsActivity.LOG, message, error);
  }

  @Override public void onAccountLogoutSuccess()
  {
    MainSettingsActivity.LOG.debug("onAccountLogoutSuccess");
    this.onAccountIsNotLoggedIn();

    final Resources rr = NullCheck.notNull(this.getResources());
    final Context context = MainSettingsActivity.this.getApplicationContext();
    final CharSequence text =
      NullCheck.notNull(rr.getString(R.string.settings_logout_succeeded));
    final int duration = Toast.LENGTH_SHORT;

    final EditText be = NullCheck.notNull(this.barcode_edit);
    final EditText pe = NullCheck.notNull(this.pin_edit);

    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          MainSettingsActivity.editableEnable(be);
          MainSettingsActivity.editableEnable(pe);
          be.setText("");
          pe.setText("");

          final Toast toast = Toast.makeText(context, text, duration);
          toast.show();
        }
      });
  }

  @Override public void onAccountSyncAuthenticationFailure(
    final String message)
  {
    MainSettingsActivity.LOG.error("failed to sync account: {}", message);
  }

  @Override public void onAccountSyncBook(
    final BookID book)
  {
    MainSettingsActivity.LOG.debug("synced book: {}", book);
  }

  @Override public void onAccountSyncFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    LogUtilities.errorWithOptionalException(
      MainSettingsActivity.LOG, message, error);
  }

  @Override public void onAccountSyncSuccess()
  {
    MainSettingsActivity.LOG.debug("completed sync");
  }

  @Override public void onAccountSyncBookDeleted(final BookID book)
  {
    MainSettingsActivity.LOG.debug("deleted book: {}", book);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final DocumentStoreType docs = app.getDocumentStore();

    final LayoutInflater inflater = NullCheck.notNull(this.getLayoutInflater());
    final Resources resources = NullCheck.notNull(this.getResources());

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(R.layout.settings, content_area, false));
    content_area.addView(layout);
    content_area.requestLayout();

    final TextView in_barcode_label = NullCheck.notNull(
      (TextView) this.findViewById(R.id.settings_barcode_label));
    final EditText in_barcode_edit = NullCheck.notNull(
      (EditText) this.findViewById(R.id.settings_barcode_edit));

    final TextView in_pin_label = NullCheck.notNull(
      (TextView) this.findViewById(R.id.settings_pin_label));
    final EditText in_pin_edit =
      NullCheck.notNull((EditText) this.findViewById(R.id.settings_pin_edit));

    final Button in_login =
      NullCheck.notNull((Button) this.findViewById(R.id.settings_login));
    final TextView in_adobe_accounts = NullCheck.notNull(
      (TextView) this.findViewById(R.id.settings_adobe_accounts));

    /**
     * If Adobe DRM support is available, then enable the accounts
     * management section. This is not yet implemented and is therefore
     * currently always inactive.
     */

    in_adobe_accounts.setEnabled(false);

    /**
     * Get labels from the current authentication document.
     */

    final AuthenticationDocumentType auth_doc =
      docs.getAuthenticationDocument();
    in_barcode_label.setText(auth_doc.getLabelLoginUserID());
    in_pin_label.setText(auth_doc.getLabelLoginPassword());

    /**
     * If an EULA is defined, configure the EULA to open a web view displaying
     * the policy on click. Otherwise, disable the text.
     */

    final TextView in_eula =
      NullCheck.notNull((TextView) this.findViewById(R.id.settings_eula));
    in_eula.setEnabled(false);

    docs.getEULA().map_(
      new ProcedureType<EULAType>()
      {
        @Override public void call(final EULAType eula)
        {
          in_eula.setEnabled(true);
          in_eula.setOnClickListener(
            new OnClickListener()
            {
              @Override public void onClick(final View v)
              {
                final Intent i =
                  new Intent(MainSettingsActivity.this, WebViewActivity.class);
                final Bundle b = new Bundle();
                WebViewActivity.setActivityArguments(
                  b,
                  eula.documentGetReadableURL().toString(),
                  resources.getString(R.string.settings_eula),
                  SimplifiedPart.PART_SETTINGS);
                i.putExtras(b);
                MainSettingsActivity.this.startActivity(i);
                MainSettingsActivity.this.overridePendingTransition(0, 0);
              }
            });
        }
      });

    /**
     * Enable/disable the privacy policy field.
     */

    final TextView in_privacy =
      NullCheck.notNull((TextView) this.findViewById(R.id.settings_privacy));
    in_privacy.setEnabled(false);

    docs.getPrivacyPolicy().map_(
      new ProcedureType<SyncedDocumentType>()
      {
        @Override public void call(final SyncedDocumentType policy)
        {
          in_privacy.setEnabled(true);
          in_privacy.setOnClickListener(
            new OnClickListener()
            {
              @Override public void onClick(final View v)
              {
                final Intent i = new Intent(
                  MainSettingsActivity.this, WebViewActivity.class);
                final Bundle b = new Bundle();
                WebViewActivity.setActivityArguments(
                  b,
                  policy.documentGetReadableURL().toString(),
                  resources.getString(R.string.settings_privacy),
                  SimplifiedPart.PART_SETTINGS);
                i.putExtras(b);
                MainSettingsActivity.this.startActivity(i);
                MainSettingsActivity.this.overridePendingTransition(0, 0);
              }
            });
        }
      });

    /**
     * Enable/disable the acknowledgements field.
     */

    final TextView in_acknowledgements =
      NullCheck.notNull((TextView) this.findViewById(R.id.settings_credits));
    in_acknowledgements.setEnabled(false);

    docs.getAcknowledgements().map_(
      new ProcedureType<SyncedDocumentType>()
      {
        @Override public void call(final SyncedDocumentType ack)
        {
          in_acknowledgements.setEnabled(true);
          in_acknowledgements.setOnClickListener(
            new OnClickListener()
            {
              @Override public void onClick(final View v)
              {
                final Intent i = new Intent(
                  MainSettingsActivity.this, WebViewActivity.class);
                final Bundle b = new Bundle();
                WebViewActivity.setActivityArguments(
                  b,
                  ack.documentGetReadableURL().toString(),
                  resources.getString(R.string.settings_privacy),
                  SimplifiedPart.PART_SETTINGS);
                i.putExtras(b);
                MainSettingsActivity.this.startActivity(i);
                MainSettingsActivity.this.overridePendingTransition(0, 0);
              }
            });
        }
      });

    /**
     * Set a text change listener on both login fields that enables the login
     * button if both fields are non-empty.
     */

    in_barcode_edit.addTextChangedListener(
      new TextWatcher()
      {
        @Override public void onTextChanged(
          final @Nullable CharSequence s,
          final int start,
          final int before,
          final int count)
        {
          MainSettingsActivity.this.enableLoginIfFieldsNonEmpty(
            in_login, in_pin_edit, in_barcode_edit);
        }

        @Override public void beforeTextChanged(
          final @Nullable CharSequence s,
          final int start,
          final int count,
          final int after)
        {
          // Nothing
        }

        @Override public void afterTextChanged(
          final @Nullable Editable s)
        {
          // Nothing
        }
      });

    in_pin_edit.addTextChangedListener(
      new TextWatcher()
      {
        @Override public void onTextChanged(
          final @Nullable CharSequence s,
          final int start,
          final int before,
          final int count)
        {
          MainSettingsActivity.this.enableLoginIfFieldsNonEmpty(
            in_login, in_pin_edit, in_barcode_edit);
        }

        @Override public void beforeTextChanged(
          final @Nullable CharSequence s,
          final int start,
          final int count,
          final int after)
        {
          // Nothing
        }

        @Override public void afterTextChanged(
          final @Nullable Editable s)
        {
          // Nothing
        }
      });

    in_login.setEnabled(false);

    /**
     * Temporary "alternate URIs" selection.
     */

    final TextView in_alt_uris =
      NullCheck.notNull((TextView) this.findViewById(R.id.settings_alt_uris));
    in_alt_uris.setEnabled(true);
    in_alt_uris.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(final View v)
        {
          final Bundle b = new Bundle();
          SimplifiedActivity.setActivityArguments(b, false);
          final Intent i = new Intent();
          i.setClass(
            MainSettingsActivity.this, AlternateFeedURIsActivity.class);
          i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
          i.putExtras(b);
          MainSettingsActivity.this.startActivity(i);
        }
      });

    this.navigationDrawerSetActionBarTitle();
    this.barcode_edit = in_barcode_edit;
    this.pin_edit = in_pin_edit;
    this.login = in_login;
  }

  private void enableLoginIfFieldsNonEmpty(
    final Button in_login,
    final EditText in_pin_edit,
    final EditText in_barcode_edit)
  {
    in_login.setEnabled(
      in_pin_edit.getText().length() != 0 && in_barcode_edit.length() != 0);
  }

  @Override protected void onResume()
  {
    super.onResume();

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    books.accountGetCachedLoginDetails(this);
  }
}
