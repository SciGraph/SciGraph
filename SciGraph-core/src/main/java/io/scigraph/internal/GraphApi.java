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

import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.prefixcommons.CurieUtil;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import io.scigraph.frames.NodeProperties;
import io.scigraph.neo4j.DirectedRelationshipType;
import io.scigraph.owlapi.OwlRelationships;
import io.scigraph.owlapi.curies.AddCuries;

public class GraphApi {

  private final GraphDatabaseService graphDb;
  private final CypherUtil cypherUtil;
  private final CurieUtil curieUtil;

  @Inject
  public GraphApi(GraphDatabaseService graphDb, CypherUtil cypherUtil, CurieUtil curieUtil) {
    this.graphDb = graphDb;
    this.cypherUtil = cypherUtil;
    this.curieUtil = curieUtil;
  }

  /***
   * @param parent
   * @param relationship
   * @param traverseEquivalentEdges
   * @return the entailment
   */
  public Collection<Node> getEntailment(Node parent, DirectedRelationshipType relationship,
      boolean traverseEquivalentEdges) {
    Set<Node> entailment = new HashSet<>();
    TraversalDescription description = graphDb.traversalDescription().depthFirst()
        .relationships(relationship.getType(), relationship.getDirection())
        .evaluator(Evaluators.fromDepth(0)).evaluator(Evaluators.all());
    if (traverseEquivalentEdges) {
      description = description.relationships(OwlRelationships.OWL_EQUIVALENT_CLASS);
    }
    for (Path path : description.traverse(parent)) {
      entailment.add(path.endNode());
    }
    return entailment;
  }

  @AddCuries
  public Graph getNeighbors(Set<Node> nodes, int depth, Set<DirectedRelationshipType> types,
      final Optional<Predicate<Node>> includeNode) {
    TraversalDescription description = graphDb.traversalDescription().breadthFirst()
        .evaluator(Evaluators.toDepth(depth)).uniqueness(Uniqueness.RELATIONSHIP_RECENT);
    for (DirectedRelationshipType type : types) {
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
    Graph graph = new TinkerGraph();
    TinkerGraphUtil tgu = new TinkerGraphUtil(graph, curieUtil);
    for (Path path : description.traverse(nodes)) {
      Relationship relationship = path.lastRelationship();
      if (null != relationship) {
        tgu.addEdge(relationship);
      }
    }
    if (isEmpty(graph.getEdges())) {
      // If nothing was added to the graph add the root nodes
      for (Node node : nodes) {
        tgu.addNode(node);
      }
    }
    return graph;
  }

  public Graph getEdges(RelationshipType type, boolean entail, long skip, long limit) {
    String query = "MATCH path = (start)-[r:" + type.name() + (entail ? "!" : "") + "]->(end) "
        + " RETURN path "
        // TODO: This slows down the query dramatically.
        // + " ORDER BY ID(r) "
        + " SKIP " + skip + " LIMIT " + limit;
    Graph graph = new TinkerGraph();
    TinkerGraphUtil tgu = new TinkerGraphUtil(graph, curieUtil);
    Result result;
    try {
      result = cypherUtil.execute(query);
      while (result.hasNext()) {
        Map<String, Object> map = result.next();
        Path path = (Path) map.get("path");
        tgu.addPath(path);
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      // Return and empty graph if the limit is too high...
    }
    return graph;
  }

  public Optional<Node> getNode(String id, Optional<String> lblHint) {
    String iriResolved = curieUtil.getIri(id).orElse(id);
    Optional<Node> node = Optional.empty();
    if (lblHint.isPresent()) {
      Label hintLabel = Label.label(lblHint.get());
      Node hit = graphDb.findNode(hintLabel, NodeProperties.IRI, iriResolved);
      if (hit != null) {
        node = Optional.of(hit);
      }
    } else {
      String startQuery =
          "MATCH (n {" + NodeProperties.IRI + ": \"" + iriResolved + "\"}) RETURN n";
      Result res = cypherUtil.execute(startQuery);
      if (res.hasNext()) {
        node = Optional.of((Node) res.next().get("n"));
      }
    }

    return node;
  }

  public Graph getReachableNodes(Node start, List<String> rels, Set<String> lbls) {
    Set<Node> acc = Sets.newHashSet(start);
    if (rels.isEmpty()) {
      Set<Node> newAcc = Sets.newHashSet();
      String query = "MATCH (n)-[]->(m) WHERE ID(n) = " + start.getId() + " RETURN m";
      Result res = cypherUtil.execute(query);
      while (res.hasNext()) {
        newAcc.add((Node) res.next().get("m"));
      }
      acc = newAcc;
    } else {
      for (String rel : rels) {
        Set<Node> newAcc = Sets.newHashSet();
        for (Node n : acc) {
          String query = "MATCH (n)-[:" + rel + "]->(m) WHERE ID(n) = " + n.getId() + " RETURN m";
          Result res = cypherUtil.execute(query);
          while (res.hasNext()) {
            newAcc.add((Node) res.next().get("m"));
          }
        }
        acc = newAcc;
      }
    }

    TinkerGraphUtil tgu = new TinkerGraphUtil(curieUtil);
    for (Node n : acc) {
      if (lbls.isEmpty()) {
        tgu.addNode(n);
      } else {
        Set<String> nodeLabels =
            Sets.newHashSet(n.getLabels()).stream().map(l -> l.name()).collect(Collectors.toSet());
        nodeLabels.retainAll(lbls);
        if (!nodeLabels.isEmpty()) {
          tgu.addNode(n);
        }
      }
    }

    return tgu.getGraph();
  }

  /***
   * @return All the {@link RelationshipType}s in the graph.
   */
  public Collection<RelationshipType> getAllRelationshipTypes() {
    Set<RelationshipType> relationships = new HashSet<>();
    try (Transaction tx = graphDb.beginTx()) {
      relationships.addAll(newHashSet(graphDb.getAllRelationshipTypes()));
      tx.success();
    }
    return relationships;
  }

  /***
   * @return All the property keys in the graph.
   */
  public Collection<String> getAllPropertyKeys() {
    Set<String> propertyKeys = new HashSet<>();
    try (Transaction tx = graphDb.beginTx()) {
      propertyKeys.addAll(newHashSet(graphDb.getAllPropertyKeys()));
      tx.success();
    }
    return propertyKeys;
  }

}
