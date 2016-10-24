package org.nypl.simplified.cardcreator.listener;

import org.nypl.simplified.cardcreator.model.AddressResponse;

/**
 * Created by aferditamuriqi on 8/26/16.
 *
 */

public interface AddressListenerType {

    /**
     * @param response address response
     */
    void onAddressValidationSucceeded(AddressResponse response);

    /**
     * @param response address response
     */
    void onAddressValidationFailed(AddressResponse response);

    /**
     * @param message message
     */
    void onAddressValidationError(String message);

}
