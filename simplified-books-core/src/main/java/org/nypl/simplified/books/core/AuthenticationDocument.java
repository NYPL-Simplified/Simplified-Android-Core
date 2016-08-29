package org.nypl.simplified.books.core;

import com.io7m.jnull.NullCheck;
import org.nypl.simplified.opds.core.OPDSAuthenticationDocument;
import org.nypl.simplified.opds.core.OPDSAuthenticationDocumentLabels;
import org.nypl.simplified.opds.core.OPDSAuthenticationDocumentParserType;
import org.nypl.simplified.opds.core.OPDSParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The default implementation of the {@link AuthenticationDocumentType}.
 */

public final class AuthenticationDocument implements AuthenticationDocumentType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(AuthenticationDocument.class));
  }

  private final AuthenticationDocumentValuesType            defaults;
  private final AtomicReference<OPDSAuthenticationDocument> data;
  private final OPDSAuthenticationDocumentParserType        parser;
  private final ExecutorService                             exec;

  private AuthenticationDocument(
    final ExecutorService in_exec,
    final OPDSAuthenticationDocumentParserType in_parser,
    final AuthenticationDocumentValuesType in_defaults)
  {
    this.exec = NullCheck.notNull(in_exec);
    this.parser = NullCheck.notNull(in_parser);
    this.defaults = NullCheck.notNull(in_defaults);
    this.data = new AtomicReference<OPDSAuthenticationDocument>();
  }

  /**
   * @param in_exec     An executor
   * @param in_parser   An OPDS authentication document parser
   * @param in_defaults Default values for the document
   *
   * @return A new authentication document
   */

  public static AuthenticationDocumentType newDocument(
    final ExecutorService in_exec,
    final OPDSAuthenticationDocumentParserType in_parser,
    final AuthenticationDocumentValuesType in_defaults)
  {
    return new AuthenticationDocument(in_exec, in_parser, in_defaults);
  }

  @Override public String getLabelLoginUserID()
  {
    final OPDSAuthenticationDocument opds = this.data.get();
    if (opds != null) {
      final Map<String, String> labels = opds.getLabels();
      final String name =
        OPDSAuthenticationDocumentLabels.LABEL_LOGIN.getName();
      if (labels.containsKey(name)) {
        return NullCheck.notNull(labels.get(name));
      }
    }

    return this.defaults.getLabelLoginUserID();
  }

  @Override public String getLabelLoginPassword()
  {
    final OPDSAuthenticationDocument opds = this.data.get();
    if (opds != null) {
      final Map<String, String> labels = opds.getLabels();
      final String name =
        OPDSAuthenticationDocumentLabels.LABEL_PASSWORD.getName();
      if (labels.containsKey(name)) {
        return NullCheck.notNull(labels.get(name));
      }
    }

    return this.defaults.getLabelLoginPassword();
  }

  @Override
  public String getLabelLoginPatronName() {
    final OPDSAuthenticationDocument opds = this.data.get();
    if (opds != null) {
      final Map<String, String> labels = opds.getLabels();
      final String name =
        OPDSAuthenticationDocumentLabels.LABEL_NAME.getName();
      if (labels.containsKey(name)) {
        return NullCheck.notNull(labels.get(name));
      }
    }

    return this.defaults.getLabelLoginPatronName();
  }

  @Override public void documentUpdate(final InputStream s)
  {
    NullCheck.notNull(s);

    LOG.debug("submitting document update");
    this.exec.submit(
      new Runnable()
      {
        @Override public void run()
        {
          try {
            AuthenticationDocument.this.update(s);
          } catch (final OPDSParseException e) {
            AuthenticationDocument.LOG.error("could not update document: ", e);
          }
        }
      });
  }

  private void update(final InputStream s)
    throws OPDSParseException
  {
    LOG.debug("updating document");
    this.data.set(this.parser.parseFromStream(s));
  }
}
