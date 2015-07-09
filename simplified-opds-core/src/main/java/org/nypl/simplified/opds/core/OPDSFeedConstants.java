package org.nypl.simplified.opds.core;

import java.net.URI;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

public final class OPDSFeedConstants
{
  public static final URI    ACQUISITION_URI_PREFIX;
  public static final String ACQUISITION_URI_PREFIX_TEXT;
  public static final URI    ATOM_URI;
  public static final String ATOM_URI_TEXT;
  public static final URI    DUBLIN_CORE_TERMS_URI;
  public static final URI    FACET_URI;
  public static final String FACET_URI_TEXT;
  public static final URI    GROUP_URI;
  public static final String GROUP_URI_TEXT;
  public static final URI    IMAGE_URI;
  public static final String IMAGE_URI_TEXT;
  public static final URI    OPDS_URI;
  public static final String OPDS_URI_TEXT;
  public static final URI    THUMBNAIL_URI;
  public static final String THUMBNAIL_URI_TEXT;
  public static final String DUBLIN_CORE_TERMS_URI_TEXT;
  public static final String SCHEMA_URI_TEXT;
  public static final URI    SCHEMA_URI;
  public static final URI    SIMPLIFIED_URI;
  public static final String SIMPLIFIED_URI_TEXT;

  static {
    ATOM_URI = NullCheck.notNull(URI.create("http://www.w3.org/2005/Atom"));
    ATOM_URI_TEXT = NullCheck.notNull(OPDSFeedConstants.ATOM_URI.toString());

    DUBLIN_CORE_TERMS_URI =
      NullCheck.notNull(URI.create("http://purl.org/dc/terms/"));
    DUBLIN_CORE_TERMS_URI_TEXT =
      NullCheck.notNull(OPDSFeedConstants.DUBLIN_CORE_TERMS_URI.toString());

    ACQUISITION_URI_PREFIX =
      NullCheck.notNull(URI.create("http://opds-spec.org/acquisition"));
    ACQUISITION_URI_PREFIX_TEXT =
      NullCheck.notNull(OPDSFeedConstants.ACQUISITION_URI_PREFIX.toString());

    FACET_URI = NullCheck.notNull(URI.create("http://opds-spec.org/facet"));
    FACET_URI_TEXT =
      NullCheck.notNull(OPDSFeedConstants.FACET_URI.toString());

    GROUP_URI = NullCheck.notNull(URI.create("http://opds-spec.org/group"));
    GROUP_URI_TEXT =
      NullCheck.notNull(OPDSFeedConstants.GROUP_URI.toString());

    OPDS_URI =
      NullCheck.notNull(URI.create("http://opds-spec.org/2010/catalog"));
    OPDS_URI_TEXT = NullCheck.notNull(OPDSFeedConstants.OPDS_URI.toString());

    THUMBNAIL_URI =
      NullCheck.notNull(URI.create("http://opds-spec.org/image/thumbnail"));
    THUMBNAIL_URI_TEXT =
      NullCheck.notNull(OPDSFeedConstants.THUMBNAIL_URI.toString());

    IMAGE_URI = NullCheck.notNull(URI.create("http://opds-spec.org/image"));
    IMAGE_URI_TEXT =
      NullCheck.notNull(OPDSFeedConstants.IMAGE_URI.toString());

    SCHEMA_URI = NullCheck.notNull(URI.create("http://schema.org/"));
    SCHEMA_URI_TEXT =
      NullCheck.notNull(OPDSFeedConstants.SCHEMA_URI.toString());

    SIMPLIFIED_URI =
      NullCheck.notNull(URI.create("http://librarysimplified.org/terms/"));
    SIMPLIFIED_URI_TEXT =
      NullCheck.notNull(OPDSFeedConstants.SIMPLIFIED_URI.toString());
  }

  private OPDSFeedConstants()
  {
    throw new UnreachableCodeException();
  }
}
