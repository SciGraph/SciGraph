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

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;

import jersey.repackaged.com.google.common.base.Splitter;

import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.glassfish.jersey.process.Inflector;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Transaction;

import scala.collection.convert.Wrappers.SeqWrapper;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.assistedinject.Assisted;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import edu.sdsc.scigraph.internal.GraphAspect;
import edu.sdsc.scigraph.internal.TinkerGraphUtil;
import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.owlapi.curies.AddCurries;
import edu.sdsc.scigraph.owlapi.curies.CurieUtil;
import edu.sdsc.scigraph.services.jersey.MultivaluedMapUtils;
import edu.sdsc.scigraph.services.swagger.beans.resource.Apis;

class CypherInflector implements Inflector<ContainerRequestContext, TinkerGraph> {

  private static final Logger logger = Logger.getLogger(CypherInflector.class.getName());

  private static final String ENTAILMENT_REGEX = "\\:[a-zA-Z0-9_:.|]*!";
  private static Pattern pattern = Pattern.compile(ENTAILMENT_REGEX);

  private final GraphDatabaseService graphDb;
  private final ExecutionEngine engine;
  private final Apis config;
  private final CurieUtil curieUtil;
  private final Map<String, GraphAspect> aspectMap;

  @Inject
  CypherInflector(GraphDatabaseService graphDb, ExecutionEngine engine, CurieUtil curieUtil,
      @Assisted Apis config, Map<String, GraphAspect> aspectMap) {
    this.graphDb = graphDb;
    this.engine = engine;
    this.config = config;
    this.curieUtil = curieUtil;
    this.aspectMap = aspectMap;
  }

  @AddCurries
  @Override
  public TinkerGraph apply(ContainerRequestContext context) {
    logger.fine("Serving dynamic request");
    Multimap<String, String> paramMap = MultivaluedMapUtils.merge(context.getUriInfo());
    paramMap = resolveCuries(paramMap);
    String query = substituteRelationships(config.getQuery(), paramMap);
    Map<String, Object> flatMap = flattenMap(paramMap);
    try (Transaction tx = graphDb.beginTx()) {
      long start = System.currentTimeMillis();
      query = entailRelationships(query);
      logger.fine((System.currentTimeMillis() - start) + " to entail relationships" );
      start = System.currentTimeMillis();
      ExecutionResult result = engine.execute(query, flatMap);
      logger.fine((System.currentTimeMillis() - start) + " to execute query" );
      start = System.currentTimeMillis();
      TinkerGraph graph = resultToGraph(result);
      logger.fine((System.currentTimeMillis() - start) + " to convert to graph" );
      start = System.currentTimeMillis();
      for (String key: aspectMap.keySet()) {
        if (flatMap.containsKey(key) && "true".equals(flatMap.get(key))) {
          aspectMap.get(key).invoke(graph);
        }
      }
      tx.success();
      return graph;
    }
  }

  Map<String, Object> flattenMap(Multimap<String, String> paramMap) {
    Map<String, Object> flatMap = new HashMap<>();
    for (Entry<String, Collection<String>> entry: paramMap.asMap().entrySet()) {
      flatMap.put(entry.getKey(), getFirst(entry.getValue(), null));
    }
    return flatMap;
  }

  Multimap<String, String> resolveCuries(Multimap<String, String> paramMap) {
    Multimap<String, String> map = ArrayListMultimap.create();
    for (Entry<String, String> entry: paramMap.entries()) {
      Optional<String> iri = curieUtil.getIri(entry.getValue());
      if (iri.isPresent()) {
        map.put(entry.getKey(), GraphUtil.getFragment(iri.get()));
      } else {
        map.put(entry.getKey(), entry.getValue());
      }
    }
    return map;
  }

  static TinkerGraph resultToGraph(ExecutionResult result) {
    TinkerGraph graph = new TinkerGraph();
    for (Map<String, Object> map: result) {
      for (Object value: map.values()) {
        if (null == value) {
          continue;
        } else if (value instanceof PropertyContainer) {
          TinkerGraphUtil.addElement(graph, (PropertyContainer)value);
        } else if (value instanceof SeqWrapper) {
          for (Object thing: (SeqWrapper<?>)value) {
            if (thing instanceof PropertyContainer) {
              TinkerGraphUtil.addElement(graph, (PropertyContainer) thing);
            }
          }
        } else {
          logger.warning("Not converting " + value.getClass() + " to tinker graph");
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
        tx.success();
      }
    }
    return entailedTypes;
  }

  String entailRelationships(String cypher) {
    Matcher m = pattern.matcher(cypher);
    StringBuffer buffer = new StringBuffer();
    while (m.find()) {
      String group = m.group().substring(1, m.group().length() - 1);
      Set<String> parentTypes = newHashSet(Splitter.on('|').split(group));
      String entailedTypes = on('|').join(getEntailedRelationshipTypes(parentTypes));
      m.appendReplacement(buffer, ":" + entailedTypes);
    }
    m.appendTail(buffer);
    return buffer.toString();
  }

  String substituteRelationships(String query, final Multimap<String, String> valueMap) {
    StrSubstitutor substitutor = new StrSubstitutor(new StrLookup<String>() {
      @Override
      public String lookup(String key) {
        return on('|').join(valueMap.get(key));
      }
    });
    return substitutor.replace(query);
  }

}
