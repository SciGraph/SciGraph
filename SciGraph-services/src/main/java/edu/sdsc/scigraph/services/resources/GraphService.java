/**
 * Copyright (C) 2014 Christopher Condit (condit@sdsc.edu)
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
import java.util.LinkedList;
import java.util.List;
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
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.kernel.Traversal;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.services.api.graph.ConceptDTO;
import edu.sdsc.scigraph.services.jersey.BaseResource;
import edu.sdsc.scigraph.services.jersey.CustomMediaTypes;
import edu.sdsc.scigraph.services.jersey.JaxRsUtil;
import edu.sdsc.scigraph.vocabulary.Vocabulary;

@Path("/graph") 
@Api(value = "/graph", description = "Graph services")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, CustomMediaTypes.APPLICATION_JSONP})
public class GraphService extends BaseResource {

  private final Vocabulary<Concept> vocabulary;
  private final Graph<Concept> graph;

  @Inject
  GraphService(Vocabulary<Concept> vocabulary, Graph<Concept> graph) {
    this.vocabulary = vocabulary;
    this.graph = graph;
  }

  @XmlRootElement
  static class Vertex {
    @XmlElement
    @JsonProperty
    String id;

    @XmlElement
    @JsonProperty("lbl")
    String label;

    Vertex() {}

    Vertex(String id, String label) {
      this.id = id;
      this.label = label;
    }
  }

  @XmlRootElement
  static class Edge {
    @XmlElement
    @JsonProperty("sub")
    String subject;

    @XmlElement
    @JsonProperty("obj")
    String object;

    @XmlElement
    @JsonProperty("pred")
    String predicate;

    Edge() {}

    Edge(String subject, String object, String predicate) {
      this.subject = subject;
      this.object = object;
      this.predicate = predicate;
    }

  }

  @XmlRootElement
  static class GraphPath {

    @XmlElement
    @JsonProperty
    List<Vertex> nodes = new ArrayList<>();

    @XmlElement
    @JsonProperty
    List<Edge> edges = new ArrayList<>();

  }

  GraphPath getGraphPathFromPath(org.neo4j.graphdb.Path path) {
    GraphPath graphPath = new GraphPath();
    graphPath.nodes = newArrayList(transform(path.nodes(), new Function<Node, Vertex>() {
      @Override
      public Vertex apply(Node input) {
        Concept c = graph.getOrCreateFramedNode(input);
        //TODO: Chooses first label as a convention
        List<String> labels = graph.getProperties(graph.getNode((String)c.asVertex().getProperty(CommonProperties.URI)).get(), NodeProperties.LABEL, String.class);
        return new Vertex(c.getFragment(), getFirst(labels, null));
      }
    }));

    graphPath.edges = newArrayList(transform(path.relationships(), new Function<Relationship, Edge>() {
      @Override
      public Edge apply(Relationship input) {
        return new Edge((String)input.getStartNode().getProperty(CommonProperties.FRAGMENT),
            (String)input.getEndNode().getProperty(CommonProperties.FRAGMENT),
            input.getType().name());
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
      @ApiParam( value = "Start node ID", required = true )
      @PathParam("startId") String startId,
      @ApiParam( value = "End node ID", required = true )
      @PathParam("endId") String endId,
      @ApiParam( value = "Maximum path length", required = false )
      @QueryParam("length") @DefaultValue("1") int length,
      @ApiParam( value = "JSONP callback", required = false )
      @QueryParam("callback") @DefaultValue("fn") String callback) {
    Vocabulary.Query query = new Vocabulary.Query.Builder(startId).build();
    Concept startConcept = getOnlyElement(vocabulary.getConceptFromId(query));
    Node startNode = graph.getOrCreateNode(startConcept.getUri());

    query = new Vocabulary.Query.Builder(endId).build();
    Concept endConcept = getOnlyElement(vocabulary.getConceptFromId(query));
    Node endNode = graph.getOrCreateNode(endConcept.getUri());

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

    GraphPath graphPath = getGraphPathFromPath(path);

    GenericEntity<GraphPath> response = new GenericEntity<GraphPath>(graphPath){};
    return JaxRsUtil.wrapJsonp(request, response, callback);
  }

  @GET
  @Path("/paths/simple/{startId}/{endId}")
  @ApiOperation(value = "Get all simple paths.", response = String.class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getPath(
      @ApiParam( value = "Start node ID", required = true )
      @PathParam("startId") String startId,
      @ApiParam( value = "End node ID", required = true )
      @PathParam("endId") String endId,
      @ApiParam( value = "Maximum path length", required = false )
      @QueryParam("length") @DefaultValue("1") int length,
      @ApiParam( value = "JSONP callback", required = false )
      @QueryParam("callback") @DefaultValue("fn") String callback) {
    Vocabulary.Query query = new Vocabulary.Query.Builder(startId).build();
    Concept startConcept = getOnlyElement(vocabulary.getConceptFromId(query));
    Node startNode = graph.getOrCreateNode(startConcept.getUri());

    query = new Vocabulary.Query.Builder(endId).build();
    Concept endConcept = getOnlyElement(vocabulary.getConceptFromId(query));
    Node endNode = graph.getOrCreateNode(endConcept.getUri());

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

    PathFinder<org.neo4j.graphdb.Path> finder = GraphAlgoFactory.allSimplePaths(expander, length);
    Iterable<org.neo4j.graphdb.Path> paths = finder.findAllPaths(startNode, endNode);
    if ((null == paths) || (0 == Iterables.count(paths))) {
      throw new WebApplicationException(404);
    }
    List<GraphPath> returnedPaths = new ArrayList<>();
    for (org.neo4j.graphdb.Path path: paths) {
      returnedPaths.add(getGraphPathFromPath(path));
    }

    GenericEntity<List<GraphPath>> response = new GenericEntity<List<GraphPath>>(returnedPaths){};
    return JaxRsUtil.wrapJsonp(request, response, callback);
  }

  @GET
  @Path("/descendants/{relationship}/{id}")
  @ApiOperation(value = "Get descendants", response = ConceptDTO .class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getSubclasses(
      @ApiParam( value = "Type of relationship to use (try SUPERCLASS_OF or has_proper_part)", required = true )
      @PathParam("relationship") String relationship,
      @ApiParam( value = "ID to find", required = true )
      @PathParam("id") String id,
      @ApiParam( value = "How deep to traverse descendants", required = false )
      @QueryParam("depth") @DefaultValue("1") int depth,
      @ApiParam( value = "JSONP callback", required = false )
      @QueryParam("callback") @DefaultValue("fn") String callback) {
    Vocabulary.Query query = new Vocabulary.Query.Builder(id).build();
    Concept concept = getOnlyElement(vocabulary.getConceptFromId(query));
    Node node = graph.getOrCreateNode(concept.getUri());
    LinkedList<ConceptDTO> dtos = new LinkedList<>();

    RelationshipType type = DynamicRelationshipType.withName(relationship);

    //TODO: include equivalences
    for (org.neo4j.graphdb.Path path: Traversal.description()
        .depthFirst()
        .relationships(type, Direction.OUTGOING)
        .evaluator(Evaluators.toDepth(depth))
        .traverse(node)) {
      ConceptDTO dto = new ConceptDTO();
      dto.setUri((String)path.endNode().getProperty("uri"));
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
    GenericEntity<ConceptDTO> response = new GenericEntity<ConceptDTO>(dtos.getLast()){};
    return JaxRsUtil.wrapJsonp(request, response, callback);
  }
  
  @GET
  @Path("/neighbors/{id}")
  @ApiOperation(value = "Get neighbors", response = ConceptDTO .class)
  @Timed
  @CacheControl(maxAge = 2, maxAgeUnit = TimeUnit.HOURS)
  public Object getNeighbors(
      @ApiParam( value = "Starting ID", required = true )
      @PathParam("id") String id,
      @ApiParam( value = "How far to traverse neighbors", required = false )
      @QueryParam("depth") @DefaultValue("1") int depth,
      @ApiParam( value = "JSONP callback", required = false )
      @QueryParam("callback") @DefaultValue("fn") String callback) {
    Vocabulary.Query query = new Vocabulary.Query.Builder(id).build();
    Concept concept = getOnlyElement(vocabulary.getConceptFromId(query));
    Node node = graph.getOrCreateNode(concept.getUri());
    List<GraphPath> graphPaths = new ArrayList<>();

    for (org.neo4j.graphdb.Path path: Traversal.description()
        .depthFirst()
        .evaluator(Evaluators.toDepth(depth))
        .traverse(node)) {
      graphPaths.add(getGraphPathFromPath(path));
    }
    GenericEntity<List<GraphPath>> response = new GenericEntity<List<GraphPath>>(graphPaths){};
    return JaxRsUtil.wrapJsonp(request, response, callback);
  }

}
