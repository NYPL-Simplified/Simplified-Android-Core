package org.nypl.simplified.cardcreator.listener;

import org.nypl.simplified.cardcreator.model.UsernameResponse;

/**
 * Created by aferditamuriqi on 8/26/16.
 *
 */

public interface UsernameListenerType {

    /**
     * @param response username response
     */
    void onUsernameValidationSucceeded(UsernameResponse response);

    /**
     * @param response username response
     */
    void onUsernameValidationFailed(UsernameResponse response);

    /**
     * @param message message
     */
    void onUsernameValidationError(String message);

}
