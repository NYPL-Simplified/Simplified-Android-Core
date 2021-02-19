package org.nypl.drm.core;

import android.os.Process;
import java.util.Objects;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * The default implementation of the {@link AdobeAdeptExecutorType}. The
 * executor instantiates a new instance of the {@link AdobeAdeptConnectorType}
 * interface from the given {@link AdobeAdeptConnectorFactoryType}, and runs all
 * submitted {@link AdobeAdeptProcedureType} values on a single dedicated
 * background thread.
 */

public final class AdobeAdeptExecutor implements AdobeAdeptExecutorType
{
  private static final Logger LOG;

  static {
    LOG = Objects.requireNonNull(LoggerFactory.getLogger(AdobeAdeptExecutor.class));
  }

  private final ExecutorService         exec;
  private final AdobeAdeptConnectorType connector;

  private AdobeAdeptExecutor(
    final ExecutorService in_exec,
    final AdobeAdeptConnectorType in_connector)
  {
    this.exec = Objects.requireNonNull(in_exec);
    this.connector = Objects.requireNonNull(in_connector);
  }

  /**
   * Construct a new executor using the given connector factory and arguments.
   *
   * @param factory A connector factory
   * @param p       Connector parameters
   *
   * @return A new executor
   *
   * @throws DRMException         If DRM is not supported, or there was an error
   *                              initializing the DRM
   * @throws InterruptedException If initialization of the DRM package was
   *                              interrupted
   */

  public static AdobeAdeptExecutorType newExecutor(
    final AdobeAdeptConnectorFactoryType factory,
    final AdobeAdeptConnectorParameters p)
    throws DRMException, InterruptedException
  {
    Objects.requireNonNull(factory);
    Objects.requireNonNull(p);

    final ThreadFactory tf = Executors.defaultThreadFactory();
    final ThreadFactory named = new ThreadFactory()
    {
      @Override public Thread newThread(
        final Runnable r)
      {
        /**
         * Apparently, it's necessary to use {@link android.os.Process} to set
         * the thread priority, rather than the standard Java thread
         * functions.
         */

        final Thread t = tf.newThread(
          new Runnable()
          {
            @Override public void run()
            {
              android.os.Process.setThreadPriority(
                Process.THREAD_PRIORITY_BACKGROUND);
              Objects.requireNonNull(r).run();
            }
          });
        t.setName("nypl-drm-adobe-task");
        AdobeAdeptExecutor.LOG.trace("created thread: {}", t);
        return t;
      }
    };

    final ExecutorService exec = Executors.newSingleThreadExecutor(named);
    final AdobeAdeptConnectorType connector;

    try {
      connector = exec.submit(
        new Callable<AdobeAdeptConnectorType>()
        {
          @Override public AdobeAdeptConnectorType call()
            throws Exception
          {
            return factory.get(p);
          }
        }).get();
    } catch (final InterruptedException e) {
      exec.shutdownNow();
      throw e;
    } catch (final ExecutionException e) {
      exec.shutdownNow();
      final Throwable cause = e.getCause();
      if (cause instanceof DRMException) {
        throw (DRMException) cause;
      }

      // XXX: Not exactly true...
      throw new UnreachableCodeException();
    }

    return new AdobeAdeptExecutor(exec, connector);
  }

  @Override public void execute(final AdobeAdeptProcedureType p)
  {
    Objects.requireNonNull(p);

    final AdobeAdeptConnectorType c = this.connector;
    this.exec.execute(
      new Runnable()
      {
        @Override public void run()
        {
          AdobeAdeptExecutor.LOG.trace("executing {}", p);
          p.executeWith(c);
        }
      });
  }
}
