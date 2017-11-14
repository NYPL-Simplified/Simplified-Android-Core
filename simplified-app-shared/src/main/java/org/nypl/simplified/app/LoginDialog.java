package org.nypl.simplified.app;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.drm.core.AdobeVendorID;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.AccountAuthProvider;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.AccountLoginListenerType;
import org.nypl.simplified.books.core.AccountPIN;
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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A reusable login dialog.
 */

public final class LoginDialog extends DialogFragment
  implements AccountLoginListenerType, DeviceActivationListenerType
{
  private static final String BARCODE_ID;
  private static final Logger LOG;
  private static final String PIN_ID;
  private static final String TEXT_ID;
  private static final String ACCOUNT_ID;
  private static final String PIN_ALLOWS_LETTERS;
  private static final String PIN_LENGTH;

  static {
    LOG = LogUtilities.getLog(LoginDialog.class);
  }

  static {
    BARCODE_ID = "org.nypl.simplified.app.LoginDialog.barcode";
    PIN_ID = "org.nypl.simplified.app.LoginDialog.pin";
    TEXT_ID = "org.nypl.simplified.app.LoginDialog.text";
    ACCOUNT_ID = "org.nypl.simplified.app.LoginDialog.accountid";
    PIN_ALLOWS_LETTERS = "org.nypl.simplified.app.LoginDialog.pinAllowsLetters";
    PIN_LENGTH = "org.nypl.simplified.app.LoginDialog.pinLength";
  }

  private @Nullable EditText          barcode_edit;
  private @Nullable LoginListenerType listener;
  private @Nullable Button            login;
  private @Nullable EditText          pin_edit;
  private @Nullable TextView          text;
  private @Nullable Button            cancel;

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
     * however, these are the only error codes that can be assigned useful
     * messages.
     */

    if (message.startsWith("E_ACT_TOO_MANY_ACTIVATIONS")) {
      return rr.getString(R.string.settings_login_failed_adobe_device_limit);
    } else if (message.startsWith("E_ADEPT_REQUEST_EXPIRED")) {
      return rr.getString(
        R.string.settings_login_failed_adobe_device_bad_clock);
    } else {
      return rr.getString(R.string.settings_login_failed_device);
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

    final Account account = Simplified.getCurrentAccount();

    final Bundle b = new Bundle();
    b.putSerializable(LoginDialog.TEXT_ID, text);
    b.putSerializable(LoginDialog.PIN_ID, pin);
    b.putSerializable(LoginDialog.BARCODE_ID, barcode);
    b.putSerializable(LoginDialog.PIN_ALLOWS_LETTERS, account.pinAllowsLetters());
    b.putSerializable(LoginDialog.PIN_LENGTH, account.getPinLength());

    final LoginDialog d = new LoginDialog();
    d.setArguments(b);
    return d;
  }

  /**
   * @param text Text
   * @param barcode Barcode
   * @param pin Pin
   * @param account Library Account
   * @return Login Dialog
   */

  public static LoginDialog newDialog(
    final String text,
    final AccountBarcode barcode,
    final AccountPIN pin,
    final Account account) {

    NullCheck.notNull(text);
    NullCheck.notNull(barcode);
    NullCheck.notNull(pin);
    NullCheck.notNull(account);

    final Bundle b = new Bundle();
    b.putSerializable(LoginDialog.TEXT_ID, text);
    b.putSerializable(LoginDialog.PIN_ID, pin);
    b.putSerializable(LoginDialog.BARCODE_ID, barcode);
    b.putSerializable(LoginDialog.ACCOUNT_ID, account.getPathComponent());
    b.putSerializable(LoginDialog.PIN_ALLOWS_LETTERS, account.pinAllowsLetters());
    b.putSerializable(LoginDialog.PIN_LENGTH, account.getPinLength());

    final LoginDialog d = new LoginDialog();
    d.setArguments(b);
    return d;

  }

  private void onAccountLoginFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    final String s = NullCheck.notNull(
      String.format(
        "login failed: %s", message));

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

    final LoginListenerType ls = this.listener;
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
      "onAccountLoginFailureServerError: {}", code);

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
    final AccountCredentials creds)
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

    final LoginListenerType ls = this.listener;
    if (ls != null) {
      try {
        ls.onLoginSuccess(creds);
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

    final LoginListenerType ls = this.listener;
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

    final String account_id = b.getString(LoginDialog.ACCOUNT_ID);
    final int pin_length = b.getInt(LoginDialog.PIN_LENGTH);
    final boolean pin_allows_letters = b.getBoolean(LoginDialog.PIN_ALLOWS_LETTERS);

    final ViewGroup in_layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(
        R.layout.login_dialog, container, false));

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
    if (!pin_allows_letters) {
      in_pin_edit.setInputType(InputType.TYPE_CLASS_NUMBER);
    }
    if (pin_length != 0) {
      in_pin_edit.setFilters(new InputFilter[] {
          new InputFilter.LengthFilter(pin_length)
      });
    }

    final Button in_login_button =
      NullCheck.notNull((Button) in_layout.findViewById(R.id.login_dialog_ok));
    final Button in_login_cancel_button = NullCheck.notNull(
      (Button) in_layout.findViewById(R.id.login_dialog_cancel));

    final CheckBox in_eula_checkbox =
      NullCheck.notNull((CheckBox) in_layout.findViewById(R.id.eula_checkbox));

    final Button in_login_request_new_code = NullCheck.notNull(
      (Button) in_layout.findViewById(R.id.request_new_codes));

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    DocumentStoreType docs = app.getDocumentStore();

    final AuthenticationDocumentType auth_doc =
      docs.getAuthenticationDocument();
    in_barcode_label.setText(auth_doc.getLabelLoginUserID());
    in_pin_label.setText(auth_doc.getLabelLoginPassword());

    final Resources rr = NullCheck.notNull(this.getResources());
    final OptionType<AdobeVendorID> adobe_vendor = Option.some(
      new AdobeVendorID(rr.getString(R.string.feature_adobe_vendor_id)));

    BooksType books = app.getBooks();

    if (account_id != null)
    {
      final Account account = new AccountsRegistry(getActivity()).getAccount(Integer.valueOf(account_id));
      books = Simplified.getBooks(account, getActivity(), Simplified.getCatalogAppServices().getAdobeDRMExecutor());
      docs = Simplified.getDocumentStore(account, getActivity().getResources());
    }

    in_text.setText(initial_txt);
    in_barcode_edit.setText(initial_bar.toString());
    in_pin_edit.setText(initial_pin.toString());

    in_login_button.setEnabled(false);
    final BooksType final_books = books;
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
          final AccountAuthProvider provider =
            new AccountAuthProvider(rr.getString(R.string.feature_default_auth_provider_name));

          final AccountCredentials creds =
            new AccountCredentials(adobe_vendor, barcode, pin,  Option.some(provider));
          final_books.accountLogin(creds, LoginDialog.this);
        }
      });

    in_login_cancel_button.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          LoginDialog.this.onCancel(null);
          LoginDialog.this.dismiss();
        }
      });

    final boolean request_new_code = rr.getBoolean(R.bool.feature_default_auth_provider_request_new_code);

    if (request_new_code) {
      in_login_request_new_code.setOnClickListener(
        new OnClickListener() {
          @Override
          public void onClick(
            final @Nullable View v) {
            final Intent browser_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(rr.getString(R.string.feature_default_auth_provider_request_new_code_uri)));
            startActivity(browser_intent);
          }
        });
    }
    else
    {
      // card creator (currently deactivated)
//      in_login_request_new_code.setOnClickListener(
//        new OnClickListener() {
//          @Override
//          public void onClick(
//            final @Nullable View v) {
//            final Intent cardcreator = new Intent(LoginDialog.this.getActivity(), CardCreatorActivity.class);
//            startActivity(cardcreator);
//          }
//        });
//      in_login_request_new_code.setText("Sign Up");
      in_login_request_new_code.setVisibility(View.GONE);
    }


    final AtomicBoolean in_barcode_empty = new AtomicBoolean(true);
    final AtomicBoolean in_pin_empty = new AtomicBoolean(true);


    final OptionType<EULAType> eula_opt = docs.getEULA();

    if (eula_opt.isSome()) {
      final Some<EULAType> some_eula = (Some<EULAType>) eula_opt;
      final EULAType eula = some_eula.get();


      in_eula_checkbox.setChecked(eula.eulaHasAgreed());

      in_eula_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final CompoundButton button, final boolean checked) {

          eula.eulaSetHasAgreed(checked);
          in_login_button.setEnabled(
            (!in_barcode_empty.get()) && (!in_pin_empty.get()) && in_eula_checkbox.isChecked());

        }
      });

      if (eula.eulaHasAgreed()) {
        LoginDialog.LOG.debug("EULA: agreed");

      } else {
        LoginDialog.LOG.debug("EULA: not agreed");

      }
    } else {
      LoginDialog.LOG.debug("EULA: unavailable");
    }

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
            (!in_barcode_empty.get()) && (!in_pin_empty.get()) && in_eula_checkbox.isChecked());
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
            (!in_barcode_empty.get()) && (!in_pin_empty.get()) && in_eula_checkbox.isChecked());
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
    final LoginListenerType in_listener)
  {
    this.listener = NullCheck.notNull(in_listener);
  }

  @Override public void onAccountSyncAuthenticationFailure(final String message)
  {
    // Nothing
  }

  @Override public void onAccountSyncBook(final BookID book)
  {
    // Nothing
  }

  @Override public void onAccountSyncFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    LogUtilities.errorWithOptionalException(LoginDialog.LOG, message, error);
  }

  @Override public void onAccountSyncSuccess()
  {
    // Nothing
  }

  @Override public void onAccountSyncBookDeleted(final BookID book)
  {
    // Nothing
  }


  @Override
  public void onDeviceActivationFailure(final String message) {
    // Nothing
  }

  @Override
  public void onDeviceActivationSuccess() {
    // Nothing
  }
}
