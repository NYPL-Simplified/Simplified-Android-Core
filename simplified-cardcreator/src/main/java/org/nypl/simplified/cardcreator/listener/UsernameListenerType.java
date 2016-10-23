package org.nypl.simplified.cardcreator.listener;

import org.nypl.simplified.cardcreator.model.UsernameResponse;

/**
 * Created by aferditamuriqi on 8/26/16.
 *
 */

public interface UsernameListenerType {

    void onUsernameValidationSucceeded(UsernameResponse response);

    void onUsernameValidationFailed(UsernameResponse response);

    void onUsernameValidationError(String message);

}
