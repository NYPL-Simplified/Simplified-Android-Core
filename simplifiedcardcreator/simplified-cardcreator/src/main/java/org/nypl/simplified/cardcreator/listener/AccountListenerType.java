package org.nypl.simplified.cardcreator.listener;

import org.nypl.simplified.cardcreator.model.NewPatronResponse;

/**
 * Created by aferditamuriqi on 8/26/16.
 *
 */

public interface AccountListenerType {

    /**
     * @param response new patron response
     */
    void onAccountCreationSucceeded(NewPatronResponse response);

    /**
     * @param response  new patron response
     */
    void onAccountCreationFailed(NewPatronResponse response);

    /**
     * @param message message
     */
    void onAccountCreationError(final String message);

}
