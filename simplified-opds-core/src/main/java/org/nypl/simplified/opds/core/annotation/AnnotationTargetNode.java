package org.nypl.simplified.opds.core.annotation;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 */
public class AnnotationTargetNode {

  @SerializedName("source")
  @Expose
  private String source;

  @SerializedName("annotationSelectorNode")
  @Expose
  private AnnotationSelectorNode annotationSelectorNode;

  /**
   * No args constructor for use in serialization
   */
  public AnnotationTargetNode() {
  }

  /**
   * @param in_Annotation_selectorNode The annotationSelectorNode
   * @param in_source the sourece
   */
  public AnnotationTargetNode(final String in_source, final AnnotationSelectorNode in_Annotation_selectorNode) {
    this.source = in_source;
    this.annotationSelectorNode = in_Annotation_selectorNode;
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
   * @return The annotationSelectorNode
   */
  public AnnotationSelectorNode getAnnotationSelectorNode() {
    return this.annotationSelectorNode;
  }

  /**
   * @param in_Annotation_selectorNode The annotationSelectorNode
   */
  public void setAnnotationSelectorNode(final AnnotationSelectorNode in_Annotation_selectorNode) {
    this.annotationSelectorNode = in_Annotation_selectorNode;
  }


  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

}
