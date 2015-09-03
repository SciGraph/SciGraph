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
package io.scigraph.internal;

import java.util.logging.Logger;

import io.scigraph.frames.NodeProperties;
import io.scigraph.owlapi.OwlRelationships;

import javax.inject.Inject;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.tooling.GlobalGraphOperations;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;

public class EquivalenceAspect implements GraphAspect {
  private static final Logger logger = Logger.getLogger(EquivalenceAspect.class.getName());

  static final RelationshipType IS_EQUIVALENT = OwlRelationships.OWL_EQUIVALENT_CLASS;
  static final String ORIGINAL_REFERENCE_KEY_SOURCE = "equivalentOriginalNodeSource";
  static final String ORIGINAL_REFERENCE_KEY_TARGET = "equivalentOriginalNodeTarget";
  static final String CLIQUE_LEADER = "cliqueLeader";


  private final GraphDatabaseService graphDb;
  private final CypherUtil cypherUtil;

  @Inject
  EquivalenceAspect(GraphDatabaseService graphDb, CypherUtil graphUtil) {
    this.graphDb = graphDb;
    this.cypherUtil = graphUtil;
  }

  @Override
  public void invoke(Graph graph) {
    try (Transaction tx = graphDb.beginTx()) {
      // Set<Long> nodeIds = newHashSet(transform(graph.getVertices(), new Function<Vertex, Long>()
      // {
      // @Override
      // public Long apply(Vertex vertex) {
      // return Long.valueOf((String) vertex.getId());
      // }
      // }));
      // Long nodeId = nodeIds.iterator().next();
      // String query = "MATCH path = (n)-[:equivalentClass*]->(e) where id(n) = " + nodeId +
      // " return path";
      // Result result = cypherUtil.execute(query);
      // while (result.hasNext()) {
      // Map<String, Object> map = result.next();
      // System.out.println(((Path) map.get("path")));
      // }
      GlobalGraphOperations globalGraphOperations = GlobalGraphOperations.at(graphDb);
      // Iterator<Node> iterator = globalGraphOperations.getAllNodes().iterator();
      // iterator.next();
      // Node firstNode = iterator.next();

      for (Node baseNode : globalGraphOperations.getAllNodes()) {
        logger.info("Processing Node - " + baseNode.getProperty(NodeProperties.IRI));
        // Marking the edges as not movable
        for (Relationship r : baseNode.getRelationships()) {
          if (r.getStartNode().getId() == baseNode.getId()) {
            r.setProperty(ORIGINAL_REFERENCE_KEY_SOURCE, CLIQUE_LEADER);
          } else {
            r.setProperty(ORIGINAL_REFERENCE_KEY_TARGET, CLIQUE_LEADER);
          }
        }


        for (Node currentNode : graphDb.traversalDescription().relationships(IS_EQUIVALENT).uniqueness(Uniqueness.NODE_GLOBAL).traverse(baseNode)
            .nodes()) {
          logger.info("Processing underNode - " + currentNode.getProperty(NodeProperties.IRI));
          if (currentNode.getId() != baseNode.getId()) {
            Iterable<Relationship> rels = currentNode.getRelationships();
            for (Relationship rel : rels) {
              if (!(sourceHasAlreadyMoved(rel) && targetHasAlreadyMoved(rel)) && (rel.getType().name() != IS_EQUIVALENT.name())) {
                if (rel.getOtherNode(currentNode).getId() != baseNode.getId()) {
                  if ((rel.getEndNode().getId() == currentNode.getId()) && !targetHasAlreadyMoved(rel)) {
                    logger.info("MOVE TARGET " + rel.getType() + " FROM " + currentNode.getProperty(NodeProperties.IRI) + " TO "
                        + baseNode.getProperty(NodeProperties.IRI));
                    moveRelationship(currentNode, baseNode, rel, ORIGINAL_REFERENCE_KEY_TARGET, graph);
                  } else if ((rel.getStartNode().getId() == currentNode.getId()) && !sourceHasAlreadyMoved(rel)) {
                    logger.info("MOVE SOURCE " + rel.getType() + " FROM " + currentNode.getProperty(NodeProperties.IRI) + " TO "
                        + baseNode.getProperty(NodeProperties.IRI));

                    moveRelationship(currentNode, baseNode, rel, ORIGINAL_REFERENCE_KEY_SOURCE, graph);
                  }
                }
              }
            }
          }
        }
        // TODO move the equivalence edges
      }

      tx.success();
    }
  }

  private void moveRelationship(Node from, Node to, Relationship rel, String property, Graph tinkerGraph) {
    // Neo4J Graph
    Relationship newRel = to.createRelationshipTo(rel.getOtherNode(from), rel.getType());
    copyProperties(rel, newRel);
    rel.delete();
    newRel.setProperty(property, from.getProperty(NodeProperties.IRI));

    // TinkerGraph
    TinkerGraphUtil.removeEdge(tinkerGraph, rel);
    TinkerGraphUtil.addEdge(tinkerGraph, newRel);
  }

  private boolean targetHasAlreadyMoved(Relationship rel) {
    return rel.getProperty(ORIGINAL_REFERENCE_KEY_TARGET, null) != null;
  }

  private boolean sourceHasAlreadyMoved(Relationship rel) {
    return rel.getProperty(ORIGINAL_REFERENCE_KEY_SOURCE, null) != null;
  }

  private void copyProperties(PropertyContainer source, PropertyContainer target) {
    for (String key : source.getPropertyKeys())
      target.setProperty(key, source.getProperty(key));
  }
}
