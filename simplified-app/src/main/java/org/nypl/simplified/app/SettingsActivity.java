package org.nypl.simplified.app;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
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
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountGetCachedCredentialsListenerType;
import org.nypl.simplified.books.core.AccountLoginListenerType;
import org.nypl.simplified.books.core.AccountLogoutListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.AccountSyncListenerType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The activity displaying the settings for the application.
 */

@SuppressWarnings("synthetic-access") public final class SettingsActivity
  extends SimplifiedActivity implements AccountLogoutListenerType,
  AccountLoginListenerType,
  AccountGetCachedCredentialsListenerType,
  AccountSyncListenerType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(SettingsActivity.class);
  }

  private @Nullable EditText barcode_edit;
  private @Nullable Button   login;
  private @Nullable EditText pin_edit;

  /**
   * Construct an activity.
   */

  public SettingsActivity()
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
    SettingsActivity.LOG.debug("account is logged in: {}", barcode);

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
          SettingsActivity.editableDisable(in_barcode_edit);
          SettingsActivity.editableDisable(in_pin_edit);

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
                      SettingsActivity.editableDisable(in_pin_edit);
                      SettingsActivity.editableDisable(in_barcode_edit);
                      books.accountLogout(SettingsActivity.this);
                    }
                  });
                final FragmentManager fm =
                  SettingsActivity.this.getFragmentManager();
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
          SettingsActivity.editableEnable(in_barcode_edit);
          SettingsActivity.editableEnable(in_pin_edit);

          in_login.setText(rr.getString(R.string.settings_log_in));
          in_login.setOnClickListener(
            new OnClickListener()
            {
              @Override public void onClick(
                final @Nullable View v)
              {
                in_login.setEnabled(false);
                SettingsActivity.editableDisable(in_pin_edit);
                SettingsActivity.editableDisable(in_barcode_edit);

                final Editable barcode_text = in_barcode_edit.getText();
                final AccountBarcode barcode = new AccountBarcode(
                  NullCheck.notNull(
                    barcode_text.toString()));
                final Editable pin_text = in_pin_edit.getText();
                final AccountPIN pin =
                  new AccountPIN(NullCheck.notNull(pin_text.toString()));
                books.accountLogin(barcode, pin, SettingsActivity.this);
              }
            });
        }
      });
  }

  @Override public void onAccountLoginFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    SettingsActivity.LOG.debug("onAccountLoginFailure");
    LogUtilities.errorWithOptionalException(
      SettingsActivity.LOG, message, error);

    final Resources rr = NullCheck.notNull(this.getResources());
    final SettingsActivity ctx = this;
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
                SettingsActivity.this.onAccountIsNotLoggedIn();
              }
            });
          a.show();
        }
      });
  }

  @Override public void onAccountLoginSuccess(
    final AccountBarcode barcode,
    final AccountPIN pin)
  {
    SettingsActivity.LOG.debug("account login succeeded: {}", barcode);
    this.onAccountIsLoggedIn(barcode, pin);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    final Resources rr = NullCheck.notNull(this.getResources());
    final Context context = SettingsActivity.this.getApplicationContext();
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
          books.accountSync(SettingsActivity.this);
        }
      });
  }

  @Override public void onAccountLogoutFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    SettingsActivity.LOG.debug("onAccountLogoutFailure");
    LogUtilities.errorWithOptionalException(
      SettingsActivity.LOG, message, error);
  }

  @Override public void onAccountLogoutSuccess()
  {
    SettingsActivity.LOG.debug("onAccountLogoutSuccess");
    this.onAccountIsNotLoggedIn();

    final Resources rr = NullCheck.notNull(this.getResources());
    final Context context = SettingsActivity.this.getApplicationContext();
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
          SettingsActivity.editableEnable(be);
          SettingsActivity.editableEnable(pe);
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
    SettingsActivity.LOG.error("failed to sync account: {}", message);
  }

  @Override public void onAccountSyncBook(
    final BookID book)
  {
    SettingsActivity.LOG.error("synced book: {}", book);
  }

  @Override public void onAccountSyncFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    LogUtilities.errorWithOptionalException(
      SettingsActivity.LOG, message, error);
  }

  @Override public void onAccountSyncSuccess()
  {
    SettingsActivity.LOG.debug("completed sync");
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final LayoutInflater inflater = NullCheck.notNull(this.getLayoutInflater());
    final Resources resources = NullCheck.notNull(this.getResources());

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(
        R.layout.settings, content_area, false));
    content_area.addView(layout);
    content_area.requestLayout();

    final EditText in_barcode_edit = NullCheck.notNull(
      (EditText) this.findViewById(R.id.settings_barcode_edit));
    final EditText in_pin_edit =
      NullCheck.notNull((EditText) this.findViewById(R.id.settings_pin_edit));
    final Button in_login =
      NullCheck.notNull((Button) this.findViewById(R.id.settings_login));
    final TextView in_adobe =
      NullCheck.notNull((TextView) this.findViewById(R.id.settings_adobe_drm));

    final AtomicBoolean in_barcode_empty = new AtomicBoolean(true);
    final AtomicBoolean in_pin_empty = new AtomicBoolean(true);

    in_barcode_edit.addTextChangedListener(
      new TextWatcher()
      {
        @Override public void onTextChanged(
          final @Nullable CharSequence s,
          final int start,
          final int before,
          final int count)
        {
          in_barcode_empty.set(NullCheck.notNull(s).length() == 0);
          in_login.setEnabled(
            (in_barcode_empty.get() == false) && (in_pin_empty.get() == false));
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
          in_pin_empty.set(NullCheck.notNull(s).length() == 0);
          in_login.setEnabled(
            (in_barcode_empty.get() == false) && (in_pin_empty.get() == false));
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

    this.navigationDrawerSetActionBarTitle();

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    if (app.getAdobeDRMExecutor().isSome()) {
      in_adobe.setText(
        resources.getText(R.string.settings_adobe_drm_supported));
    } else {
      in_adobe.setText(
        resources.getText(R.string.settings_adobe_drm_unsupported));
    }

    this.barcode_edit = in_barcode_edit;
    this.pin_edit = in_pin_edit;
    this.login = in_login;
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
