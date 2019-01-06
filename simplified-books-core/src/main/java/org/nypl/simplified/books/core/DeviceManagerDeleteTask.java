package org.nypl.simplified.books.core;


import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

import org.nypl.drm.core.AdobeDeviceID;
import org.nypl.simplified.http.core.HTTP;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.opds.core.DRMLicensor;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by aferditamuriqi on 10/24/16.
 *
 */


public class DeviceManagerDeleteTask
  implements Runnable
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(DeviceManagerDeleteTask.class);
  }

  private final AccountCredentials creds;

  DeviceManagerDeleteTask(
    final AccountCredentials in_creds)
  {
    this.creds = NullCheck.notNull(in_creds);
  }

  @Override
  public void run()
  {
    if (this.creds.getDrmLicensor().isSome()) {

      try {
        final DRMLicensor licensor = ((Some<DRMLicensor>) this.creds.getDrmLicensor()).get();
        if (licensor.getDeviceManager().isSome()) {
          final String url = ((Some<String>) licensor.getDeviceManager()).get();

          final String content_type = "vnd.librarysimplified/drm-device-id-list";
          if (this.creds.getAdobeDeviceID().isSome()) {
          final String device_id = ((Some<AdobeDeviceID>) this.creds.getAdobeDeviceID()).get().getValue();
            final URI uri = new URI(url + "/" + device_id);
            LOG.debug("uri %s", uri);
            final AccountBarcode barcode = this.creds.getBarcode();
            final AccountPIN pin = this.creds.getPin();

            final OptionType<HTTPAuthType> http_auth =
              Option.some((HTTPAuthType) HTTPAuthBasic.create(barcode.toString(), pin.toString()));

            HTTP.newHTTP().delete(http_auth, uri, content_type);
          }
        }
      } catch (URISyntaxException e) {
        e.printStackTrace();
      }
    }
  }
}
