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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import com.google.common.base.Function;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.neo4j.HierarchyVisitor.Callback;
import edu.sdsc.scigraph.owlapi.OwlRelationships;
import edu.sdsc.scigraph.util.GraphTestBase;

public class HierarchyVisitorTest extends GraphTestBase {

  GraphInterface graph;
  long a, b, c, d, e, f, g, h, i, j;

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

  long createNode(String id) {
    long node = graph.createNode("http://example.org/" + id);
    graph.setNodeProperty(node, CommonProperties.FRAGMENT, id);
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
    graph = new GraphInterfaceTransactionImpl(graphDb, new ConcurrentHashMap<String, Long>(), new RelationshipMap());
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
    graph.createRelationship(b, a, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.createRelationship(c, a, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.createRelationship(e, d, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.createRelationship(f, e, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.createRelationship(e, c, OwlRelationships.OWL_EQUIVALENT_CLASS);

    graph.createRelationship(h, g, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.createRelationship(i, g, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.createRelationship(j, h, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.createRelationship(j, i, OwlRelationships.RDFS_SUBCLASS_OF);
  }

  @Test
  public void testGetRootNodesWithProvidedRoot() {
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graphDb, OwlRelationships.RDFS_SUBCLASS_OF, emptyCallback).rootUris(graphDb.getNodeById(a)).build();
    assertThat(visitor.getRootNodes(), contains(graphDb.getNodeById(a)));
  }

  @Test
  public void testGetRootNodesWithProvidedRoots() {
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graphDb, OwlRelationships.RDFS_SUBCLASS_OF, emptyCallback).
        rootUris(graphDb.getNodeById(a), graphDb.getNodeById(d)).build();
    assertThat(visitor.getRootNodes(), hasItems(graphDb.getNodeById(a), graphDb.getNodeById(d)));
  }

  @Test
  public void testGetRootNodesWithoutProvidedRoots() {
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graphDb, OwlRelationships.RDFS_SUBCLASS_OF, emptyCallback).build();
    assertThat(visitor.getRootNodes(), hasItems(graphDb.getNodeById(a), graphDb.getNodeById(d)));
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
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graphDb, OwlRelationships.RDFS_SUBCLASS_OF, callback).includeEquivalentClasses(false).build();
    visitor.traverse();
    assertThat(callback.fragments, containsInAnyOrder(getExpectedNonEquivalentFragments().toArray()));
  }

  @Test
  public void testEquivalentTraverse() {
    CollectingCallback callback = new CollectingCallback();
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graphDb, OwlRelationships.RDFS_SUBCLASS_OF, callback).includeEquivalentClasses(true).build();
    visitor.traverse();
    List<List<String>> expected = getExpectedNonEquivalentFragments();
    expected.add(newArrayList("a", "e"));
    expected.add(newArrayList("a", "e", "f"));
    expected.add(newArrayList("d", "c"));
    assertThat(callback.fragments, containsInAnyOrder(expected.toArray()));
  }

  @Test
  public void testMultipleEquivalences() {
    long l = createNode("l");
    long k = createNode("k");
    graph.createRelationship(k, l, OwlRelationships.RDFS_SUBCLASS_OF);
    graph.createRelationship(e, k, OwlRelationships.OWL_EQUIVALENT_CLASS);
    graph.createRelationship(c, k, OwlRelationships.OWL_EQUIVALENT_CLASS);
    CollectingCallback callback = new CollectingCallback();
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graphDb, OwlRelationships.RDFS_SUBCLASS_OF, callback).includeEquivalentClasses(true).build();
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
