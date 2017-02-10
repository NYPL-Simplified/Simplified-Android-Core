package org.nypl.simplified.cardcreator.validation;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;

import org.nypl.simplified.cardcreator.CardCreator;
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

public class UsernameValidationTask {

  private static final String TAG = "UsernameValidationTask";

  private final String username;

  private final UsernameListenerType listener;

  private final CardCreator card_creator;

  /**
   * @param in_listener username listener
   * @param in_username username
   * @param in_card_creator card creator
   */

  public UsernameValidationTask(final UsernameListenerType in_listener, final String in_username, final CardCreator in_card_creator) {
    this.username = in_username;
    this.listener = in_listener;
    this.card_creator = in_card_creator;
  }

  /**
   *
   */
  public void run() {

    final HTTPType http = HTTP.newHTTP();
    URI uri = null;

    try {
      uri = new URI(this.card_creator.getUrl()).resolve(this.card_creator.getVersion() + "/validate/username");
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }

    final OptionType<HTTPAuthType> auth =
      Option.some((HTTPAuthType) new HTTPAuthBasic(this.card_creator.getUsername(), this.card_creator.getPassword()));

    final ObjectNode obj = JsonNodeFactory.instance.objectNode();

    obj.set("username", JsonNodeFactory.instance.textNode(this.username));

    final HTTPResultType<InputStream> result;

    try {
      final String body = JSONSerializerUtilities.serializeToString(obj);

      result = http.post(auth, uri, body.getBytes(), "application/json");

      result.matchResult(

        new HTTPResultMatcherType<InputStream, Unit, Exception>() {
          @Override
          public Unit onHTTPError(final HTTPResultError<InputStream> error) throws Exception {
            UsernameValidationTask.this.listener.onUsernameValidationError(error.getMessage());
            return Unit.unit();
          }

          @Override
          public Unit onHTTPException(final HTTPResultException<InputStream> exception) throws Exception {
            UsernameValidationTask.this.listener.onUsernameValidationError(exception.getError().getMessage());
            return Unit.unit();
          }

          @Override
          public Unit onHTTPOK(final HTTPResultOKType<InputStream> result) throws Exception {

            final UsernameResponse name_response = new UsernameResponse(result.getValue());

            if (name_response.getType().equals("available-username")) {
              UsernameValidationTask.this.listener.onUsernameValidationSucceeded(name_response);
            } else {
              UsernameValidationTask.this.listener.onUsernameValidationFailed(name_response);
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
