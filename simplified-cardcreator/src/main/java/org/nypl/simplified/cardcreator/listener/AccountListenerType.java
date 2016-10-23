package org.nypl.simplified.cardcreator.listener;

import org.nypl.simplified.cardcreator.model.NewPatronResponse;

/**
 * Created by aferditamuriqi on 8/26/16.
 *
 */

public interface AccountListenerType {

    void onAccountCreationSucceeded(NewPatronResponse response);

    void onAccountCreationFailed(NewPatronResponse response);

    void onAccountCreationError(String message);

}
