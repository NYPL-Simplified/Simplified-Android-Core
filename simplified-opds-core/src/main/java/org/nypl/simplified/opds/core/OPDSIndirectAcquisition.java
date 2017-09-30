package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import java.io.Serializable;
import java.net.URI;

/**
 * A specific OPDS acquisition.
 *
 * http://opds-spec.org/specs/opds-catalog-1-1-20110627/#Acquisition_Feeds
 */

public final class OPDSIndirectAcquisition implements Serializable
{
  private static final long serialVersionUID = 1L;
  private final OptionType<String> type;
  private OptionType<DRMLicensor> licensor;
  private final OptionType<URI> download_url;
  private final OptionType<String> ccid;


  /**
   * Construct an acquisition.
   *
   * @param in_type The type
   * @param in_licensor      DRM Licensor
   * @param in_download_url download link
   * @param in_ccid urms ccid
   */

  public OPDSIndirectAcquisition(
    final OptionType<String> in_type,
    final OptionType<DRMLicensor> in_licensor,
    final OptionType<URI> in_download_url,
    final OptionType<String> in_ccid)
  {
    this.type = NullCheck.notNull(in_type);
    this.licensor = NullCheck.notNull(in_licensor);
    this.download_url = NullCheck.notNull(in_download_url);
    this.ccid = NullCheck.notNull(in_ccid);
  }

  @Override public boolean equals(
    final @Nullable Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final OPDSIndirectAcquisition other = (OPDSIndirectAcquisition) obj;
    return (this.type == other.type && this.download_url == other.download_url && this.ccid == other.ccid);
  }

  /**
   * @return The type of the acquisition
   */

  public OptionType<String> getType()
  {
    return this.type;
  }


  /**
   * @return licensor
   */
  public OptionType<DRMLicensor> getLicensor() {
    return this.licensor;
  }

  /**
   * @return link
   */
  public OptionType<URI> getDownloadUrl() {
    return this.download_url;
  }

  /**
   * @return URMS ccid
   */
  public OptionType<String> getCcid() {
    return this.ccid;
  }


  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.type.hashCode();
    return result;
  }

  @Override public String toString()
  {
    final StringBuilder builder = new StringBuilder(64);
    builder.append("[OPDSIndirectAcquisition ");
    builder.append(this.type);
    builder.append("]");
    return NullCheck.notNull(builder.toString());
  }

  /**
   * @param in_licensor  drm licensor
   */
  public void setLicensor(final OptionType<DRMLicensor> in_licensor) {
    this.licensor = in_licensor;
  }
}
