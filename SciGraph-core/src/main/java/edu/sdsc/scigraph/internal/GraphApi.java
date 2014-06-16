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

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Iterables.getOnlyElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

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
import org.neo4j.kernel.Traversal;
import org.neo4j.tooling.GlobalGraphOperations;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.neo4j.EdgeType;
import edu.sdsc.scigraph.neo4j.Graph;

public class GraphApi {

  private final Graph<Concept> graph;

  @Inject
  GraphApi(Graph<Concept> graph) {
    this.graph = graph;
  }

  public boolean classIsInCategory(Node candidate, Node parentConcept) {
    return classIsInCategory(candidate, parentConcept, EdgeType.SUBCLASS_OF);
  }

  public boolean classIsInCategory(Node candidate, Node parent, RelationshipType... relationships ) {
    TraversalDescription description = Traversal.description().depthFirst()
        .evaluator(new Evaluator() {
          @Override
          public Evaluation evaluate(Path path) {
            Optional<String> type = graph.getProperty(path.endNode(), NodeProperties.TYPE, String.class);
            if (type.isPresent() && "OWLClass".equals(type.get())) {
              return Evaluation.INCLUDE_AND_CONTINUE;
            } else {
              return Evaluation.EXCLUDE_AND_PRUNE;
            }
          }
        });
    for (RelationshipType type: relationships) {
      description.relationships(type, Direction.OUTGOING);
    }

    for ( Path position : description.traverse( candidate ) ) {
      if (position.endNode().equals(parent)) {
        return true;
      }
    }
    return false;
  }

  /***
   * TODO: Add a boolean for equivalent classes
   * @param parent
   * @param type
   * @param direction
   * @return
   */
  Collection<Node> getEntailment(Node parent, RelationshipType type, Direction direction) {
    Set<Node> entailment = new HashSet<>();
    for (Path path: Traversal.description()
        .depthFirst()
        .relationships(type, direction)
        .evaluator(Evaluators.fromDepth(1))
        .evaluator(Evaluators.all())
        .traverse(parent)) {
      entailment.add(path.endNode());
    }
    return entailment;
  }

  private final Function<Node, Concept> conceptTransformer = new Function<Node, Concept>() {
    @Override
    public Concept apply(Node input) {
      return graph.getOrCreateFramedNode(input);
    }
  };

  /***
   * Get "inferred" classes.
   * 
   * <p>"Inferred" classes are a legacy NIF graph pattern. They are sometimes called "defined" classes.
   * They translate to the following graph pattern:
   * <pre>
   * c-EQUIVALENT_TO->ANON (SomeValuesFrom)->PROPERTY->birnlex_17
   *                   |
   *                 CLASS
   *                   |
   *                 someRole<-birnlex_17-inferredClass
   * </pre>
   * @param c
   * @return The inferred classes related to c
   */
  public Collection<Concept> getInferredClasses(Concept c) {
    Node parent = graph.getOrCreateNode(c.getUri());
    Collection<Node> inferredClasses = new ArrayList<>();
    for (Relationship r: parent.getRelationships(Direction.OUTGOING, EdgeType.EQUIVALENT_TO)) {
      Optional<String> endType = graph.getProperty(r.getEndNode(), CommonProperties.TYPE, String.class);
      if (endType.isPresent() && "OWLObjectSomeValuesFrom".equals(endType.get())) {
        Relationship property = getOnlyElement(r.getEndNode().getRelationships(Direction.OUTGOING, EdgeType.PROPERTY), null);
        if ((null != property) && 
            "http://ontology.neuinfo.org/NIF/Backend/BIRNLex-OBO-UBO.owl#birnlex_17".equals(property.getEndNode().getProperty("uri"))) {
          for (Relationship bearerOf: r.getEndNode().getRelationships(Direction.OUTGOING, EdgeType.CLASS)) {
            Node role = bearerOf.getEndNode();
            inferredClasses.addAll(getEntailment(role, DynamicRelationshipType.withName("birnlex_17"), Direction.INCOMING));
          }
        }
      }
    }
    return transform(inferredClasses, conceptTransformer);
  }

  /**
   * Get all the self loops in the Neo4j graph.
   * @return  A Set of self loop edges. An empty set will be returned 
   *   if no self loops are found in in the graph. 
   */
  public Set<Relationship> getSelfLoops() {
	  GraphDatabaseService graphDb = graph.getGraphDb();
	  
	  Set<Relationship> result = new HashSet<Relationship> ();
	  
	  for (Relationship n : GlobalGraphOperations.at(graphDb).getAllRelationships()) {
		if ( n.getStartNode().equals(n.getEndNode())) {
			result.add(n);
		}
	  }
	  return result;
  }
  
  public Graph<Concept> getGraph() {return graph;}
  
}
