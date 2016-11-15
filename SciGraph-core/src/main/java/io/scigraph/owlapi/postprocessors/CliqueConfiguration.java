/**
 * Copyright (C) 2014 The SciGraph authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.scigraph.owlapi.postprocessors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CliqueConfiguration {
  @JsonProperty
  private Set<String> relationships = new HashSet<String>();
  private String leaderAnnotation = "";
  private List<String> leaderPriority = new ArrayList<String>();
  private Set<String> leaderForbiddenLabels = new HashSet<String>();
  private int batchCommitSize = 100;

  public Set<String> getRelationships() {
    return relationships;
  }

  public void setRelationships(Set<String> relationships) {
    this.relationships = relationships;
  }

  public String getLeaderAnnotation() {
    return leaderAnnotation;
  }

  public void setLeaderAnnotation(String leaderAnnotation) {
    this.leaderAnnotation = leaderAnnotation;
  }

  public List<String> getLeaderPriority() {
    return leaderPriority;
  }

  public void setLeaderPriority(List<String> leaderPriority) {
    this.leaderPriority = leaderPriority;
  }

  public Set<String> getLeaderForbiddenLabels() {
    return leaderForbiddenLabels;
  }

  public void setLeaderForbiddenLabels(Set<String> leaderForbiddenLabels) {
    this.leaderForbiddenLabels = leaderForbiddenLabels;
  }

  public int getBatchCommitSize() {
    return batchCommitSize;
  }

  public void setBatchCommitSize(int batchCommitSize) {
    this.batchCommitSize = batchCommitSize;
  }
}
