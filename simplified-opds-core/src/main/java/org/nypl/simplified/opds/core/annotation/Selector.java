package org.nypl.simplified.opds.core.annotation;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 */

public class Selector {

  @SerializedName("type")
  @Expose
  private String type;
  @SerializedName("value")
  @Expose
  private String value;

  /**
   * No args constructor for use in serialization
   */
  public Selector() {
  }

  /**
   * @param in_value the value
   * @param in_type the type
   */
  public Selector(final String in_type, final String in_value) {
    this.type = in_type;
    this.value = in_value;
  }

  /**
   * @return The type
   */
  public String getType() {
    return this.type;
  }

  /**
   * @param in_type The @type
   */
  public void setType(final String in_type) {
    this.type = in_type;
  }


  /**
   * @return The value
   */
  public String getValue() {
    return this.value;
  }

  /**
   * @param in_value The @value
   */
  public void setValue(final String in_value) {
    this.value = in_value;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

}
