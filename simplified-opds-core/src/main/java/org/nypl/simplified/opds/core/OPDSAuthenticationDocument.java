package org.nypl.simplified.opds.core;

import com.io7m.jnull.NullCheck;

import java.util.Map;

/**
 * An OPDS 1.2 authentication document.
 */

public final class OPDSAuthenticationDocument
{
  private final String                id;
  private final Map<String, OPDSLink> links;
  private final Map<String, String>   labels;

  /**
   * Construct an authentication document.
   *
   * @param in_id          The ID of the catalog provider
   * @param in_links       A set of links
   * @param in_labels      A set of labels
   */

  public OPDSAuthenticationDocument(
    final String in_id,
    final Map<String, OPDSLink> in_links,
    final Map<String, String> in_labels)
  {
    this.id = NullCheck.notNull(in_id);
    this.links = NullCheck.notNull(in_links);
    this.labels = NullCheck.notNull(in_labels);
  }

  /**
   * @return The ID of the catalog provider
   */

  public String getId()
  {
    return this.id;
  }

  /**
   * @return A set of labels
   */

  public Map<String, String> getLabels()
  {
    return this.labels;
  }

  /**
   * @return A set of links
   */

  public Map<String, OPDSLink> getLinks()
  {
    return this.links;
  }


  @Override public String toString()
  {
    final StringBuilder sb = new StringBuilder("OPDSAuthenticationDocument{");
    sb.append("id='").append(this.id).append('\'');
    sb.append(", links=").append(this.links);
    sb.append(", labels=").append(this.labels);
    sb.append('}');
    return sb.toString();
  }

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final OPDSAuthenticationDocument that = (OPDSAuthenticationDocument) o;
    if (!this.id.equals(that.id)) {
      return false;
    }
    if (!this.links.equals(that.links)) {
      return false;
    }
    return this.labels.equals(that.labels);
  }

  @Override public int hashCode()
  {
    int result = this.id.hashCode();
    result = 31 * result + this.links.hashCode();
    result = 31 * result + this.labels.hashCode();
    return result;
  }
}
