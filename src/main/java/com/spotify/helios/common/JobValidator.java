/**
 * Copyright (C) 2013 Spotify AB
 */

package com.spotify.helios.common;

import com.google.common.collect.Sets;

import com.spotify.helios.common.descriptors.Job;
import com.spotify.helios.common.descriptors.JobId;
import com.spotify.helios.common.descriptors.PortMapping;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class JobValidator {

  public static final Pattern DOMAIN_PATTERN =
      Pattern.compile("^(?:(?:[a-zA-Z0-9]|(?:[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9]))" +
                      "(\\.(?:[a-zA-Z0-9]|(?:[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])))*)\\.?$");

  public static final Pattern IPV4_PATTERN =
      Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

  public static final Pattern NAMESPACE_PATTERN = Pattern.compile("^([a-z0-9_]{4,30})$");
  public static final Pattern REPO_PATTERN = Pattern.compile("^([a-z0-9-_.]+)$");
  public static final Pattern DIGIT_PERIOD = Pattern.compile("^[0-9.]+$");

  public Set<String> validate(final Job job) {
    final Set<String> errors = Sets.newHashSet();

    // Check that the job name and version only contains allowed characters
    final Pattern nameVersionPattern = Pattern.compile("[0-9a-zA-Z-_.]+");
    if (!nameVersionPattern.matcher(job.getId().getName()).matches()) {
      errors.add(format("Job name may only contain [0-9a-zA-Z-_.]."));
    }
    if (!nameVersionPattern.matcher(job.getId().getVersion()).matches()) {
      errors.add(format("Job version may only contain [0-9a-zA-Z-_.]."));
    }

    // Validate image name
    validateImageReference(job.getImage(), errors);

    // Check that the job id is correct
    final JobId recomputedId = job.toBuilder().build().getId();
    if (!recomputedId.equals(job.getId())) {
      errors.add(format("Id mismatch: %s != %s", job.getId(), recomputedId));
    }

    // Check that there's not external port collission
    final Set<Integer> externalPorts = Sets.newHashSet();
    for (final PortMapping mapping : job.getPorts().values()) {
      Integer externalMappedPort = mapping.getExternalPort();
      if (externalPorts.contains(externalMappedPort) && externalMappedPort != null) {
        errors.add(format("Duplicate external port mapping: %s", externalMappedPort));
      }
      externalPorts.add(externalMappedPort);
    }

    return errors;
  }

  @SuppressWarnings("ConstantConditions")
  private boolean validateImageReference(final String imageRef, final Collection<String> errors) {
    boolean valid = true;

    final String repo;
    final String tag;

    final int lastColon = imageRef.lastIndexOf(':');
    if (lastColon != -1 && !(tag = imageRef.substring(lastColon + 1)).contains("/")) {
      repo = imageRef.substring(0, lastColon);
      valid &= validateTag(tag, errors);
    } else {
      repo = imageRef;
    }

    final String INVALID_REPOSITORY_NAME =
        "Invalid repository name (ex: \"registry.domain.tld/myrepos\")";
    if (repo.contains("://")) {
      // It cannot contain a scheme!
      errors.add(INVALID_REPOSITORY_NAME);
      return false;
    }

    final String[] nameParts = repo.split("/", 2);
    if (!nameParts[0].contains(".") && !nameParts[0].contains(".") && !nameParts[0].contains(":") &&
        !nameParts[0].equals("localhost")) {
      // This is a Docker Index repos (ex: samalba/hipache or ubuntu)
      return validateRepositoryName(repo, errors);
    }

    if (nameParts.length < 2) {
      // There is a dot in repos name (and no registry address)
      // Is it a Registry address without repos name?
      errors.add(INVALID_REPOSITORY_NAME);
      return false;
    }

    final String endpoint = nameParts[0];
    final String reposName = nameParts[1];
    valid &= validateEndpoint(endpoint, errors);
    valid &= validateRepositoryName(reposName, errors);
    return valid;
  }

  private boolean validateTag(final String tag, final Collection<String> errors) {
    boolean valid = true;
    if (tag.isEmpty()) {
      errors.add("Tag cannot be empty");
      valid = false;
    }
    if (tag.contains("/") || tag.contains(":")) {
      errors.add(format("Illegal tag: \"%s\"", tag));
      valid = false;
    }
    return valid;
  }

//  func ParseRepositoryTag(repos string) (string, string) {
//    n := strings.LastIndex(repos, ":")
//    if n < 0 {
//      return repos, ""
//    }
//    if tag := repos[n+1:]; !strings.Contains(tag, "/") {
//      return repos[:n], tag
//    }
//    return repos, ""
//  }


  private boolean validateEndpoint(final String endpoint, final Collection<String> errors) {
    final String[] parts = endpoint.split(":", 2);
    if (!validateAddress(parts[0], errors)) {
      return false;
    }
    if (parts.length > 1) {
      final int port;
      try {
        port = Integer.valueOf(parts[1]);
      } catch (NumberFormatException e) {
        errors.add(String.format("Invalid port in endpoint: \"%s\"", endpoint));
        return false;
      }
      if (port < 0 || port > 65535) {
        errors.add(String.format("Invalid port in endpoint: \"%s\"", endpoint));
        return false;
      }
    }
    return true;
  }

  private boolean validateAddress(final String address, final Collection<String> errors) {
    if (IPV4_PATTERN.matcher(address).matches()) {
      return true;
    } else if (!DOMAIN_PATTERN.matcher(address).matches() || DIGIT_PERIOD.matcher(address).find()) {
      errors.add(String.format("Invalid domain name: \"%s\"", address));
      return false;
    }
    return true;
  }

  private boolean validateRepositoryName(final String repositoryName,
                                         final Collection<String> errors) {
    boolean valid = true;
    String repo;
    String name;
    final String[] nameParts = repositoryName.split("/", 2);
    if (nameParts.length < 2) {
      repo = "library";
      name = nameParts[0];
    } else {
      repo = nameParts[0];
      name = nameParts[1];
    }
    if (!NAMESPACE_PATTERN.matcher(repo).matches()) {
      errors.add(
          format("Invalid namespace name (%s), only [a-z0-9_] are allowed, size between 4 and 30",
                 repo));
      valid = false;
    }
    if (!REPO_PATTERN.matcher(name).matches()) {
      errors.add(format("Invalid repository name (%s), only [a-z0-9-_.] are allowed", name));
      valid = false;
    }
    return valid;
  }
}
