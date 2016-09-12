package org.nypl.simplified.app;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.app.catalog.MainCatalogActivity;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

/**
 *
 */
public final class LoginActivity extends Activity {


  /**
   * Construct login activity
   */
  public LoginActivity()
  {
  }

  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(LoginActivity.class);
  }

  @Override
  protected void onCreate(final Bundle state) {
    super.onCreate(state);
    this.setContentView(R.layout.login_view);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final Resources rr = NullCheck.notNull(this.getResources());
    final boolean clever_enabled = rr.getBoolean(R.bool.feature_auth_provider_clever);

    final ImageButton barcode = NullCheck.notNull((ImageButton) findViewById(R.id.login_with_barcode));

    barcode.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(final View view) {
        LoginActivity.this.onLoginWithBarcode();
      }
    });

    final ImageButton clever = NullCheck.notNull((ImageButton) findViewById(R.id.login_with_clever));

    if (clever_enabled) {
      clever.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
          LoginActivity.this.onLoginWithClever();
        }
      });
      clever.setVisibility(View.VISIBLE);

    } else {
      clever.setVisibility(View.GONE);
    }
  }





  private void openCatalog() {
    final Intent i = new Intent(this, MainCatalogActivity.class);

    i.putExtra("reload", true);

    this.startActivity(i);
    this.overridePendingTransition(0, 0);
    this.finish();
  }


  /**
   *
   */
  public void onLoginWithBarcode() {


    final LoginListenerType login_listener = new LoginListenerType() {
      @Override
      public void onLoginAborted() {
        LoginActivity.LOG.trace("feed auth: aborted login");
//        listener.onAuthenticationNotProvided();
      }

      @Override
      public void onLoginFailure(
        final OptionType<Throwable> error,
        final String message) {
        LogUtilities.errorWithOptionalException(
          LoginActivity.LOG, "failed login", error);
//        listener.onAuthenticationError(error, message);
      }

      @Override
      public void onLoginSuccess(
        final AccountCredentials creds) {
        LoginActivity.LOG.trace(
          "feed auth: login supplied new credentials");
        LoginActivity.this.openCatalog();
      }
    };


    final FragmentManager fm = this.getFragmentManager();
    UIThread.runOnUIThread(
      new Runnable() {
        @Override
        public void run() {
          final AccountBarcode barcode = new AccountBarcode("");
          final AccountPIN pin = new AccountPIN("");

          final LoginDialog df =
            LoginDialog.newDialog("Login required", barcode, pin);
          df.setLoginListener(login_listener);
          df.show(fm, "login-dialog");
        }
      });

  }

  /**
   *
   */
  public void onLoginWithClever() {

    final Intent i = new Intent(this, CleverLoginActivity.class);

    this.startActivityForResult(i, 1);

    this.overridePendingTransition(0, 0);

  }

  @Override
  protected void onActivityResult(final int request_code, final int result_code, final Intent data) {
    super.onActivityResult(request_code, result_code, data);

    if (result_code == 1) {

      this.openCatalog();
      final SimplifiedCatalogAppServicesType app =
        Simplified.getCatalogAppServices();

      final BooksType books = app.getBooks();
      books.fulfillExistingBooks();

    }
  }
}
