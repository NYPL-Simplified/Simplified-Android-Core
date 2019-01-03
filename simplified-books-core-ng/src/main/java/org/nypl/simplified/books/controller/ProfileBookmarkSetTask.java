package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.profiles.ProfileEvent;
import org.nypl.simplified.books.profiles.ProfilePreferences;
import org.nypl.simplified.books.profiles.ProfilePreferencesChanged;
import org.nypl.simplified.books.profiles.ProfileType;
import org.nypl.simplified.books.reader.ReaderBookLocation;
import org.nypl.simplified.observable.ObservableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Callable;

final class ProfileBookmarkSetTask implements Callable<Unit> {

  private static final Logger LOG = LoggerFactory.getLogger(ProfileBookmarkSetTask.class);

  private final ProfileType profile;
  private final BookID book_id;
  private final ReaderBookLocation new_location;
  private final ObservableType<ProfileEvent> events;

  ProfileBookmarkSetTask(
      final ProfileType profile,
      final ObservableType<ProfileEvent> events,
      final BookID book_id,
      final ReaderBookLocation new_location) {

    this.profile =
        NullCheck.notNull(profile, "Profile");
    this.events =
        NullCheck.notNull(events, "Events");
    this.book_id =
        NullCheck.notNull(book_id, "Book ID");
    this.new_location =
        NullCheck.notNull(new_location, "Location");
  }

  @Override
  public Unit call() throws IOException {

    try {
      LOG.debug("[{}] saving bookmark {}", this.book_id.brief(), this.new_location);

      final ProfilePreferences preferences = profile.preferences();

      this.profile.preferencesUpdate(
          preferences.withReaderBookmarks(
              preferences.readerBookmarks().withBookmark(book_id, new_location)));

      this.events.send(ProfilePreferencesChanged.builder()
          .setChangedReaderBookmarks(true)
          .setChangedReaderPreferences(false)
          .build());

    } catch (final Exception e) {
      LOG.error("[{}] could not save bookmark: ", this.book_id.brief(), e);
      throw e;
    }

    return Unit.unit();
  }
}
