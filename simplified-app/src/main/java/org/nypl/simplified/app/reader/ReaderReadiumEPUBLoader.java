package org.nypl.simplified.app.reader;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.readium.sdk.android.Container;
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

@SuppressWarnings("synthetic-access") public final class ReaderReadiumEPUBLoader
  implements ReaderReadiumEPUBLoaderType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderReadiumEPUBLoader.class);
  }

  private final ConcurrentHashMap<File, Container> containers;
  private final ExecutorService                    exec;

  private ReaderReadiumEPUBLoader(
    final ExecutorService in_exec)
  {
    this.exec = NullCheck.notNull(in_exec);
    this.containers = new ConcurrentHashMap<File, Container>();
  }

  private static Container loadFromFile(
    final File f)
    throws FileNotFoundException, IOException
  {
    /**
     * Readium will happily segfault if passed a filename that does not refer
     * to a file that exists.
     */

    if (f.isFile() == false) {
      throw new FileNotFoundException("No such file");
    }

    /**
     * The majority of logged messages will be useless noise.
     */

    final SdkErrorHandler errors = new SdkErrorHandler()
    {
      @Override public boolean handleSdkError(
        final @Nullable String message,
        final boolean is_severe)
      {
        ReaderReadiumEPUBLoader.LOG.debug("{}", message);
        return true;
      }
    };

    final Runnable filter_handler = new Runnable()
    {
      @Override public void run()
      {
        ReaderReadiumEPUBLoader.LOG.debug("filter chain populated");
      }
    };

    EPub3.setSdkErrorHandler(errors);
    EPub3.setPostFilterPopulationHandler(filter_handler);
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
   * @param in_exec An executor service
   *
   * @return A new EPUB loader
   */

  public static ReaderReadiumEPUBLoaderType newLoader(
    final ExecutorService in_exec)
  {
    return new ReaderReadiumEPUBLoader(in_exec);
  }

  @Override public void loadEPUB(
    final File f,
    final ReaderReadiumEPUBLoadListenerType l)
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
            final Container c;
            if (cs.containsKey(f)) {
              c = NullCheck.notNull(cs.get(f));
            } else {
              c = ReaderReadiumEPUBLoader.loadFromFile(f);
              cs.put(f, c);
            }

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
}
