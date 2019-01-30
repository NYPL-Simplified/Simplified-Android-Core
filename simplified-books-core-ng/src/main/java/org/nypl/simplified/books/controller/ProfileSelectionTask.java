package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.profiles.ProfileAnonymousEnabledException;
import org.nypl.simplified.books.profiles.ProfileEvent;
import org.nypl.simplified.books.profiles.ProfileID;
import org.nypl.simplified.books.profiles.ProfileNonexistentException;
import org.nypl.simplified.books.profiles.ProfileSelected;
import org.nypl.simplified.books.profiles.ProfilesDatabaseType;
import org.nypl.simplified.observable.ObservableType;

import java.util.concurrent.Callable;

import static org.nypl.simplified.books.profiles.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED;

final class ProfileSelectionTask implements Callable<Unit> {

  private final ProfilesDatabaseType profiles;
  private final ProfileID profile_id;
  private final ObservableType<ProfileEvent> profile_events;

  ProfileSelectionTask(
      final ProfilesDatabaseType in_profiles,
      final ObservableType<ProfileEvent> in_events,
      final ProfileID in_id) {

    this.profiles =
        NullCheck.notNull(in_profiles, "Profiles");
    this.profile_events =
        NullCheck.notNull(in_events, "Events");
    this.profile_id =
        NullCheck.notNull(in_id, "ID");
  }

  @Override
  public Unit call() throws ProfileNonexistentException, ProfileAnonymousEnabledException {
    if (this.profiles.anonymousProfileEnabled() == ANONYMOUS_PROFILE_ENABLED) {
      this.profile_events.send(ProfileSelected.of());
      return Unit.unit();
    }

    this.profiles.setProfileCurrent(this.profile_id);
    this.profile_events.send(ProfileSelected.of());
    return Unit.unit();
  }
}
