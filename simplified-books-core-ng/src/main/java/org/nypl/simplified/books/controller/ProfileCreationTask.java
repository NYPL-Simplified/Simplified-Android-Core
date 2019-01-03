package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.Option;
import com.io7m.jnull.NullCheck;

import org.joda.time.LocalDate;
import org.nypl.simplified.books.accounts.AccountProvider;
import org.nypl.simplified.books.profiles.ProfileCreationEvent;
import org.nypl.simplified.books.profiles.ProfileCreationEvent.ProfileCreationFailed;
import org.nypl.simplified.books.profiles.ProfileCreationEvent.ProfileCreationSucceeded;
import org.nypl.simplified.books.profiles.ProfileEvent;
import org.nypl.simplified.books.profiles.ProfileType;
import org.nypl.simplified.books.profiles.ProfilesDatabaseType;
import org.nypl.simplified.observable.ObservableType;

import java.util.concurrent.Callable;

import static org.nypl.simplified.books.profiles.ProfileCreationEvent.ProfileCreationFailed.ErrorCode.ERROR_DISPLAY_NAME_ALREADY_USED;
import static org.nypl.simplified.books.profiles.ProfileCreationEvent.ProfileCreationFailed.ErrorCode.ERROR_GENERAL;

final class ProfileCreationTask implements Callable<ProfileCreationEvent> {

  private final ProfilesDatabaseType profiles;
  private final ObservableType<ProfileEvent> profile_events;
  private final String display_name;
  private final String gender;
  private final LocalDate date;
  private final AccountProvider account_provider;

  ProfileCreationTask(
      final ProfilesDatabaseType in_profiles,
      final ObservableType<ProfileEvent> in_profile_events,
      final AccountProvider in_account_provider,
      final String in_display_name,
      final String in_gender,
      final LocalDate in_date) {

    this.profiles =
        NullCheck.notNull(in_profiles, "Profiles");
    this.profile_events =
        NullCheck.notNull(in_profile_events, "Profile events");
    this.account_provider =
        NullCheck.notNull(in_account_provider, "Account provider");
    this.display_name =
        NullCheck.notNull(in_display_name, "Display name");
    this.gender =
        NullCheck.notNull(in_gender, "Gender");
    this.date =
        NullCheck.notNull(in_date, "Date");
  }

  private ProfileCreationEvent execute() {

    if (profiles.findProfileWithDisplayName(this.display_name).isSome()) {
      return ProfileCreationFailed.of(
          this.display_name, ERROR_DISPLAY_NAME_ALREADY_USED, Option.none());
    }

    try {
      final ProfileType profile =
          this.profiles.createProfile(this.account_provider, this.display_name);

      profile.preferencesUpdate(
          profile.preferences()
              .toBuilder()
              .setGender(this.gender)
              .setDateOfBirth(this.date)
              .build());

      return ProfileCreationSucceeded.of(this.display_name, profile.id());
    } catch (final Exception e) {
      return ProfileCreationFailed.of(this.display_name, ERROR_GENERAL, Option.some(e));
    }
  }

  @Override
  public ProfileCreationEvent call() throws Exception {
    final ProfileCreationEvent event = execute();
    this.profile_events.send(event);
    return event;
  }
}
