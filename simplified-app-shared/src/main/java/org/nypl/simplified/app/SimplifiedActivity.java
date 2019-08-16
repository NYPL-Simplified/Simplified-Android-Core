package org.nypl.simplified.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The base activity type used by all activities in the project.
 */

public abstract class SimplifiedActivity extends AppCompatActivity {

  private static final Logger LOG = LoggerFactory.getLogger(SimplifiedActivity.class);

  /**
   * @return {@code true} if this activity is the last activity remaining in the application
   */

  protected final boolean isLastActivity()
  {
    return ACTIVITY_COUNT <= 1;
  }

  private static int ACTIVITY_COUNT;

  @Override
  protected void onCreate(final @Nullable Bundle state) {
    super.onCreate(state);

    LOG.debug("onCreate: {}", state);

    this.overridePendingTransition(R.anim.activity_open, R.anim.activity_close);
    ACTIVITY_COUNT += 1;
    LOG.debug("activity count: {}", ACTIVITY_COUNT);
  }

  @Override
  public void finish() {
    super.finish();

    if (this.isLastActivity()) {
      this.overridePendingTransition(0, R.anim.activity_external_close);
    } else {
      this.overridePendingTransition(R.anim.activity_open, R.anim.activity_close);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    ACTIVITY_COUNT -= 1;
    LOG.debug("activity count: {}", ACTIVITY_COUNT);
  }

  @Override
  protected void onResume() {
    super.onResume();
    overridePendingTransition(R.anim.activity_open, R.anim.activity_close);
  }

  @Override
  protected void onStart() {
    super.onStart();
    overridePendingTransition(R.anim.activity_open, R.anim.activity_close);
  }

  @Override
  protected void onNewIntent(final Intent intent) {
    super.onNewIntent(intent);
    overridePendingTransition(R.anim.activity_open, R.anim.activity_close);
  }

  @Override
  public void startActivityForResult(
      final Intent intent,
      final int requestCode) {
    super.startActivityForResult(intent, requestCode);
    this.overridePendingTransition(R.anim.activity_open, R.anim.activity_close);
  }

  @Override
  public void startActivityForResult(
      final Intent intent,
      final int requestCode,
      final @Nullable Bundle options) {
    super.startActivityForResult(intent, requestCode, options);
    this.overridePendingTransition(R.anim.activity_open, R.anim.activity_close);
  }

  @Override
  public void startActivity(final Intent intent) {
    super.startActivity(intent);
    this.overridePendingTransition(R.anim.activity_open, R.anim.activity_close);
  }

  @Override
  public void startActivity(
      final Intent intent,
      final @Nullable  Bundle options) {
    super.startActivity(intent, options);
    this.overridePendingTransition(R.anim.activity_open, R.anim.activity_close);
  }
}
