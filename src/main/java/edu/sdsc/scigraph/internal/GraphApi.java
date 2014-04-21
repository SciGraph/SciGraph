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
package edu.sdsc.scigraph.internal;

import javax.inject.Inject;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

import com.google.common.base.Optional;

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

}
