package org.nypl.drm.core;

import java.util.Objects;
import com.io7m.jnull.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

//@formatter:off

/**
 * <p>The default implementation of the {@link AdobeAdeptConnectorFactoryType}
 * interface.</p>
 *
 * <p>
 * Note that as of the time of writing, the current available implementations
 * of the {@link AdobeAdeptConnectorType} suffer from the following severe
 * limitations:
 * </p>
 * <ul>
 *   <li>
 *     There may only be one instance of the {@link AdobeAdeptConnectorType}
 *     in the
 *     virtual machine at any one time. This is because the Adobe Adept
 *     Connector
 *     uses global, static storage internally and cannot realistically support
 *     multiple clients per process.
 *   </li>
 *   <li>
 *     In addition to not being thread-safe, the connector is also not
 *     re-entrant.
 *     It is not possible to call any of the API functions from within a
 *     callback
 *     that has been passed to one of the earlier API functions. Implementations
 *     of the {@link AdobeAdeptConnectorType} type are required to perform
 *     run-time checks to catch attempts to do this.
 *   </li>
 *   <li>
 *     An instance of the {@link AdobeAdeptConnectorType} cannot be used from
 *     a thread other than the one on which it was instantiated.
 *     Consequentially,
 *     because the instance will inevitably need to perform network and disk
 *     I/O,
 *     the {@link #get()} method should be called on a background thread and all
 *     operations should take place there.
 *   </li>
 *   <li>
 *     If the package name provided to the {@link #get()} method does not match
 *     the name given in your Adobe-provided certificate, the connector will
 *     unceremoniously abort the entire virtual machine.
 *   </li>
 *   <li>
 *     If any of the paths passed to {@link #get()} are not writable, the
 *     connector will typically go into infinite request loops. The default
 *     implementation of the {@link AdobeAdeptNetProviderType},
 *     {@link AdobeAdeptNetProvider} has some internal checks to at least
 *     catch these bugs when they happen, but typically is not able to recover
 *     from them.
 *   </li>
 * </ul>
 */

//@formatter:on

public final class AdobeAdeptConnectorFactory
  implements AdobeAdeptConnectorFactoryType
{
  private static final     String                  CLASS_NAME;
  private static final     Logger                  LOG;
  private static @Nullable AdobeAdeptConnectorType INSTANCE;

  static {
    LOG = Objects.requireNonNull(LoggerFactory.getLogger(
        AdobeAdeptConnectorFactory.class));

    CLASS_NAME = "org.nypl.drm.adobe.AdobeAdeptConnector";
  }

  private AdobeAdeptConnectorFactory()
  {
    // Nothing
  }

  /**
   * @return A new factory
   */

  public static AdobeAdeptConnectorFactoryType get()
  {
    return new AdobeAdeptConnectorFactory();
  }

  @Override public AdobeAdeptConnectorType get(
    final AdobeAdeptConnectorParameters p)
    throws DRMException
  {
    Objects.requireNonNull(p);

    try {
      if (AdobeAdeptConnectorFactory.INSTANCE != null) {
        AdobeAdeptConnectorFactory.LOG.debug(
          "returning saved instance {}", AdobeAdeptConnectorFactory.INSTANCE);
        return Objects.requireNonNull(AdobeAdeptConnectorFactory.INSTANCE);
      }

      AdobeAdeptConnectorFactory.LOG.debug(
        "looking up class {}", AdobeAdeptConnectorFactory.CLASS_NAME);
      @SuppressWarnings("unchecked")
      final Class<? extends AdobeAdeptConnectorType> c =
        (Class<? extends AdobeAdeptConnectorType>) Class.forName(
          AdobeAdeptConnectorFactory.CLASS_NAME);

      AdobeAdeptConnectorFactory.LOG.debug(
        "looking up 'get' method on {}", c);
      final Method gm = c.getMethod(
        "get",
        AdobeAdeptConnectorParameters.class);

      AdobeAdeptConnectorFactory.LOG.debug("invoking 'get' method on {}", c);
      final AdobeAdeptConnectorType instance = Objects.requireNonNull((AdobeAdeptConnectorType) gm.invoke(null, p));

      AdobeAdeptConnectorFactory.INSTANCE = instance;
      AdobeAdeptConnectorFactory.LOG.debug("returning fresh instance");
      return instance;

    } catch (final ClassNotFoundException e) {
      throw new DRMUnsupportedException(e);
    } catch (final NoSuchMethodException e) {
      throw new DRMUnsupportedException(e);
    } catch (final SecurityException e) {
      throw new DRMUnsupportedException(e);
    } catch (final IllegalAccessException e) {
      throw new DRMUnsupportedException(e);
    } catch (final IllegalArgumentException e) {
      throw new DRMUnsupportedException(e);
    } catch (final InvocationTargetException e) {
      throw new DRMUnsupportedException(e);
    }
  }
}
