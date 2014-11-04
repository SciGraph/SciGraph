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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Iterables.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.tooling.GlobalGraphOperations;

import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.neo4j.DirectedRelationshipType;
import edu.sdsc.scigraph.owlapi.OwlLabels;
import edu.sdsc.scigraph.owlapi.OwlRelationships;

public class GraphApi {

  private final GraphDatabaseService graphDb;

  @Inject
  GraphApi(GraphDatabaseService graphDb) {
    this.graphDb = graphDb;
  }

  public boolean classIsInCategory(Node candidate, Node parentConcept) {
    return classIsInCategory(candidate, parentConcept, OwlRelationships.RDF_SUBCLASS_OF);
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

  /***
   * TODO: Add a boolean for equivalent classes
   * 
   * @param parent
   * @param type
   * @param direction
   * @return
   */
  Collection<Node> getEntailment(Node parent, RelationshipType type, Direction direction) {
    Set<Node> entailment = new HashSet<>();
    for (Path path : graphDb.traversalDescription().depthFirst()
        .relationships(type, direction)
        .evaluator(Evaluators.fromDepth(1)).evaluator(Evaluators.all()).traverse(parent)) {
      entailment.add(path.endNode());
    }
    return entailment;
  }

  /***
   * Get "inferred" classes.
   * 
   * <p>
   * "Inferred" classes are a legacy NIF graph pattern. They are sometimes called "defined" classes.
   * They translate to the following graph pattern:
   * 
   * <pre>
   * (c)-[:EQUIVALENT_TO]->(SomeValuesFrom)->[:PROPERTY]->(birnlex_17)
   *                              |
   *                            [:CLASS]
   *                              |
   *                           (someRole)<-[:birnlex_17]-(inferredClass)
   * </pre>
   * 
   * @param c
   * @return The inferred classes related to c
   */
  public Collection<Node> getInferredClasses(Concept c) {
    Node parent = graphDb.getNodeById(c.getId());
    Collection<Node> inferredClasses = new ArrayList<>();
    for (Relationship r : parent.getRelationships(Direction.OUTGOING, OwlRelationships.OWL_EQUIVALENT_CLASS)) {
      if (r.getEndNode().hasLabel(OwlLabels.OWL_SOME_VALUES_FROM)) {
        Relationship property = getOnlyElement(
            r.getEndNode().getRelationships(Direction.OUTGOING, OwlRelationships.PROPERTY), null);
        if (null != property
            && "http://ontology.neuinfo.org/NIF/Backend/BIRNLex-OBO-UBO.owl#birnlex_17"
            .equals(property.getEndNode().getProperty("uri"))) {
          for (Relationship bearerOf : r.getEndNode().getRelationships(Direction.OUTGOING,
              OwlRelationships.CLASS)) {
            Node role = bearerOf.getEndNode();
            inferredClasses.addAll(getEntailment(role,
                DynamicRelationshipType.withName("birnlex_17"), Direction.INCOMING));
          }
        }
      }
    }
    return inferredClasses;
  }

  /**
   * Get all the self loops in the Neo4j graph.
   * 
   * @return A set of self loop edges. An empty set will be returned if no self loops are found in
   *         in the graph.
   */
  public Set<Relationship> getSelfLoops() {
    Set<Relationship> result = new HashSet<Relationship>();

    for (Relationship n : GlobalGraphOperations.at(graphDb).getAllRelationships()) {
      if (n.getStartNode().equals(n.getEndNode())) {
        result.add(n);
      }
    }
    return result;
  }

  public TinkerGraph getNeighbors(Node node, int distance, Set<DirectedRelationshipType> types/*, Optional<Predicate<Node>> includeNode*/) {
    TraversalDescription description = graphDb.traversalDescription().depthFirst().evaluator(Evaluators.toDepth(distance));
    for (DirectedRelationshipType type: types) {
      description = description.relationships(type.getType(), type.getDirection());
    }
    TinkerGraph graph = new TinkerGraph();
    for (Path path: description.traverse(node)) {
      Relationship relationship = path.lastRelationship();
      if (null != relationship) {
        TinkerGraphUtil.addEdge(graph, relationship);
      }
    }
    if (isEmpty(graph.getEdges())) {
      TinkerGraphUtil.addNode(graph, node);
    }
    return graph;
  }

}
