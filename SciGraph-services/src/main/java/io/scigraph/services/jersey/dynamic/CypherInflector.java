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
package io.scigraph.services.jersey.dynamic;

import static com.google.common.collect.Iterables.getFirst;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.tinkerpop.blueprints.Graph;
import io.scigraph.internal.CypherUtil;
import io.scigraph.internal.GraphAspect;
import io.scigraph.internal.TinkerGraphUtil;
import io.scigraph.owlapi.curies.AddCuries;
import io.scigraph.services.api.graph.ArrayPropertyTransformer;
import io.scigraph.services.jersey.MultivaluedMapUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

import io.swagger.models.Path;
import org.glassfish.jersey.process.Inflector;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.prefixcommons.CurieUtil;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.assistedinject.Assisted;
import org.prefixcommons.CurieUtil;

import io.scigraph.internal.CypherUtil;
import io.scigraph.internal.GraphAspect;
import io.scigraph.internal.TinkerGraphUtil;
import io.scigraph.owlapi.curies.AddCuries;
import io.scigraph.services.api.graph.ArrayPropertyTransformer;
import io.scigraph.services.jersey.MultivaluedMapUtils;

class CypherInflector implements Inflector<ContainerRequestContext, Response> {

  private static final Logger logger = Logger.getLogger(CypherInflector.class.getName());

  private final GraphDatabaseService graphDb;
  private final CypherUtil cypherUtil;
  private final String pathName;
  private final Path path;
  private final CurieUtil curieUtil;
  private final Map<String, GraphAspect> aspectMap;

  @Inject
  CypherInflector(GraphDatabaseService graphDb, CypherUtil cypherUtil, CurieUtil curieUtil,
      @Assisted String pathName, @Assisted Path path, Map<String, GraphAspect> aspectMap) {
    this.graphDb = graphDb;
    this.cypherUtil = cypherUtil;
    this.pathName = pathName;
    this.path = path;
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
      Result result = cypherUtil.execute((String)path.getVendorExtensions().get("x-query"), paramMap);
      logger.fine((System.currentTimeMillis() - start) + " to execute query" );
      start = System.currentTimeMillis();
      TinkerGraphUtil tgu = new TinkerGraphUtil(curieUtil);
      Graph graph = tgu.resultToGraph(result);
      tgu.setGraph(graph);
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
        tgu.project(projection);
      }
      ArrayPropertyTransformer.transform(graph);
      tx.success();
      Object cacheControlNode = path.getVendorExtensions().get("x-cacheControl");
      if (cacheControlNode != null) {
          try {
            CacheControl cacheControl = new ObjectMapper().readValue(cacheControlNode.toString(), CacheControl.class);
            return Response.ok(graph).cacheControl(cacheControl).build();
          }
          catch (Throwable e) {
            return Response.ok(graph).cacheControl(null).build();
          }
      }
      return Response.ok(graph).cacheControl(null).build();
    }
  }

  Multimap<String, Object> resolveCuries(Multimap<String, Object> paramMap) {
    Multimap<String, Object> map = ArrayListMultimap.create();
    for (Entry<String, Object> entry: paramMap.entries()) {
      if (entry.getValue() instanceof String) {
        Optional<String> iri = curieUtil.getIri((String)entry.getValue());
        if (iri.isPresent()) {
          map.put(entry.getKey(), iri.get());
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
