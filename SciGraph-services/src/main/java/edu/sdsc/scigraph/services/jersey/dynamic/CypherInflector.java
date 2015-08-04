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

import static com.google.common.collect.Iterables.getFirst;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.process.Inflector;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.assistedinject.Assisted;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import edu.sdsc.scigraph.internal.CypherUtil;
import edu.sdsc.scigraph.internal.GraphAspect;
import edu.sdsc.scigraph.internal.TinkerGraphUtil;
import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.owlapi.curies.AddCuries;
import edu.sdsc.scigraph.owlapi.curies.CurieUtil;
import edu.sdsc.scigraph.services.api.graph.ArrayPropertyTransformer;
import edu.sdsc.scigraph.services.jersey.MultivaluedMapUtils;
import edu.sdsc.scigraph.services.swagger.beans.resource.Apis;

class CypherInflector implements Inflector<ContainerRequestContext, Response> {

  private static final Logger logger = Logger.getLogger(CypherInflector.class.getName());

  private final GraphDatabaseService graphDb;
  private final CypherUtil cypherUtil;
  private final Apis config;
  private final CurieUtil curieUtil;
  private final Map<String, GraphAspect> aspectMap;

  @Inject
  CypherInflector(GraphDatabaseService graphDb, CypherUtil cypherUtil, CurieUtil curieUtil,
      @Assisted Apis config, Map<String, GraphAspect> aspectMap) {
    this.graphDb = graphDb;
    this.cypherUtil = cypherUtil;
    this.config = config;
    this.curieUtil = curieUtil;
    this.aspectMap = aspectMap;
  }

  @AddCuries
  @Override
  public Response apply(ContainerRequestContext context) {
    logger.fine("Serving dynamic request");
    Multimap<String, Object> paramMap = MultivaluedMapUtils.merge(context.getUriInfo());
    paramMap = resolveCuries(paramMap);
    try (Transaction tx = graphDb.beginTx()) {
      long start = System.currentTimeMillis();
      start = System.currentTimeMillis();
      Result result = cypherUtil.execute(config.getQuery(), paramMap);
      logger.fine((System.currentTimeMillis() - start) + " to execute query" );
      start = System.currentTimeMillis();
      TinkerGraph graph = TinkerGraphUtil.resultToGraph(result);
      logger.fine((System.currentTimeMillis() - start) + " to convert to graph" );
      start = System.currentTimeMillis();
      for (String key: aspectMap.keySet()) {
        if ("true".equals(getFirst(paramMap.get(key), "false"))) {
          aspectMap.get(key).invoke(graph);
        }
      }
      if (paramMap.containsKey("project")) {
        @SuppressWarnings("unchecked")
        Collection<String> projection = (Collection<String>)(Collection<?>)paramMap.get("project");
        TinkerGraphUtil.project(graph, projection);
      }
      ArrayPropertyTransformer.transform(graph);
      tx.success();
      return Response.ok(graph).cacheControl(config.getCacheControl()).build();
    }
  }

  Multimap<String, Object> resolveCuries(Multimap<String, Object> paramMap) {
    Multimap<String, Object> map = ArrayListMultimap.create();
    for (Entry<String, Object> entry: paramMap.entries()) {
      if (entry.getValue() instanceof String) {
        Optional<String> iri = curieUtil.getIri((String)entry.getValue());
        if (iri.isPresent()) {
          map.put(entry.getKey(), GraphUtil.getFragment(iri.get()));
        } else {
          map.put(entry.getKey(), entry.getValue());
        }
      } else {
        map.put(entry.getKey(), entry.getValue());
      }
    }
    return map;
  }

}
