package org.nypl.simplified.app;

/**
 * <p>
 * The type of <i>expensive</i> objects who's lifetime should be carefully
 * controlled but that cannot (due to restrictions imposed by, for example,
 * the Android API) be controlled by lexical scope.
 * </p>
 * <p>
 * A good example is the type of navigation lane views: There is a
 * long-running asynchronous operation that downloads and displays all of the
 * book thumbnails. The thumbnails require a relatively large amount of memory
 * and so should be pushed out of memory at the earliest opportunity (i.e,
 * when the view containing the images is moved offscreen). When the view
 * moves offscreen, the download should also be halted to avoid holding up any
 * new lanes that have appeared onscreen. The navigation lane view type
 * removes all images and halts downloads on {@link #expensiveStop()}.
 * </p>
 */

public interface ExpensiveType extends
  ExpensiveStartableType,
  ExpensiveStoppableType
{
  // No extra functions.
}
