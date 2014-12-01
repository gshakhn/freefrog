package com.couragelabs.freefrog;

import org.junit.Test;
import org.pcollections.HashTreePSet;

import static com.couragelabs.freefrog.Assert.assertThrows;
import static java.lang.String.format;
import static junit.framework.Assert.assertEquals;

public class RoleTest {

  public static final String DOMAIN = "testDomain";
  public static final String PURPOSE = "Doing stuff";

  @Test
  public void itUpdatesPurpose() {
    Role testRole = new Role().addDomain(DOMAIN);
    assertEquals(null, testRole.getPurpose());
    assertEquals(PURPOSE, testRole.updatePurpose(PURPOSE).getPurpose());
  }

  @Test
  public void itUpdatesDomains() {
    Role testRole = new Role().addDomain(DOMAIN);
    assertEquals(HashTreePSet.singleton(DOMAIN), testRole.getDomains());
    assertThrows(IllegalStateException.class,
        format("Domain '%s' already exists.", DOMAIN),
        () -> testRole.addDomain(DOMAIN));
    assertEquals(HashTreePSet.empty(), testRole.removeDomain(DOMAIN)
        .getDomains());
  }
}
