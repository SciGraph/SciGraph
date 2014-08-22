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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import edu.sdsc.scigraph.frames.NodeProperties;

/***
 * Used for building the Solr synonym files to support object property entailment.
 */
public class HierarchyVisitor {

  private final Graph graph;
  private final RelationshipType edgeType;
  private final Set<String> rootUris;
  private final boolean includeEquivalentClasses;
  private final Callback callback;

  public static class Builder {
    private final Graph graph;
    private final RelationshipType edgeType;
    private final Callback callback;

    private Set<String> rootUris = new HashSet<>();
    private boolean includeEquivalentClasses = true;

    public Builder(Graph graph, RelationshipType edgeType, Callback callback) {
      this.graph = graph;
      this.edgeType = edgeType;
      this.callback = callback;
    }

    public Builder rootUris(String ... uris) {
      for (String uri: uris) {
        rootUris.add(uri); 
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
    this.edgeType = builder.edgeType;
    this.rootUris = builder.rootUris;
    this.includeEquivalentClasses = builder.includeEquivalentClasses;
    this.callback = builder.callback;
  }

  Collection<Node> getRootNodes() {
    Set<Node> roots = new HashSet<>();
    if (!rootUris.isEmpty()) {
      roots.addAll(newHashSet(transform(rootUris, new Function<String, Node>() {
        @Override
        public Node apply(String uri) {
          checkState(graph.nodeExists(uri), "Failed to find root node " + uri);
          return graph.getOrCreateNode(uri);
        }
      })));
    } else {
      ResourceIterator<Map<String,Object>> result = graph.runCypherQuery(
          String.format("START n = node(*) " + 
              "MATCH (n)-[:%1$s]->(s) " +
 "WHERE not(()-[:%1$s]->n) AND (not(has(n.anonymous)) OR n.anonymous = false) "
              +
              "RETURN DISTINCT n", edgeType.toString()));
      while (result.hasNext()) {
        Map<String, Object> map = result.next();
        roots.add((Node)map.get("n"));
      }
    }
    return roots;
  }

  public void traverse() {
    traverse(getRootNodes().toArray(new Node[getRootNodes().size()]));
  }

  void traverse(Node... roots) {
    TraversalDescription description = graph.getGraphDb().traversalDescription()
        .uniqueness(Uniqueness.RELATIONSHIP_PATH)
        .depthFirst()
        .expand(new PathExpander<Void>() {

          @Override
          public Iterable<Relationship> expand(Path path, BranchState<Void> state) {
            Set<RelationshipType> types = new HashSet<>();
            types.add(EdgeType.SUPERCLASS_OF);
            if (includeEquivalentClasses && null != path.lastRelationship()
                && !path.lastRelationship().isType(EdgeType.EQUIVALENT_TO)) {
              types.add(EdgeType.EQUIVALENT_TO);
            }
            return path.endNode().getRelationships(Direction.OUTGOING, 
                   types.toArray(new RelationshipType[0]));
          }

          @Override
          public PathExpander<Void> reverse() {
            return null;
          }
        });

    for (Path position: description.traverse(roots)) {
      List<Node> path = new ArrayList<>();
      PeekingIterator<PropertyContainer> iter = Iterators.peekingIterator(position.iterator());
      while (iter.hasNext()) {
        PropertyContainer container = iter.next();
        if (container instanceof Node) {
          if (graph.getProperty(container, NodeProperties.ANONYMOUS, Boolean.class).or(false)) {
            // Ignore paths with anonymous nodes
          }
          else if (iter.hasNext() &&
              ((Relationship)iter.peek()).isType(EdgeType.EQUIVALENT_TO)) {
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
