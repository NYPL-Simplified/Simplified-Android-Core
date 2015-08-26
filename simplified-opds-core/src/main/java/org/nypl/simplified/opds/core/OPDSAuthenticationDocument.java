package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * An OPDS 1.2 authentication document.
 */

public final class OPDSAuthenticationDocument
{
  private final List<URI>             types;
  private final String                title;
  private final String                id;
  private final OptionType<String>    text_prompt;
  private final Map<String, OPDSLink> links;
  private final Map<String, String>   labels;

  /**
   * Construct an authentication document.
   *
   * @param in_id          The ID of the catalog provider
   * @param in_types       The list of authentication types
   * @param in_title       The title of the catalog
   * @param in_text_prompt An optional text prompt
   * @param in_links       A set of links
   * @param in_labels      A set of labels
   */

  public OPDSAuthenticationDocument(
    final String in_id,
    final List<URI> in_types,
    final String in_title,
    final OptionType<String> in_text_prompt,
    final Map<String, OPDSLink> in_links,
    final Map<String, String> in_labels)
  {
    this.id = NullCheck.notNull(in_id);
    this.types = NullCheck.notNull(in_types);
    this.title = NullCheck.notNull(in_title);
    this.text_prompt = NullCheck.notNull(in_text_prompt);
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

  /**
   * @return An optional text prompt
   */

  public OptionType<String> getTextPrompt()
  {
    return this.text_prompt;
  }

  /**
   * @return The title of the catalog
   */

  public String getTitle()
  {
    return this.title;
  }

  /**
   * @return The list of authentication types
   */

  public List<URI> getTypes()
  {
    return this.types;
  }

  @Override public String toString()
  {
    final StringBuilder sb = new StringBuilder("OPDSAuthenticationDocument{");
    sb.append("id='").append(this.id).append('\'');
    sb.append(", types=").append(this.types);
    sb.append(", title='").append(this.title).append('\'');
    sb.append(", text_prompt=").append(this.text_prompt);
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
    if (!this.types.equals(that.types)) {
      return false;
    }
    if (!this.title.equals(that.title)) {
      return false;
    }
    if (!this.id.equals(that.id)) {
      return false;
    }
    if (!this.text_prompt.equals(that.text_prompt)) {
      return false;
    }
    if (!this.links.equals(that.links)) {
      return false;
    }
    return this.labels.equals(that.labels);
  }

  @Override public int hashCode()
  {
    int result = this.types.hashCode();
    result = 31 * result + this.title.hashCode();
    result = 31 * result + this.id.hashCode();
    result = 31 * result + this.text_prompt.hashCode();
    result = 31 * result + this.links.hashCode();
    result = 31 * result + this.labels.hashCode();
    return result;
  }
}
