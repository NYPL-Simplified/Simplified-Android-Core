package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jnull.Nullable;

import org.joda.time.DateTime;
import org.nypl.simplified.parser.api.ParseError;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The type of OPDS acquisition feeds.
 */

public final class OPDSAcquisitionFeed implements Serializable {
  private static final long serialVersionUID = 1L;

  private final List<OPDSAcquisitionFeedEntry> entries;
  private final Map<String, List<OPDSFacet>> facets_by_group;
  private final List<OPDSFacet> facets_order;
  private final Map<String, OPDSGroup> groups;
  private final List<String> groups_order;
  private final String id;
  private final OptionType<URI> next;
  private final OptionType<OPDSSearchLink> search;
  private final String title;
  private final DateTime updated;
  private final URI uri;
  private final OptionType<URI> terms_of_service;
  private final OptionType<URI> about;
  private final OptionType<URI> licenses;
  private final OptionType<DRMLicensor> licensor;
  private final OptionType<URI> privacy_policy;
  private final List<ParseError> errors;

  private OPDSAcquisitionFeed(
    final URI in_uri,
    final List<OPDSAcquisitionFeedEntry> in_entries,
    final Map<String, OPDSGroup> in_groups,
    final List<String> in_groups_order,
    final String in_id,
    final DateTime in_updated,
    final String in_title,
    final OptionType<URI> in_next,
    final OptionType<OPDSSearchLink> in_search,
    final List<OPDSFacet> in_facets_order,
    final Map<String, List<OPDSFacet>> in_facets,
    final OptionType<URI> in_terms_of_service,
    final OptionType<URI> in_about,
    final OptionType<URI> in_privacy_policy,
    final OptionType<URI> in_licenses,
    final OptionType<DRMLicensor> in_licensor,
    final List<ParseError> in_errors) {
    this.uri =
      Objects.requireNonNull(in_uri);
    this.entries =
      Objects.requireNonNull(Collections.unmodifiableList(in_entries));
    this.facets_order =
      Objects.requireNonNull(Collections.unmodifiableList(in_facets_order));
    this.facets_by_group =
      Objects.requireNonNull(Collections.unmodifiableMap(in_facets));
    this.groups =
      Objects.requireNonNull(Collections.unmodifiableMap(in_groups));
    this.groups_order =
      Objects.requireNonNull(Collections.unmodifiableList(in_groups_order));
    this.id =
      Objects.requireNonNull(in_id);
    this.updated =
      Objects.requireNonNull(in_updated);
    this.title =
      Objects.requireNonNull(in_title);
    this.next =
      Objects.requireNonNull(in_next);
    this.search =
      Objects.requireNonNull(in_search);
    this.terms_of_service =
      Objects.requireNonNull(in_terms_of_service);
    this.about =
      Objects.requireNonNull(in_about);
    this.privacy_policy =
      Objects.requireNonNull(in_privacy_policy);
    this.licenses =
      Objects.requireNonNull(in_licenses);
    this.licensor = in_licensor;
    this.errors =
      Objects.requireNonNull(in_errors, "in_errors");
  }

  /**
   * Construct an acquisition feed builder.
   *
   * @param in_uri     The feed URI
   * @param in_id      The feed ID
   * @param in_updated The feed updated date
   * @param in_title   The feed title
   * @return A new builder
   */

  public static OPDSAcquisitionFeedBuilderType newBuilder(
    final URI in_uri,
    final String in_id,
    final DateTime in_updated,
    final String in_title) {
    return new Builder(in_uri, in_title, in_id, in_updated);
  }

  @Override
  public boolean equals(
    final @Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final OPDSAcquisitionFeed other = (OPDSAcquisitionFeed) obj;
    return this.uri.equals(other.uri)
      && this.entries.equals(other.entries)
      && this.facets_by_group.equals(other.facets_by_group)
      && this.facets_order.equals(other.facets_order)
      && this.groups.equals(other.groups)
      && this.groups_order.equals(other.groups_order)
      && this.id.equals(other.id)
      && this.title.equals(other.title)
      && this.updated.equals(other.updated)
      && this.next.equals(other.next)
      && this.search.equals(other.search)
      && this.terms_of_service.equals(other.terms_of_service)
      && this.about.equals(other.about)
      && this.privacy_policy.equals(other.privacy_policy);
  }

  /**
   * @return The list of feed entries
   */

  public List<OPDSAcquisitionFeedEntry> getFeedEntries() {
    return this.entries;
  }

  /**
   * @return The feed facets, by group
   */

  public Map<String, List<OPDSFacet>> getFeedFacetsByGroup() {
    return this.facets_by_group;
  }

  /**
   * @return The feed facets, in order
   */

  public List<OPDSFacet> getFeedFacetsOrder() {
    return this.facets_order;
  }

  /**
   * @return The feed groups, by name
   */

  public Map<String, OPDSGroup> getFeedGroups() {
    return this.groups;
  }

  /**
   * @return The feed groups, in declaration order
   */

  public List<String> getFeedGroupsOrder() {
    return this.groups_order;
  }

  /**
   * @return The feed ID
   */

  public String getFeedID() {
    return this.id;
  }

  /**
   * @return The link to the next feed, if any
   */

  public OptionType<URI> getFeedNext() {
    return this.next;
  }

  /**
   * @return The search document, if any
   */

  public OptionType<OPDSSearchLink> getFeedSearchURI() {
    return this.search;
  }

  /**
   * @return The feed title
   */

  public String getFeedTitle() {
    return this.title;
  }

  /**
   * @return The feed update time
   */

  public DateTime getFeedUpdated() {
    return this.updated;
  }

  /**
   * @return The link to the app about, if any
   */

  public OptionType<URI> getFeedAbout() {
    return this.about;
  }

  /**
   * @return The link to the app about, if any
   */

  public OptionType<URI> getFeedLicenses() {
    return this.licenses;
  }

  /**
   * @return licensor information used to active adobe device
   */
  public OptionType<DRMLicensor> getLicensor() {
    return this.licensor;
  }

  /**
   * @return The link to the terms of service, if any
   */

  public OptionType<URI> getFeedTermsOfService() {
    return this.terms_of_service;
  }

  /**
   * @return The link to the privacy policy, if any
   */

  public OptionType<URI> getFeedPrivacyPolicy() {
    return this.privacy_policy;
  }

  /**
   * @return The feed URI
   */

  public URI getFeedURI() {
    return this.uri;
  }

  /**
   * @return The parse errors
   */

  public List<ParseError> getErrors() {
    return this.errors;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.uri.hashCode();
    result = (prime * result) + this.entries.hashCode();
    result = (prime * result) + this.groups.hashCode();
    result = (prime * result) + this.groups_order.hashCode();
    result = (prime * result) + this.facets_by_group.hashCode();
    result = (prime * result) + this.facets_order.hashCode();
    result = (prime * result) + this.id.hashCode();
    result = (prime * result) + this.title.hashCode();
    result = (prime * result) + this.updated.hashCode();
    result = (prime * result) + this.next.hashCode();
    result = (prime * result) + this.search.hashCode();
    result = (prime * result) + this.terms_of_service.hashCode();
    result = (prime * result) + this.about.hashCode();
    result = (prime * result) + this.privacy_policy.hashCode();
    result = (prime * result) + this.licenses.hashCode();
    return result;
  }

  private static final class Builder implements OPDSAcquisitionFeedBuilderType {
    private final List<OPDSAcquisitionFeedEntry> entries;
    private final Map<String, List<OPDSFacet>> facets_by_group;
    private final List<OPDSFacet> facets_order;
    private final Map<String, URI> group_uris;
    private final Map<String, List<OPDSAcquisitionFeedEntry>> groups;
    private final List<String> groups_order;
    private final String id;
    private final String title;
    private final DateTime updated;
    private final URI uri;
    private OptionType<URI> next;
    private OptionType<OPDSSearchLink> search;
    private OptionType<URI> terms_of_service;
    private OptionType<URI> privacy_policy;
    private OptionType<URI> about;
    private OptionType<URI> licenses;
    private OptionType<DRMLicensor> licensor;
    private final List<ParseError> errors;

    private Builder(
      final URI in_uri,
      final String in_title,
      final String in_id,
      final DateTime in_updated) {
      this.uri = Objects.requireNonNull(in_uri);
      this.title = Objects.requireNonNull(in_title);
      this.id = Objects.requireNonNull(in_id);
      this.updated = Objects.requireNonNull(in_updated);
      this.entries = new ArrayList<OPDSAcquisitionFeedEntry>(32);
      this.facets_order = new ArrayList<OPDSFacet>(4);
      this.facets_by_group = new HashMap<String, List<OPDSFacet>>(4);
      this.groups_order = new ArrayList<String>(32);
      this.groups = new HashMap<String, List<OPDSAcquisitionFeedEntry>>(32);
      this.group_uris = new HashMap<String, URI>(32);
      this.next = Option.none();
      this.search = Option.none();
      this.terms_of_service = Option.none();
      this.privacy_policy = Option.none();
      this.about = Option.none();
      this.licenses = Option.none();
      this.licensor = Option.none();
      this.errors = new ArrayList<>();
    }

    @Override
    public OPDSAcquisitionFeedBuilderType addParseError(ParseError error) {
      this.errors.add(Objects.requireNonNull(error, "error"));
      return this;
    }

    @Override
    public OPDSAcquisitionFeedBuilderType addEntry(
      final OPDSAcquisitionFeedEntry e) {
      Objects.requireNonNull(e);

      final Set<Pair<String, URI>> in_groups = e.getGroups();
      if (in_groups.isEmpty()) {
        this.entries.add(e);
      } else {
        for (final Pair<String, URI> b : in_groups) {
          Objects.requireNonNull(b);
          final String b_name = b.getLeft();
          final URI b_uri = b.getRight();

          final List<OPDSAcquisitionFeedEntry> es;
          if (this.groups.containsKey(b_name)) {
            es = Objects.requireNonNull(this.groups.get(b_name));
          } else {
            es = new ArrayList<OPDSAcquisitionFeedEntry>(32);
            this.groups_order.add(b_name);
          }

          es.add(e);
          this.groups.put(b_name, es);
          this.group_uris.put(b_name, b_uri);
        }
      }

      this.errors.addAll(e.getErrors());
      return this;
    }

    @Override
    public OPDSAcquisitionFeedBuilderType addFacet(
      final OPDSFacet f) {
      Objects.requireNonNull(f);

      final String group = f.getGroup();
      final List<OPDSFacet> fs;
      if (this.facets_by_group.containsKey(group)) {
        fs = Objects.requireNonNull(this.facets_by_group.get(group));
      } else {
        fs = new ArrayList<OPDSFacet>(4);
      }
      fs.add(f);
      this.facets_by_group.put(group, fs);
      this.facets_order.add(f);
      return this;
    }

    @Override
    public OPDSAcquisitionFeedBuilderType setAboutOption(final OptionType<URI> u) {
      this.about = Objects.requireNonNull(u);
      return this;
    }

    @Override
    public OPDSAcquisitionFeedBuilderType setTermsOfServiceOption(final OptionType<URI> u) {
      this.terms_of_service = Objects.requireNonNull(u);
      return this;
    }

    @Override
    public OPDSAcquisitionFeedBuilderType setLisensor(final OptionType<DRMLicensor> in_licensor) {
      this.licensor = in_licensor;
      return this;
    }

    @Override
    public OPDSAcquisitionFeedBuilderType setPrivacyPolicyOption(final OptionType<URI> u) {
      this.privacy_policy = Objects.requireNonNull(u);
      return this;
    }

    @Override
    public OPDSAcquisitionFeed build() {
      final Map<String, OPDSGroup> r_groups =
        new HashMap<String, OPDSGroup>(this.groups.size());

      for (final String name : this.groups.keySet()) {
        final String nn_name = Objects.requireNonNull(name);
        final List<OPDSAcquisitionFeedEntry> in_entries =
          Objects.requireNonNull(this.groups.get(nn_name));
        final URI in_uri = Objects.requireNonNull(this.group_uris.get(nn_name));
        r_groups.put(nn_name, new OPDSGroup(nn_name, in_uri, in_entries));
      }

      return new OPDSAcquisitionFeed(
        this.uri,
        this.entries,
        r_groups,
        this.groups_order,
        this.id,
        this.updated,
        this.title,
        this.next,
        this.search,
        this.facets_order,
        this.facets_by_group,
        this.terms_of_service,
        this.about,
        this.privacy_policy,
        this.licenses,
        this.licensor,
        this.errors);
    }

    @Override
    public OPDSAcquisitionFeedBuilderType setNextOption(
      final OptionType<URI> in_next) {
      this.next = Objects.requireNonNull(in_next);
      return this;
    }

    @Override
    public OPDSAcquisitionFeedBuilderType setSearchOption(
      final OptionType<OPDSSearchLink> in_search) {
      this.search = Objects.requireNonNull(in_search);
      return this;
    }
  }
}
