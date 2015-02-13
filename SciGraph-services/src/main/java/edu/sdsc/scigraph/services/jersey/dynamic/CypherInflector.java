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
package edu.sdsc.scigraph.services.jersey.dynamic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.process.Inflector;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Transaction;

import com.google.inject.assistedinject.Assisted;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import edu.sdsc.scigraph.internal.TinkerGraphUtil;

final class CypherInflector implements Inflector<ContainerRequestContext, TinkerGraph> {

  private static final Logger logger = Logger.getLogger(CypherInflector.class.getName());
  
  private final GraphDatabaseService graphDb;
  private final ExecutionEngine engine;
  private final CypherResourceConfig config;

  @Inject
  CypherInflector(GraphDatabaseService graphDb, ExecutionEngine engine, @Assisted CypherResourceConfig config) {
    this.graphDb = graphDb;
    this.engine = engine;
    this.config = config;
  }

  static Map<String, Object> flatten(MultivaluedMap<String, String> map) {
    Map<String, Object> flatMap = new HashMap<>();
    for (Entry<String, List<String>> entry: map.entrySet()) {
      flatMap.put(entry.getKey(), entry.getValue().get(0));
    }
    return flatMap;
  }

  static TinkerGraph resultToGraph(ExecutionResult result) {
    TinkerGraph graph = new TinkerGraph();
    for (Map<String, Object> map: result) {
      for (Entry<String, Object> entry: map.entrySet()) {
        if (entry.getValue() instanceof PropertyContainer) {
          TinkerGraphUtil.addElement(graph, (PropertyContainer)entry.getValue());
        }
      }
    }
    return graph;
  }

  @Override
  public TinkerGraph apply(ContainerRequestContext context) {
    logger.fine("Serving dynamic request");
    MultivaluedMap<String, String> params = context.getUriInfo().getQueryParameters();
    Map<String, Object> flatMap = flatten(params);
    try (Transaction tx = graphDb.beginTx()) {
      ExecutionResult result = engine.execute(config.getQuery(), flatMap);
      TinkerGraph graph = resultToGraph(result);
      tx.success();
      return graph;
    }
  }

}
