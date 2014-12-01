package com.couragelabs.freefrog;

import org.pcollections.PMap;

import java.util.function.BiFunction;

public class RoleManipulationContext {
  private final Circle circle;
  private final String roleName;
  private final PMap<String, Role> roles;

  public RoleManipulationContext(Circle circle, String roleName,
                                 PMap<String, Role> roles) {
    this.roleName = roleName;
    this.roles = roles.containsKey(roleName) ? roles :
        roles.plus(roleName, new Role());
    this.circle = new Circle(circle, this.roles);
  }

  /**
   * We are done manipulating the role
   *
   * @return a new circle with the modified role
   */
  public Circle done() {
    return circle;
  }

  /**
   * @param fn  function to execute on the role
   * @param arg the argument to pass to the role
   * @return a new context with that role changed into the result of the fn and
   * its parent circle changed as a result.
   */
  public RoleManipulationContext change(BiFunction<Role, String, Role> fn,
                                        String arg) {
    return new RoleManipulationContext(circle, roleName, roles.plus(roleName,
        fn.apply(roles.get(roleName), arg)));
  }
}
