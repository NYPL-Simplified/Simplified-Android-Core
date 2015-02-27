package org.nypl.simplified.app.tests;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.nypl.simplified.app.BitmapCacheListenerType;
import org.nypl.simplified.app.BitmapCacheScalingType;
import org.nypl.simplified.app.BitmapScalingDiskCache;
import org.nypl.simplified.app.BitmapScalingOptions;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.junreachable.UnreachableCodeException;

@SuppressWarnings({ "boxing", "null", "unused", "synthetic-access" }) public final class BitmapScalingDiskCacheTest extends
  InstrumentationTestCase
{
  private static final String TAG;

  static {
    TAG = "BFCT";
  }

  private File prepareCacheDirectory()
  {
    final Context ctx = this.getInstrumentation().getContext();
    final File in_file = ctx.getExternalCacheDir();

    if (in_file.isDirectory() == false) {
      Log.d(BitmapScalingDiskCacheTest.TAG, "creating cache directory");
      final boolean created = in_file.mkdirs();
      Assert.assertTrue(created);
    }

    Log.d(BitmapScalingDiskCacheTest.TAG, "clearing cache directory");
    for (final File name : in_file.listFiles()) {
      Log.d(BitmapScalingDiskCacheTest.TAG, "deleting " + name);
      name.delete();
    }

    return in_file;
  }

  /**
   * Verify that downloaded but non-decodable images are not cached.
   */

  public void testExampleErrorRecoverable()
    throws Exception
  {
    final Context ctx = this.getInstrumentation().getContext();
    final Resources rr = ctx.getResources();
    final File in_file = this.prepareCacheDirectory();
    final int in_bytes = (int) (8 * Math.pow(10, 7));

    final AtomicInteger count = new AtomicInteger(0);
    final PartialFunctionType<URI, InputStream, IOException> in_transport =
      new PartialFunctionType<URI, InputStream, IOException>() {
        @Override public InputStream call(
          final URI u)
          throws IOException
        {
          final int cc = count.get();
          Log.d(BitmapScalingDiskCacheTest.TAG, "call count: " + cc);
          if (cc == 0) {
            count.incrementAndGet();
            return new ByteArrayInputStream(new byte[8]);
          }
          return rr.openRawResource(R.drawable.simplified);
        }
      };

    final ExecutorService in_e = Executors.newFixedThreadPool(1);
    final BitmapCacheScalingType bc =
      BitmapScalingDiskCache.newCache(in_e, in_transport, in_file, in_bytes);
    final BitmapScalingOptions opts = BitmapScalingOptions.scaleNone();

    final AtomicBoolean error = new AtomicBoolean(false);
    final AtomicBoolean ok = new AtomicBoolean(false);

    final ListenableFuture<Bitmap> f0 =
      bc.get(
        URI.create("http://example.com"),
        opts,
        new BitmapCacheListenerType() {
          @Override public void onFailure(
            final Throwable e)
          {
            Log.d(BitmapScalingDiskCacheTest.TAG, "Exception: " + e, e);
            error.set(true);
          }

          @Override public void onSuccess(
            final Bitmap b)
          {
            throw new UnreachableCodeException();
          }
        });

    Thread.sleep(1000);

    final ListenableFuture<Bitmap> f1 =
      bc.get(
        URI.create("http://example.com"),
        opts,
        new BitmapCacheListenerType() {
          @Override public void onFailure(
            final Throwable e)
          {
            throw new UnreachableCodeException();
          }

          @Override public void onSuccess(
            final Bitmap b)
          {
            Log.d(BitmapScalingDiskCacheTest.TAG, "Bitmap: " + b);
            Assert.assertEquals(b.getWidth(), 120);
            Assert.assertEquals(b.getHeight(), 120);
            ok.set(true);
          }
        });

    Thread.sleep(1000);

    in_e.shutdown();
    in_e.awaitTermination(10, TimeUnit.SECONDS);

    Assert.assertTrue(error.get());
    Assert.assertTrue(ok.get());

    Log.d(
      BitmapScalingDiskCacheTest.TAG,
      String.format(
        "cache size: (%d/%d)",
        bc.getSizeCurrent(),
        bc.getSizeMaximum()));
  }

  /**
   * Verify that images can be decoded.
   */

  public void testExampleOK()
    throws Exception
  {
    final Context ctx = this.getInstrumentation().getContext();
    final Resources rr = ctx.getResources();
    final File in_file = this.prepareCacheDirectory();
    final int in_bytes = (int) (8 * Math.pow(10, 7));

    final PartialFunctionType<URI, InputStream, IOException> in_transport =
      new PartialFunctionType<URI, InputStream, IOException>() {
        @Override public InputStream call(
          final URI u)
          throws IOException
        {
          return rr.openRawResource(R.drawable.simplified);
        }
      };

    final ExecutorService in_e = Executors.newFixedThreadPool(1);
    final BitmapCacheScalingType bc =
      BitmapScalingDiskCache.newCache(in_e, in_transport, in_file, in_bytes);
    final BitmapScalingOptions opts = BitmapScalingOptions.scaleNone();

    final AtomicBoolean ok = new AtomicBoolean(false);
    final ListenableFuture<Bitmap> f =
      bc.get(
        URI.create("http://example.com"),
        opts,
        new BitmapCacheListenerType() {
          @Override public void onFailure(
            final Throwable e)
          {
            Log.d(BitmapScalingDiskCacheTest.TAG, "Exception: " + e, e);
          }

          @Override public void onSuccess(
            final Bitmap b)
          {
            Log.d(BitmapScalingDiskCacheTest.TAG, "Bitmap: " + b);
            Assert.assertEquals(b.getWidth(), 120);
            Assert.assertEquals(b.getHeight(), 120);
            ok.set(true);
          }
        });

    Thread.sleep(1000);

    in_e.shutdown();
    in_e.awaitTermination(10, TimeUnit.SECONDS);
    Assert.assertTrue(ok.get());

    Log.d(
      BitmapScalingDiskCacheTest.TAG,
      String.format(
        "cache size: (%d/%d)",
        bc.getSizeCurrent(),
        bc.getSizeMaximum()));
  }
}
