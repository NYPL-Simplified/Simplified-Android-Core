package org.nypl.simplified.app;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnimplementedCodeException;

import org.nypl.simplified.accounts.api.AccountID;
import org.nypl.simplified.accounts.database.api.AccountType;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException;
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException;
import org.nypl.simplified.profiles.api.ProfileReadableType;
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ReportIssueActivity extends AppCompatActivity {

  private static final Logger LOG = LoggerFactory.getLogger(ReportIssueActivity.class);

  private EditText subject_field;
  private EditText message_field;
  private boolean mail_client_opened;
  private AccountType account;

  public ReportIssueActivity() {

  }

  @Override
  protected void onCreate(final Bundle state) {
    super.onCreate(state);
    this.setContentView(R.layout.report_issue);

    try {
      final ProfileReadableType profile =
        Simplified.getServices()
          .requireService(ProfilesControllerType.class)
          .profileCurrent();

      final Bundle extras = getIntent().getExtras();
      final AccountID accountId;
      if (extras != null) {
        accountId = new AccountID(UUID.fromString(extras.getString("selectedAccount")));
      } else {
        accountId = profile.accountCurrent().getId();
      }

      this.account =
        profile.account(accountId);
      this.subject_field =
        NullCheck.notNull(this.findViewById(R.id.subject_field));
      this.message_field =
        NullCheck.notNull(this.findViewById(R.id.message_field));

      this.mail_client_opened = false;

      final ActionBar bar = this.getActionBar();
      if (Build.VERSION.SDK_INT < 21) {
        bar.setDisplayHomeAsUpEnabled(false);
        bar.setHomeButtonEnabled(true);
        bar.setIcon(R.drawable.ic_arrow_back);
      } else {
        bar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(false);
      }

    } catch (final ProfileNoneCurrentException e) {
      throw new IllegalStateException(e);
    } catch (AccountsDatabaseNonexistentException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    this.mail_client_opened = false;
  }

  @Override
  protected void onStop() {
    super.onStop();
    this.mail_client_opened = true;
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    final Menu menu_nn = NullCheck.notNull(menu);
    final MenuInflater inflater = this.getMenuInflater();
    inflater.inflate(R.menu.report_issue, menu_nn);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn) {
    final MenuItem item = NullCheck.notNull(item_mn);

    if (item.getItemId() == R.id.submit_issue) {

      // if subject and message

      if (this.subject_field.getText().toString().isEmpty()) {
        this.subject_field.requestFocus();
        final AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setNeutralButton("OK", null);
        b.setMessage("Please enter a subject.");
        b.setTitle("Missing Subject");
        b.setCancelable(true);
        b.create().show();
      } else if (this.message_field.getText().toString().isEmpty()) {
        this.message_field.requestFocus();
        final AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setNeutralButton("OK", null);
        b.setMessage("Please enter a message.");
        b.setTitle("Missing Message");
        b.setCancelable(true);
        b.create().show();
      } else {
        final String email = this.account.getProvider().getSupportEmail();
        if (email != null) {
          this.launchEmailAppWithEmailAddress(this,
            email,
            this.subject_field.getText().toString(),
            this.message_field.getText().toString());
        } else {
          throw new UnimplementedCodeException();
        }
      }
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


  /**
   * @param activity context
   * @param email    recipient
   * @param subject  subject
   * @param message  message
   */
  private void launchEmailAppWithEmailAddress(
    final Activity activity,
    final String email,
    final String subject,
    final String message) {

    final Intent email_intent = new Intent(Intent.ACTION_SEND);
    email_intent.setType("plain/text");
    email_intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
    email_intent.putExtra(Intent.EXTRA_SUBJECT, subject);
    email_intent.putExtra(Intent.EXTRA_TEXT, getDeviceInformation(this, message));

    activity.startActivityForResult(Intent.createChooser(email_intent, "Email"), 1);
  }

  private static String getDeviceInformation(final Context activity, final String message) {
    final StringBuilder builder = new StringBuilder();
    builder.append("\n");
    builder.append(message);
    builder.append("\n\n\n");
    builder.append("========");
    builder.append("\nDevice brand: ");
    builder.append(Build.MODEL);
    builder.append("\nAndroid version: ");
    builder.append(Build.VERSION.SDK_INT);
    builder.append("\nApp package :");
    try {
      builder.append(activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).packageName);
    } catch (PackageManager.NameNotFoundException e) {
      builder.append("NA");
    }
    builder.append("\nApp version :");
    try {
      builder.append(activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName);
    } catch (PackageManager.NameNotFoundException e) {
      builder.append("NA");
    }

    try {
      builder.append("(");
      builder.append(activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode);
      builder.append(")");
    } catch (PackageManager.NameNotFoundException e) {
      builder.append("NA");
    }

    return builder.toString();
  }

  @Override
  public void onActivityResult(final int request_code, final int result_code, final Intent data) {
    if (request_code == 1 && this.mail_client_opened) {
      finish();
    }
  }

}
