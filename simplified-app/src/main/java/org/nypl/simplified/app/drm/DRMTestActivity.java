package org.nypl.simplified.app.drm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.nypl.drm.core.AdobeAdeptConnectorFactory;
import org.nypl.drm.core.AdobeAdeptConnectorFactoryType;
import org.nypl.drm.core.AdobeAdeptConnectorType;
import org.nypl.drm.core.AdobeAdeptDRMClientType;
import org.nypl.drm.core.AdobeAdeptNetProviderType;
import org.nypl.drm.core.AdobeAdeptResourceProviderType;
import org.nypl.drm.core.DRMException;
import org.nypl.drm.core.DRMUnsupportedException;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

import android.app.Activity;
import android.os.Bundle;

import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;

public final class DRMTestActivity extends Activity implements
  AdobeAdeptResourceProviderType,
  AdobeAdeptDRMClientType,
  AdobeAdeptNetProviderType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(DRMTestActivity.class);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    try {
      final AdobeAdeptConnectorFactoryType f =
        AdobeAdeptConnectorFactory.get();

      final AdobeAdeptConnectorType p =
        f.get(
          "org.nypl.simplified.app",
          this,
          this,
          this,
          "42f40c40374851a5b4a3d8375cb98924",
          "NYPL Reader",
          new File("/data/local/tmp/simplified"),
          new File("/data/local/tmp/simplified"),
          new File("/data/local/tmp/simplified"));

    } catch (final DRMUnsupportedException e) {
      DRMTestActivity.LOG.error("unsupported drm: ", e);
    } catch (final DRMException e) {
      DRMTestActivity.LOG.error("error starting drm: ", e);
    }
  }

  @Override public @Nullable byte[] getResourceAsBytes(
    final String name)
  {
    DRMTestActivity.LOG.debug("getResource: {}", name);

    if ("res:///ReaderClientCert.sig".equals(name)) {
      try {
        final InputStream is = this.getAssets().open("ReaderClientCert.sig");
        final byte[] data = new byte[is.available()];
        is.read(data);
        is.close();

        DRMTestActivity.LOG.debug("returning {} bytes", data.length);
        return data;
      } catch (final MalformedURLException e) {
        throw new UnreachableCodeException(e);
      } catch (final IOException e) {
        throw new UnreachableCodeException(e);
      }
    }
    return null;
  }

  @Override public void onWorkflowsDone(
    final int w,
    final String remaining)
  {
    // TODO Auto-generated method stub
    throw new UnimplementedCodeException();
  }

  @Override public void onWorkflowProgress(
    final int w,
    final String title,
    final double progress)
  {
    // TODO Auto-generated method stub
    throw new UnimplementedCodeException();
  }

  @Override public void onWorkflowError(
    final int w,
    final String code)
  {
    // TODO Auto-generated method stub
    throw new UnimplementedCodeException();
  }

  @Override public void onFollowupURL(
    final int w,
    final String url)
  {
    // TODO Auto-generated method stub
    throw new UnimplementedCodeException();
  }

  @Override public void onDownloadCompleted(
    final String url)
  {
    // TODO Auto-generated method stub
    throw new UnimplementedCodeException();
  }
}
