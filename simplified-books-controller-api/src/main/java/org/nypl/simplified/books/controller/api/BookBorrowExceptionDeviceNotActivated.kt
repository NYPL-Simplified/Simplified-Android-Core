package org.nypl.simplified.books.controller.api

/**
 * An exception indicating that book borrowing failed because the DRM requires an activated
 * device.
 */

class BookBorrowExceptionDeviceNotActivated : BookBorrowException("Device not active")
