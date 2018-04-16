package org.nypl.simplified.opds.core.annotation;

import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 */

public class BookAnnotation {

  @SerializedName("@context")
  @Expose
  private String context;

  //TODO Nullable
  @SerializedName("body")
  @Expose
  private JsonElement body;

  //TODO Nullable
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
  private AnnotationTargetNode target;

  /**
   * No args constructor for use in serialization
   */
  public BookAnnotation() {
  }

  /**
   * @param in_Annotation target
   * @param in_motivation motivation
   * @param in_context    @context
   * @param in_type       type
   */
  public BookAnnotation(final String in_context, final String in_type, final String in_motivation, final AnnotationTargetNode in_Annotation) {
    this.context = in_context;
    this.type = in_type;
    this.motivation = in_motivation;
    this.target = in_Annotation;
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
  public AnnotationTargetNode getTarget() {
    return this.target;
  }

  /**
   * @param in_Annotation_targetNode The target
   */
  public void setTarget(final AnnotationTargetNode in_Annotation_targetNode) {
    this.target = in_Annotation_targetNode;
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
  public JsonElement getBody() {
      return this.body;
  }

  /**
   * @param in_body The body
   */
  public void setBody(final JsonElement in_body) {
      this.body = in_body;

  }


  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

}
