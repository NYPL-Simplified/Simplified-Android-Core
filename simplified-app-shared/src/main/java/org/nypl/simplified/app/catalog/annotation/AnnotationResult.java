package org.nypl.simplified.app.catalog.annotation;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 *
 */
@Generated("org.jsonschema2pojo")
public class AnnotationResult {

  @SerializedName("@context")
  @Expose
  private List<String> context = new ArrayList<String>();
  @SerializedName("total")
  @Expose
  private Integer total;
  @SerializedName("type")
  @Expose
  private List<String> type = new ArrayList<String>();
  @SerializedName("id")
  @Expose
  private String id;
  @SerializedName("first")
  @Expose
  private First first;

  /**
   * No args constructor for use in serialization
   */
  public AnnotationResult() {
  }
  /**
   * @return The context
   */
  public List<String> getContext() {
    return this.context;
  }

  /**
   * @param in_context The @context
   */
  public void setContext(final List<String> in_context) {
    this.context = in_context;
  }

  /**
   * @return The total
   */
  public Integer getTotal() {
    return this.total;
  }

  /**
   * @param in_total The total
   */
  public void setTotal(final Integer in_total) {
    this.total = in_total;
  }

  /**
   * @return The type
   */
  public List<String> getType() {
    return this.type;
  }

  /**
   * @param in_type The type
   */
  public void setType(final List<String> in_type) {
    this.type = in_type;
  }

  /**
   * @return The id
   */
  public String getId() {
    return this.id;
  }

  /**
   * @param in_id The id
   */
  public void setId(final String in_id) {
    this.id = in_id;
  }

  /**
   * @return The first
   */
  public First getFirst() {
    return this.first;
  }

  /**
   * @param in_first The first
   */
  public void setFirst(final First in_first) {
    this.first = in_first;
  }

}
