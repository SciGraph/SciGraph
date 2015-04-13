/**
 * Copyright (C) 2014 The SciGraph authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.sdsc.scigraph.analyzer;

import org.neo4j.graphdb.Node;

public class AnalyzerResult {

  private final Node node;
  private final double count;

  public AnalyzerResult(Node n, double count) {
    this.node = n;
    this.count = count;
  }

  public AnalyzerResult(Node n, int count) {
    this.node = n;
    this.count = count;
  }

  public Node getNode() {
    return node;
  }

  public double getCount() {
    return count;
  }
}
