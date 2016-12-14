package org.nypl.simplified.app.catalog.annotation;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 */

public class Annotation {

  @SerializedName("@context")
  @Expose
  private String context;

  @SerializedName("body")
  @Expose
  private JsonObject body;
  @SerializedName("id")
  @Expose
  private String id;

  @SerializedName("type")
  @Expose
  private String type;
  @SerializedName("motivation")
  @Expose
  private String motivation;
  @SerializedName("target")
  @Expose
  private Target target;

  /**
   * No args constructor for use in serialization
   */
  public Annotation() {
  }

  /**
   * @param in_target     target
   * @param in_motivation motivation
   * @param in_context    @context
   * @param in_type       type
   */
  public Annotation(final String in_context, final String in_type, final String in_motivation, final Target in_target) {
    this.context = in_context;
    this.type = in_type;
    this.motivation = in_motivation;
    this.target = in_target;
  }

  /**
   * @return The context
   */
  public String getContext() {
    return this.context;
  }

  /**
   * @param in_context The @context
   */
  public void setContext(final String in_context) {
    this.context = in_context;
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
   * @return The motivation
   */
  public String getMotivation() {
    return this.motivation;
  }

  /**
   * @param in_motivation The motivation
   */
  public void setMotivation(final String in_motivation) {
    this.motivation = in_motivation;
  }


  /**
   * @return The target
   */
  public Target getTarget() {
    return this.target;
  }

  /**
   * @param in_target The target
   */
  public void setTarget(final Target in_target) {
    this.target = in_target;
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
   * @return The body
   */
  public JsonObject getBody() {
    return this.body;
  }

  /**
   * @param in_body The body
   */
  public void setBody(final JsonObject in_body) {
    this.body = in_body;
  }


  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

}
