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
package io.scigraph.services.resources;

import io.dropwizard.jersey.caching.CacheControl;
import io.dropwizard.jersey.params.IntParam;
import io.scigraph.internal.CypherUtil;
import io.scigraph.services.jersey.BaseResource;
import io.scigraph.services.jersey.JaxRsUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.Optional;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.neo4j.graphdb.*;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import org.prefixcommons.CurieUtil;
import io.scigraph.internal.TinkerGraphUtil;
import javax.ws.rs.core.Response;
import io.scigraph.services.api.graph.ArrayPropertyTransformer;
import com.tinkerpop.blueprints.Graph;
import io.scigraph.services.jersey.MultivaluedMapUtils;
import com.google.common.collect.Multimap;
import com.google.common.collect.ArrayListMultimap;

@Path("/cypher")
@Api(value = "/cypher", description = "Cypher utility services")
@SwaggerDefinition(tags = {@Tag(name="cypher", description="Cypher utility services")})
@Produces({MediaType.TEXT_PLAIN})
public class CypherUtilService extends BaseResource {

  final private CypherUtil cypherUtil;
  final private GraphDatabaseService graphDb;
  final private CurieUtil curieUtil;

  private static final Logger logger = Logger.getLogger(CypherUtilService.class.getName());

  @Inject
  CypherUtilService(CypherUtil cypherUtil, GraphDatabaseService graphDb, CurieUtil curieUtil) {
    this.cypherUtil = cypherUtil;
    this.graphDb = graphDb;
    this.curieUtil = curieUtil;
  }

  @GET
  @Timed
  @Produces({MediaType.TEXT_PLAIN})
  @Path("/resolve")
  @ApiOperation(value = "Cypher query resolver", response = String.class,
      notes = "Resolves curies and relationships.")
  public String resolve(
      @ApiParam(value = "The cypher query to resolve", required = true) @QueryParam("cypherQuery") String cypherQuery) {
    return cypherUtil.resolveRelationships(cypherUtil.resolveNodeIris(cypherQuery));
  }


  @GET
  @Path("/curies")
  @ApiOperation(value = "Get the curie map", response = String.class, responseContainer = "Map")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({MediaType.APPLICATION_JSON})
  public Object getCuries(
      @ApiParam(value = DocumentationStrings.JSONP_DOC, required = false) @QueryParam("callback") String callback) {
    return JaxRsUtil.wrapJsonp(request.get(),
        new GenericEntity<Map<String, String>>(cypherUtil.getCurieMap()) {}, callback);
  }

  @GET
  @Path("/execute")
  @ApiOperation(
      value = "Execute an arbitrary Cypher query.",
      response = Response.class,
      notes = "The graph is in read-only mode, this service will fail with queries which alter the graph, like CREATE, DELETE or REMOVE. Example: MATCH (n:Node{iri:'DOID:4'}) return n")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
  public Response execute(
      @ApiParam(value = "The cypher query to execute", required = true) @QueryParam("cypherQuery") String cypherQuery,
      @ApiParam(value = "Limit", required = true) @QueryParam("limit") @DefaultValue("10") IntParam limit,
      @Context UriInfo uriInfo)
      throws IOException {

    Multimap<String, Object> paramMap = MultivaluedMapUtils.merge(uriInfo);
    paramMap = resolveCuries(paramMap);
    logger.info(paramMap.toString());
    String sanitizedCypherQuery = cypherQuery.replaceAll(";", "") + " LIMIT " + limit;
    String replacedStartCurie = cypherUtil.resolveNodeIris(sanitizedCypherQuery);

    try {
      if (JaxRsUtil.getVariant(request.get()) != null
          && JaxRsUtil.getVariant(request.get()).getMediaType() == MediaType.APPLICATION_JSON_TYPE) {
        try (Transaction tx = graphDb.beginTx()) {
          Result result = cypherUtil.execute(replacedStartCurie, paramMap);

      TinkerGraphUtil tgu = new TinkerGraphUtil(curieUtil);
      Graph graph = tgu.resultToGraph(result);
      tgu.setGraph(graph);
      ArrayPropertyTransformer.transform(graph);
      tx.success();
      return Response.ok(graph).cacheControl(null).build();
        }
      } else {
          return Response.ok(cypherUtil.execute(replacedStartCurie, paramMap).resultAsString()).cacheControl(null).build();
      }
    } catch (TransactionTerminatedException e) {
        return Response.ok("The query execution exceeds dbms.transaction.timeout configuration. " +
                           "Consider using the neo4j shell instead of this service.").build();
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

  // TODO similar to ResultSerializer.java from golrLoader
  private void resultSerializer(JsonGenerator generator, String fieldName, Object value)
      throws IOException {
    if (value instanceof Node) {
      Node n = (Node) value;

      generator.writeFieldName(fieldName);

      nodeGeneration(generator, n);
    } else if (value instanceof Relationship) {
      Relationship r = (Relationship) value;

      generator.writeFieldName(fieldName);

      relationshipGeneration(generator, r);
    } else if (value instanceof org.neo4j.graphdb.Path) {
      org.neo4j.graphdb.Path p = (org.neo4j.graphdb.Path) value;

      Iterator<Node> nodes = p.nodes().iterator();
      Iterator<Relationship> relationships = p.relationships().iterator();

      generator.writeFieldName(fieldName);
      generator.writeStartObject();

      generator.writeArrayFieldStart("details");
      while (nodes.hasNext()) {
        Node n = nodes.next();
        nodeGeneration(generator, n);
        if (relationships.hasNext()) {
          relationshipGeneration(generator, relationships.next());
        }
      }
      generator.writeEndArray();

      generator.writeStringField("overview", p.toString());

      generator.writeEndObject();

    } else if (value instanceof String) {
      generator.writeStringField(fieldName, (String) value);
    } else if (value instanceof Boolean) {
      generator.writeBooleanField(fieldName, (Boolean) value);
    } else if (value instanceof Integer) {
      generator.writeNumberField(fieldName, (Integer) value);
    } else if (value instanceof Long) {
      generator.writeNumberField(fieldName, (Long) value);
    } else if (value instanceof Float) {
      generator.writeNumberField(fieldName, (Float) value);
    } else if (value instanceof Double) {
      generator.writeNumberField(fieldName, (Double) value);
    } else if (value instanceof Iterable) {
      generator.writeArrayFieldStart(fieldName);
      for (String v : (List<String>) value) {
        generator.writeString(v);
      }
      generator.writeEndArray();
    } else if (value.getClass().isArray()) {
      List<String> arr = Arrays.asList((String[]) value);
      generator.writeArrayFieldStart(fieldName);
      for (String v : arr) {
        generator.writeString(v);
      }
      generator.writeEndArray();
    } else {
      throw new IllegalArgumentException("Don't know how to serialize " + value.getClass());
    }
  }

  private void nodeGeneration(JsonGenerator generator, Node node) throws IOException {
    generator.writeStartObject();
    for (String k : node.getPropertyKeys()) {
      resultSerializer(generator, k, node.getProperty(k));
    }

    generator.writeArrayFieldStart("Neo4jLabel");
    for (Label l : node.getLabels()) {
      generator.writeString(l.name());
    }
    generator.writeEndArray();

    generator.writeEndObject();
  }

  private void relationshipGeneration(JsonGenerator generator, Relationship relationship)
      throws IOException {
    generator.writeStartObject();
    generator.writeStringField("type", relationship.getType().name());
    for (String k : relationship.getPropertyKeys()) {
      resultSerializer(generator, k, relationship.getProperty(k));
    }
    generator.writeEndObject();
  }
}
