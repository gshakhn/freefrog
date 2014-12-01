package com.couragelabs.freefrog;

import org.junit.Test;
import org.pcollections.HashTreePSet;

import static com.couragelabs.freefrog.Assert.assertThrows;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class CircleTest {
  public static final Circle TEST_CIRCLE = new Circle("Test");
  public static final String ROLE_NAME = "Programmer";
  public static final String CIRCLE_PURPOSE = "Doing great things";
  public static final String CIRCLE_DOMAIN1 = "Everything";
  public static final String ROLE_PURPOSE = "Building awesome software";
  public static final String ANOTHER_ROLE = "Another Role";
  public static final String ANOTHER_ROLE_RENAMED = "A Renamed Role";

  @Test
  public void itUpdatesItsDomains() {
    Circle c = TEST_CIRCLE.addRole(ROLE_NAME).done();
    c = c.updatePurpose(CIRCLE_PURPOSE);
    assertEquals(HashTreePSet.singleton(ROLE_NAME), c.getRoleNames());
    assertEquals(CIRCLE_PURPOSE, c.getPurpose());

    c = c.addDomain(CIRCLE_DOMAIN1);
    assertEquals(HashTreePSet.singleton(ROLE_NAME), c.getRoleNames());
    assertEquals(HashTreePSet.singleton(CIRCLE_DOMAIN1), c.getDomains());

    c = c.removeDomain(CIRCLE_DOMAIN1);
    assertEquals(HashTreePSet.singleton(ROLE_NAME), c.getRoleNames());
    assertEquals(HashTreePSet.empty(), c.getDomains());
  }

  /**
   * We don't need to do to much manipulation, since all we are doing
   * is passing functions from Role into the RoleManipulationContext.
   *
   * We just need to make sure it changes the Role into the new one
   * appropriately and we're golden.
   */
  @Test
  public void itManipulatesRoles() {
    assertTrue(TEST_CIRCLE.getRoleNames().isEmpty());
    // Add/manipulate roles and update purposes
    Circle c = TEST_CIRCLE.addRole(ROLE_NAME)
        .change(Role::updatePurpose, ROLE_PURPOSE).done()
        .addRole(ANOTHER_ROLE).done();
    assertEquals(HashTreePSet.from(asList(ROLE_NAME, ANOTHER_ROLE)),
        c.getRoleNames());
    assertEquals(ROLE_PURPOSE, c.getPurpose(ROLE_NAME));
    assertEquals(null, c.getPurpose(ANOTHER_ROLE));

    c = c.updateRole(ANOTHER_ROLE)
        .change(Role::updatePurpose, "Another purpose").done();
    assertEquals("Another purpose", c.getPurpose(ANOTHER_ROLE));

    c = c.renameRole(ANOTHER_ROLE, ANOTHER_ROLE_RENAMED);
    assertEquals(HashTreePSet.from(asList(ROLE_NAME, ANOTHER_ROLE_RENAMED)),
        c.getRoleNames());
    assertEquals("Another purpose", c.getPurpose(ANOTHER_ROLE_RENAMED));
  }

  @Test
  public void itDoesNotUpdateNonexistentRoles() {
    assertThrows(RoleNotFoundException.class,
        format("Role not found: %s", ROLE_NAME),
        () -> TEST_CIRCLE.updateRole(ROLE_NAME));
  }
}
