package org.nypl.simplified.books.controller.api

/**
 * An exception indicating that book revoking failed because the DRM requires an activated
 * device.
 */

class BookRevokeExceptionDeviceNotActivated : BookRevokeException("Device not active")
