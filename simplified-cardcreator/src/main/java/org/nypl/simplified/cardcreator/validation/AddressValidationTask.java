package org.nypl.simplified.cardcreator.validation;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;

import org.nypl.simplified.cardcreator.CardCreator;
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

    private final AddressListenerType listener;

    private final String line_1;
    private final String line_2;
    private final String city;
    private final String state;
    private final String zip;

    private final Boolean is_work_address;

    private final CardCreator card_creator;

    /**
     * @param in_listener address listener
     * @param in_line_1 address line 1
     * @param in_line_2 address line 2
     * @param in_city address state
     * @param in_state address state
     * @param in_zip address zip
     * @param in_is_work_address address is work address
     * @param in_card_creator card creator
     */
    public AddressValidationTask(final AddressListenerType in_listener,
                                 final String in_line_1,
                                 final String in_line_2,
                                 final String in_city,
                                 final String in_state,
                                 final String in_zip,
                                 final boolean in_is_work_address,
                                 final CardCreator in_card_creator) {
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

        URI uri = null;

        try {
            uri = new URI(this.card_creator.getUrl()).resolve("/validate/address");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }


        final ObjectNode address = JsonNodeFactory.instance.objectNode();
        address.set("line_1", JsonNodeFactory.instance.textNode(this.line_1));
        address.set("line_2", JsonNodeFactory.instance.textNode(this.line_2));
        address.set("city", JsonNodeFactory.instance.textNode(this.city));
        address.set("state", JsonNodeFactory.instance.textNode(this.state));
        address.set("zip", JsonNodeFactory.instance.textNode(this.zip));


        final ObjectNode obj = JsonNodeFactory.instance.objectNode();
        obj.set("address", address);
        obj.set("is_work_or_school_address", JsonNodeFactory.instance.booleanNode(this.is_work_address));

        final HTTPType http = HTTP.newHTTP();

        final OptionType<HTTPAuthType> auth =
          Option.some((HTTPAuthType) new HTTPAuthBasic(this.card_creator.getUsername(), this.card_creator.getPassword()));

        final HTTPResultType<InputStream> result;

        try {
            final String body = JSONSerializerUtilities.serializeToString(obj);

            result = http.post(auth, uri, body.getBytes(), "application/json");
            result.matchResult(

                    new HTTPResultMatcherType<InputStream, Unit, Exception>() {
                        @Override
                        public Unit onHTTPError(final HTTPResultError<InputStream> error) throws Exception {
                            AddressValidationTask.this.listener.onAddressValidationError(error.getMessage());
                            return Unit.unit();
                        }

                        @Override
                        public Unit onHTTPException(final HTTPResultException<InputStream> exception) throws Exception {
                            AddressValidationTask.this.listener.onAddressValidationError(exception.getError().getMessage());
                            return Unit.unit();
                        }

                        @Override
                        public Unit onHTTPOK(final HTTPResultOKType<InputStream> result) throws Exception {

                            final AddressResponse address_response = new AddressResponse(result.getValue());

                            if (address_response.getType().equals("valid-address") || address_response.getType().equals("alternate-addresses")) {
                                AddressValidationTask.this.listener.onAddressValidationSucceeded(address_response);
                            } else {
                                AddressValidationTask.this.listener.onAddressValidationFailed(address_response);
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
