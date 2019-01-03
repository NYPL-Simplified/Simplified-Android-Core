package org.nypl.simplified.tests.books.profiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.books.profiles.ProfileDescription;
import org.nypl.simplified.books.profiles.ProfileDescriptionJSON;
import org.nypl.simplified.books.profiles.ProfilePreferences;
import org.nypl.simplified.books.profiles.ProfilePreferencesJSON;
import org.nypl.simplified.books.reader.ReaderPreferences;

public abstract class ProfilePreferencesJSONContract {

  @Test
  public final void testRoundTrip()
      throws Exception {

    final ObjectMapper mapper = new ObjectMapper();

    final ProfilePreferences preferences_0 =
        ProfilePreferences.builder()
            .setDateOfBirth(new LocalDate(1985, 1, 1))
            .setReaderPreferences(ReaderPreferences.builder().build())
            .build();

    final ObjectNode node =
        ProfilePreferencesJSON.serializeToJSON(mapper, preferences_0);
    final ProfilePreferences preferences_1 =
        ProfilePreferencesJSON.deserializeFromJSON(mapper, node);

    Assert.assertEquals(preferences_0, preferences_1);
  }

}
