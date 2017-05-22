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

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.sort;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.prefixcommons.CurieUtil;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import io.dropwizard.jersey.caching.CacheControl;
import io.dropwizard.jersey.params.BooleanParam;
import io.dropwizard.jersey.params.IntParam;
import io.dropwizard.jersey.params.LongParam;
import io.scigraph.frames.Concept;
import io.scigraph.internal.CypherUtil;
import io.scigraph.internal.GraphApi;
import io.scigraph.internal.TinkerGraphUtil;
import io.scigraph.neo4j.DirectedRelationshipType;
import io.scigraph.owlapi.OwlLabels;
import io.scigraph.services.api.graph.ArrayPropertyTransformer;
import io.scigraph.services.jersey.BadRequestException;
import io.scigraph.services.jersey.BaseResource;
import io.scigraph.services.jersey.CustomMediaTypes;
import io.scigraph.services.jersey.JaxRsUtil;
import io.scigraph.services.jersey.UnknownClassException;
import io.scigraph.vocabulary.Vocabulary;

@Path("/graph")
@Api(value = "/graph", description = "Graph services")
@SwaggerDefinition(tags = {@Tag(name="graph", description="Graph services")})
public class GraphService extends BaseResource {

  private final Vocabulary vocabulary;
  private final GraphDatabaseService graphDb;
  private final GraphApi api;
  private final CurieUtil curieUtil;
  private final CypherUtil cypherUtil;

  @Inject
  GraphService(Vocabulary vocabulary, GraphDatabaseService graphDb, GraphApi api,
      CurieUtil curieUtil, CypherUtil cypherUtil) {
    this.vocabulary = vocabulary;
    this.graphDb = graphDb;
    this.api = api;
    this.curieUtil = curieUtil;
    this.cypherUtil = cypherUtil;
  }

  @GET
  @Path("/neighbors")
  @ApiOperation(value = "Get neighbors", response = Graph.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_GRAPHSON,
      MediaType.APPLICATION_XML, CustomMediaTypes.APPLICATION_GRAPHML,
      CustomMediaTypes.APPLICATION_XGMML, CustomMediaTypes.TEXT_GML, CustomMediaTypes.TEXT_CSV,
      CustomMediaTypes.TEXT_TSV, CustomMediaTypes.IMAGE_JPEG, CustomMediaTypes.IMAGE_PNG})
  public Object getNeighborsFromMultipleRoots(
      @ApiParam(value = DocumentationStrings.GRAPH_ID_DOC,
          required = true) @QueryParam("id") Set<String> ids,
      @ApiParam(value = "How far to traverse neighbors",
          required = false) @QueryParam("depth") @DefaultValue("1") IntParam depth,
      @ApiParam(value = "Traverse blank nodes",
          required = false) @QueryParam("blankNodes") @DefaultValue("false") BooleanParam traverseBlankNodes,
      @ApiParam(value = "Which relationship to traverse",
          required = false) @QueryParam("relationshipType") Optional<String> relationshipType,
      @ApiParam(value = DocumentationStrings.DIRECTION_DOC, required = false,
          allowableValues = DocumentationStrings.DIRECTION_ALLOWED) @QueryParam("direction") @DefaultValue("BOTH") String direction,
      @ApiParam(value = "Should subproperties and equivalent properties be included",
          required = false) @QueryParam("entail") @DefaultValue("false") BooleanParam entail,
      @ApiParam(value = DocumentationStrings.PROJECTION_DOC,
          required = false) @QueryParam("project") @DefaultValue("*") Set<String> projection,
      @ApiParam(value = DocumentationStrings.JSONP_DOC,
          required = false) @QueryParam("callback") String callback) {
    Set<Concept> roots = new HashSet<>();
    for (String id : ids) {
      Vocabulary.Query query = new Vocabulary.Query.Builder(id).build();
      Optional<Concept> concept = vocabulary.getConceptFromId(query);
      if (!concept.isPresent()) {
        throw new UnknownClassException(id);
      }
      roots.add(concept.get());
    }
    Set<DirectedRelationshipType> types = new HashSet<>();
    if (relationshipType.isPresent()) {
      String relationshipTypeString = relationshipType.get();
      relationshipTypeString = curieUtil.getIri(relationshipTypeString).orElse(relationshipTypeString);
      if (!getRelationshipTypeNames().contains(relationshipTypeString)) {
        throw new BadRequestException("Unknown relationship type: " + relationshipTypeString);
      }

      Direction dir = Direction.valueOf(direction);
      try {
        if (entail.get()) {
          Set<RelationshipType> relationshipTypes =
              cypherUtil.getEntailedRelationshipTypes(newHashSet(relationshipTypeString));
          types = newHashSet(transform(relationshipTypes,
              new Function<RelationshipType, DirectedRelationshipType>() {
                @Override
                public DirectedRelationshipType apply(RelationshipType type) {
                  return new DirectedRelationshipType(type, dir);
                }
              }));
        } else {
          RelationshipType type = RelationshipType.withName(relationshipTypeString);
          types.add(new DirectedRelationshipType(type, dir));
        }
      } catch (Exception e) {
        throw new BadRequestException("Unknown direction: " + direction);
      }
    }
    Graph tg = new TinkerGraph();
    try (Transaction tx = graphDb.beginTx()) {
      Iterable<Node> nodes = transform(roots, new Function<Concept, Node>() {
        @Override
        public Node apply(Concept concept) {
          return graphDb.getNodeById(concept.getId());
        }
      });
      Optional<Predicate<Node>> nodePredicate = Optional.empty();
      if (!traverseBlankNodes.get()) {
        Predicate<Node> predicate = new Predicate<Node>() {
          @Override
          public boolean apply(Node node) {
            return !(Iterables.contains(node.getLabels(), OwlLabels.OWL_ANONYMOUS));
          }
        };
        nodePredicate = Optional.of(predicate);
      }
      tg = api.getNeighbors(newHashSet(nodes), depth.get(), types, nodePredicate);
      tx.success();
    }
    TinkerGraphUtil tgu = new TinkerGraphUtil(tg, curieUtil);
    tgu.project(projection);
    ArrayPropertyTransformer.transform(tg);
    GenericEntity<Graph> response = new GenericEntity<Graph>(tg) {};
    return JaxRsUtil.wrapJsonp(request.get(), response, callback);
  }

  @GET
  @Path("/neighbors/{id}")
  @ApiOperation(value = "Get neighbors", response = Graph.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_GRAPHSON,
      MediaType.APPLICATION_XML, CustomMediaTypes.APPLICATION_GRAPHML,
      CustomMediaTypes.APPLICATION_XGMML, CustomMediaTypes.TEXT_GML, CustomMediaTypes.TEXT_CSV,
      CustomMediaTypes.TEXT_TSV, CustomMediaTypes.IMAGE_JPEG, CustomMediaTypes.IMAGE_PNG})
  public Object getNeighbors(
      @ApiParam(value = DocumentationStrings.GRAPH_ID_DOC,
          required = true) @PathParam("id") String id,
      @ApiParam(value = "How far to traverse neighbors",
          required = false) @QueryParam("depth") @DefaultValue("1") IntParam depth,
      @ApiParam(value = "Traverse blank nodes",
          required = false) @QueryParam("blankNodes") @DefaultValue("false") BooleanParam traverseBlankNodes,
      @ApiParam(value = "Which relationship to traverse",
          required = false) @QueryParam("relationshipType") Optional<String> relationshipType,
      @ApiParam(value = DocumentationStrings.DIRECTION_DOC, required = false,
          allowableValues = DocumentationStrings.DIRECTION_ALLOWED) @QueryParam("direction") @DefaultValue("BOTH") String direction,
      @ApiParam(value = "Should subproperties and equivalent properties be included",
          required = false) @QueryParam("entail") @DefaultValue("false") BooleanParam entail,
      @ApiParam(value = DocumentationStrings.PROJECTION_DOC,
          required = false) @QueryParam("project") @DefaultValue("*") Set<String> projection,
      @ApiParam(value = DocumentationStrings.JSONP_DOC,
          required = false) @QueryParam("callback") String callback) {
    return getNeighborsFromMultipleRoots(newHashSet(id), depth, traverseBlankNodes,
        relationshipType, direction, entail, projection, callback);
  }

  @GET
  @Path("/{id}")
  @ApiOperation(value = "Get all properties of a node", response = Graph.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_GRAPHSON,
      MediaType.APPLICATION_XML, CustomMediaTypes.APPLICATION_GRAPHML,
      CustomMediaTypes.APPLICATION_XGMML, CustomMediaTypes.TEXT_GML, CustomMediaTypes.TEXT_CSV,
      CustomMediaTypes.TEXT_TSV, CustomMediaTypes.IMAGE_JPEG, CustomMediaTypes.IMAGE_PNG})
  public Object getNode(
      @ApiParam(value = DocumentationStrings.GRAPH_ID_DOC,
          required = true) @PathParam("id") String id,
      @ApiParam(value = DocumentationStrings.PROJECTION_DOC,
          required = false) @QueryParam("project") @DefaultValue("*") Set<String> projection,
      @ApiParam(value = DocumentationStrings.JSONP_DOC,
          required = false) @QueryParam("callback") String callback) {
    return getNeighbors(id, new IntParam("0"), new BooleanParam("false"), Optional.<String>empty(),
        null, new BooleanParam("false"), projection, callback);
  }

  @GET
  @Path("/edges/{type}")
  @ApiOperation(value = "Get nodes connected by an edge type", response = Graph.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_GRAPHSON,
      MediaType.APPLICATION_XML, CustomMediaTypes.APPLICATION_GRAPHML,
      CustomMediaTypes.APPLICATION_XGMML, CustomMediaTypes.TEXT_GML, CustomMediaTypes.TEXT_CSV,
      CustomMediaTypes.TEXT_TSV, CustomMediaTypes.IMAGE_JPEG, CustomMediaTypes.IMAGE_PNG})
  public Object getEdges(
      @ApiParam(value = "The type of the edge", required = true) @PathParam("type") String type,
      @ApiParam(value = "Should subproperties and equivalent properties be included",
          required = false) @QueryParam("entail") @DefaultValue("true") BooleanParam entail,
      @ApiParam(value = "The number of edges to be returned",
          required = false) @QueryParam("limit") @DefaultValue("100") LongParam limit,
      @ApiParam(value = "The number of edges to skip",
          required = false) @QueryParam("skip") @DefaultValue("0") LongParam skip,
      @ApiParam(value = DocumentationStrings.JSONP_DOC,
          required = false) @QueryParam("callback") String callback) {
    Graph edgeGraph = new TinkerGraph();
    try (Transaction tx = graphDb.beginTx()) {
      RelationshipType relationshipType = RelationshipType.withName(type);
      edgeGraph = api.getEdges(relationshipType, entail.get(), skip.get(), limit.get());
      tx.success();
    }
    GenericEntity<Graph> response = new GenericEntity<Graph>(edgeGraph) {};
    return JaxRsUtil.wrapJsonp(request.get(), response, callback);
  }

  // TODO: Move this to scigraph-core
  List<String> getRelationshipTypeNames() {
    return newArrayList(
        transform(api.getAllRelationshipTypes(), new Function<RelationshipType, String>() {
          @Override
          public String apply(RelationshipType type) {
            return type.name();
          }
        }));
  }

  @GET
  @Path("/relationship_types")
  @ApiOperation(value = "Get all relationship types", response = String.class,
      responseContainer = "List")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({MediaType.APPLICATION_JSON})
  public Object getRelationships(@ApiParam(value = DocumentationStrings.JSONP_DOC,
      required = false) @QueryParam("callback") String callback) {
    List<String> relationships = getRelationshipTypeNames();
    sort(relationships);
    return JaxRsUtil.wrapJsonp(request.get(), new GenericEntity<List<String>>(relationships) {},
        callback);
  }

  @GET
  @Path("/properties")
  @ApiOperation(value = "Get all property keys", response = String.class,
      responseContainer = "List")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({MediaType.APPLICATION_JSON})
  public Object getProperties(@ApiParam(value = DocumentationStrings.JSONP_DOC,
      required = false) @QueryParam("callback") String callback) {
    List<String> propertyKeys = new ArrayList<>(api.getAllPropertyKeys());
    sort(propertyKeys);
    return JaxRsUtil.wrapJsonp(request.get(), new GenericEntity<List<String>>(propertyKeys) {},
        callback);
  }

  @GET
  @Path("/reachablefrom/{id}")
  @ApiOperation(
      value = "Get all the nodes reachable from a starting point, traversing the provided edges.",
      response = Graph.class, responseContainer = "List")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_GRAPHSON,
      MediaType.APPLICATION_XML, CustomMediaTypes.APPLICATION_GRAPHML,
      CustomMediaTypes.APPLICATION_XGMML, CustomMediaTypes.TEXT_GML, CustomMediaTypes.TEXT_CSV,
      CustomMediaTypes.TEXT_TSV, CustomMediaTypes.IMAGE_JPEG, CustomMediaTypes.IMAGE_PNG})
  public Object reachableFrom(
      @ApiParam(value = "The type of the edge", required = true) @PathParam("id") String id,
      @ApiParam(value = "A label hint to find the start node.",
          required = false) @QueryParam("hint") Optional<String> hint,
      @ApiParam(
          value = "A list of relationships to traverse, in order. Supports cypher operations such as relA|relB or relA*.",
          required = false) @QueryParam("relationships") List<String> relationships,
      @ApiParam(value = "A list of node labels to filter.",
          required = false) @QueryParam("lbls") Set<String> lbls,
      @ApiParam(value = DocumentationStrings.JSONP_DOC,
          required = false) @QueryParam("callback") String callback) {
    Graph graph = new TinkerGraph();

    try (Transaction tx = graphDb.beginTx()) {
      Optional<Node> startNode = api.getNode(id, hint);
      if (!startNode.isPresent()) {
        return Response.status(Response.Status.NOT_FOUND).build();
      } else {
        graph = api.getReachableNodes(startNode.get(), relationships, lbls);
      }
      tx.success();
    }
    GenericEntity<Graph> response = new GenericEntity<Graph>(graph) {};
    return JaxRsUtil.wrapJsonp(request.get(), response, callback);
  }

}
