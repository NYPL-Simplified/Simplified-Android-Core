package org.nypl.simplified.tests;

import com.io7m.jfunctional.ProcedureType;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;

import java.util.List;

public final class EventAssertions {

  /**
   * Assert that the element at {@code index} in {@code events} is of type {@code clazz} and
   * obeys the property given in {@code property}.
   */

  public static <T extends TB, TB> void isTypeAndMatches(
      final Class<T> clazz,
      final List<TB> events,
      final int index,
      final ProcedureType<T> property)
  {
    Assert.assertTrue(
        new StringBuilder(64)
            .append("List of events must be at least ")
            .append(index + 1)
            .append(" elements long (is: ")
            .append(events.size())
            .append(")")
            .toString(),
        events.size() >= index + 1);

    final TB x = events.get(index);
    Assert.assertThat(x, IsInstanceOf.instanceOf(clazz));
    final T y = (T) x;
    property.call(y);
  }

  /**
   * Assert that the element at {@code index} in {@code events} is of type {@code clazz}.
   */

  public static <T extends TB, TB> void isType(
      final Class<T> clazz,
      final List<TB> events,
      final int index)
  {
    isTypeAndMatches(clazz, events, index, e -> {});
  }

  /**
   * Assert that the elements in the given list have the corresponding types.
   */

  public static void isListWithTypes(
    final List<?> events,
    final List<Class<?>> types)
  {
    Assert.assertEquals(
      "Expected a list of " + types.size() + " events",
      events.size(),
      types.size());

    for (int index = 0; index < events.size(); ++index) {
      Class<?> expectedClass = types.get(index);
      Object received = events.get(index);

      Assert.assertThat(
        String.format("Expected [%d] %s == Received [%d] %s\n", index, expectedClass, index, received),
        received,
        new IsInstanceOf(expectedClass));
    }
  }
}
