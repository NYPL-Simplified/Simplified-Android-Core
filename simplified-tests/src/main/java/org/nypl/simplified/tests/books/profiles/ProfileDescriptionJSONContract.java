package org.nypl.simplified.tests.books.profiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.profiles.ProfileDescriptionJSON;
import org.nypl.simplified.profiles.api.ProfileDescription;
import org.nypl.simplified.profiles.api.ProfilePreferences;
import org.nypl.simplified.reader.api.ReaderPreferences;

public abstract class ProfileDescriptionJSONContract {

  @Test
  public final void testRoundTrip()
      throws Exception {

    final ObjectMapper mapper = new ObjectMapper();

    final ProfileDescription description_0 =
        ProfileDescription.builder(
            "Kermit",
            ProfilePreferences.builder()
                .setDateOfBirth(new LocalDate(1985, 1, 1))
                .setReaderPreferences(ReaderPreferences.builder().build())
                .build()).build();

    final ObjectNode node =
        ProfileDescriptionJSON.serializeToJSON(mapper, description_0);
    final ProfileDescription description_1 =
        ProfileDescriptionJSON.deserializeFromJSON(mapper, node);

    Assert.assertEquals(description_0, description_1);
  }

}
