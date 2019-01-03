package org.nypl.simplified.books.document_store;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.books.authentication_document.AuthenticationDocument;
import org.nypl.simplified.books.authentication_document.AuthenticationDocumentType;
import org.nypl.simplified.books.authentication_document.AuthenticationDocumentValuesType;
import org.nypl.simplified.books.clock.ClockType;
import org.nypl.simplified.books.eula.EULA;
import org.nypl.simplified.books.eula.EULAType;
import org.nypl.simplified.books.synced_document.SyncedDocument;
import org.nypl.simplified.books.synced_document.SyncedDocumentType;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSAuthenticationDocumentParserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * The default implementation of the {@link DocumentStoreType} interface.
 */

public final class DocumentStore implements DocumentStoreType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(DocumentStore.class));
  }

  private final OptionType<SyncedDocumentType> privacy;
  private final OptionType<SyncedDocumentType> about;
  private final OptionType<SyncedDocumentType> acknowledgement;
  private final OptionType<SyncedDocumentType> licenses;
  private final AuthenticationDocumentType authentication;
  private final OptionType<EULAType>           eula;

  private DocumentStore(
    final OptionType<SyncedDocumentType> in_acknowledgement,
    final OptionType<SyncedDocumentType> in_privacy,
    final OptionType<SyncedDocumentType> in_about,
    final OptionType<SyncedDocumentType> in_licenses,
    final AuthenticationDocumentType in_authentication,
    final OptionType<EULAType> in_eula)
  {
    this.acknowledgement = NullCheck.notNull(in_acknowledgement);
    this.about = NullCheck.notNull(in_about);
    this.privacy = NullCheck.notNull(in_privacy);
    this.authentication = NullCheck.notNull(in_authentication);
    this.eula = NullCheck.notNull(in_eula);
    this.licenses = NullCheck.notNull(in_licenses);
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

  public static DocumentStoreBuilderType newBuilder(
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

  /**
   * Attempt to fetch the login form (synchronously) at {@code uri}. If the form
   * cannot be fetched, nothing happens.
   *
   * @param http An HTTP interface
   * @param docs A document store
   * @param uri  A URI
   */

  public static void fetchLoginForm(
    final DocumentStoreType docs,
    final HTTPType http,
    final URI uri)
  {
    DocumentStore.LOG.debug(
      "fetching login form on {}", uri);

    final OptionType<HTTPAuthType> no_auth = Option.none();
    final HTTPResultType<InputStream> r = http.get(no_auth, uri, 0L);
    r.matchResult(
      new HTTPResultMatcherType<InputStream, Unit, UnreachableCodeException>()
      {
        @Override public Unit onHTTPError(final HTTPResultError<InputStream> e)
        {
          DocumentStore.tryUpdateLoginForm(e, docs);
          return Unit.unit();
        }

        @Override
        public Unit onHTTPException(final HTTPResultException<InputStream> e)
        {
          DocumentStore.LOG.error(
            "error connecting to server: ", e.getError());
          return Unit.unit();
        }

        @Override public Unit onHTTPOK(final HTTPResultOKType<InputStream> e)
        {
          DocumentStore.LOG.error(
            "server returned status {} for unauthenticated fetch!",
            e.getStatus());
          return Unit.unit();
        }
      });
  }

  private static void tryUpdateLoginForm(
    final HTTPResultError<InputStream> e,
    final DocumentStoreType docs)
  {
    if (e.getStatus() == HttpURLConnection.HTTP_UNAUTHORIZED) {
      DocumentStore.LOG.debug(
        "login fetch returned {}", e.getStatus());

      String type = "application/octet-stream";
      final Map<String, List<String>> headers = e.getResponseHeaders();
      if (headers.containsKey("Content-Type")) {
        final List<String> types = headers.get("Content-Type");
        if (types.isEmpty() == false) {
          type = NullCheck.notNull(types.get(0));
        }
      }

      DocumentStore.LOG.debug("login fetch returned type {}", type);
      if ("application/vnd.opds.authentication.v1.0+json".equals(type)) {
        final AuthenticationDocumentType auth =
          docs.getAuthenticationDocument();
        auth.documentUpdate(e.getData());
      }
    }
  }

  @Override public OptionType<SyncedDocumentType> getPrivacyPolicy()
  {
    return this.privacy;
  }

  @Override public OptionType<SyncedDocumentType> getAbout()
  {
    return this.about;
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

  @Override public OptionType<SyncedDocumentType> getLicenses() {
    return this.licenses;
  }

  private static class Builder implements DocumentStoreBuilderType
  {
    private final File                                 base;
    private final AuthenticationDocumentValuesType     values;
    private final ClockType                            clock;
    private final HTTPType                             http;
    private final ExecutorService                      exec;
    private final OPDSAuthenticationDocumentParserType parser;
    private       OptionType<SyncedDocumentType>       privacy;
    private       OptionType<SyncedDocumentType>       acknowledgments;
    private       OptionType<SyncedDocumentType>       about;
    private       OptionType<SyncedDocumentType>       licenses;
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
      this.about = Option.none();
      this.acknowledgments = Option.none();
      this.eula = Option.none();
      this.licenses = Option.none();
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

    @Override
    public void enableLicenses(final FunctionType<Unit, InputStream> f)
            throws IOException
    {
      this.licenses = Option.some(
              SyncedDocument.newDocument(
                      this.clock,
                      this.http,
                      this.exec,
                      this.base,
                      "software-licenses.html",
                      f));
    }
    @Override
    public void enableAbout(final FunctionType<Unit, InputStream> f)
      throws IOException
    {
      this.about = Option.some(
        SyncedDocument.newDocument(
          this.clock,
          this.http,
          this.exec,
          this.base,
          "about.html",
          f));
    }

    @Override public DocumentStoreType build()
    {
      final AuthenticationDocumentType auth =
        AuthenticationDocument.newDocument(this.exec, this.parser, this.values);
      return new DocumentStore(
        this.acknowledgments, this.privacy, this.about, this.licenses, auth,  this.eula);
    }
  }
}
