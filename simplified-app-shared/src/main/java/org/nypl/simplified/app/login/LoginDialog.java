package org.nypl.simplified.app.login;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.drm.core.AdobeVendorID;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.accounts.AccountAuthenticationProvider;
import org.nypl.simplified.books.accounts.AccountBarcode;
import org.nypl.simplified.books.accounts.AccountEventLogin;
import org.nypl.simplified.books.accounts.AccountPIN;
import org.nypl.simplified.books.accounts.AccountProviderAuthenticationDescription;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.authentication_document.AuthenticationDocumentType;
import org.nypl.simplified.books.controller.ProfilesControllerType;
import org.nypl.simplified.books.document_store.DocumentStoreType;
import org.nypl.simplified.books.eula.EULAType;
import org.nypl.simplified.books.logging.LogUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed;
import static org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed.ErrorCode.ERROR_GENERAL;
import static org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginSucceeded;

/**
 * A reusable login dialog.
 */

public final class LoginDialog extends AppCompatDialogFragment {

  private static final Logger LOG = LoggerFactory.getLogger(LoginDialog.class);

  private EditText barcode_edit;
  private ImageButton scan;
  private Button login;
  private EditText pin_edit;
  private TextView text;
  private Button cancel;
  private ProfilesControllerType controller;
  private AccountType account;
  private ListeningExecutorService executor;
  private FluentFuture<Unit> login_task;
  private LoginSucceededType on_login_success;
  private LoginFailedType on_login_failure;
  private LoginCancelledType on_login_cancelled;
  private AccountProviderAuthenticationDescription authentication;
  private DocumentStoreType documents;

  /**
   * Construct a new dialog.
   */

  public LoginDialog() {
    // Fragments must have no-arg constructors.
  }

  /**
   * @param rr      The application resources
   * @param message The error message returned by the device activation code
   * @return An appropriate humanly-readable error message
   */

  public static String getDeviceActivationErrorMessage(
    final Resources rr,
    final String message) {

    /*
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
   * Create a new login dialog. The given callback functions will be executed on the UI thread with
   * the results of the login operation. Any strings passed to the callbacks will be properly
   * localized and do not require further processing.
   *
   * @param on_login_success   A function evaluated on login success
   * @param on_login_cancelled A function evaluated on login cancellation
   * @param on_login_failure   A function evaluated on login failure
   * @return A new dialog
   */

  public static LoginDialog newDialog(
    final ProfilesControllerType controller,
    final ListeningExecutorService executor,
    final DocumentStoreType documents,
    final String text,
    final AccountType account,
    final LoginSucceededType on_login_success,
    final LoginCancelledType on_login_cancelled,
    final LoginFailedType on_login_failure) {

    NullCheck.notNull(controller, "Controller");
    NullCheck.notNull(documents, "Documents");
    NullCheck.notNull(text, "Text");
    NullCheck.notNull(account, "Account");
    NullCheck.notNull(on_login_success, "Success");
    NullCheck.notNull(on_login_cancelled, "Cancel");
    NullCheck.notNull(on_login_failure, "Failure");

    return account.provider().authentication().accept(
      new OptionVisitorType<AccountProviderAuthenticationDescription, LoginDialog>() {
        @Override
        public LoginDialog none(final None<AccountProviderAuthenticationDescription> none) {
          throw new IllegalArgumentException(
            "Attempted to log in on an account that does not require authentication!");
        }

        @Override
        public LoginDialog some(final Some<AccountProviderAuthenticationDescription> some) {
          final AccountProviderAuthenticationDescription authentication = some.get();

          final LoginDialog d = new LoginDialog();
          d.setRequiredArguments(
            controller,
            executor,
            documents,
            account,
            authentication,
            on_login_success,
            on_login_cancelled,
            on_login_failure);
          return d;
        }
      });
  }

  private void setRequiredArguments(
    final ProfilesControllerType controller,
    final ListeningExecutorService executor,
    final DocumentStoreType documents,
    final AccountType account,
    final AccountProviderAuthenticationDescription authentication,
    final LoginSucceededType on_login_success,
    final LoginCancelledType on_login_cancelled,
    final LoginFailedType on_login_failure) {

    this.controller =
      NullCheck.notNull(controller, "controller");
    this.executor =
      NullCheck.notNull(executor, "Executor");
    this.account =
      NullCheck.notNull(account, "Account");
    this.authentication =
      NullCheck.notNull(authentication, "Authentication");
    this.documents =
      NullCheck.notNull(documents, "Documents");
    this.on_login_success =
      NullCheck.notNull(on_login_success, "On login success");
    this.on_login_failure =
      NullCheck.notNull(on_login_failure, "On login failure");
    this.on_login_cancelled =
      NullCheck.notNull(on_login_cancelled, "On login cancelled");
  }

  private void onAccountLoginFailure(
    final OptionType<Exception> error,
    final String message) {

    final String s = NullCheck.notNull(String.format("login failed: %s", message));
    LogUtilities.errorWithOptionalException(LOG, s, error);

    UIThread.runOnUIThread(() -> {
      this.text.setText(message);
      this.barcode_edit.setEnabled(true);
      this.pin_edit.setEnabled(true);
      this.login.setEnabled(true);
      this.cancel.setEnabled(true);
      try {
        this.on_login_failure.onLoginFailed(error, message);
      } catch (Exception e) {
        LOG.error("ignored exception in login failed callback: {}", e);
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();

    final Resources rr = NullCheck.notNull(this.getResources());
    final int h = (int) rr.getDimension(R.dimen.login_dialog_height);
    final int w = (int) rr.getDimension(R.dimen.login_dialog_width);

    final Dialog dialog = NullCheck.notNull(this.getDialog());
    final Window window = NullCheck.notNull(dialog.getWindow());
    window.setLayout(w, h);
    window.setGravity(Gravity.CENTER);
  }

  @Override
  public void onCancel(final @Nullable DialogInterface dialog) {
    LOG.debug("login aborted");

    FluentFuture<Unit> task = this.login_task;
    if (task != null) {
      this.login_task.cancel(true);
    }

    UIThread.runOnUIThread(() -> {
      try {
        this.on_login_cancelled.onLoginCancelled();
      } catch (Exception e) {
        LOG.error("ignored exception in cancelled callback: ", e);
      }
    });
  }

  @Override
  public void onCreate(
    final @Nullable Bundle state) {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedLoginDialog);
  }

  @Override
  public View onCreateView(
    final @Nullable LayoutInflater inflater_mn,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state) {

    final LayoutInflater inflater = NullCheck.notNull(inflater_mn);

    final ViewGroup in_layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(R.layout.login_dialog, container, false));

    final TextView in_text =
      NullCheck.notNull(in_layout.findViewById(R.id.login_dialog_text));
    final TextView in_barcode_label =
      NullCheck.notNull(in_layout.findViewById(R.id.login_dialog_barcode_text_view));
    final EditText in_barcode_edit =
      NullCheck.notNull(in_layout.findViewById(R.id.login_dialog_barcode_text_edit));
    final ImageButton in_barcode_scan_button =
      NullCheck.notNull(in_layout.findViewById(R.id.login_dialog_barcode_scan_button));
    final TextView in_pin_label =
      NullCheck.notNull(in_layout.findViewById(R.id.login_dialog_pin_text_view));
    final EditText in_pin_edit =
      NullCheck.notNull(in_layout.findViewById(R.id.login_dialog_pin_text_edit));

    /*
     * If the passcode is not allowed to contain letters, then don't let users enter them.
     */

    if (!this.authentication.passCodeMayContainLetters()) {
      in_pin_edit.setInputType(
        InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    }

    /*
     * If the passcode has a known length, limit the input to that length.
     */

    if (this.authentication.passCodeLength() != 0) {
      in_pin_edit.setFilters(new InputFilter[]{
        new InputFilter.LengthFilter(this.authentication.passCodeLength())
      });
    }

    /*
     * If a PIN is not required, hide the PIN entry elements.
     */

    if (!this.authentication.requiresPin()) {
      in_pin_label.setVisibility(View.INVISIBLE);
      in_pin_edit.setVisibility(View.INVISIBLE);
    }

    final Button in_login_button =
      NullCheck.notNull(in_layout.findViewById(R.id.login_dialog_ok));
    final Button in_login_cancel_button =
      NullCheck.notNull(in_layout.findViewById(R.id.login_dialog_cancel));
    final CheckBox in_eula_checkbox =
      NullCheck.notNull(in_layout.findViewById(R.id.eula_checkbox));
    final Button in_login_request_new_code =
      NullCheck.notNull(in_layout.findViewById(R.id.request_new_codes));

    // XXX: This "authentication document" is supposed to be part of the account provider
    final AuthenticationDocumentType auth_doc = this.documents.getAuthenticationDocument();
    in_barcode_label.setText(auth_doc.getLabelLoginUserID());
    in_pin_label.setText(auth_doc.getLabelLoginPassword());

    final Resources rr = NullCheck.notNull(this.getResources());
    final OptionType<AdobeVendorID> adobe_vendor = Option.some(new AdobeVendorID(rr.getString(R.string.feature_adobe_vendor_id)));

    /*
     * If the account provider supports barcode scanning, show the scan button.
     */

    if (this.account.provider().supportsBarcodeScanner()) {
      in_barcode_scan_button.setVisibility(View.VISIBLE);
    }

    // XXX: Where does this information come from?
    // in_text.setText(initial_txt);
    // in_barcode_edit.setText(initial_bar.toString());
    // in_pin_edit.setText(initial_pin.toString());

    in_login_button.setEnabled(false);
    in_login_button.setOnClickListener(button -> {
      in_barcode_edit.setEnabled(false);
      in_pin_edit.setEnabled(false);
      in_login_button.setEnabled(false);
      in_login_cancel_button.setEnabled(false);
      in_barcode_scan_button.setEnabled(false);

      final Editable barcode_edit_text = in_barcode_edit.getText();
      final Editable pin_edit_text = in_pin_edit.getText();

      final AccountBarcode barcode =
        AccountBarcode.create(NullCheck.notNull(barcode_edit_text.toString()));

      final AccountPIN pin;
      if (!this.authentication.requiresPin()) {
        // Server requires blank string for No-PIN accounts
        pin = AccountPIN.create("");
      } else {
        pin = AccountPIN.create(NullCheck.notNull(pin_edit_text.toString()));
      }

      final AccountAuthenticationProvider provider =
        AccountAuthenticationProvider.create(
          rr.getString(R.string.feature_default_auth_provider_name));

      final AccountAuthenticationCredentials accountCreds =
        AccountAuthenticationCredentials.builder(pin, barcode)
          .setAuthenticationProvider(provider)
          .build();

      this.login_task =
        this.controller.profileAccountLogin(this.account.id(), accountCreds)
          .catching(Exception.class, this::onLoginFailed, this.executor)
          .transform(this::onAccountEvent, this.executor);
    });

    in_login_cancel_button.setOnClickListener(view -> {
      this.onCancel(null);
      this.dismiss();
    });

    in_barcode_scan_button.setOnClickListener(v -> {
      // IntentIntegrator exit will fire on Scan or Back and hit the onActivityResult method.
      IntentIntegrator
        .forSupportFragment(this)
        .setPrompt(getString(R.string.barcode_scanner_prompt))
        .setBeepEnabled(false)
        .initiateScan();
    });

    final boolean request_new_code =
      rr.getBoolean(R.bool.feature_default_auth_provider_request_new_code);

    if (request_new_code) {
      in_login_request_new_code.setOnClickListener(v -> {
        final Intent browser_intent =
          new Intent(
            Intent.ACTION_VIEW,
            Uri.parse(rr.getString(R.string.feature_default_auth_provider_request_new_code_uri)));
        startActivity(browser_intent);
      });
    } else {
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
    final AtomicBoolean in_pin_empty = new AtomicBoolean(this.authentication.requiresPin());

    final OptionType<EULAType> eula_opt = this.documents.getEULA();
    if (eula_opt.isSome()) {
      final Some<EULAType> some_eula = (Some<EULAType>) eula_opt;
      final EULAType eula = some_eula.get();
      in_eula_checkbox.setChecked(eula.eulaHasAgreed());
      in_eula_checkbox.setOnCheckedChangeListener((button, checked) -> {
        eula.eulaSetHasAgreed(checked);
        in_login_button.setEnabled(
          (!in_barcode_empty.get())
            && (!in_pin_empty.get())
            && in_eula_checkbox.isChecked());
      });
      if (eula.eulaHasAgreed()) {
        LOG.debug("EULA: agreed");
      } else {
        LOG.debug("EULA: not agreed");
      }
    } else {
      LOG.debug("EULA: unavailable");
    }

    in_barcode_edit.addTextChangedListener(
      new TextWatcher() {
        @Override
        public void afterTextChanged(
          final @Nullable Editable s) {
          // Nothing
        }

        @Override
        public void beforeTextChanged(
          final @Nullable CharSequence s,
          final int start,
          final int count,
          final int after) {
          // Nothing
        }

        @Override
        public void onTextChanged(
          final @Nullable CharSequence s,
          final int start,
          final int before,
          final int count) {
          in_barcode_empty.set(NullCheck.notNull(s).length() == 0);
          in_login_button.setEnabled(
            (!in_barcode_empty.get()) && (!in_pin_empty.get()) && in_eula_checkbox.isChecked());
        }
      });

    in_pin_edit.addTextChangedListener(
      new TextWatcher() {
        @Override
        public void afterTextChanged(
          final @Nullable Editable s) {
          // Nothing
        }

        @Override
        public void beforeTextChanged(
          final @Nullable CharSequence s,
          final int start,
          final int count,
          final int after) {
          // Nothing
        }

        @Override
        public void onTextChanged(
          final @Nullable CharSequence s,
          final int start,
          final int before,
          final int count) {
          in_pin_empty.set(NullCheck.notNull(s).length() == 0);
          in_login_button.setEnabled(
            (!in_barcode_empty.get()) && (!in_pin_empty.get()) && in_eula_checkbox.isChecked());
        }
      });

    this.barcode_edit = in_barcode_edit;
    this.scan = in_barcode_scan_button;
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

  private Unit onAccountEvent(AccountEventLogin event) {
    return event.matchLogin(
      this::onAccountEventLoginSuccess,
      this::onAccountEventLoginFailed);
  }

  private Unit onAccountEventLoginFailed(final AccountLoginFailed failure) {
    switch (failure.errorCode()) {
      case ERROR_PROFILE_CONFIGURATION: {
        this.onAccountLoginFailure(
          failure.exception(),
          this.getResources().getString(R.string.settings_login_failed_server));
      }
      case ERROR_NETWORK_EXCEPTION: {
        this.onAccountLoginFailure(
          failure.exception(),
          this.getResources().getString(R.string.settings_login_failed_server));
      }
      case ERROR_CREDENTIALS_INCORRECT: {
        this.onAccountLoginFailure(
          failure.exception(),
          this.getResources().getString(R.string.settings_login_failed_credentials));
        return Unit.unit();
      }
      case ERROR_SERVER_ERROR: {
        this.onAccountLoginFailure(
          failure.exception(),
          this.getResources().getString(R.string.settings_login_failed_server));
      }
      case ERROR_ACCOUNT_NONEXISTENT: {
        this.onAccountLoginFailure(
          failure.exception(),
          this.getResources().getString(R.string.settings_login_failed_server));
      }
      case ERROR_GENERAL: {
        this.onAccountLoginFailure(
          failure.exception(),
          this.getResources().getString(R.string.settings_login_failed_server));
      }
    }

    throw new UnreachableCodeException();
  }

  private Unit onAccountEventLoginSuccess(
    final AccountLoginSucceeded success) {
    LOG.debug("login succeeded");

    UIThread.runOnUIThread(() -> {
      try {
        this.on_login_success.onLoginSucceeded(success.credentials());
      } catch (Exception e) {
        LOG.error("ignored exception in succeeded callback: ", e);
      }
      this.dismiss();
    });
    return Unit.unit();
  }

  private AccountEventLogin onLoginFailed(Exception exception) {
    return AccountLoginFailed.of(ERROR_GENERAL, Option.of(exception));
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
    if (result == null) {
      super.onActivityResult(requestCode, resultCode, data);
      return;
    }

    if (result.getContents() == null) {
      Toast.makeText(this.getActivity(), R.string.barcode_scanning_error, Toast.LENGTH_LONG)
        .show();
    } else {
      this.barcode_edit.setText(result.getContents());
      this.pin_edit.requestFocus();
    }
  }
}
