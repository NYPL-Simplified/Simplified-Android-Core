package org.nypl.simplified.app.profiles;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedActivity;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.controller.ProfilesControllerType;
import org.nypl.simplified.books.idle_timer.ProfileIdleTimeOutSoon;
import org.nypl.simplified.books.idle_timer.ProfileIdleTimedOut;
import org.nypl.simplified.books.profiles.ProfileEvent;
import org.nypl.simplified.observable.ObservableSubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An activity that handles profile inactivity timeouts. Does nothing if the anonymous
 * profile is enabled.
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
     * If profiles are enabled (ie, the anonymous profile is disabled), then subscribe to
     * profile timeout events.
     */

    final ProfilesControllerType profiles = Simplified.getProfilesController();
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

  private void onProfileEvent(final ProfileEvent event) {
    if (event instanceof ProfileIdleTimedOut) {
      LOG.debug("profile idle timer: timed out");
      UIThread.runOnUIThread(this::timedOut);
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

  private void timedOut() {

    /*
     * Dismiss any warning dialog that might be onscreen.
     */

    final ProfileTimeOutDialog dialog = this.warn_dialog;
    if (dialog != null) {
      this.warn_dialog = null;
      dialog.setListener(view -> {});
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
    Simplified.getProfilesController()
        .profileIdleTimer()
        .reset();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    final ProfilesControllerType profiles = Simplified.getProfilesController();
    switch (profiles.profileAnonymousEnabled()) {
      case ANONYMOUS_PROFILE_ENABLED: {
        break;
      }
      case ANONYMOUS_PROFILE_DISABLED: {
        this.profile_event_sub.unsubscribe();
        break;
      }
    }
  }
}
