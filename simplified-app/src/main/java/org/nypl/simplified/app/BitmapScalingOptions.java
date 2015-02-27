package org.nypl.simplified.app;

import com.io7m.jnull.NullCheck;

public final class BitmapScalingOptions
{
  public static enum Type
  {
    TYPE_SCALE_PRESERVE,
    TYPE_SCALE_SIZE_HINT
  }

  public static BitmapScalingOptions scaleNone()
  {
    return new BitmapScalingOptions(Type.TYPE_SCALE_PRESERVE, 0, 0);
  }

  public static BitmapScalingOptions scaleSizeHint(
    final int width,
    final int height)
  {
    return new BitmapScalingOptions(Type.TYPE_SCALE_SIZE_HINT, width, height);
  }

  private final int  height;
  private final Type type;
  private final int  width;

  private BitmapScalingOptions(
    final Type in_op,
    final int in_w,
    final int in_h)
  {
    this.type = NullCheck.notNull(in_op);
    this.width = in_w;
    this.height = in_h;
  }

  public int getHeight()
  {
    return this.height;
  }

  public Type getType()
  {
    return this.type;
  }

  public int getWidth()
  {
    return this.width;
  }
}
