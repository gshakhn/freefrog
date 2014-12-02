package com.couragelabs.freefrog;

import org.pcollections.HashTreePSet;
import org.pcollections.PSet;

import static java.lang.String.format;

/**
 * This is a Holacracy Role.
 */
public class Role {
  private final String purpose;
  private final PSet<String> domains;

  public Role() {
    this(null, null);
  }

  Role(String purpose, PSet<String> domains) {
    this.purpose = purpose;
    this.domains = domains == null ? HashTreePSet.<String>empty() : domains;
  }

  public String getPurpose() {
    return purpose;
  }

  /**
   * @return a new Role with the purpose changed to the given one
   */
  public Role updatePurpose(String purpose) {
    return new Role(purpose, domains);
  }

  public PSet<String> getDomains() {
    return domains;
  }

  protected void validateRoleState(boolean condition, String format,
                                   String value) {
    if (condition) {
      throw new IllegalStateException(format(format, value));
    }
  }

  /**
   * @return a new Role with the given domain added
   */
  @SuppressWarnings("unchecked")
  public <T extends Role> T addDomain(String domain) {
    validateRoleState(domains.contains(domain), "Domain '%s' already exists.",
        domain);
    return (T) changeDomains(domains.plus(domain));
  }

  /**
   * @return a new Role with the given domain removed
   */
  @SuppressWarnings("unchecked")
  public <T extends Role> T removeDomain(String domain) {
    validateRoleState(!domains.contains(domain), "Domain '%s' doesn't exist.",
        domain);
    return (T) changeDomains(domains.minus(domain));
  }

  protected Role changeDomains(PSet<String> newDomains) {
    return new Role(purpose, newDomains);
  }
}
