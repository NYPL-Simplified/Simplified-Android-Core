package org.nypl.simplified.tests;

import com.io7m.jfunctional.ProcedureType;

import org.junit.jupiter.api.Assertions;

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
    final ProcedureType<T> property) {
    Assertions.assertTrue(
      events.size() >= index + 1,
      new StringBuilder(64)
        .append("List of events must be at least ")
        .append(index + 1)
        .append(" elements long (is: ")
        .append(events.size())
        .append(")")
        .toString());

    final TB x = events.get(index);
    if (!clazz.isAssignableFrom(x.getClass())) {
      Assertions.fail(
        String.format("Expected class %s != received class %s", clazz, x.getClass())
      );
    }

    final T y = (T) x;
    property.call(y);
  }

  /**
   * Assert that the element at {@code index} in {@code events} is of type {@code clazz}.
   */

  public static <T extends TB, TB> void isType(
    final Class<T> clazz,
    final List<TB> events,
    final int index) {
    isTypeAndMatches(clazz, events, index, e -> {
    });
  }

  /**
   * Assert that the elements in the given list have the corresponding types.
   */

  public static void isListWithTypes(
    final List<?> events,
    final List<Class<?>> types) {
    Assertions.assertEquals(
      events.size(),
      types.size(),
      "Expected a list of " + types.size() + " events"
    );

    for (int index = 0; index < events.size(); ++index) {
      Class<?> expectedClass = types.get(index);
      Object received = events.get(index);

      if (received.getClass() != expectedClass) {
        Assertions.fail(
          String.format("Expected [%d] %s == Received [%d] %s\n", index, expectedClass, index, received)
        );
      }
    }
  }
}
