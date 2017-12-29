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
package io.scigraph.internal.reachability;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import io.scigraph.internal.reachability.ReachabilityIndex;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.google.common.base.Predicate;
import org.neo4j.test.rule.ImpermanentDatabaseRule;

public class ReachabilityIndexTest {

  @ClassRule
  public static ImpermanentDatabaseRule graphDb = new ImpermanentDatabaseRule();

  @ClassRule
  public static TemporaryFolder folder = new TemporaryFolder();

  static String path;


  static final RelationshipType type = RelationshipType.withName("foo");

  static ReachabilityIndex index;
  static Node a, b, c, d, e, f;

  @BeforeClass
  public static void setup() throws InterruptedException, IOException {
    path = folder.newFolder().getAbsolutePath();
    try (Transaction tx = graphDb.beginTx()) {
      a = graphDb.createNode();
      b = graphDb.createNode();
      a.createRelationshipTo(b, type);
      c = graphDb.createNode();
      a.createRelationshipTo(c, type);
      c.createRelationshipTo(a, type);

      d = graphDb.createNode();

      e = graphDb.createNode();
      f = graphDb.createNode();
      e.createRelationshipTo(f, type);
      a.createRelationshipTo(e, type);

      tx.success();
    }
    index = new ReachabilityIndex(graphDb);
    index.createIndex(new Predicate<Node>() {
      @Override
      public boolean apply(Node input) {
        return !input.equals(e);
      }
    });
  }


  @Test
  public void testEmptyGraph() {
    GraphDatabaseService testDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(path));
    new ReachabilityIndex(testDb);
    testDb.shutdown();
  }

  @Test(expected = IllegalStateException.class)
  public void testUncreatedIndex() {
    GraphDatabaseService testDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(path));
    ReachabilityIndex index = new ReachabilityIndex(testDb);
    index.canReach(a, b);
    testDb.shutdown();
  }

  @Test
  public void testSelfReachability() {
    assertThat(index.canReach(a, a), is(true));
  }

  @Test
  public void testDirectionalReachability() {
    assertThat(index.canReach(a, b), is(true));
    assertThat(index.canReach(b, a), is(false));
  }

  @Test
  public void testBidirectionalReachability() {
    assertThat(index.canReach(a, c), is(true));
    assertThat(index.canReach(b, c), is(false));
  }

  @Test
  public void testDisconnectedNode() {
    for (Node n : newArrayList(a, b, c)) {
      assertThat(index.canReach(n, d), is(false));
      assertThat(index.canReach(d, n), is(false));
    }
  }

  @Test
  public void testForbiddenNodes() {
    assertThat(index.canReach(a, f), is(false));
    assertThat(index.canReach(e, f), is(false));
    assertThat(index.canReach(a, e), is(false));
  }

  @Test
  public void testGetConnectedPairs() {
    Set<Node> src = newHashSet(a, d);
    Set<Node> dest = newHashSet(b, c);
    Set<Pair<Node, Node>> r = new HashSet<>();
    r.add(Pair.of(a, b));
    r.add(Pair.of(a, c));
    Set<Pair<Node, Node>> result = index.getConnectedPairs(src, dest);
    assertThat(result, is(r));
  }

  @Test
  public void testAllReachable() {
    Set<Node> dest = newHashSet(b, c);
    assertThat(index.allReachable(newHashSet(a), dest), is(true));
    dest.add(d);
    assertThat(index.allReachable(newHashSet(a), dest), is(false));
    Set<Node> src = newHashSet(a, c);
    assertThat(index.allReachable(src, newHashSet(c)), is(true));
    src.add(b);
    assertThat(index.allReachable(src, newHashSet(c)), is(false));
  }

}
