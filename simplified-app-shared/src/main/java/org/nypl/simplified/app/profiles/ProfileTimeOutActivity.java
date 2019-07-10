package org.nypl.simplified.app.profiles;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedActivity;
import org.nypl.simplified.app.splash.SplashActivity;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.observable.ObservableSubscriptionType;
import org.nypl.simplified.profiles.api.ProfileEvent;
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimeOutSoon;
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimedOut;
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An activity that handles profile inactivity timeouts. Timeouts are not processed if the
 * anonymous profile is active.
 * <p>
 * Additionally, the activity checks to see if the profile system has been properly initialized
 * and redirects the user back to the profile selection screen if it hasn't. The reason for this
 * check is due to the awful Android architecture: When an app crashes, the app is restarted with
 * the activity stack intact, minus the activity that caused the crash. This means that if the
 * user restarts the app, they will never even see the profile selection screen and the profile
 * system will not be initialized.
 */

public abstract class ProfileTimeOutActivity extends SimplifiedActivity {

  private static final Logger LOG = LoggerFactory.getLogger(ProfileTimeOutActivity.class);

  private ObservableSubscriptionType<ProfileEvent> profile_event_sub;
  private boolean foreground;
  private ProfileTimeOutDialog warn_dialog;

  @Override
  protected void onPause() {
    super.onPause();
    this.foreground = false;
  }

  @Override
  protected void onResume() {
    super.onResume();
    this.foreground = true;
  }

  @Override
  protected void onCreate(final @Nullable Bundle state) {
    super.onCreate(state);

    /*
     * If the profile system appears not to be initialized, then send the user back to the
     * profile screen. The reason this code is necessary is due to Anroid's horrendous approach
     * to crash recovery, and will be made obsolete when the application is rewritten to use
     * a single activity for all screens.
     */

    boolean booted =
      Simplified.Companion.getApplication()
        .getServicesBooting()
        .isDone();

    /*
     * If the application has not booted then we are presumably recovering from a crash
     * and have been shoved back into the middle of the application without having
     * gone via the splash screen. Go back to the splash screen!
     */

    if (!booted) {
      this.openSplashScreen();
      this.finish();
    }
  }

  @Override
  protected void onStart() {
    super.onStart();

    /*
     * If profiles are enabled (ie, the anonymous profile is disabled), then subscribe to
     * profile timeout events.
     */

    final ProfilesControllerType profiles =
      Simplified.getServices().getProfilesController();

    switch (profiles.profileAnonymousEnabled()) {
      case ANONYMOUS_PROFILE_ENABLED: {
        break;
      }
      case ANONYMOUS_PROFILE_DISABLED: {
        this.profile_event_sub = profiles.profileEvents().subscribe(this::onProfileEvent);
        break;
      }
    }
  }

  @Override
  protected void onStop() {
    super.onStop();

    if (this.profile_event_sub != null) {
      this.profile_event_sub.unsubscribe();
      this.profile_event_sub = null;
    }
  }

  private void onProfileEvent(final ProfileEvent event) {
    if (event instanceof ProfileIdleTimedOut) {
      LOG.debug("profile idle timer: timed out");
      UIThread.runOnUIThread(this::goToProfileSelection);
    }
    if (event instanceof ProfileIdleTimeOutSoon) {
      LOG.debug("profile idle timer: time out warning");
      UIThread.runOnUIThread(this::timeOutSoon);
    }
  }

  private void timeOutSoon() {

    /*
     * If this is the foreground activity, warn the user that they'll be logged out unless they
     * explicitly close the dialog.
     */

    if (this.foreground) {
      this.warn_dialog = ProfileTimeOutDialog.newDialog(view -> resetTimer());
      this.warn_dialog.show(this.getFragmentManager(), "profile-timeout-dialog");
    }
  }

  private void goToProfileSelection() {

    /*
     * Dismiss any warning dialog that might be onscreen.
     */

    final ProfileTimeOutDialog dialog = this.warn_dialog;
    if (dialog != null) {
      this.warn_dialog = null;
      dialog.setListener(view -> {
      });
      dialog.dismiss();
    }

    /*
     * Only the foreground activity is responsible for opening the profile selection screen.
     */

    if (this.foreground) {
      this.openProfileSelection();
    }

    UIThread.runOnUIThreadDelayed(this::finish, 500L);
  }

  private void openSplashScreen() {
    LOG.debug("opening splash screen");
    final Intent intent = new Intent();
    intent.setClass(this, SplashActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    this.startActivity(intent);
  }

  private void openProfileSelection() {
    LOG.debug("opening profile selection screen");
    final Intent intent = new Intent();
    intent.setClass(this, ProfileSelectionActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    this.startActivity(intent);
  }

  @Override
  public void onUserInteraction() {
    super.onUserInteraction();

    /*
     * Each time the user interacts with something onscreen, reset the timer.
     */

    this.resetTimer();
  }

  private void resetTimer() {
    LOG.debug("reset profile idle timer");
    Simplified.getServices()
      .getProfilesController()
      .profileIdleTimer()
      .reset();
  }
}
