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
package edu.sdsc.scigraph.services.health;

import javax.inject.Inject;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import com.codahale.metrics.health.HealthCheck;

import edu.sdsc.scigraph.neo4j.Graph;

public class Neo4jHealthCheck extends HealthCheck {

  private final Graph graph;

  @Inject
  Neo4jHealthCheck(Graph graph) {
    this.graph = graph;
  }

  @Override
  protected Result check() throws Exception {
    Node node = null;
    try (Transaction tx = graph.getGraphDb().beginTx()) {
      node = graph.getGraphDb().getNodeById(1);
      tx.success();
    }

    if (null != node) {
      return Result.healthy();
    } else {
      return Result.unhealthy("Could not find nodes in the graph");
    }
  }

}
