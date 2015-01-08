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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
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
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.internal.GraphApi;
import edu.sdsc.scigraph.neo4j.DirectedRelationshipType;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.services.api.graph.ConceptDTO;
import edu.sdsc.scigraph.services.api.graph.NodeDTO;
import edu.sdsc.scigraph.services.jersey.BaseResource;
import edu.sdsc.scigraph.services.jersey.CustomMediaTypes;
import edu.sdsc.scigraph.services.jersey.JaxRsUtil;
import edu.sdsc.scigraph.services.jersey.UnknownClassException;
import edu.sdsc.scigraph.vocabulary.Vocabulary;

@Path("/graph")
@Api(value = "/graph", description = "Graph services")
@Produces({MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP,
  MediaType.APPLICATION_XML})
public class GraphService extends BaseResource {

  private final Vocabulary vocabulary;
  private final Graph graph;
  private final GraphApi api;

  @Inject
  GraphService(Vocabulary vocabulary, Graph graph, GraphApi api) {
    this.vocabulary = vocabulary;
    this.graph = graph;
    this.api = api;
  }

  @GET
  @Path("/neighbors/{id}")
  @ApiOperation(value = "Get neighbors", response = ConceptDTO.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  @Produces({MediaType.APPLICATION_JSON, CustomMediaTypes.APPLICATION_JSONP,
    MediaType.APPLICATION_XML, CustomMediaTypes.APPLICATION_GRAPHML, CustomMediaTypes.APPLICATION_GRAPHSON, CustomMediaTypes.TEXT_GML, 
    CustomMediaTypes.IMAGE_JPEG, CustomMediaTypes.IMAGE_PNG})
  public Object getNeighbors2(
      @ApiParam(value = DocumentationStrings.GRAPH_ID_DOC, required = true)
      @PathParam("id") String id,
      @ApiParam(value = "How far to traverse neighbors", required = false)
      @QueryParam("depth") @DefaultValue("1") final IntParam depth,
      @ApiParam(value = "Traverse blank nodes", required = false)
      @QueryParam("blankNodes") @DefaultValue("false") final BooleanParam traverseBlankNodes,
      @ApiParam(value = "Which relationship to traverse", required = false)
      @QueryParam("relationshipType") final String relationshipType,
      @ApiParam(value = "Which direction to traverse: in, out, both (default). Only used if relationshipType is specified.", required = false)
      @QueryParam("direction") @DefaultValue("both") final String direction,
      @ApiParam(value = DocumentationStrings.JSONP_DOC, required = false )
      @QueryParam("callback") String callback) {
    Vocabulary.Query query = new Vocabulary.Query.Builder(id).build();
    Collection<Concept> concepts = vocabulary.getConceptFromId(query);
    if (concepts.isEmpty()) {
      throw new UnknownClassException(id);
    }
    Concept concept = getFirst(vocabulary.getConceptFromId(query), null);
    Set<DirectedRelationshipType> types = new HashSet<>();
    if (!isNullOrEmpty(relationshipType)) {
      RelationshipType type = DynamicRelationshipType.withName(relationshipType);
      Direction dir = Direction.BOTH;
      switch (direction) {
        case "in" : dir = Direction.INCOMING;
        break;
        case "out" : dir = Direction.OUTGOING;
        break;
        case "both" : dir = Direction.BOTH;
        break;
      }
      types.add(new DirectedRelationshipType(type, dir));
    }
    TinkerGraph tg = new TinkerGraph();
    try (Transaction tx = graph.getGraphDb().beginTx()) {
      Node node = graph.getGraphDb().getNodeById(concept.getId());
      Optional<Predicate<Node>> nodePredicate = Optional.absent();
      if (!traverseBlankNodes.get()) {
        Predicate<Node> predicate = new Predicate<Node>() {
          @Override
          public boolean apply(Node node) {
            //TODO: This should be done with properties...
            return !((String)node.getProperty(CommonProperties.URI)).startsWith("http://ontology.neuinfo.org/anon/");
          }};
        nodePredicate = Optional.of(predicate);
      }
      tg = api.getNeighbors(newHashSet(node), depth.get(), types, nodePredicate);
      tx.success();
    }
    GenericEntity<TinkerGraph> response = new GenericEntity<TinkerGraph>(tg) {};
    return JaxRsUtil.wrapJsonp(request, response, callback);
  }

  @GET
  @Path("/relationship_types")
  @ApiOperation(value = "Get all relationship types", response = String.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getRelationships(
      @ApiParam(value = DocumentationStrings.JSONP_DOC, required = false)
      @QueryParam("callback") String callback) {
    List<String> relationships = new ArrayList<>();
    try (Transaction tx = graph.getGraphDb().beginTx()) {
      relationships = newArrayList(transform(GlobalGraphOperations.at(graph.getGraphDb()).getAllRelationshipTypes(),
          new Function<RelationshipType, String>() {
        @Override
        public String apply(RelationshipType relationshipType) {
          return relationshipType.name();
        }

      }));
    }
    return JaxRsUtil.wrapJsonp(request, new GenericEntity<List<String>>(relationships) {}, callback);
  }

  @GET
  @Path("/{id}")
  @ApiOperation(value = "Get all properties of a node", response = ConceptDTO.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getNode(
      @ApiParam(value = DocumentationStrings.GRAPH_ID_DOC, required = true)
      @PathParam("id") String id,
      @ApiParam(value = DocumentationStrings.JSONP_DOC, required = false)
      @QueryParam("callback") String callback) {
    Vocabulary.Query query = new Vocabulary.Query.Builder(id).build();
    Collection<Concept> concepts = vocabulary.getConceptFromId(query);
    if (concepts.isEmpty()) {
      throw new UnknownClassException(id);
    }
    Concept concept = getOnlyElement(vocabulary.getConceptFromId(query));
    NodeDTO dto = new NodeDTO();
    try (Transaction tx = graph.getGraphDb().beginTx()) {
      dto.setUri(concept.getUri());
      Node node = graph.getGraphDb().getNodeById(concept.getId());
      for (String key: node.getPropertyKeys()) {
        if (CommonProperties.URI.equals(key)) {
          continue;
        }
        dto.getProperties().put(key, node.getProperty(key));
      }
      for (Label label: node.getLabels()) {
        dto.getTypes().add(label.name());
      }
      tx.success();
    }
    return JaxRsUtil.wrapJsonp(request, new GenericEntity<NodeDTO>(dto) {}, callback);
  }

}
