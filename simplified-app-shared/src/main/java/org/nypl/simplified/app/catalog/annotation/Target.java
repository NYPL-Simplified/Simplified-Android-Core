package org.nypl.simplified.app.catalog.annotation;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 */
public class Target {

  @SerializedName("source")
  @Expose
  private String source;
  @SerializedName("selector")
  @Expose
  private Selector selector;

  /**
   * No args constructor for use in serialization
   */
  public Target() {
  }

  /**
   * @param in_selector The selector
   * @param in_source the sourece
   */
  public Target(final String in_source, final Selector in_selector) {
    this.source = in_source;
    this.selector = in_selector;
  }

  /**
   * @return The source
   */
  public String getSource() {
    return this.source;
  }

  /**
   * @param in_source The source
   */
  public void setSource(final String in_source) {
    this.source = in_source;
  }


  /**
   * @return The selector
   */
  public Selector getSelector() {
    return this.selector;
  }

  /**
   * @param in_selector The selector
   */
  public void setSelector(final Selector in_selector) {
    this.selector = in_selector;
  }


  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

}
