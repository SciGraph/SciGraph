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
package io.scigraph.neo4j;

import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.contains;
import io.scigraph.frames.CommonProperties;
import io.scigraph.lucene.LuceneUtils;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public class GraphBatchImplIT {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  static RelationshipType TYPE = RelationshipType.withName("foo");

  String path;
  GraphBatchImpl graph;
  GraphDatabaseService graphDb;
  ReadableIndex<Node> nodeIndex;
  long foo;

  @Before
  public void setup() throws IOException {
    path = folder.newFolder().getAbsolutePath();
    BatchInserter inserter = BatchInserters.inserter(new File(path));
    graph =
        new GraphBatchImpl(inserter, CommonProperties.IRI, newHashSet("prop1", "prop2"),
            newHashSet("prop1"), new IdMap(), new RelationshipMap());
    foo = graph.createNode("http://example.org/foo");
  }

  GraphDatabaseService getGraphDB() {
    graph.shutdown();
    graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(path));
    graphDb.beginTx();
    nodeIndex = graphDb.index().getNodeAutoIndexer().getAutoIndex();
    return graphDb;
  }

  @Test
  public void testNodeCreation() {
    GraphDatabaseService graphDb = getGraphDB();
    assertThat(size(graphDb.getAllNodes()), is(1));
    //IndexHits<Node> hits = nodeIndex.query(CommonProperties.IRI + ":http\\://example.org/foo");
    IndexHits<Node> hits = nodeIndex.query(CommonProperties.IRI + ":http\\:\\/\\/example.org\\/foo");
    assertThat(hits.getSingle().getId(), is(foo));
  }

  @Test
  public void testPropertySetting() {
    graph.addNodeProperty(foo, "prop1", "foo");
    getGraphDB();
    IndexHits<Node> hits = nodeIndex.query("prop1:foo");
    assertThat((String)hits.getSingle().getProperty("prop1"), is("foo"));
  }

  @Test
  public void testMultiplePropertySetting() {
    graph.addNodeProperty(foo, "prop1", "bar");
    graph.addNodeProperty(foo, "prop1", "baz");
    getGraphDB();
    IndexHits<Node> hits = nodeIndex.query("prop1:bar");
    assertThat((String[])hits.getSingle().getProperty("prop1"), is(arrayContaining("bar", "baz")));
  }

  @Test
  public void testPropertyIndex() {
    graph.addNodeProperty(foo, "prop1", "foo");
    getGraphDB();
    IndexHits<Node> hits = nodeIndex.query("prop1:foo");
    assertThat(hits.getSingle().getId(), is(foo));
  }

  @Test
  public void testExactPropertyIndex() {
    graph.addNodeProperty(foo, "prop1", "foo");
    getGraphDB();
    IndexHits<Node> hits = nodeIndex.query("prop1" + LuceneUtils.EXACT_SUFFIX + ":foo");
    assertThat(hits.getSingle().getId(), is(foo));
  }

  @Test
  public void testReplacePropertyIndex() {
    graph.setNodeProperty(foo, "prop1", "foo");
    graph.setNodeProperty(foo, "prop1", "bar");
    getGraphDB();
    IndexHits<Node> hits = nodeIndex.query("prop1:foo");
    assertThat(hits.size(), is(0));
    hits = nodeIndex.query("prop1:bar");
    assertThat(hits.size(), is(1));
  }

  @Test
  public void testMultiplePropertyValueIndex() {
    graph.addNodeProperty(foo, "prop1", "foo");
    graph.addNodeProperty(foo, "prop1", "bar");
    getGraphDB();
    IndexHits<Node> hits = nodeIndex.query("prop1:foo");
    assertThat(hits.size(), is(1));
    hits = nodeIndex.query("prop1:bar");
    assertThat(hits.size(), is(1));
  }

  @Test
  public void testMultiplePropertyNameIndex() {
    graph.addNodeProperty(foo, "prop1", "foo");
    graph.addNodeProperty(foo, "prop2", "bar");
    getGraphDB();
    IndexHits<Node> hits = nodeIndex.query("prop1:foo");
    assertThat(hits.size(), is(1));
    hits = nodeIndex.query("prop2:bar");
    assertThat(hits.size(), is(1));
  }

  @Test
  public void testSingleNodeLabel() {
    graph.setLabel(foo, Label.label("foo"));
    getGraphDB();
    assertThat(graphDb.getNodeById(0).getLabels(), contains(Label.label("foo")));
  }

  @Test
  public void testMultipleNodeLabels() {
    graph.addLabel(foo, Label.label("foo"));
    graph.addLabel(foo, Label.label("bar"));
    getGraphDB();
    assertThat(graphDb.getNodeById(0).getLabels(),
        hasItems(Label.label("foo"), Label.label("bar")));
  }

  @Test
  public void testHasRelationship() {
    long a = graph.createNode("a");
    long b = graph.createNode("b");
    graph.createRelationship(a, b, TYPE);
    assertThat(graph.getRelationship(a, b, TYPE).isPresent(), is(true));
    assertThat(graph.getRelationship(b, a, TYPE).isPresent(), is(false));
  }

  @Test
  public void testRelationshipProperty() {
    long a = graph.createNode("a");
    long b = graph.createNode("b");
    long foo = graph.createRelationship(a, b, TYPE);
    graph.setRelationshipProperty(foo, "foo", "bar");
    getGraphDB();
    Relationship rel = graphDb.getRelationshipById(foo);
    assertThat(GraphUtil.getProperty(rel, "foo", String.class).get(), is("bar"));
  }

  @Test
  public void testCreateRelationshipPairwise() {
    long a = graph.createNode("a");
    long b = graph.createNode("b");
    long c = graph.createNode("c");
    graph.createRelationshipsPairwise(newHashSet(a, b, c), TYPE);
    getGraphDB();
    assertThat(size(graphDb.getNodeById(a).getRelationships(TYPE)), is(2));
    assertThat(size(graphDb.getNodeById(b).getRelationships(TYPE)), is(2));
    assertThat(size(graphDb.getNodeById(c).getRelationships(TYPE)), is(2));
  }

}
