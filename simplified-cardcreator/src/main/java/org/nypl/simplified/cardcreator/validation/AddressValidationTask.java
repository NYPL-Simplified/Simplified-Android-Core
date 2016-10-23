package org.nypl.simplified.cardcreator.validation;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;

import org.nypl.simplified.cardcreator.CardCreator;
import org.nypl.simplified.cardcreator.Constants;
import org.nypl.simplified.cardcreator.listener.AddressListenerType;
import org.nypl.simplified.cardcreator.model.AddressResponse;
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

public class AddressValidationTask implements Runnable {

    private static final String TAG = "AddressValidationTask";

    private final AddressListenerType listener;

    private final String line_1;
    private final String line_2;
    private final String city;
    private final String state;
    private final String zip;

    private final Boolean is_work_address;

    private final CardCreator card_creator;

    public AddressValidationTask(AddressListenerType in_listener,
                                 String in_line_1,
                                 String in_line_2,
                                 String in_city,
                                 String in_state,
                                 String in_zip,
                                 boolean in_is_work_address,
                                 CardCreator in_card_creator) {
        this.line_1 = in_line_1;
        this.line_2 = in_line_2;
        this.city = in_city;
        this.state = in_state;
        this.zip = in_zip;
        this.listener = in_listener;
        this.is_work_address = in_is_work_address;
        this.card_creator = in_card_creator;

    }

    @Override
    public void run() {

        HTTPType http = HTTP.newHTTP();
        URI uri = null;

        try {
            uri = new URI(this.card_creator.getUrl()).resolve("v1/validate/address");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        final OptionType<HTTPAuthType> auth =
          Option.some((HTTPAuthType) new HTTPAuthBasic(this.card_creator.getUsername(), this.card_creator.getPassword()));

        final ObjectNode address = JsonNodeFactory.instance.objectNode();
        address.set("line_1", JsonNodeFactory.instance.textNode(this.line_1.toString()));
        address.set("line_2", JsonNodeFactory.instance.textNode(this.line_2.toString()));
        address.set("city", JsonNodeFactory.instance.textNode(this.city.toString()));
        address.set("state", JsonNodeFactory.instance.textNode(this.state.toString()));
        address.set("zip", JsonNodeFactory.instance.textNode(this.zip.toString()));


        final ObjectNode name = JsonNodeFactory.instance.objectNode();
        name.set("address", address);
        name.set("is_work_or_school_address", JsonNodeFactory.instance.booleanNode(this.is_work_address));


        final HTTPResultType<InputStream> result;

        try {
            String user = JSONSerializerUtilities.serializeToString(name);

            result = http.post(auth, uri, user.getBytes(), "application/json");

            result.matchResult(

                    new HTTPResultMatcherType<InputStream, Unit, Exception>() {
                        @Override
                        public Unit onHTTPError(HTTPResultError<InputStream> httpResultError) throws Exception {
                            AddressValidationTask.this.listener.onAddressValidationError(httpResultError.getMessage());
                            return Unit.unit();
                        }

                        @Override
                        public Unit onHTTPException(HTTPResultException<InputStream> httpResultException) throws Exception {
                            AddressValidationTask.this.listener.onAddressValidationError(httpResultException.getError().getMessage());
                            return Unit.unit();
                        }

                        @Override
                        public Unit onHTTPOK(HTTPResultOKType<InputStream> httpResultOKType) throws Exception {

                            AddressResponse addressResponse = new AddressResponse(httpResultOKType.getValue());

                            if (addressResponse.type.equals("valid-address") || addressResponse.type.equals("alternate-addresses")) {
                                AddressValidationTask.this.listener.onAddressValidationSucceeded(addressResponse);
                            } else {
                                AddressValidationTask.this.listener.onAddressValidationFailed(addressResponse);
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
