package org.nypl.simplified.cardcreator.listener;

import org.nypl.simplified.cardcreator.model.AddressResponse;

/**
 * Created by aferditamuriqi on 8/26/16.
 *
 */

public interface AddressListenerType {

    void onAddressValidationSucceeded(AddressResponse response);

    void onAddressValidationFailed(AddressResponse response);

    void onAddressValidationError(String message);

}
