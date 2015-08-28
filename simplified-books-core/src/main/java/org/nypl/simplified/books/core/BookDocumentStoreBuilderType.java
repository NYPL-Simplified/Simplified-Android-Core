package org.nypl.simplified.books.core;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Unit;

import java.io.IOException;
import java.io.InputStream;

/**
 * The type of mutable builders for document stores.
 */

public interface BookDocumentStoreBuilderType
{
  /**
   * Enable an EULA.
   *
   * @param f A function that returns an EULA document
   *
   * @throws IOException On I/O errors during initialization
   */

  void enableEULA(FunctionType<Unit, InputStream> f)
    throws IOException;

  /**
   * Enable a privacy policy.
   *
   * @param f A function that returns a privacy policy document
   *
   * @throws IOException On I/O errors during initialization
   */

  void enablePrivacyPolicy(FunctionType<Unit, InputStream> f)
    throws IOException;

  /**
   * Enable an acknowledgements document.
   *
   * @param f A function that returns an acknowledgements document
   *
   * @throws IOException On I/O errors during initialization
   */

  void enableAcknowledgements(FunctionType<Unit, InputStream> f)
    throws IOException;

  /**
   * @return A new document store configuration
   */

  DocumentStoreType build();
}
