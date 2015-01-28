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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import edu.sdsc.scigraph.neo4j.HierarchyVisitor.Callback;
import edu.sdsc.scigraph.owlapi.OwlRelationships;
import edu.sdsc.scigraph.util.GraphTestBase;

public class HierarchyVisitorTest extends GraphTestBase {

  Node a, b, c, d, e, f, g, h, i, j;

  Callback emptyCallback = new Callback() {
    @Override
    public void processPath(List<Node> path) {}
  };

  static class CollectingCallback implements Callback {

    List<List<Node>> nodes = new ArrayList<>();

    @Override
    public void processPath(List<Node> path) {
      nodes.add(path);
    }

  };
  
  CollectingCallback callback = new CollectingCallback();

  /**********
   *   a   d l    g
   *  / \  | |   / \
   * b   c-e-k  h   i
   *       |     \ /
   *       f      j
   **********/
  @Before
  public void createNodes() {
    a = graphDb.createNode();
    b = graphDb.createNode();
    c = graphDb.createNode();
    d = graphDb.createNode();
    e = graphDb.createNode();
    f = graphDb.createNode();
    g = graphDb.createNode();
    h = graphDb.createNode();
    i = graphDb.createNode();
    j = graphDb.createNode();
    b.createRelationshipTo(a, OwlRelationships.RDFS_SUBCLASS_OF);
    c.createRelationshipTo(a, OwlRelationships.RDFS_SUBCLASS_OF);
    e.createRelationshipTo(d, OwlRelationships.RDFS_SUBCLASS_OF);
    f.createRelationshipTo(e, OwlRelationships.RDFS_SUBCLASS_OF);
    e.createRelationshipTo(c, OwlRelationships.OWL_EQUIVALENT_CLASS);

    h.createRelationshipTo(g, OwlRelationships.RDFS_SUBCLASS_OF);
    i.createRelationshipTo(g, OwlRelationships.RDFS_SUBCLASS_OF);
    j.createRelationshipTo(h, OwlRelationships.RDFS_SUBCLASS_OF);
    j.createRelationshipTo(i, OwlRelationships.RDFS_SUBCLASS_OF);
  }

  @Test
  public void testGetRootNodesWithProvidedRoot() {
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graphDb, OwlRelationships.RDFS_SUBCLASS_OF, emptyCallback).rootUris(a).build();
    assertThat(visitor.getRootNodes(), contains(a));
  }

  @Test
  public void testGetRootNodesWithProvidedRoots() {
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graphDb, OwlRelationships.RDFS_SUBCLASS_OF, emptyCallback).
        rootUris(a, d).build();
    assertThat(visitor.getRootNodes(), hasItems(a, d));
  }

  @Test
  public void testGetRootNodesWithoutProvidedRoots() {
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graphDb, OwlRelationships.RDFS_SUBCLASS_OF, emptyCallback).build();
    assertThat(visitor.getRootNodes(), hasItems(a, d));
  }

  List<List<Node>> getExpectedNonEquivalentFragments() {
    List<List<Node>> expected= new ArrayList<>();
    expected.add(newArrayList(a));
    expected.add(newArrayList(a, b));
    expected.add(newArrayList(a, c));
    expected.add(newArrayList(d));
    expected.add(newArrayList(d, e));
    expected.add(newArrayList(d, e, f));
    expected.add(newArrayList(g));
    expected.add(newArrayList(g, h));
    expected.add(newArrayList(g, i));
    expected.add(newArrayList(g, h, j));
    expected.add(newArrayList(g, i, j));
    return expected;
  }

  @Test
  public void testNonEquivalentTraverse() {
    CollectingCallback callback = new CollectingCallback();
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graphDb, OwlRelationships.RDFS_SUBCLASS_OF, callback).includeEquivalentClasses(false).build();
    visitor.traverse();
    assertThat(callback.nodes, containsInAnyOrder(getExpectedNonEquivalentFragments().toArray()));
  }

  @Test
  public void testEquivalentTraverse() {
    CollectingCallback callback = new CollectingCallback();
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graphDb, OwlRelationships.RDFS_SUBCLASS_OF, callback).includeEquivalentClasses(true).build();
    visitor.traverse();
    List<List<Node>> expected = getExpectedNonEquivalentFragments();
    expected.add(newArrayList(a, e));
    expected.add(newArrayList(a, e, f));
    expected.add(newArrayList(d, c));
    assertThat(callback.nodes, containsInAnyOrder(expected.toArray()));
  }

  @Test
  public void testMultipleEquivalences() {
    Node l = graphDb.createNode();
    Node k = graphDb.createNode();
    k.createRelationshipTo(l, OwlRelationships.RDFS_SUBCLASS_OF);
    e.createRelationshipTo(k, OwlRelationships.OWL_EQUIVALENT_CLASS);
    c.createRelationshipTo(k, OwlRelationships.OWL_EQUIVALENT_CLASS);
    HierarchyVisitor visitor = new HierarchyVisitor.Builder(graphDb, OwlRelationships.RDFS_SUBCLASS_OF, callback).includeEquivalentClasses(true).build();
    visitor.traverse();
    List<List<Node>> expected = getExpectedNonEquivalentFragments();
    expected.add(newArrayList(a, e));
    expected.add(newArrayList(a, k));
    expected.add(newArrayList(a, e, f));
    expected.add(newArrayList(d, c));
    expected.add(newArrayList(d, k));
    expected.add(newArrayList(l));
    expected.add(newArrayList(l, k));
    expected.add(newArrayList(l, e));
    expected.add(newArrayList(l, e, f));
    expected.add(newArrayList(l, c));
    assertThat(callback.nodes, containsInAnyOrder(expected.toArray()));
  }

}
