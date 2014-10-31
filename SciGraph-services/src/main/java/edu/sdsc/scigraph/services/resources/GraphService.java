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
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import io.dropwizard.jersey.caching.CacheControl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.tooling.GlobalGraphOperations;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.representations.monarch.GraphPath;
import edu.sdsc.scigraph.representations.monarch.GraphPath.Edge;
import edu.sdsc.scigraph.representations.monarch.GraphPath.Vertex;
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

  @Inject
  GraphService(Vocabulary vocabulary, Graph graph) {
    this.vocabulary = vocabulary;
    this.graph = graph;
  }

  GraphPath getGraphPathFromPath(org.neo4j.graphdb.Path path) {
    GraphPath graphPath = new GraphPath();
    graphPath.nodes = newArrayList(transform(path.nodes(), new Function<Node, Vertex>() {
      @Override
      public Vertex apply(Node input) {
        Concept c = graph.getOrCreateFramedNode(input);
        // HACK: Chooses first label as a convention
        Vertex v = new Vertex(c.getFragment(), getFirst(c.getLabels(), null));
        if (Iterables.count(c.getCategories()) > 0) {
          v.meta.put("categories", newArrayList(c.getCategories()));
        }
        v.meta.put("type", newArrayList(c.getTypes()));
        return v;
      }
    }));

    graphPath.edges =
        newArrayList(transform(path.relationships(), new Function<Relationship, Edge>() {
          @Override
          public Edge apply(Relationship input) {
            Edge e =
                new Edge((String) input.getStartNode().getProperty(CommonProperties.FRAGMENT),
                    (String) input.getEndNode().getProperty(CommonProperties.FRAGMENT), input
                    .getType().name());
            /*Optional<String> type = graph.getProperty(input, CommonProperties.TYPE, String.class);
            if (type.isPresent()) {
              e.meta.put("type", type.get());
            }*/
            return e;
          }
        }));
    return graphPath;
  }

  @GET
  @Path("/paths/short/{startId}/{endId}")
  @ApiOperation(value = "Get shortest path.", response = String.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getShortestPath(
      @ApiParam(value = "Start node ID. " + DocumentationStrings.GRAPH_ID_DOC, required = true)
      @PathParam("startId") String startId,
      @ApiParam(value = "End node ID. " + DocumentationStrings.GRAPH_ID_DOC, required = true)
      @PathParam("endId") String endId,
      @ApiParam(value = "Maximum path length", required = false)
      @QueryParam("length") @DefaultValue("1") int length,
      @ApiParam( value = DocumentationStrings.JSONP_DOC, required = false )
      @QueryParam("callback") @DefaultValue("fn") String callback) {
    Vocabulary.Query query = new Vocabulary.Query.Builder(startId).build();
    Concept startConcept = getOnlyElement(vocabulary.getConceptFromId(query));
    Node startNode = graph.getGraphDb().getNodeById(startConcept.getId());

    query = new Vocabulary.Query.Builder(endId).build();
    Concept endConcept = getOnlyElement(vocabulary.getConceptFromId(query));
    Node endNode = graph.getGraphDb().getNodeById(endConcept.getId());

    GraphPath graphPath = null;
    try (Transaction tx = graph.getGraphDb().beginTx()) {
      PathExpander<Void> expander = new PathExpander<Void>() {

        @Override
        public Iterable<Relationship> expand(org.neo4j.graphdb.Path path, BranchState<Void> state) {
          return path.endNode().getRelationships();
        }

        @Override
        public PathExpander<Void> reverse() {
          return this;
        }
      };

      PathFinder<org.neo4j.graphdb.Path> finder = GraphAlgoFactory.shortestPath(expander, length);
      org.neo4j.graphdb.Path path = finder.findSinglePath(startNode, endNode);
      if (null == path) {
        throw new WebApplicationException(404);
      }

      graphPath = getGraphPathFromPath(path);
      tx.success();
    }

    GenericEntity<GraphPath> response = new GenericEntity<GraphPath>(graphPath) {};
    return JaxRsUtil.wrapJsonp(request, response, callback);
  }

  @GET
  @Path("/paths/simple/{startId}/{endId}")
  @ApiOperation(value = "Get all simple paths.", response = String.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getPath(
      @ApiParam(value = "Start node ID. " + DocumentationStrings.GRAPH_ID_DOC, required = true)
      @PathParam("startId") String startId,
      @ApiParam(value = "End node ID. " + DocumentationStrings.GRAPH_ID_DOC, required = true)
      @PathParam("endId") String endId,
      @ApiParam(value = "Maximum path length", required = false)
      @QueryParam("length") @DefaultValue("1") int length,
      @ApiParam(value = DocumentationStrings.JSONP_DOC, required = false) 
      @QueryParam("callback") @DefaultValue("fn") String callback) {
    Vocabulary.Query query = new Vocabulary.Query.Builder(startId).build();
    Concept startConcept = getOnlyElement(vocabulary.getConceptFromId(query));
    Node startNode = graph.getGraphDb().getNodeById(startConcept.getId());

    query = new Vocabulary.Query.Builder(endId).build();
    Concept endConcept = getOnlyElement(vocabulary.getConceptFromId(query));
    Node endNode = graph.getGraphDb().getNodeById(endConcept.getId());

    List<GraphPath> returnedPaths = new ArrayList<>();
    try (Transaction tx = graph.getGraphDb().beginTx()) {
      PathExpander<Void> expander = new PathExpander<Void>() {

        @Override
        public Iterable<Relationship> expand(org.neo4j.graphdb.Path path, BranchState<Void> state) {
          Set<Node> seenNodes = new HashSet<>();
          for (Node node : path.nodes()) {
            if (seenNodes.contains(node)) {
              return Iterables.empty();
            } else {
              seenNodes.add(node);
            }
          }
          return path.endNode().getRelationships();
        }

        @Override
        public PathExpander<Void> reverse() {
          return this;
        }
      };

      PathFinder<org.neo4j.graphdb.Path> finder = GraphAlgoFactory.allSimplePaths(expander, length);
      Iterable<org.neo4j.graphdb.Path> paths = finder.findAllPaths(startNode, endNode);
      if ((null == paths) || (0 == Iterables.count(paths))) {
        throw new WebApplicationException(404);
      }
      for (org.neo4j.graphdb.Path path : paths) {
        returnedPaths.add(getGraphPathFromPath(path));
      }
    }
    GenericEntity<List<GraphPath>> response = new GenericEntity<List<GraphPath>>(returnedPaths) {};
    return JaxRsUtil.wrapJsonp(request, response, callback);
  }

  @GET
  @Path("/descendants/{relationship}/{id}")
  @ApiOperation(value = "Get descendants", response = ConceptDTO.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getSubclasses(
      @ApiParam(value = "Type of relationship to use", required = true) @PathParam("relationship") String relationship,
      @ApiParam(value = DocumentationStrings.GRAPH_ID_DOC, required = true) @PathParam("id") String id,
      @ApiParam(value = "How deep to traverse descendants", required = false) @QueryParam("depth") @DefaultValue("1") int depth,
      @ApiParam( value = DocumentationStrings.JSONP_DOC, required = false )
      @QueryParam("callback") @DefaultValue("fn") String callback) {
    Vocabulary.Query query = new Vocabulary.Query.Builder(id).build();
    Concept concept = getOnlyElement(vocabulary.getConceptFromId(query));
    Node node = graph.getGraphDb().getNodeById(concept.getId());
    LinkedList<ConceptDTO> dtos = new LinkedList<>();

    RelationshipType type = DynamicRelationshipType.withName(relationship);

    try (Transaction tx = graph.getGraphDb().beginTx()) {
      // TODO: include equivalences
      for (org.neo4j.graphdb.Path path : graph.getGraphDb().traversalDescription().depthFirst()
          .relationships(type, Direction.OUTGOING).evaluator(Evaluators.toDepth(depth))
          .traverse(node)) {
        ConceptDTO dto = new ConceptDTO();
        dto.setUri((String) path.endNode().getProperty("uri"));
        int numPathNodes = path.length() + 1;
        if (numPathNodes > dtos.size()) {
          if (!dtos.isEmpty()) {
            dtos.peek().getDescendants().add(dto);
          }
          dtos.push(dto);
        } else {
          while (dtos.size() > numPathNodes) {
            dtos.pop();
          }
          dtos.pop();
          dtos.peek().getDescendants().add(dto);
          dtos.push(dto);
        }
      }
      tx.success();
    }
    GenericEntity<ConceptDTO> response = new GenericEntity<ConceptDTO>(dtos.getLast()) {};
    return JaxRsUtil.wrapJsonp(request, response, callback);
  }

  @GET
  @Path("/neighbors/{id}")
  @ApiOperation(value = "Get neighbors", response = ConceptDTO.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getNeighbors(
      @ApiParam(value = DocumentationStrings.GRAPH_ID_DOC, required = true)
      @PathParam("id") String id,
      @ApiParam(value = "How far to traverse neighbors", required = false)
      @QueryParam("depth") @DefaultValue("1") final int depth,
      @ApiParam(value = "Traverse blank nodes", required = false)
      @QueryParam("blankNodes") @DefaultValue("false") final boolean traverseBlankNodes,
      @ApiParam(value = DocumentationStrings.JSONP_DOC, required = false )
      @QueryParam("callback") @DefaultValue("fn") String callback) {
    Vocabulary.Query query = new Vocabulary.Query.Builder(id).build();
    Collection<Concept> concepts = vocabulary.getConceptFromId(query);
    if (concepts.isEmpty()) {
      throw new UnknownClassException(id);
    }
    Concept concept = getFirst(vocabulary.getConceptFromId(query), null);
    GenericEntity<GraphPathWrapper> response = null;
    try (Transaction tx = graph.getGraphDb().beginTx()) {
      Node node = graph.getGraphDb().getNodeById(concept.getId());
      List<GraphPath> graphPaths = new ArrayList<>();

      for (org.neo4j.graphdb.Path path : graph.getGraphDb().traversalDescription().depthFirst()
          .evaluator(new Evaluator() {

            @Override
            public Evaluation evaluate(org.neo4j.graphdb.Path path) {
              Optional<String> uri = GraphUtil.getProperty(path.endNode(), NodeProperties.URI, String.class);
              // TODO: This should work with the anonymous property - but some blank nodes seem to not be getting it set
              if (!traverseBlankNodes && uri.or("").startsWith("http://ontology.neuinfo.org/anon/")) {
                return Evaluation.EXCLUDE_AND_PRUNE;
              }
              if (path.length() > depth) {
                return Evaluation.EXCLUDE_AND_PRUNE;
              }
              return Evaluation.INCLUDE_AND_CONTINUE;
            }
          }).traverse(node)) {
        graphPaths.add(getGraphPathFromPath(path));
      }

      response = new GenericEntity<GraphPathWrapper>(new GraphPathWrapper(graphPaths)) {};
      tx.success();
    }
    return JaxRsUtil.wrapJsonp(request, response, callback);
  }

  @GET
  @Path("/relationship_types")
  @ApiOperation(value = "Get all relationship types", response = String.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getRelationships(
      @ApiParam(value = DocumentationStrings.JSONP_DOC, required = false)
      @QueryParam("callback") @DefaultValue("fn") String callback) {
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
      @QueryParam("callback") @DefaultValue("fn") String callback) {
    Vocabulary.Query query = new Vocabulary.Query.Builder(id).build();
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

  @XmlRootElement
  static class GraphPathWrapper {

    @XmlElement
    @JsonProperty
    List<GraphPath> paths;

    public GraphPathWrapper() {}

    public GraphPathWrapper(List<GraphPath> paths) {
      this.paths = paths;
    }

  }

}
