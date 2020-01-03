package org.nypl.simplified.documents.store;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.clock.ClockType;
import org.nypl.simplified.documents.eula.EULA;
import org.nypl.simplified.documents.eula.EULAType;
import org.nypl.simplified.documents.synced.SyncedDocument;
import org.nypl.simplified.documents.synced.SyncedDocumentType;
import org.nypl.simplified.http.core.HTTPType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

/**
 * The default implementation of the {@link DocumentStoreType} interface.
 */

public final class DocumentStore implements DocumentStoreType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(DocumentStore.class));
  }

  private final OptionType<SyncedDocumentType> privacy;
  private final OptionType<SyncedDocumentType> about;
  private final OptionType<SyncedDocumentType> acknowledgement;
  private final OptionType<SyncedDocumentType> licenses;
  private final OptionType<EULAType>           eula;

  private DocumentStore(
    final OptionType<SyncedDocumentType> in_acknowledgement,
    final OptionType<SyncedDocumentType> in_privacy,
    final OptionType<SyncedDocumentType> in_about,
    final OptionType<SyncedDocumentType> in_licenses,
    final OptionType<EULAType> in_eula)
  {
    this.acknowledgement = NullCheck.notNull(in_acknowledgement);
    this.about = NullCheck.notNull(in_about);
    this.privacy = NullCheck.notNull(in_privacy);
    this.eula = NullCheck.notNull(in_eula);
    this.licenses = NullCheck.notNull(in_licenses);
  }

  /**
   * @param in_clock       A clock
   * @param in_http        An HTTP interface
   * @param in_exec        An executor
   * @param in_base        The base directory for the document store
   * @return A new document store builder
   */

  public static DocumentStoreBuilderType newBuilder(
    final ClockType in_clock,
    final HTTPType in_http,
    final ExecutorService in_exec,
    final File in_base)
  {
    return new Builder(in_clock, in_http, in_exec, in_base);
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
    private final ClockType clock;
    private final HTTPType                             http;
    private final ExecutorService                      exec;
    private       OptionType<SyncedDocumentType>       privacy;
    private       OptionType<SyncedDocumentType>       acknowledgments;
    private       OptionType<SyncedDocumentType>       about;
    private       OptionType<SyncedDocumentType>       licenses;
    private       OptionType<EULAType>                 eula;

    Builder(
      final ClockType in_clock,
      final HTTPType in_http,
      final ExecutorService in_exec,
      final File in_base)
    {
      this.clock = NullCheck.notNull(in_clock);
      this.http = NullCheck.notNull(in_http);
      this.exec = NullCheck.notNull(in_exec);
      this.base = NullCheck.notNull(in_base);

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
      return new DocumentStore(this.acknowledgments, this.privacy, this.about, this.licenses, this.eula);
    }
  }
}
