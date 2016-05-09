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
package io.scigraph.services.health;

import static com.google.common.collect.Iterables.size;

import javax.inject.Inject;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import ru.vyarus.dropwizard.guice.module.installer.feature.health.NamedHealthCheck;

public class Neo4jHealthCheck extends NamedHealthCheck {

  private final GraphDatabaseService graphDb;

  @Inject
  Neo4jHealthCheck(GraphDatabaseService graphDb) {
    this.graphDb = graphDb;
  }

  @Override
  protected Result check() throws Exception {
    int count = 0;
    try (Transaction tx = graphDb.beginTx()) {
      count = size(graphDb.getAllNodes());
      tx.success();
    }

    if (count > 0) {
      return Result.healthy();
    } else {
      return Result.unhealthy("There are no nodes in the graph.");
    }
  }

  @Override
  public String getName() {
    return "Neo4j Health Check";
  }

}
