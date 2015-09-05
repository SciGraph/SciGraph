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

import io.scigraph.frames.NodeProperties;
import io.scigraph.owlapi.OwlLabels;
import io.scigraph.owlapi.OwlRelationships;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.tooling.GlobalGraphOperations;

import com.tinkerpop.blueprints.Graph;

public class EquivalenceAspect implements GraphAspect {
  private static final Logger logger = Logger.getLogger(EquivalenceAspect.class.getName());

  static final RelationshipType IS_EQUIVALENT = OwlRelationships.OWL_EQUIVALENT_CLASS;
  static final String ORIGINAL_REFERENCE_KEY_SOURCE = "equivalentOriginalNodeSource";
  static final String ORIGINAL_REFERENCE_KEY_TARGET = "equivalentOriginalNodeTarget";
  static final Label CLIQUE_LEADER_LABEL = DynamicLabel.label("cliqueLeader");
  static final String CLIQUE_LEADER_PROPERTY = "cliqueLeader";


  private final GraphDatabaseService graphDb;

  @Inject
  public EquivalenceAspect(GraphDatabaseService graphDb) {
    this.graphDb = graphDb;
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

        // Skip anonymous node, they cannot be leader
        if (!baseNode.hasLabel(OwlLabels.OWL_ANONYMOUS)) {

          logger.info("Processing Node - " + baseNode.getProperty(NodeProperties.IRI));
          // Marking the edges as not movable
          for (Relationship r : baseNode.getRelationships()) {
            if (r.getStartNode().getId() == baseNode.getId()) {
              r.setProperty(ORIGINAL_REFERENCE_KEY_SOURCE, CLIQUE_LEADER_PROPERTY);
            } else {
              r.setProperty(ORIGINAL_REFERENCE_KEY_TARGET, CLIQUE_LEADER_PROPERTY);
            }
          }

          // Keep a list of equivalentNodes to move them to the leader the processing
          List<Node> equivalentNodes = new ArrayList<Node>();

          // Move all the edges except the equivalences
          for (Node currentNode : graphDb.traversalDescription().relationships(IS_EQUIVALENT, Direction.BOTH).uniqueness(Uniqueness.NODE_GLOBAL)
              .traverse(baseNode).nodes()) {
            if (currentNode.getId() != baseNode.getId()) {
              logger.info("Processing underNode - " + currentNode.getProperty(NodeProperties.IRI));
              equivalentNodes.add(currentNode);
              Iterable<Relationship> rels = currentNode.getRelationships();
              for (Relationship rel : rels) {
                if (!(sourceHasAlreadyMoved(rel) && targetHasAlreadyMoved(rel)) && !(rel.getType().name().equals(IS_EQUIVALENT.name()))) {
                  if (rel.getOtherNode(currentNode).getId() != baseNode.getId()) {
                    if ((rel.getEndNode().getId() == currentNode.getId()) && !targetHasAlreadyMoved(rel)) {
                      logger.info("MOVE TARGET " + rel.getType() + " FROM " + currentNode.getProperty(NodeProperties.IRI) + " TO "
                          + baseNode.getProperty(NodeProperties.IRI));

                      electAsCliqueLeader(baseNode);
                      moveRelationship(currentNode, baseNode, rel, ORIGINAL_REFERENCE_KEY_TARGET, graph);
                      // TODO clean that...
                      // Move rdfs:label if non-existing on leader
                      if(!baseNode.hasProperty(NodeProperties.LABEL)){
                        if(currentNode.hasProperty(NodeProperties.LABEL)){
                          baseNode.setProperty(NodeProperties.LABEL, currentNode.getProperty(NodeProperties.LABEL));
                        }
                        // TODO dirty hack...
                        if(currentNode.hasProperty("http://www.w3.org/2000/01/rdf-schema#label")){
                          baseNode.setProperty("http://www.w3.org/2000/01/rdf-schema#label", currentNode.getProperty("http://www.w3.org/2000/01/rdf-schema#label"));
                        }
                      }
                    } else if ((rel.getStartNode().getId() == currentNode.getId()) && !sourceHasAlreadyMoved(rel)) {
                      logger.info("MOVE SOURCE " + rel.getType() + " FROM " + currentNode.getProperty(NodeProperties.IRI) + " TO "
                          + baseNode.getProperty(NodeProperties.IRI));

                      electAsCliqueLeader(baseNode);
                      moveRelationship(currentNode, baseNode, rel, ORIGINAL_REFERENCE_KEY_SOURCE, graph);
                      // TODO clean that...
                      // Move rdfs:label if non-existing on leader
                      if(!baseNode.hasProperty(NodeProperties.LABEL)){
                        if(currentNode.hasProperty(NodeProperties.LABEL)){
                          baseNode.setProperty(NodeProperties.LABEL, currentNode.getProperty(NodeProperties.LABEL));
                        }
                        // TODO dirty hack...
                        if(currentNode.hasProperty("http://www.w3.org/2000/01/rdf-schema#label")){
                          baseNode.setProperty("http://www.w3.org/2000/01/rdf-schema#label", currentNode.getProperty("http://www.w3.org/2000/01/rdf-schema#label"));
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          
          // No equivalent so elected as CliqueLeader
          if(!baseNode.hasRelationship(IS_EQUIVALENT)){
            electAsCliqueLeader(baseNode);
          }

          logger.info("EQUIVALENT NODES: " + equivalentNodes);
          // move equivalence edges
          for (Node equivalentNode : equivalentNodes) {
            for (Relationship equivalentRel : equivalentNode.getRelationships(IS_EQUIVALENT)) {
              if (!(sourceHasAlreadyMoved(equivalentRel) || targetHasAlreadyMoved(equivalentRel))) {
                if (equivalentRel.getEndNode().getId() == equivalentNode.getId()) {
                  logger.info("MOVE EQU TARGET " + equivalentRel.getType() + " FROM " + equivalentNode.getProperty(NodeProperties.IRI) + " TO "
                      + baseNode.getProperty(NodeProperties.IRI));
                  moveRelationship(equivalentNode, baseNode, equivalentRel, ORIGINAL_REFERENCE_KEY_TARGET, graph);
                } else if (equivalentRel.getStartNode().getId() == equivalentNode.getId()) {
                  logger.info("MOVE EQU SOURCE " + equivalentRel.getType() + " FROM " + equivalentNode.getProperty(NodeProperties.IRI) + " TO "
                      + baseNode.getProperty(NodeProperties.IRI));
                  moveRelationship(equivalentNode, baseNode, equivalentRel, ORIGINAL_REFERENCE_KEY_SOURCE, graph);
                }
              }
            }
          }

        }

        tx.success();
      }
    }
  }
  
  private void electAsCliqueLeader(Node n){
    if(!n.hasLabel(CLIQUE_LEADER_LABEL)){
      n.addLabel(CLIQUE_LEADER_LABEL);
    }
  }


  private void moveRelationship(Node from, Node to, Relationship rel, String property, Graph tinkerGraph) {
    Relationship newRel = null;
    if (property == ORIGINAL_REFERENCE_KEY_TARGET) {
      newRel = rel.getOtherNode(from).createRelationshipTo(to, rel.getType());
    } else {
      newRel = to.createRelationshipTo(rel.getOtherNode(from), rel.getType());
    }
    // Neo4J Graph
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
