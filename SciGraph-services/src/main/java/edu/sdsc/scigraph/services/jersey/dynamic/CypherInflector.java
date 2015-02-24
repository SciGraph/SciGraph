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

import static com.google.common.collect.Sets.newHashSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;

import jersey.repackaged.com.google.common.base.Splitter;

import org.glassfish.jersey.process.Inflector;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Transaction;

import scala.collection.convert.Wrappers.SeqWrapper;

import com.google.common.base.Joiner;
import com.google.inject.assistedinject.Assisted;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import edu.sdsc.scigraph.internal.TinkerGraphUtil;
import edu.sdsc.scigraph.services.swagger.beans.resource.Apis;

final class CypherInflector implements Inflector<ContainerRequestContext, TinkerGraph> {

  private static final Logger logger = Logger.getLogger(CypherInflector.class.getName());

  private static final String ENTAILMENT_REGEX = "\\:[a-zA-Z0-9_:.|]*!";
  private static Pattern pattern = Pattern.compile(ENTAILMENT_REGEX);

  private final GraphDatabaseService graphDb;
  private final ExecutionEngine engine;
  private final Apis config;

  @Inject
  CypherInflector(GraphDatabaseService graphDb, ExecutionEngine engine, @Assisted Apis config) {
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
        } else if (entry.getValue() instanceof SeqWrapper) {
          SeqWrapper<?> wrapper = (SeqWrapper<?>)entry.getValue();
          for (Object thing: wrapper) {
            if (thing instanceof PropertyContainer) {
              TinkerGraphUtil.addElement(graph, (PropertyContainer) thing);
            }
          }
        } else {
          logger.warning("Not converting " + entry.getValue().getClass() + " to tinker graph");
        }
      }
    }
    return graph;
  }

  Set<String> getEntailedRelationshipTypes(Set<String> parents) {
    Set<String> entailedTypes = new HashSet<>();
    for (String parent: parents) {
      entailedTypes.add(parent);
      Map<String, Object> params = new HashMap<>();
      params.put("fragment", parent);
      try (Transaction tx = graphDb.beginTx()) {
        ExecutionResult result = engine.execute(
            "START parent=node:node_auto_index(fragment={fragment}) " +
                "MATCH (parent)<-[:subPropertyOf*]-(subProperty) " +
                "RETURN distinct subProperty.fragment as subProperty", params);
        for (Map<String, Object> resultMap: result) {
          entailedTypes.add((String) resultMap.get("subProperty"));
        }
      }
    }
    return entailedTypes;
  }

  /***
   * 
   * @param cypher
   * @return
   */
  String entailRelationships(String cypher) {
    Matcher m = pattern.matcher(cypher);
    StringBuffer buffer = new StringBuffer();
    while (m.find()) {
      String group = m.group().substring(1, m.group().length() - 1);
      Set<String> parentTypes = newHashSet(Splitter.on('|').split(group));
      String entailedTypes = Joiner.on('|').join(getEntailedRelationshipTypes(parentTypes));
      m.appendReplacement(buffer, ":" + entailedTypes);
    }
    m.appendTail(buffer);
    return buffer.toString();
  }

  @Override
  public TinkerGraph apply(ContainerRequestContext context) {
    logger.fine("Serving dynamic request");
    MultivaluedMap<String, String> params = context.getUriInfo().getQueryParameters();
    Map<String, Object> flatMap = flatten(params);
    try (Transaction tx = graphDb.beginTx()) {
      String query = entailRelationships(config.getQuery());
      ExecutionResult result = engine.execute(query, flatMap);
      TinkerGraph graph = resultToGraph(result);
      tx.success();
      return graph;
    }
  }

}
