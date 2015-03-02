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
package edu.sdsc.scigraph.internal;

import static com.google.common.collect.Iterables.isEmpty;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.neo4j.DirectedRelationshipType;
import edu.sdsc.scigraph.owlapi.CurieUtil;
import edu.sdsc.scigraph.owlapi.OwlLabels;
import edu.sdsc.scigraph.owlapi.OwlRelationships;

public class GraphApi {

  private final GraphDatabaseService graphDb;
  
  private final CurieUtil curieUtil;

  @Inject
  public GraphApi(GraphDatabaseService graphDb, CurieUtil curieUtil) {
    this.graphDb = graphDb;
    this.curieUtil = curieUtil;
  }

  /***
   * TODO: Add a boolean for equivalent classes
   * 
   * @param parent
   * @param type
   * @param direction
   * @return
   */
  public Collection<Node> getEntailment(Node parent, RelationshipType type, Direction direction) {
    Set<Node> entailment = new HashSet<>();
    for (Path path : graphDb.traversalDescription().depthFirst()
        .relationships(type, direction)
        .evaluator(Evaluators.fromDepth(0))
        .evaluator(Evaluators.all())
        .traverse(parent)) {
      entailment.add(path.endNode());
    }
    return entailment;
  }

  
  public boolean classIsInCategory(Node candidate, Node parentConcept) {
    return classIsInCategory(candidate, parentConcept, OwlRelationships.RDFS_SUBCLASS_OF);
  }

  public boolean classIsInCategory(Node candidate, Node parent, RelationshipType... relationships) {
    TraversalDescription description = graphDb.traversalDescription().depthFirst()
        .evaluator(new Evaluator() {
          @Override
          public Evaluation evaluate(Path path) {
            if (path.endNode().hasLabel(OwlLabels.OWL_CLASS)) {
              return Evaluation.INCLUDE_AND_CONTINUE;
            } else {
              return Evaluation.EXCLUDE_AND_PRUNE;
            }
          }
        });
    for (RelationshipType type : relationships) {
      description.relationships(type, Direction.OUTGOING);
    }

    for (Path position : description.traverse(candidate)) {
      if (position.endNode().equals(parent)) {
        return true;
      }
    }
    return false;
  }


  void addCuries(TinkerGraph graph) {
    for (Vertex vertex: graph.getVertices()) {
      String uri = (String)vertex.getProperty(CommonProperties.URI);
      Optional<String> curie = curieUtil.getCurie(uri);
      if (curie.isPresent()) {
        vertex.setProperty(CommonProperties.CURIE, curie.get());
      }
    }
  }
  
  public TinkerGraph getNeighbors(Set<Node> nodes, int depth, Set<DirectedRelationshipType> types, final Optional<Predicate<Node>> includeNode) {
    TraversalDescription description = graphDb.traversalDescription().depthFirst().evaluator(Evaluators.toDepth(depth));
    for (DirectedRelationshipType type: types) {
      description = description.relationships(type.getType(), type.getDirection());
    }
    if (includeNode.isPresent()) {
      description = description.evaluator(new Evaluator() {
        @Override
        public Evaluation evaluate(Path path) {
          if (includeNode.get().apply(path.endNode())) {
            return Evaluation.INCLUDE_AND_CONTINUE;
          } else {
            return Evaluation.EXCLUDE_AND_PRUNE;
          }
        }
      });
    }
    TinkerGraph graph = new TinkerGraph();
    for (Path path: description.traverse(nodes)) {
      Relationship relationship = path.lastRelationship();
      if (null != relationship) {
        TinkerGraphUtil.addEdge(graph, relationship);
      }
    }
    if (isEmpty(graph.getEdges())) { 
      // If nothing was added to the graph add the root nodes
      for (Node node: nodes) {
        TinkerGraphUtil.addNode(graph, node);
      }
    }
    addCuries(graph);
    return graph;
  }

}
