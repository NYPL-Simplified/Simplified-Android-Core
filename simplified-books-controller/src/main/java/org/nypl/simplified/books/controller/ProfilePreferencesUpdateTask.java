package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.profiles.api.ProfileEvent;
import org.nypl.simplified.profiles.api.ProfilePreferences;
import org.nypl.simplified.profiles.api.ProfilePreferencesChanged;
import org.nypl.simplified.profiles.api.ProfileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Callable;

import io.reactivex.subjects.Subject;

final class ProfilePreferencesUpdateTask implements Callable<Unit> {

  private static final Logger LOG = LoggerFactory.getLogger(ProfilePreferencesUpdateTask.class);

  private final ProfileType profile;
  private final ProfilePreferences preferences;
  private final Subject<ProfileEvent> events;

  ProfilePreferencesUpdateTask(
    final Subject<ProfileEvent> events,
    final ProfileType profile,
    final ProfilePreferences preferences) {

    this.events =
      NullCheck.notNull(events, "Events");
    this.profile =
      NullCheck.notNull(profile, "Profile");
    this.preferences =
      NullCheck.notNull(preferences, "Preferences");
  }

  @Override
  public Unit call() throws Exception {

    try {
      final ProfilePreferences old_prefs = this.profile.preferences();
      this.profile.preferencesUpdate(this.preferences);
      this.events.onNext(
        ProfilePreferencesChanged.builder()
          .setChangedReaderPreferences(
            !old_prefs.readerPreferences().equals(this.preferences.readerPreferences()))
          .build());
    } catch (final IOException e) {
      LOG.error("could not update preferences: ", e);
    }
    return Unit.unit();
  }
}
