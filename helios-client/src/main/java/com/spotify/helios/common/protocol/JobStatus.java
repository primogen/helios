/**
 * Copyright (C) 2013 Spotify AB
 */

package com.spotify.helios.common.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spotify.helios.common.descriptors.Deployment;
import com.spotify.helios.common.descriptors.Job;
import com.spotify.helios.common.descriptors.TaskStatus;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.ImmutableSet.copyOf;

public class JobStatus {

  private final Job job;
  private final Map<String, TaskStatus> taskStatuses;
  private final Map<String, Deployment> deployments;

  // TODO (dano): remove this field when all masters are upgraded
  @Deprecated
  private final Set<String> deployedHosts;

  public JobStatus(@JsonProperty("job") final Job job,
                   @JsonProperty("taskStatuses") final Map<String, TaskStatus> taskStatuses,
                   @JsonProperty("deployments") final Map<String, Deployment> deployments,
                   @JsonProperty("deployedHosts") final Set<String> deployedHosts) {
    this.job = job;
    this.taskStatuses = taskStatuses;
    this.deployments = deployments;
    this.deployedHosts = deployedHosts;
  }

  public JobStatus(final Builder builder) {
    this.job = builder.job;
    this.taskStatuses = builder.taskStatuses;
    this.deployments = builder.deployments;
    this.deployedHosts = builder.deployedHosts;
  }

  public Job getJob() {
    return job;
  }

  /** @return a map of host to task status */
  public Map<String, TaskStatus> getTaskStatuses() {
    return taskStatuses;
  }

  /** @return a map of host to deployment */
  public Map<String, Deployment> getDeployments() {
    return deployments;
  }

  @Deprecated
  public Set<String> getDeployedHosts() {
    return deployedHosts;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    JobStatus jobStatus = (JobStatus) o;

    if (deployedHosts != null ? !deployedHosts.equals(jobStatus.deployedHosts)
                              : jobStatus.deployedHosts != null) {
      return false;
    }
    if (deployments != null ? !deployments.equals(jobStatus.deployments)
                            : jobStatus.deployments != null) {
      return false;
    }
    if (job != null ? !job.equals(jobStatus.job) : jobStatus.job != null) {
      return false;
    }
    if (taskStatuses != null ? !taskStatuses.equals(jobStatus.taskStatuses)
                             : jobStatus.taskStatuses != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = job != null ? job.hashCode() : 0;
    result = 31 * result + (taskStatuses != null ? taskStatuses.hashCode() : 0);
    result = 31 * result + (deployments != null ? deployments.hashCode() : 0);
    result = 31 * result + (deployedHosts != null ? deployedHosts.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "JobStatus{" +
           "job=" + job +
           ", taskStatuses=" + taskStatuses +
           ", deployments=" + deployments +
           ", deployedHosts=" + deployedHosts +
           '}';
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Job job;
    private Map<String, TaskStatus> taskStatuses;
    private Set<String> deployedHosts;
    public Map<String, Deployment> deployments;

    public Builder setJob(final Job job) {
      this.job = job;
      return this;
    }

    public Builder setTaskStatuses(final Map<String, TaskStatus> taskStatuses) {
      this.taskStatuses = copyOf(taskStatuses);
      return this;
    }

    @Deprecated
    public Builder setDeployedHosts(final Set<String> deployedHosts) {
      this.deployedHosts = copyOf(deployedHosts);
      return this;
    }

    public Builder setDeployments(final Map<String, Deployment> deployments) {
      this.deployments = copyOf(deployments);
      return this;
    }

    public JobStatus build() {
      return new JobStatus(this);
    }
  }
}
