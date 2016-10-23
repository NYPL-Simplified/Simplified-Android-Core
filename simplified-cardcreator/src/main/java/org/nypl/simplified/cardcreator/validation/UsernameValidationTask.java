package org.nypl.simplified.cardcreator.validation;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;

import org.nypl.simplified.cardcreator.CardCreator;
import org.nypl.simplified.cardcreator.Constants;
import org.nypl.simplified.cardcreator.listener.UsernameListenerType;
import org.nypl.simplified.cardcreator.model.UsernameResponse;
import org.nypl.simplified.http.core.HTTP;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.json.core.JSONSerializerUtilities;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by aferditamuriqi on 8/26/16.
 *
 */

public class UsernameValidationTask implements Runnable {

  private static final String TAG = "UsernameValidationTask";

  private final String username;

  private final UsernameListenerType listener;

  private final CardCreator card_creator;

  public UsernameValidationTask(UsernameListenerType in_listener, String in_username, CardCreator in_card_creator) {
    this.username = in_username;
    this.listener = in_listener;
    this.card_creator = in_card_creator;
  }

  @Override
  public void run() {

    HTTPType http = HTTP.newHTTP();
    URI uri = null;

    try {
      uri = new URI(this.card_creator.getUrl()).resolve("v1/validate/username");
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }

    final OptionType<HTTPAuthType> auth =
      Option.some((HTTPAuthType) new HTTPAuthBasic(this.card_creator.getUsername(), this.card_creator.getPassword()));

    final ObjectNode name = JsonNodeFactory.instance.objectNode();

    name.set("username", JsonNodeFactory.instance.textNode(this.username.toString()));

    final HTTPResultType<InputStream> result;

    try {
      String user = JSONSerializerUtilities.serializeToString(name);

      result = http.post( auth, uri, user.getBytes(), "application/json");

      result.matchResult(

        new HTTPResultMatcherType<InputStream, Unit, Exception>() {
          @Override
          public Unit onHTTPError(HTTPResultError<InputStream> httpResultError) throws Exception {
            UsernameValidationTask.this.listener.onUsernameValidationError(httpResultError.getMessage());
            return Unit.unit();
          }

          @Override
          public Unit onHTTPException(HTTPResultException<InputStream> httpResultException) throws Exception {
            UsernameValidationTask.this.listener.onUsernameValidationError(httpResultException.getError().getMessage());
            return Unit.unit();
          }

          @Override
          public Unit onHTTPOK(HTTPResultOKType<InputStream> httpResultOKType) throws Exception {

            UsernameResponse nameResponse = new UsernameResponse(httpResultOKType.getValue());

            if (nameResponse.type.equals("available-username")) {
              UsernameValidationTask.this.listener.onUsernameValidationSucceeded(nameResponse);
            } else {
              UsernameValidationTask.this.listener.onUsernameValidationFailed(nameResponse);
            }

            return Unit.unit();
          }
        }
      );
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}
