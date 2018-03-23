package org.nypl.simplified.opds.core.annotation;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 *
 */
@Generated("org.jsonschema2pojo")
public class First {

  @SerializedName("items")
  @Expose
  private List<BookAnnotation> items = new ArrayList<BookAnnotation>();
  @SerializedName("type")
  @Expose
  private String type;
  @SerializedName("id")
  @Expose
  private String id;

  /**
   * No args constructor for use in serialization
   */
  public First() {
  }
  /**
   * @return The items
   */
  public List<BookAnnotation> getItems() {
    return this.items;
  }

  /**
   * @param in_items The items
   */
  public void setItems(final List<BookAnnotation> in_items) {
    this.items = in_items;
  }

  /**
   * @return The type
   */
  public String getType() {
    return this.type;
  }

  /**
   * @param in_type The type
   */
  public void setType(final String in_type) {
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

}
