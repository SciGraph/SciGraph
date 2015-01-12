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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import com.google.common.base.Function;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.neo4j.HierarchyVisitor.Callback;
import edu.sdsc.scigraph.owlapi.OwlRelationships;
import edu.sdsc.scigraph.util.GraphTestBase;

public class HierarchyVisitorTest extends GraphTestBase {

  Graph graph;
  Node a, b, c, d, e, f, g, h, i, j;

  Callback emptyCallback = new Callback() {
    @Override
    public void processPath(List<Node> path) {}
  };

  static class CollectingCallback implements Callback {

    List<List<String>> fragments = new ArrayList<>();

    @Override
    public void processPath(List<Node> path) {
      fragments.add(transform(path, new Function<Node, String>() {
        @Override
        public String apply(Node input) {
          return (String)input.getProperty(CommonProperties.FRAGMENT);
        }
      }));
    }

  };

  Node createNode(String id) {
    Node node = graph.getOrCreateNode("http://example.org/" + id);
    return node;
  }

  /**********
   *   a   d l    g
   *  / \  | |   / \
   * b   c-e-k  h   i
   *       |     \ /
   *       f      j
   **********/
  @Before
  public void createNodes() {
    graph = new Graph(graphDb);
    a = createNode("a");
    b = createNode("b");
    c = createNode("c");
    d = createNode("d");
    e = createNode("e");
    f = createNode("f");
    g = createNode("g");
    h = createNode("h");
    i = createNode("i");
    j = createNode("j");
    graph.getOrCreateRelationship(b, a, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.getOrCreateRelationship(c, a, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.getOrCreateRelationship(e, d, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.getOrCreateRelationship(f, e, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.getOrCreateRelationship(e, c, OwlRelationships.OWL_EQUIVALENT_CLASS);

    graph.getOrCreateRelationship(h, g, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.getOrCreateRelationship(i, g, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.getOrCreateRelationship(j, h, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.getOrCreateRelationship(j, i, OwlRelationships.RDFS_SUBCLASS_OF);
  }

  @Test
  public void testGetRootNodesWithProvidedRoot() {
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graph, OwlRelationships.RDFS_SUBCLASS_OF, emptyCallback).rootUris("http://example.org/a").build();
    assertThat(visitor.getRootNodes(), hasItems(a));
  }

  @Test
  public void testGetRootNodesWithProvidedRoots() {
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graph, OwlRelationships.RDFS_SUBCLASS_OF, emptyCallback).
        rootUris("http://example.org/a", "http://example.org/d").build();
    assertThat(visitor.getRootNodes(), hasItems(a, d));
  }

  @Test
  public void testGetRootNodesWithoutProvidedRoots() {
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graph, OwlRelationships.RDFS_SUBCLASS_OF, emptyCallback).build();
    assertThat(visitor.getRootNodes(), hasItems(a, d));
  }

  List<List<String>> getExpectedNonEquivalentFragments() {
    List<List<String>> expected= new ArrayList<>();
    expected.add(newArrayList("a"));
    expected.add(newArrayList("a", "b"));
    expected.add(newArrayList("a", "c"));
    expected.add(newArrayList("d"));
    expected.add(newArrayList("d", "e"));
    expected.add(newArrayList("d", "e", "f"));
    expected.add(newArrayList("g"));
    expected.add(newArrayList("g", "h"));
    expected.add(newArrayList("g", "i"));
    expected.add(newArrayList("g", "h", "j"));
    expected.add(newArrayList("g", "i", "j"));
    return expected;
  }

  @Test
  public void testNonEquivalentTraverse() {
    CollectingCallback callback = new CollectingCallback();
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graph, OwlRelationships.RDFS_SUBCLASS_OF, callback).includeEquivalentClasses(false).build();
    visitor.traverse();
    assertThat(callback.fragments, containsInAnyOrder(getExpectedNonEquivalentFragments().toArray()));
  }

  @Test
  public void testEquivalentTraverse() {
    CollectingCallback callback = new CollectingCallback();
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graph, OwlRelationships.RDFS_SUBCLASS_OF, callback).includeEquivalentClasses(true).build();
    visitor.traverse();
    List<List<String>> expected = getExpectedNonEquivalentFragments();
    expected.add(newArrayList("a", "e"));
    expected.add(newArrayList("a", "e", "f"));
    expected.add(newArrayList("d", "c"));
    assertThat(callback.fragments, containsInAnyOrder(expected.toArray()));
  }

  @Test
  public void testMultipleEquivalences() {
    Node l = createNode("http://example.org/l");
    Node k = createNode("http://example.org/k");
    graph.getOrCreateRelationship(k, l, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.getOrCreateRelationship(e, k, OwlRelationships.OWL_EQUIVALENT_CLASS);
    graph.getOrCreateRelationship(c, k, OwlRelationships.OWL_EQUIVALENT_CLASS);
    CollectingCallback callback = new CollectingCallback();
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graph, OwlRelationships.RDFS_SUBCLASS_OF, callback).includeEquivalentClasses(true).build();
    visitor.traverse();
    List<List<String>> expected = getExpectedNonEquivalentFragments();
    expected.add(newArrayList("a", "e"));
    expected.add(newArrayList("a", "k"));
    expected.add(newArrayList("a", "e", "f"));
    expected.add(newArrayList("d", "c"));
    expected.add(newArrayList("d", "k"));
    expected.add(newArrayList("l"));
    expected.add(newArrayList("l", "k"));
    expected.add(newArrayList("l", "e"));
    expected.add(newArrayList("l", "e", "f"));
    expected.add(newArrayList("l", "c"));
    assertThat(callback.fragments, containsInAnyOrder(expected.toArray()));
  }

}
