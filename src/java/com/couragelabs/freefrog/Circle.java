package com.couragelabs.freefrog;

import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PSet;

import java.util.Set;

/**
 * A circle is a Role but with Roles within.
 */
public class Circle extends Role {
  private final String name;
  private final PMap<String, Role> roles;

  public Circle(String name) {
    this(name, HashTreePMap.<String, Role>empty());
  }

  Circle(String name, PMap<String, Role> roles) {
    this(name, null, null, roles);
  }

  public Circle(String name, String purpose, PSet<String> domains,
                PMap<String, Role> roles) {
    super(purpose, domains);
    this.name = name;
    this.roles = roles;
  }

  public Circle(Circle circle, PMap<String, Role> roles) {
    this(circle.name, circle.getPurpose(), circle.getDomains(), roles);
  }

  /**
   * @return the set of roles this circle has
   */
  public Set<String> getRoleNames() {
    return roles.keySet();
  }

  /**
   * Add a new role and allow you to manipulate it.
   * @param name Name of the role to create.
   * @return A context that allows manipulation of the role.
   */
  public RoleManipulationContext addRole(String name) {
    return new RoleManipulationContext(this, name, roles);
  }

  /**
   * Create a context wherein one can update an existing role within this
   * Circle.
   * @param name Name of the role to update.
   * @return A context that allows manipulation of the role.
   */
  public RoleManipulationContext updateRole(String name) {
    if (roles.containsKey(name)) {
      return new RoleManipulationContext(this, name, roles);
    }
    throw new RoleNotFoundException(String.format("Role not found: %s", name));
  }

  /**
   * @return A new Circle with the role oldName named as role newName
   */
  public Circle renameRole(String oldName, String newName) {
    return new Circle(name, getPurpose(), getDomains(),
        roles.minus(oldName).plus(newName, roles.get(oldName)));
  }

  /**
   * @return The purpose of the given role.
   */
  public String getPurpose(String roleName) {
    return roles.get(roleName).getPurpose();
  }

  /**
   * @return a new Circle with the given new purpose
   */
  @Override
  public Circle updatePurpose(String purpose) {
    return new Circle(name, purpose, getDomains(), roles);
  }

  @Override
  protected Circle changeDomains(PSet<String> newDomains) {
    return new Circle(name, getPurpose(), newDomains, roles);
  }
}
