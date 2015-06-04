package org.nypl.simplified.tenprint;

public interface TenPrintInputBuilderType
{
  /**
   * @return Input parameters based on all of the values given so far
   */

  TenPrintInput build();

  /**
   * Set the book author.
   *
   * @param in_author
   *          The author
   */

  void setAuthor(
    String in_author);

  /**
   * Set the base brightness for colors.
   *
   * @param in_base_brightness
   *          The brightness level
   */

  void setBaseBrightness(
    float in_base_brightness);

  /**
   * Set the base saturation level for colors.
   *
   * @param in_base_saturation
   *          The saturation level
   */

  void setBaseSaturation(
    float in_base_saturation);

  /**
   * Set the color distance.
   *
   * @param in_color_distance
   *          The distance
   */

  void setColorDistance(
    float in_color_distance);

  /**
   * Set the cover height.
   *
   * @param in_cover_height
   *          The cover height
   */

  void setCoverHeight(
    int in_cover_height);

  /**
   * Enable or disable the display of debugging symbols and frames in the
   * generated artwork
   * 
   * @param b
   *          <tt>true</tt> iff debugging views should be enabled
   */

  void setDebuggingArtwork(
    boolean b);

  /**
   * Set the grid scale.
   *
   * @param in_grid_scale
   *          The grid scale
   */

  void setGridScale(
    float in_grid_scale);

  /**
   * Set whether or not colors should be inverted.
   *
   * @param in_invert
   *          <tt>true</tt> iff colors should be inverted
   */

  void setInvert(
    boolean in_invert);

  /**
   * Set the margin.
   *
   * @param in_margin
   *          The margin
   */

  void setMargin(
    int in_margin);

  /**
   * Set the thickness of the shapes rendered inside each grid square.
   *
   * @param in_shape_thickness
   *          The thickness
   */

  void setShapeThickness(
    int in_shape_thickness);

  /**
   * Set the book title.
   *
   * @param in_title
   *          The title
   */

  void setTitle(
    String in_title);
}
