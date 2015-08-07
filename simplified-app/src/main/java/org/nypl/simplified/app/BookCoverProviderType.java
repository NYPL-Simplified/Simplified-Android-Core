package org.nypl.simplified.app;

import android.widget.ImageView;
import com.squareup.picasso.Callback;
import org.nypl.simplified.books.core.FeedEntryOPDS;

/**
 * The type of cover providers.
 */

public interface BookCoverProviderType
{
  /**
   * <p> Load or generate a cover based on <tt>e</tt> into the image view
   * <tt>i</tt>, at width <tt>w</tt> and height <tt>h</tt>. </p> <p> Must only
   * be called from the UI thread. </p>
   *
   * @param e The feed entry
   * @param i The image view
   * @param w The width
   * @param h The height
   */

  void loadCoverInto(
    final FeedEntryOPDS e,
    final ImageView i,
    final int w,
    final int h);

  /**
   * <p> Load or generate a cover based on <tt>e</tt> into the image view
   * <tt>i</tt>, at width <tt>w</tt> and height <tt>h</tt>, calling <tt>c</tt>
   * on completion. </p> <p> Must only be called from the UI thread. </p>
   *
   * @param e The feed entry
   * @param i The image view
   * @param w The width
   * @param h The height
   * @param c The callback
   */

  void loadCoverIntoWithCallback(
    final FeedEntryOPDS e,
    final ImageView i,
    final int w,
    final int h,
    final Callback c);

  /**
   * Pause loading of any covers. Loading will continue upon calling {@link
   * #loadingThumbnailsContinue()}.
   */

  void loadingThumbailsPause();

  /**
   * Continue loading of covers after having been paused with {@link
   * #loadingThumbailsPause()}. Has no effect if loading is not paused.
   */

  void loadingThumbnailsContinue();

  /**
   * <p> Load or generate a thumbnail based on <tt>e</tt> into the image view
   * <tt>i</tt>, at width <tt>w</tt> and height <tt>h</tt>. </p> <p> Must only
   * be called from the UI thread. </p>
   *
   * @param e The feed entry
   * @param i The image view
   * @param w The width
   * @param h The height
   */

  void loadThumbnailInto(
    final FeedEntryOPDS e,
    final ImageView i,
    final int w,
    final int h);

  /**
   * <p> Load or generate a thumbnail based on <tt>e</tt> into the image view
   * <tt>i</tt>, at width <tt>w</tt> and height <tt>h</tt>, calling <tt>c</tt>
   * on completion. </p> <p> Must only be called from the UI thread. </p>
   *
   * @param e The feed entry
   * @param i The image view
   * @param w The width
   * @param h The height
   * @param c The callback
   */

  void loadThumbnailIntoWithCallback(
    final FeedEntryOPDS e,
    final ImageView i,
    final int w,
    final int h,
    final Callback c);
}
