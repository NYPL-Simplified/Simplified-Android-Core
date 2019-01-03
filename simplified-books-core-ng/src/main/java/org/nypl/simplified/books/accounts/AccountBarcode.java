package org.nypl.simplified.books.accounts;

import com.google.auto.value.AutoValue;

/**
 * The type of account barcodes.
 * <p>
 * Account barcodes are expected to be 5-14 digit numbers, but the type does not
 * (currently) enforce this fact.
 */

@AutoValue
public abstract class AccountBarcode {

  AccountBarcode() {

  }

  /**
   * Construct a barcode.
   *
   * @param in_value The raw barcode value
   */

  public static AccountBarcode create(String in_value) {
    return new AutoValue_AccountBarcode(in_value);
  }

  /**
   * @return The actual barcode value
   */

  public abstract String value();

  @Override
  public String toString() {
    return value();
  }
}
