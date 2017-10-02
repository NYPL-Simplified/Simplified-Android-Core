package org.nypl.simplified.cardcreator.validation;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;

import org.nypl.simplified.cardcreator.CardCreator;
import org.nypl.simplified.cardcreator.R;
import org.nypl.simplified.prefs.Prefs;
import org.nypl.simplified.cardcreator.listener.AccountListenerType;
import org.nypl.simplified.cardcreator.model.NewPatronResponse;
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

public class CreatePatronTask {

    private final Prefs prefs;

    private final AccountListenerType listener;
    private  final CardCreator card_creator;

    /**
     * @param in_listener account listener
     * @param in_prefs share prefs
     * @param in_card_creator card creator
     */
    public CreatePatronTask(final AccountListenerType in_listener, final Prefs in_prefs, final CardCreator in_card_creator) {
        this.prefs = in_prefs;
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
            uri = new URI(this.card_creator.getUrl()).resolve(this.card_creator.getVersion() + "/create_patron");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        final OptionType<HTTPAuthType> auth =
          Option.some((HTTPAuthType) new HTTPAuthBasic(this.card_creator.getUsername(), this.card_creator.getPassword()));

        final ObjectNode card = JsonNodeFactory.instance.objectNode();
        card.set("name", JsonNodeFactory.instance.textNode(this.prefs.getString(this.card_creator.getResources().getString(R.string.LAST_NAME_DATA_KEY)) + ", "
          + this.prefs.getString(this.card_creator.getResources().getString(R.string.FIRST_NAME_DATA_KEY))));

        if (!this.prefs.getString(this.card_creator.getResources().getString(R.string.MIDDLE_NAME_DATA_KEY)).isEmpty())
        {
            card.set("name", JsonNodeFactory.instance.textNode(this.prefs.getString(this.card_creator.getResources().getString(R.string.LAST_NAME_DATA_KEY)) + ", "
              + this.prefs.getString(this.card_creator.getResources().getString(R.string.FIRST_NAME_DATA_KEY)) + " "
              + this.prefs.getString(this.card_creator.getResources().getString(R.string.MIDDLE_NAME_DATA_KEY))));
        }

        card.set("email", JsonNodeFactory.instance.textNode(this.prefs.getString(this.card_creator.getResources().getString(R.string.EMAIL_DATA_KEY))));
        card.set("pin", JsonNodeFactory.instance.textNode(this.prefs.getString(this.card_creator.getResources().getString(R.string.PIN_DATA_KEY))));
        card.set("username", JsonNodeFactory.instance.textNode(this.prefs.getString(this.card_creator.getResources().getString(R.string.USERNAME_DATA_KEY))));

        final ObjectNode address = JsonNodeFactory.instance.objectNode();
        address.set("line_1", JsonNodeFactory.instance.textNode(this.prefs.getString(this.card_creator.getResources().getString(R.string.STREET1_H_DATA_KEY))));
        address.set("line_2", JsonNodeFactory.instance.textNode(this.prefs.getString(this.card_creator.getResources().getString(R.string.STREET2_H_DATA_KEY))));
        address.set("city", JsonNodeFactory.instance.textNode(this.prefs.getString(this.card_creator.getResources().getString(R.string.CITY_H_DATA_KEY))));
        address.set("state", JsonNodeFactory.instance.textNode(this.prefs.getString(this.card_creator.getResources().getString(R.string.STATE_H_DATA_KEY))));
        address.set("zip", JsonNodeFactory.instance.textNode(this.prefs.getString(this.card_creator.getResources().getString(R.string.ZIP_H_DATA_KEY))));
        card.set("address", address);

        if (this.prefs.getBoolean(this.card_creator.getResources().getString(R.string.WORK_IN_NY_DATA_KEY))
          || this.prefs.getBoolean(this.card_creator.getResources().getString(R.string.SCHOOL_IN_NY_DATA_KEY))) {
            final ObjectNode work_address = JsonNodeFactory.instance.objectNode();
            work_address.set("line_1", JsonNodeFactory.instance.textNode(this.prefs.getString(this.card_creator.getResources().getString(R.string.STREET1_W_DATA_KEY))));
            work_address.set("line_2", JsonNodeFactory.instance.textNode(this.prefs.getString(this.card_creator.getResources().getString(R.string.STREET2_W_DATA_KEY))));
            work_address.set("city", JsonNodeFactory.instance.textNode(this.prefs.getString(this.card_creator.getResources().getString(R.string.CITY_W_DATA_KEY))));
            work_address.set("state", JsonNodeFactory.instance.textNode(this.prefs.getString(this.card_creator.getResources().getString(R.string.STATE_W_DATA_KEY))));
            work_address.set("zip", JsonNodeFactory.instance.textNode(this.prefs.getString(this.card_creator.getResources().getString(R.string.ZIP_W_DATA_KEY))));
            card.set("work_address", work_address);
        }


        final HTTPResultType<InputStream> result;

        try {
            final String card_string = JSONSerializerUtilities.serializeToString(card);

            result = http.post(auth, uri, card_string.getBytes(), "application/json");

            result.matchResult(

              new HTTPResultMatcherType<InputStream, Unit, Exception>() {
                  @Override
                  public Unit onHTTPError(final HTTPResultError<InputStream> error) throws Exception {

                      CreatePatronTask.this.listener.onAccountCreationError(error.getMessage());

                      return Unit.unit();
                  }

                  @Override
                  public Unit onHTTPException(final HTTPResultException<InputStream> exceptin) throws Exception {

                      CreatePatronTask.this.listener.onAccountCreationError(exceptin.getError().getMessage());

                      return Unit.unit();
                  }

                  @Override
                  public Unit onHTTPOK(final HTTPResultOKType<InputStream> result) throws Exception {

                      final NewPatronResponse name_response = new NewPatronResponse(result.getValue());

                      if (name_response.getType().equals("card-granted")) {
                          CreatePatronTask.this.listener.onAccountCreationSucceeded(name_response);
                      } else {
                          CreatePatronTask.this.listener.onAccountCreationFailed(name_response);
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
