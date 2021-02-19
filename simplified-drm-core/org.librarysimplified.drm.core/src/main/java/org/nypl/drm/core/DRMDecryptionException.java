package org.nypl.drm.core;

/**
 * An error occurred during content decryption.
 */

public final class DRMDecryptionException extends DRMException
{
    private static final long serialVersionUID = 1L;

    public DRMDecryptionException(final String message)
    {
        super(message);
    }

}
