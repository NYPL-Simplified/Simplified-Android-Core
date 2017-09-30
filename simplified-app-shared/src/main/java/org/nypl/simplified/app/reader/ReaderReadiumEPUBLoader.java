package org.nypl.simplified.app.reader;

import android.content.Context;

import com.bugsnag.android.Severity;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.drm.core.AdobeAdeptContentFilterType;
import org.nypl.drm.core.AdobeAdeptContentRightsClientType;
import org.nypl.drm.core.DRMException;
import org.nypl.drm.core.DRMUnsupportedException;
import org.nypl.simplified.app.AdobeDRMServices;
import org.nypl.simplified.books.core.BookDatabase;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.bugsnag.IfBugsnag;
import org.nypl.simplified.files.FileUtilities;
import org.nypl.simplified.opds.core.DRMLicensor;
import org.readium.sdk.android.Container;
import org.readium.sdk.android.ContentFilterErrorHandler;
import org.readium.sdk.android.EPub3;
import org.readium.sdk.android.Package;
import org.readium.sdk.android.SdkErrorHandler;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * The default implementation of the {@link ReaderReadiumEPUBLoaderType}
 * interface.
 */

public final class ReaderReadiumEPUBLoader
  implements ReaderReadiumEPUBLoaderType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderReadiumEPUBLoader.class);
  }

  private final ConcurrentHashMap<File, Container> containers;
  private final ExecutorService                    exec;
  private final Context                            context;

  private ReaderReadiumEPUBLoader(
    final Context in_context,
    final ExecutorService in_exec)
  {
    this.exec = NullCheck.notNull(in_exec);
    this.context = NullCheck.notNull(in_context);
    this.containers = new ConcurrentHashMap<File, Container>();
  }

  private static Container loadFromFile(
    final Context ctx,
    final File f,
    final DRMLicensor.DRM drm_type)
    throws IOException
  {
    /**
     * Readium will happily segfault if passed a filename that does not refer
     * to a file that exists.
     */

    if (!f.isFile()) {
      throw new FileNotFoundException("No such file");
    }


    /**
     * If Adobe rights exist for the given book, then those rights must
     * be read so that they can be fed to the content filter plugin. If
     * no rights file exists, it may either be that the file has been lost
     * or that the book is not encrypted. If the book actually is encrypted
     * and there is no rights information, then unfortunately there is
     * nothing that can be done about this. This is not something that should
     * happen in practice and likely indicates database tampering or a bug
     * in the program.
     */
    switch (drm_type) {
      case ADOBE:
      {
        final OptionType<File> adobe_rights =
          BookDatabase.getAdobeRightsFileForEPUB(f);

        final byte[] adobe_rights_data;
        if (adobe_rights.isSome()) {
          ReaderReadiumEPUBLoader.LOG.debug("Adobe rights data exists, loading it");
          final File adobe_rights_file = ((Some<File>) adobe_rights).get();
          adobe_rights_data = FileUtilities.fileReadBytes(adobe_rights_file);
          ReaderReadiumEPUBLoader.LOG.debug(
            "Loaded {} bytes of Adobe rights data", adobe_rights_data.length);
        } else {
          ReaderReadiumEPUBLoader.LOG.debug("No Adobe rights data exists");
          adobe_rights_data = new byte[0];
        }


        /**
         * The majority of logged messages will be useless noise.
         */

        final ContentFilterErrorHandler content_filter_errors = new ContentFilterErrorHandler() {
          @Override
          public void handleContentFilterError(
            final @Nullable String filter_id,
            final long error_code,
            final @Nullable String message) {
            ReaderReadiumEPUBLoader.LOG.error("{}:{}: {}", filter_id, error_code, message);
            IfBugsnag.get().notify(
              new ReaderReadiumContentFilterException(
                String.format("%s:%d: %s", filter_id, error_code, message)),
              Severity.ERROR);
          }
        };

        /**
         * The Readium SDK will call the given filter handler when the
         * filter chain has been populated. It is at this point that it
         * is necessary to register the Adobe content filter plugin, if
         * one is to be used. The plugin will call the given rights client
         * every time it needs to load rights data (which will only be once,
         * given the way that the application creates a new instance of
         * Readium each time a book is opened).
         */

        final AdobeAdeptContentRightsClientType rights_client =
          new AdobeAdeptContentRightsClientType() {
            @Override
            public byte[] getRightsData(final String path) {
              ReaderReadiumEPUBLoader.LOG.debug(
                "returning {} bytes of rights data for path {}",
                adobe_rights_data.length,
                path);
              return adobe_rights_data;
            }
          };

        final Runnable filter_handler = new Runnable() {
          @Override
          public void run() {
            ReaderReadiumEPUBLoader.LOG.debug("Registering content filter");

            try {

              final AdobeAdeptContentFilterType adobe =
                AdobeDRMServices.newAdobeContentFilter(
                  ctx, AdobeDRMServices.getPackageOverride(ctx.getResources()));
              adobe.registerFilter(rights_client);
              ReaderReadiumEPUBLoader.LOG.debug("Content filter registered");
            } catch (final DRMUnsupportedException e) {
              ReaderReadiumEPUBLoader.LOG.error(
                "DRM is not supported: ", e);
            } catch (final DRMException e) {
              ReaderReadiumEPUBLoader.LOG.error(
                "DRM could not be initialized: ", e);
            }
          }
        };
        EPub3.initialize();
        ReaderReadiumEPUBLoader.LOG.debug("EPub3.initialize()");
        ReaderReadiumEPUBLoader.LOG.debug("drm_type " + drm_type);

        EPub3.setContentFilterErrorHandler(content_filter_errors);
        EPub3.setContentFiltersRegistrationHandler(filter_handler);
    }
        break;
      case URMS: {
        final ContentFilterErrorHandler content_filter_errors = new ContentFilterErrorHandler() {
          @Override
          public void handleContentFilterError(
            final @Nullable String filter_id,
            final long error_code,
            final @Nullable String message) {
            ReaderReadiumEPUBLoader.LOG.error("{}:{}: {}", filter_id, error_code, message);

          }
        };
        final Runnable filter_handler = new Runnable() {
          @Override
          public void run() {
            ReaderReadiumEPUBLoader.LOG.debug("Registering NO content filter");

          }
        };
        EPub3.initialize();
        ReaderReadiumEPUBLoader.LOG.debug("EPub3.initialize()");
        ReaderReadiumEPUBLoader.LOG.debug("drm_type " + drm_type);

        EPub3.setContentFilterErrorHandler(content_filter_errors);
        EPub3.setContentFiltersRegistrationHandler(filter_handler);
      }
        break;
      case LCP:
      case NONE:
      default:

        break;
    }

    final SdkErrorHandler errors = new SdkErrorHandler()
    {
      @Override public boolean handleSdkError(
        final @Nullable String message,
        final boolean is_severe)
      {
        ReaderReadiumEPUBLoader.LOG.debug("{}", message);
        if (is_severe) {
          IfBugsnag.get().notify(new ReaderReadiumSdkException(message), Severity.ERROR);
        }
        return true;
      }
    };

    EPub3.setSdkErrorHandler(errors);
    final Container c = EPub3.openBook(f.toString());
    EPub3.setSdkErrorHandler(null);


    /**
     * Only the default package is considered important. If the package has no
     * spine items, then the package is considered to be unusable.
     */

    final Package p = c.getDefaultPackage();
    if (p.getSpineItems().isEmpty()) {
      throw new IOException("Loaded package had no spine items");
    }
    return c;
  }

  /**
   * Construct a new EPUB loader.
   *
   * @param in_context The application context
   * @param in_exec    An executor service
   *
   * @return A new EPUB loader
   */

  public static ReaderReadiumEPUBLoaderType newLoader(
    final Context in_context,
    final ExecutorService in_exec)
  {
    return new ReaderReadiumEPUBLoader(in_context, in_exec);
  }

  @Override public void loadEPUB(
    final File f,
    final ReaderReadiumEPUBLoadListenerType l,
    final DRMLicensor.DRM drm_type)
  {
    NullCheck.notNull(f);
    NullCheck.notNull(l);

    /**
     * This loader caches references to loaded containers. It's not actually
     * expected that there will be more than one container for the lifetime of
     * the process.
     */

    final ConcurrentHashMap<File, Container> cs = this.containers;
    this.exec.submit(
      new Runnable()
      {
        @Override public void run()
        {
          try {
            final Container c = ReaderReadiumEPUBLoader.loadFromFile(
              ReaderReadiumEPUBLoader.this.context, f, drm_type);

            l.onEPUBLoadSucceeded(c);
          } catch (final Throwable x0) {
            try {
              l.onEPUBLoadFailed(x0);
            } catch (final Throwable x1) {
              ReaderReadiumEPUBLoader.LOG.error("{}", x1.getMessage(), x1);
            }
          }
        }
      });
  }

  private static abstract class ReaderReadiumRuntimeException extends Exception
  {
    ReaderReadiumRuntimeException(final String in_message)
    {
      super(in_message);
    }
  }

  private static final class ReaderReadiumSdkException extends ReaderReadiumRuntimeException
  {
    ReaderReadiumSdkException(final String in_message)
    {
      super(in_message);
    }
  }

  private static final class ReaderReadiumContentFilterException extends ReaderReadiumRuntimeException
  {
    ReaderReadiumContentFilterException(final String in_message)
    {
      super(in_message);
    }
  }
}
