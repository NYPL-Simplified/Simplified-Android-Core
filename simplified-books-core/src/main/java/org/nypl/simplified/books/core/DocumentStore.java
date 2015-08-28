package org.nypl.simplified.books.core;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSAuthenticationDocumentParserType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

/**
 * The default implementation of the {@link DocumentStoreType} interface.
 */

public final class DocumentStore implements DocumentStoreType
{
  private final OptionType<SyncedDocumentType> privacy;
  private final OptionType<SyncedDocumentType> acknowledgement;
  private final AuthenticationDocumentType     authentication;
  private final OptionType<EULAType>           eula;

  private DocumentStore(
    final OptionType<SyncedDocumentType> in_acknowledgement,
    final OptionType<SyncedDocumentType> in_privacy,
    final AuthenticationDocumentType in_authentication,
    final OptionType<EULAType> in_eula)
  {
    this.acknowledgement = NullCheck.notNull(in_acknowledgement);
    this.privacy = NullCheck.notNull(in_privacy);
    this.authentication = NullCheck.notNull(in_authentication);
    this.eula = NullCheck.notNull(in_eula);
  }

  /**
   * @param in_clock       A clock
   * @param in_http        An HTTP interface
   * @param in_exec        An executor
   * @param in_base        The base directory for the document store
   * @param in_auth_values The default authentication document values
   * @param in_parser      An OPDS authentication document parser
   *
   * @return A new document store builder
   */

  public static BookDocumentStoreBuilderType newBuilder(
    final ClockType in_clock,
    final HTTPType in_http,
    final ExecutorService in_exec,
    final File in_base,
    final AuthenticationDocumentValuesType in_auth_values,
    final OPDSAuthenticationDocumentParserType in_parser)
  {
    return new Builder(
      in_clock, in_http, in_exec, in_base, in_auth_values, in_parser);
  }

  @Override public OptionType<SyncedDocumentType> getPrivacyPolicy()
  {
    return this.privacy;
  }

  @Override public OptionType<SyncedDocumentType> getAcknowledgements()
  {
    return this.acknowledgement;
  }

  @Override public AuthenticationDocumentType getAuthenticationDocument()
  {
    return this.authentication;
  }

  @Override public OptionType<EULAType> getEULA()
  {
    return this.eula;
  }

  private static class Builder implements BookDocumentStoreBuilderType
  {
    private final File                                 base;
    private final AuthenticationDocumentValuesType     values;
    private final ClockType                            clock;
    private final HTTPType                             http;
    private final ExecutorService                      exec;
    private final OPDSAuthenticationDocumentParserType parser;
    private       OptionType<SyncedDocumentType>       privacy;
    private       OptionType<SyncedDocumentType>       acknowledgments;
    private       OptionType<EULAType>                 eula;

    Builder(
      final ClockType in_clock,
      final HTTPType in_http,
      final ExecutorService in_exec,
      final File in_base,
      final AuthenticationDocumentValuesType in_auth_values,
      final OPDSAuthenticationDocumentParserType in_parser)
    {
      this.clock = NullCheck.notNull(in_clock);
      this.http = NullCheck.notNull(in_http);
      this.exec = NullCheck.notNull(in_exec);
      this.base = NullCheck.notNull(in_base);
      this.values = NullCheck.notNull(in_auth_values);
      this.parser = NullCheck.notNull(in_parser);

      this.privacy = Option.none();
      this.acknowledgments = Option.none();
      this.eula = Option.none();
    }

    @Override public void enableEULA(final FunctionType<Unit, InputStream> f)
      throws IOException
    {
      this.eula = Option.some(
        EULA.newEULA(this.clock, this.http, this.exec, this.base, f));
    }

    @Override
    public void enablePrivacyPolicy(final FunctionType<Unit, InputStream> f)
      throws IOException
    {
      this.privacy = Option.some(
        SyncedDocument.newDocument(
          this.clock, this.http, this.exec, this.base, "privacy.html", f));
    }

    @Override
    public void enableAcknowledgements(final FunctionType<Unit, InputStream> f)
      throws IOException
    {
      this.acknowledgments = Option.some(
        SyncedDocument.newDocument(
          this.clock,
          this.http,
          this.exec,
          this.base,
          "acknowledgments.html",
          f));
    }

    @Override public DocumentStoreType build()
    {
      final AuthenticationDocumentType auth =
        AuthenticationDocument.newDocument(this.exec, this.parser, this.values);
      return new DocumentStore(
        this.acknowledgments, this.privacy, auth, this.eula);
    }
  }
}
