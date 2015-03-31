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
package edu.sdsc.scigraph.neo4j;

import static com.google.common.collect.Iterables.addAll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import edu.sdsc.scigraph.owlapi.OwlLabels;
import edu.sdsc.scigraph.owlapi.OwlRelationships;
public class HierarchyVisitor {

  private final GraphDatabaseService graph;
  private final ExecutionEngine engine;
  private final RelationshipType edgeType;
  private final Set<Node> rootNodes;
  private final boolean includeEquivalentClasses;
  private final Callback callback;

  public static class Builder {
    private final GraphDatabaseService graph;
    private final RelationshipType edgeType;
    private final Callback callback;

    private Set<Node> rootNodes = new HashSet<>();
    private boolean includeEquivalentClasses = true;

    public Builder(GraphDatabaseService graph, RelationshipType edgeType, Callback callback) {
      this.graph = graph;
      this.edgeType = edgeType;
      this.callback = callback;
    }

    public Builder rootUris(Node ... nodes) {
      for (Node node: nodes) {
        rootNodes.add(node); 
      }
      return this;
    }

    public Builder includeEquivalentClasses(boolean include) {
      includeEquivalentClasses = include; return this;
    }

    public HierarchyVisitor build() {
      return new HierarchyVisitor(this);
    }

  }

  public interface Callback {
    void processPath(List<Node> path);
  }

  private HierarchyVisitor(Builder builder) {
    this.graph = builder.graph;
    engine = new ExecutionEngine(graph);
    this.edgeType = builder.edgeType;
    this.rootNodes = builder.rootNodes;
    this.includeEquivalentClasses = builder.includeEquivalentClasses;
    this.callback = builder.callback;
  }

  Collection<Node> getRootNodes() {
    Set<Node> roots = new HashSet<>();
    if (rootNodes.isEmpty()) {
      ResourceIterator<Map<String,Object>> result = engine.execute(
          String.format("MATCH (n)<-[:%1$s]-(s) " +
              "WHERE not(()<-[:%1$s]-n) AND not(n:anonymous) " +
              "RETURN DISTINCT n", edgeType.toString())).iterator();
      while (result.hasNext()) {
        Map<String, Object> map = result.next();
        roots.add((Node)map.get("n"));
      }
    } else {
      roots.addAll(rootNodes);
    }
    return roots;
  }

  public void traverse() {
    traverse(getRootNodes().toArray(new Node[getRootNodes().size()]));
  }

  void traverse(Node... roots) {
    TraversalDescription description = graph.traversalDescription()
        .uniqueness(Uniqueness.RELATIONSHIP_PATH)
        .depthFirst()
        .expand(new PathExpander<Void>() {

          @Override
          public Iterable<Relationship> expand(Path path, BranchState<Void> state) {
            Set<Relationship> relationships = new HashSet<>();
            addAll(relationships, path.endNode().getRelationships(Direction.INCOMING, OwlRelationships.RDFS_SUBCLASS_OF));
            if (includeEquivalentClasses && null != path.lastRelationship()
                && !path.lastRelationship().isType(OwlRelationships.OWL_EQUIVALENT_CLASS)) {
              addAll(relationships, path.endNode().getRelationships(OwlRelationships.OWL_EQUIVALENT_CLASS));
            }
            return relationships;
          }

          @Override
          public PathExpander<Void> reverse() {
            return null;
          }
        });

    try (Transaction tx = graph.beginTx()) {
      for (Path position: description.traverse(roots)) {
        List<Node> path = new ArrayList<>();
        PeekingIterator<PropertyContainer> iter = Iterators.peekingIterator(position.iterator());
        while (iter.hasNext()) {
          PropertyContainer container = iter.next();
          if (container instanceof Node) {
            if (((Node) container).hasLabel(OwlLabels.OWL_ANONYMOUS)) {
              // Ignore paths with anonymous nodes
            }
            else if (iter.hasNext() &&
                ((Relationship)iter.peek()).isType(OwlRelationships.OWL_EQUIVALENT_CLASS)) {
              // Ignore the path hop representing the equivalence
            } else {
              path.add((Node)container);
            }
          }
        }
        callback.processPath(path);
      }
    }
  }

}
