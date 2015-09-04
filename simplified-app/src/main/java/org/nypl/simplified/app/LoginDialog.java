package org.nypl.simplified.app;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountLoginListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.AuthenticationDocumentType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.DocumentStoreType;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A reusable login dialog.
 */

public final class LoginDialog extends DialogFragment
  implements AccountLoginListenerType
{
  private static final String BARCODE_ID;
  private static final Logger LOG;
  private static final String PIN_ID;
  private static final String TEXT_ID;

  static {
    LOG = LogUtilities.getLog(DialogFragment.class);
  }

  static {
    BARCODE_ID = "org.nypl.simplified.app.LoginDialog.barcode";
    PIN_ID = "org.nypl.simplified.app.LoginDialog.pin";
    TEXT_ID = "org.nypl.simplified.app.LoginDialog.text";
  }

  private @Nullable EditText                    barcode_edit;
  private @Nullable LoginControllerListenerType listener;
  private @Nullable Button                      login;
  private @Nullable EditText                    pin_edit;
  private @Nullable ViewGroup                   root_layout;
  private @Nullable TextView                    text;
  private @Nullable Button                      cancel;

  /**
   * Construct a new dialog.
   */

  public LoginDialog()
  {
    // Fragments must have no-arg constructors.
  }

  /**
   * @param rr      The application resources
   * @param message The error message returned by the device activation code
   *
   * @return An appropriate humanly-readable error message
   */

  public static String getDeviceActivationErrorMessage(
    final Resources rr,
    final String message)
  {
    /**
     * This is absolutely not the way to do this. The nypl-drm-adobe
     * interfaces should be expanded to return values of an enum type. For now,
     * however, this is the only error code that can be assigned a useful
     * message.
     */

    if (message.startsWith("E_ACT_TOO_MANY_ACTIVATIONS")) {
      return rr.getString(R.string.settings_login_failed_adobe_device_limit);
    } else {
      return rr.getString(R.string.settings_login_failed_adobe_device_limit);
    }
  }

  /**
   * Create a new login dialog.
   *
   * @param text    The initial dialog text.
   * @param barcode The barcode that will be used to log in
   * @param pin     The PIN that will be used to log in
   *
   * @return A new dialog
   */

  public static LoginDialog newDialog(
    final String text,
    final AccountBarcode barcode,
    final AccountPIN pin)
  {
    NullCheck.notNull(text);
    NullCheck.notNull(barcode);
    NullCheck.notNull(pin);

    final Bundle b = new Bundle();
    b.putSerializable(LoginDialog.TEXT_ID, text);
    b.putSerializable(LoginDialog.PIN_ID, pin);
    b.putSerializable(LoginDialog.BARCODE_ID, barcode);

    final LoginDialog d = new LoginDialog();
    d.setArguments(b);
    return d;
  }

  private void onAccountLoginFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    final String s =
      NullCheck.notNull(String.format("login failed: %s", message));

    LogUtilities.errorWithOptionalException(LoginDialog.LOG, s, error);

    final TextView in_text = NullCheck.notNull(this.text);
    final EditText in_barcode_edit = NullCheck.notNull(this.barcode_edit);
    final EditText in_pin_edit = NullCheck.notNull(this.pin_edit);
    final Button in_login = NullCheck.notNull(this.login);
    final Button in_cancel = NullCheck.notNull(this.cancel);

    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          in_text.setText(message);
          in_barcode_edit.setEnabled(true);
          in_pin_edit.setEnabled(true);
          in_login.setEnabled(true);
          in_cancel.setEnabled(true);
        }
      });

    final LoginControllerListenerType ls = this.listener;
    if (ls != null) {
      try {
        ls.onLoginFailure(error, message);
      } catch (final Throwable e) {
        LoginDialog.LOG.debug("{}", e.getMessage(), e);
      }
    }
  }

  @Override public void onAccountLoginFailureCredentialsIncorrect()
  {
    LoginDialog.LOG.error("onAccountLoginFailureCredentialsIncorrect");

    final Resources rr = NullCheck.notNull(this.getResources());
    final OptionType<Throwable> none = Option.none();
    this.onAccountLoginFailure(
      none, rr.getString(R.string.settings_login_failed_credentials));
  }

  @Override public void onAccountLoginFailureServerError(final int code)
  {
    LoginDialog.LOG.error(
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
    LoginDialog.LOG.error("onAccountLoginFailureLocalError: {}", message);

    final Resources rr = NullCheck.notNull(this.getResources());
    this.onAccountLoginFailure(
      error, rr.getString(R.string.settings_login_failed_server));
  }

  @Override public void onAccountLoginSuccess(
    final AccountBarcode barcode,
    final AccountPIN pin)
  {
    LoginDialog.LOG.debug("login succeeded");

    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          LoginDialog.this.dismiss();
        }
      });

    final LoginControllerListenerType ls = this.listener;
    if (ls != null) {
      try {
        ls.onLoginSuccess();
      } catch (final Throwable e) {
        LoginDialog.LOG.debug("{}", e.getMessage(), e);
      }
    }
  }

  @Override
  public void onAccountLoginFailureDeviceActivationError(final String message)
  {
    LoginDialog.LOG.error(
      "onAccountLoginFailureDeviceActivationError: {}", message);

    final OptionType<Throwable> none = Option.none();
    this.onAccountLoginFailure(
      none, LoginDialog.getDeviceActivationErrorMessage(
        this.getResources(), message));
  }

  @Override public void onResume()
  {
    super.onResume();

    final Resources rr = NullCheck.notNull(this.getResources());
    final int h = (int) rr.getDimension(R.dimen.login_dialog_height);
    final int w = (int) rr.getDimension(R.dimen.login_dialog_width);

    final Dialog dialog = NullCheck.notNull(this.getDialog());
    final Window window = NullCheck.notNull(dialog.getWindow());
    window.setLayout(w, h);
    window.setGravity(Gravity.CENTER);
  }

  @Override public void onCancel(
    final @Nullable DialogInterface dialog)
  {
    LoginDialog.LOG.debug("login aborted");

    final LoginControllerListenerType ls = this.listener;
    if (ls != null) {
      try {
        ls.onLoginAborted();
      } catch (final Throwable e) {
        LoginDialog.LOG.debug("{}", e.getMessage(), e);
      }
    }
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedLoginDialog);
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater_mn,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    final LayoutInflater inflater = NullCheck.notNull(inflater_mn);
    final Bundle b = this.getArguments();
    final AccountPIN initial_pin =
      NullCheck.notNull((AccountPIN) b.getSerializable(LoginDialog.PIN_ID));
    final AccountBarcode initial_bar = NullCheck.notNull(
      (AccountBarcode) b.getSerializable(LoginDialog.BARCODE_ID));
    final String initial_txt =
      NullCheck.notNull(b.getString(LoginDialog.TEXT_ID));

    final ViewGroup in_layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(
        R.layout.login_dialog, container, false));
    this.root_layout = in_layout;

    final TextView in_text = NullCheck.notNull(
      (TextView) in_layout.findViewById(R.id.login_dialog_text));

    final TextView in_barcode_label = NullCheck.notNull(
      (TextView) in_layout.findViewById(R.id.login_dialog_barcode_text_view));
    final EditText in_barcode_edit = NullCheck.notNull(
      (EditText) in_layout.findViewById(R.id.login_dialog_barcode_text_edit));

    final TextView in_pin_label = NullCheck.notNull(
      (TextView) in_layout.findViewById(R.id.login_dialog_pin_text_view));
    final EditText in_pin_edit = NullCheck.notNull(
      (EditText) in_layout.findViewById(R.id.login_dialog_pin_text_edit));

    final Button in_login_button =
      NullCheck.notNull((Button) in_layout.findViewById(R.id.login_dialog_ok));
    final Button in_login_cancel_button = NullCheck.notNull(
      (Button) in_layout.findViewById(R.id.login_dialog_cancel));

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final DocumentStoreType docs = app.getDocumentStore();

    final AuthenticationDocumentType auth_doc =
      docs.getAuthenticationDocument();
    in_barcode_label.setText(auth_doc.getLabelLoginUserID());
    in_pin_label.setText(auth_doc.getLabelLoginPassword());

    final BooksType books = app.getBooks();

    in_text.setText(initial_txt);
    in_barcode_edit.setText(initial_bar.toString());
    in_pin_edit.setText(initial_pin.toString());

    in_login_button.setEnabled(false);
    in_login_button.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View button)
        {
          in_barcode_edit.setEnabled(false);
          in_pin_edit.setEnabled(false);
          in_login_button.setEnabled(false);
          in_login_cancel_button.setEnabled(false);

          final Editable barcode_edit_text = in_barcode_edit.getText();
          final Editable pin_edit_text = in_pin_edit.getText();

          final AccountBarcode barcode =
            new AccountBarcode(NullCheck.notNull(barcode_edit_text.toString()));
          final AccountPIN pin =
            new AccountPIN(NullCheck.notNull(pin_edit_text.toString()));
          books.accountLogin(barcode, pin, LoginDialog.this);
        }
      });

    in_login_cancel_button.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          LoginDialog.this.dismiss();
        }
      });

    final AtomicBoolean in_barcode_empty = new AtomicBoolean(true);
    final AtomicBoolean in_pin_empty = new AtomicBoolean(true);

    in_barcode_edit.addTextChangedListener(
      new TextWatcher()
      {
        @Override public void afterTextChanged(
          final @Nullable Editable s)
        {
          // Nothing
        }

        @Override public void beforeTextChanged(
          final @Nullable CharSequence s,
          final int start,
          final int count,
          final int after)
        {
          // Nothing
        }

        @Override public void onTextChanged(
          final @Nullable CharSequence s,
          final int start,
          final int before,
          final int count)
        {
          in_barcode_empty.set(NullCheck.notNull(s).length() == 0);
          in_login_button.setEnabled(
            (in_barcode_empty.get() == false) && (in_pin_empty.get() == false));
        }
      });

    in_pin_edit.addTextChangedListener(
      new TextWatcher()
      {
        @Override public void afterTextChanged(
          final @Nullable Editable s)
        {
          // Nothing
        }

        @Override public void beforeTextChanged(
          final @Nullable CharSequence s,
          final int start,
          final int count,
          final int after)
        {
          // Nothing
        }

        @Override public void onTextChanged(
          final @Nullable CharSequence s,
          final int start,
          final int before,
          final int count)
        {
          in_pin_empty.set(NullCheck.notNull(s).length() == 0);
          in_login_button.setEnabled(
            (in_barcode_empty.get() == false) && (in_pin_empty.get() == false));
        }
      });

    this.barcode_edit = in_barcode_edit;
    this.pin_edit = in_pin_edit;
    this.login = in_login_button;
    this.cancel = in_login_cancel_button;
    this.text = in_text;

    final Dialog d = this.getDialog();
    if (d != null) {
      d.setCanceledOnTouchOutside(true);
    }

    return in_layout;
  }

  /**
   * Set the listener that will be used to receive the results of the login
   * attempt.
   *
   * @param in_listener The listener
   */

  public void setLoginListener(
    final LoginControllerListenerType in_listener)
  {
    this.listener = NullCheck.notNull(in_listener);
  }
}
