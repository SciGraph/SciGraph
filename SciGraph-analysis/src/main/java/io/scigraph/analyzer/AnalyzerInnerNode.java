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
package io.scigraph.analyzer;

import java.util.Objects;

public final class AnalyzerInnerNode {

  private final long nodeId;
  private final double count;

  public AnalyzerInnerNode(long n, double count) {
    this.nodeId = n;
    this.count = count;
  }

  public AnalyzerInnerNode(long n, int count) {
    this.nodeId = n;
    this.count = count;
  }

  public long getNodeId() {
    return nodeId;
  }

  public double getCount() {
    return count;
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeId);
  }

  @Override
  public boolean equals(Object obj) {
    if (null == obj) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(getNodeId(), ((AnalyzerInnerNode)obj).getNodeId());
  }

  @Override
  public String toString() {
    return nodeId + " (" + count + ")";
  }

}
