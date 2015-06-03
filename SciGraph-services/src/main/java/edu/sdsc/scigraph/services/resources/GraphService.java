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
package edu.sdsc.scigraph.services.resources;

import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.sort;
import io.dropwizard.jersey.caching.CacheControl;
import io.dropwizard.jersey.params.BooleanParam;
import io.dropwizard.jersey.params.IntParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.internal.GraphApi;
import edu.sdsc.scigraph.internal.TinkerGraphUtil;
import edu.sdsc.scigraph.neo4j.DirectedRelationshipType;
import edu.sdsc.scigraph.owlapi.OwlLabels;
import edu.sdsc.scigraph.services.api.graph.ArrayPropertyTransformer;
import edu.sdsc.scigraph.services.jersey.BaseResource;
import edu.sdsc.scigraph.services.jersey.CustomMediaTypes;
import edu.sdsc.scigraph.services.jersey.JaxRsUtil;
import edu.sdsc.scigraph.services.jersey.UnknownClassException;
import edu.sdsc.scigraph.vocabulary.Vocabulary;

@Path("/graph")
@Api(value = "/graph", description = "Graph services")
public class GraphService extends BaseResource {

  private final Vocabulary vocabulary;
  private final GraphDatabaseService graphDb;
  private final GraphApi api;

  @Inject
  GraphService(Vocabulary vocabulary, GraphDatabaseService graphDb, GraphApi api) {
    this.vocabulary = vocabulary;
    this.graphDb = graphDb;
    this.api = api;
  }

  @GET
  @Path("/neighbors")
  @ApiOperation(value = "Get neighbors", response = TinkerGraph.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({ MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP, CustomMediaTypes.APPLICATION_GRAPHSON,
    MediaType.APPLICATION_XML, CustomMediaTypes.APPLICATION_GRAPHML, CustomMediaTypes.APPLICATION_XGMML,
    CustomMediaTypes.TEXT_GML, CustomMediaTypes.TEXT_CSV, CustomMediaTypes.TEXT_TSV,
    CustomMediaTypes.IMAGE_JPEG, CustomMediaTypes.IMAGE_PNG})
  public Object getNeighborsFromMultipleRoots(
      @ApiParam(value = DocumentationStrings.GRAPH_ID_DOC, required = true)
      @QueryParam("id") Set<String> ids,
      @ApiParam(value = "How far to traverse neighbors", required = false)
      @QueryParam("depth") @DefaultValue("1") IntParam depth,
      @ApiParam(value = "Traverse blank nodes", required = false)
      @QueryParam("blankNodes") @DefaultValue("false") BooleanParam traverseBlankNodes,
      @ApiParam(value = "Which relationship to traverse", required = false)
      @QueryParam("relationshipType") Optional<String> relationshipType,
      @ApiParam(value = DocumentationStrings.DIRECTION_DOC, required = false, allowableValues = DocumentationStrings.DIRECTION_ALLOWED)
      @QueryParam("direction") @DefaultValue("BOTH") String direction,
      @ApiParam(value = DocumentationStrings.PROJECTION_DOC, required = false)
      @QueryParam("project") @DefaultValue("*") Set<String> projection,
      @ApiParam(value = DocumentationStrings.JSONP_DOC, required = false )
      @QueryParam("callback") String callback) {
    Set<Concept> roots = new HashSet<>();
    for (String id: ids) {
      Vocabulary.Query query = new Vocabulary.Query.Builder(id).build();
      Collection<Concept> concepts = vocabulary.getConceptFromId(query);
      if (concepts.isEmpty()) {
        throw new UnknownClassException(id);
      }
      Concept concept = getFirst(vocabulary.getConceptFromId(query), null);
      roots.add(concept);
    }
    Set<DirectedRelationshipType> types = new HashSet<>();
    if (relationshipType.isPresent()) {
      RelationshipType type = DynamicRelationshipType.withName(relationshipType.get());
      Direction dir = Direction.valueOf(direction);
      types.add(new DirectedRelationshipType(type, dir));
    }
    TinkerGraph tg = new TinkerGraph();
    try (Transaction tx = graphDb.beginTx()) {
      Iterable<Node> nodes = transform(roots, new Function<Concept, Node>() {
        @Override
        public Node apply(Concept concept) {
          return graphDb.getNodeById(concept.getId());
        }
      });
      Optional<Predicate<Node>> nodePredicate = Optional.absent();
      if (!traverseBlankNodes.get()) {
        Predicate<Node> predicate = new Predicate<Node>() {
          @Override
          public boolean apply(Node node) {
            return !(Iterables.contains(node.getLabels(), OwlLabels.OWL_ANONYMOUS));
          }};
          nodePredicate = Optional.of(predicate);
      }
      tg = api.getNeighbors(newHashSet(nodes), depth.get(), types, nodePredicate);
      tx.success();
    }
    TinkerGraphUtil.project(tg, projection);
    ArrayPropertyTransformer.transform(tg);
    GenericEntity<TinkerGraph> response = new GenericEntity<TinkerGraph>(tg) {};
    return JaxRsUtil.wrapJsonp(request.get(), response, callback);
  }

  @GET
  @Path("/neighbors/{id}")
  @ApiOperation(value = "Get neighbors", response = TinkerGraph.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({ MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP, CustomMediaTypes.APPLICATION_GRAPHSON,
    MediaType.APPLICATION_XML, CustomMediaTypes.APPLICATION_GRAPHML, CustomMediaTypes.APPLICATION_XGMML,
    CustomMediaTypes.TEXT_GML, CustomMediaTypes.TEXT_CSV, CustomMediaTypes.TEXT_TSV,
    CustomMediaTypes.IMAGE_JPEG, CustomMediaTypes.IMAGE_PNG})
  public Object getNeighbors(
      @ApiParam(value = DocumentationStrings.GRAPH_ID_DOC, required = true)
      @PathParam("id") String id,
      @ApiParam(value = "How far to traverse neighbors", required = false)
      @QueryParam("depth") @DefaultValue("1") IntParam depth,
      @ApiParam(value = "Traverse blank nodes", required = false)
      @QueryParam("blankNodes") @DefaultValue("false") BooleanParam traverseBlankNodes,
      @ApiParam(value = "Which relationship to traverse", required = false)
      @QueryParam("relationshipType") Optional<String> relationshipType,
      @ApiParam(value = DocumentationStrings.DIRECTION_DOC, required = false, allowableValues = DocumentationStrings.DIRECTION_ALLOWED)
      @QueryParam("direction") @DefaultValue("BOTH") String direction,
      @ApiParam(value = DocumentationStrings.PROJECTION_DOC, required = false)
      @QueryParam("project") @DefaultValue("*") Set<String> projection,
      @ApiParam(value = DocumentationStrings.JSONP_DOC, required = false )
      @QueryParam("callback") String callback) {
    return getNeighborsFromMultipleRoots(newHashSet(id), depth, traverseBlankNodes, relationshipType, direction, projection, callback);
  }

  @GET
  @Path("/{id}")
  @ApiOperation(value = "Get all properties of a node", response = TinkerGraph.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({ MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP, CustomMediaTypes.APPLICATION_GRAPHSON,
    MediaType.APPLICATION_XML, CustomMediaTypes.APPLICATION_GRAPHML, CustomMediaTypes.APPLICATION_XGMML,
    CustomMediaTypes.TEXT_GML, CustomMediaTypes.TEXT_CSV, CustomMediaTypes.TEXT_TSV,
    CustomMediaTypes.IMAGE_JPEG, CustomMediaTypes.IMAGE_PNG})
  public Object getNode(
      @ApiParam(value = DocumentationStrings.GRAPH_ID_DOC, required = true)
      @PathParam("id") String id,
      @ApiParam(value = DocumentationStrings.PROJECTION_DOC, required = false)
      @QueryParam("project") @DefaultValue("*") Set<String> projection,
      @ApiParam(value = DocumentationStrings.JSONP_DOC, required = false)
      @QueryParam("callback") String callback) {
    return getNeighbors(id, new IntParam("0"), new BooleanParam("false"), Optional.<String>absent(), null, projection, callback);
  }

  @GET
  @Path("/relationship_types")
  @ApiOperation(value = "Get all relationship types", response = String.class, responseContainer="List")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP, MediaType.APPLICATION_XML})
  public Object getRelationships(
      @ApiParam(value = DocumentationStrings.JSONP_DOC, required = false)
      @QueryParam("callback") String callback) {
    List<String> relationships = new ArrayList<>();
    try (Transaction tx = graphDb.beginTx()) {
      relationships = newArrayList(transform(GlobalGraphOperations.at(graphDb).getAllRelationshipTypes(),
          new Function<RelationshipType, String>() {
        @Override
        public String apply(RelationshipType relationshipType) {
          return relationshipType.name();
        }
      }));
      tx.success();
    }
    sort(relationships);
    return JaxRsUtil.wrapJsonp(request.get(), new GenericEntity<List<String>>(relationships) {}, callback);
  }

  @GET
  @Path("/properties")
  @ApiOperation(value = "Get all property keys", response = String.class, responseContainer="List")
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP, MediaType.APPLICATION_XML})
  public Object getProperties(
      @ApiParam(value = DocumentationStrings.JSONP_DOC, required = false)
      @QueryParam("callback") String callback) {
    List<String> relationships = new ArrayList<>();
    try (Transaction tx = graphDb.beginTx()) {
      for (String key: GlobalGraphOperations.at(graphDb).getAllPropertyKeys()) {
        relationships.add(key);
      }
      tx.success();
    }
    sort(relationships);
    return JaxRsUtil.wrapJsonp(request.get(), new GenericEntity<List<String>>(relationships) {}, callback);
  }

}
