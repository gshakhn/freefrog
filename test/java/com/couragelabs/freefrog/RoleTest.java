package com.couragelabs.freefrog;

import org.junit.Test;
import org.pcollections.HashTreePSet;

import java.util.function.Consumer;

import static com.couragelabs.freefrog.Assert.assertThrows;
import static java.lang.String.format;
import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNull;

public class RoleTest {

  public static final String DOMAIN = "testDomain";
  public static final String PURPOSE = "Doing stuff";

  private static <T extends Role> void postAssert(Consumer<T>[] postAsserts,
                                                  T thingToCheck) {
    if (postAsserts.length > 0) {
      postAsserts[0].accept(thingToCheck);
    }
  }

  @Test
  public void itUpdatesPurpose() {
    assertPurposeUpdates(new Role());
  }

  @SuppressWarnings("unchecked")
  @SafeVarargs
  public static <T extends Role> void assertPurposeUpdates(
      Role r, Consumer<T>... postAsserts) {
    final T thingToTest = (T) r.addDomain(DOMAIN);
    assertNull("Purpose should be null", thingToTest.getPurpose());
    assertEquals(PURPOSE, thingToTest.updatePurpose(PURPOSE).getPurpose());
    assertEquals(HashTreePSet.singleton(DOMAIN), thingToTest.getDomains());
    postAssert(postAsserts, thingToTest);
  }

  @Test
  public void itUpdatesDomains() {
    assertDomainManipulation(new Role());
  }

  @SuppressWarnings("unchecked")
  @SafeVarargs
  public static <T extends Role> void assertDomainManipulation
      (Role testRole, Consumer<T>... postAsserts) {
    final T withDomain = (T) testRole.addDomain(DOMAIN);
    assertEquals(HashTreePSet.singleton(DOMAIN), withDomain.getDomains());
    assertThrows(IllegalStateException.class,
        format("Domain '%s' already exists.", DOMAIN),
        () -> withDomain.addDomain(DOMAIN));
    postAssert(postAsserts, withDomain);

    final T removed = (T) withDomain.removeDomain(DOMAIN);
    assertEquals(HashTreePSet.<String>empty(), removed.getDomains());
    assertThrows(IllegalStateException.class,
        format("Domain '%s' doesn't exist.", "nonexistent"),
        () -> removed.removeDomain("nonexistent"));
    postAssert(postAsserts, removed);
  }
}
