package org.nypl.simplified.cardcreator.validation;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;

import org.nypl.simplified.cardcreator.CardCreator;
import org.nypl.simplified.cardcreator.Constants;
import org.nypl.simplified.cardcreator.Prefs;
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

public class CreatePatronTask implements Runnable {

    private static final String TAG = "CreatePatronTask";

    private final Prefs mPrefs;

    private final AccountListenerType listener;
    private  final CardCreator card_creator;

    public CreatePatronTask(AccountListenerType in_listener, Prefs in_prefs, CardCreator in_card_creator) {
        this.mPrefs = in_prefs;
        this.listener = in_listener;
        this.card_creator = in_card_creator;
    }

    @Override
    public void run() {


        final HTTPType http = HTTP.newHTTP();
        URI uri = null;

        try {
            uri = new URI(this.card_creator.getUrl()).resolve("v1/create_patron");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        final OptionType<HTTPAuthType> auth =
          Option.some((HTTPAuthType) new HTTPAuthBasic(this.card_creator.getUsername(), this.card_creator.getPassword()));

        final ObjectNode card = JsonNodeFactory.instance.objectNode();
        card.set("name", JsonNodeFactory.instance.textNode(mPrefs.getString(Constants.NAME_DATA_KEY)));
        card.set("email", JsonNodeFactory.instance.textNode(mPrefs.getString(Constants.EMAIL_DATA_KEY)));
        card.set("pin", JsonNodeFactory.instance.textNode(mPrefs.getString(Constants.PIN_DATA_KEY)));
        card.set("username", JsonNodeFactory.instance.textNode(mPrefs.getString(Constants.USERNAME_DATA_KEY)));

        final ObjectNode address = JsonNodeFactory.instance.objectNode();
        address.set("line_1", JsonNodeFactory.instance.textNode(mPrefs.getString(Constants.STREET1_H_DATA_KEY)));
        address.set("line_2", JsonNodeFactory.instance.textNode(mPrefs.getString(Constants.STREET2_H_DATA_KEY)));
        address.set("city", JsonNodeFactory.instance.textNode(mPrefs.getString(Constants.CITY_H_DATA_KEY)));
        address.set("state", JsonNodeFactory.instance.textNode(mPrefs.getString(Constants.STATE_H_DATA_KEY)));
        address.set("zip", JsonNodeFactory.instance.textNode(mPrefs.getString(Constants.ZIP_H_DATA_KEY)));
        card.set("address", address);

        if (mPrefs.getBoolean(Constants.WORK_IN_NY_DATA_KEY) || mPrefs.getBoolean(Constants.SCHOOL_IN_NY_DATA_KEY)) {
            final ObjectNode work_address = JsonNodeFactory.instance.objectNode();
            work_address.set("line_1", JsonNodeFactory.instance.textNode(mPrefs.getString(Constants.STREET1_W_DATA_KEY)));
            work_address.set("line_2", JsonNodeFactory.instance.textNode(mPrefs.getString(Constants.STREET2_W_DATA_KEY)));
            work_address.set("city", JsonNodeFactory.instance.textNode(mPrefs.getString(Constants.CITY_W_DATA_KEY)));
            work_address.set("state", JsonNodeFactory.instance.textNode(mPrefs.getString(Constants.STATE_W_DATA_KEY)));
            work_address.set("zip", JsonNodeFactory.instance.textNode(mPrefs.getString(Constants.ZIP_W_DATA_KEY)));
            card.set("work_address", work_address);
        }


        final HTTPResultType<InputStream> result;

        try {
            String cardString = JSONSerializerUtilities.serializeToString(card);

            result = http.post(auth, uri, cardString.getBytes(), "application/json");

            result.matchResult(

                    new HTTPResultMatcherType<InputStream, Unit, Exception>() {
                        @Override
                        public Unit onHTTPError(HTTPResultError<InputStream> httpResultError) throws Exception {

                            CreatePatronTask.this.listener.onAccountCreationError(httpResultError.getMessage());

                            return Unit.unit();
                        }

                        @Override
                        public Unit onHTTPException(HTTPResultException<InputStream> httpResultException) throws Exception {

                            CreatePatronTask.this.listener.onAccountCreationError(httpResultException.getError().getMessage());

                            return Unit.unit();
                        }

                        @Override
                        public Unit onHTTPOK(HTTPResultOKType<InputStream> httpResultOKType) throws Exception {

                            NewPatronResponse nameResponse = new NewPatronResponse(httpResultOKType.getValue());

                            if (nameResponse.type.equals("card-granted")) {
                                CreatePatronTask.this.listener.onAccountCreationSucceeded(nameResponse);
                            } else {
                                CreatePatronTask.this.listener.onAccountCreationFailed(nameResponse);
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
