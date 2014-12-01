package com.couragelabs.freefrog;

import java.util.concurrent.Callable;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.fail;

public class Assert {
  static void assertThrows(Class<? extends Exception> expectedType,
                           String expectedMessage, Callable callable) {
    try {
      callable.call();
      fail(String.format("Expected exception '%s/%s' to be thrown.",
          expectedType.getName(), expectedMessage));
    } catch (Exception e) {
      assertTrue(String.format(
              "Exception thrown '%s/%s' does not extend from '%s'.",
              e.getClass().getName(), e.getMessage(), expectedType.getName()),
          expectedType.isAssignableFrom(e.getClass()));
      assertEquals(expectedMessage, e.getMessage());
    }
  }
}
