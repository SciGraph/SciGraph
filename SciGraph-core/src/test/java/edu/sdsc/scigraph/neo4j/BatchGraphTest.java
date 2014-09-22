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

import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.contains;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.lucene.LuceneUtils;

public class BatchGraphTest {

  Path path;
  BatchGraph graph;
  GraphDatabaseService graphDb;
  ReadableIndex<Node> nodeIndex;
  long foo;

  @Before
  public void setup() throws IOException {
    path = Files.createTempDirectory("SciGraph-BatchTest");
    BatchInserter inserter = BatchInserters.inserter(path.toFile().getAbsolutePath());
    graph =
        new BatchGraph(inserter, CommonProperties.URI, newHashSet("prop1", "prop2"),
            newHashSet("prop1"));
    foo = graph.getNode("http://example.org/foo");
  }

  @After
  public void teardown() throws IOException {
    graphDb.shutdown();
    FileUtils.deleteDirectory(path.toFile());
  }

  GraphDatabaseService getGraphDB() {
    graph.shutdown();
    graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(path.toString());
    graphDb.beginTx();
    nodeIndex = graphDb.index().getNodeAutoIndexer().getAutoIndex();
    return graphDb;
  }

  @Test
  public void testNodeCreation() {
    GraphDatabaseService graphDb = getGraphDB();
    assertThat(size(GlobalGraphOperations.at(graphDb).getAllNodes()), is(1));
    IndexHits<Node> hits = nodeIndex.query(CommonProperties.URI + ":http\\://example.org/foo");
    assertThat(hits.getSingle().getId(), is(foo));
  }

  @Test
  public void testPropertySetting() {
    graph.addProperty(foo, "prop1", "foo");
    getGraphDB();
    IndexHits<Node> hits = nodeIndex.query("prop1:foo");
    assertThat((String)hits.getSingle().getProperty("prop1"), is("foo"));
  }

  @Test
  public void testMultiplePropertySetting() {
    graph.addProperty(foo, "prop1", "bar");
    graph.addProperty(foo, "prop1", "baz");
    getGraphDB();
    IndexHits<Node> hits = nodeIndex.query("prop1:bar");
    assertThat((String[])hits.getSingle().getProperty("prop1"), is(arrayContaining("bar", "baz")));
  }

  @Test
  public void testPropertyIndex() {
    graph.addProperty(foo, "prop1", "foo");
    getGraphDB();
    IndexHits<Node> hits = nodeIndex.query("prop1:foo");
    assertThat(hits.getSingle().getId(), is(foo));
  }

  @Test
  public void testExactPropertyIndex() {
    graph.addProperty(foo, "prop1", "foo");
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
    graph.addProperty(foo, "prop1", "foo");
    graph.addProperty(foo, "prop1", "bar");
    getGraphDB();
    IndexHits<Node> hits = nodeIndex.query("prop1:foo");
    assertThat(hits.size(), is(1));
    hits = nodeIndex.query("prop1:bar");
    assertThat(hits.size(), is(1));
  }

  @Test
  public void testMultiplePropertyNameIndex() {
    graph.addProperty(foo, "prop1", "foo");
    graph.addProperty(foo, "prop2", "bar");
    getGraphDB();
    IndexHits<Node> hits = nodeIndex.query("prop1:foo");
    assertThat(hits.size(), is(1));
    hits = nodeIndex.query("prop2:bar");
    assertThat(hits.size(), is(1));
  }

  @Test
  public void testSingleNodeLabel() {
    graph.setLabel(foo, DynamicLabel.label("foo"));
    getGraphDB();
    assertThat(graphDb.getNodeById(0).getLabels(), contains(DynamicLabel.label("foo")));
  }

  @Test
  public void testMultipleNodeLabels() {
    graph.addLabel(foo, DynamicLabel.label("foo"));
    graph.addLabel(foo, DynamicLabel.label("bar"));
    getGraphDB();
    assertThat(graphDb.getNodeById(0).getLabels(),
        hasItems(DynamicLabel.label("foo"), DynamicLabel.label("bar")));
  }

  @Test
  public void testHasRelationship() {
    long a = graph.getNode("a");
    long b = graph.getNode("b");
    RelationshipType type = DynamicRelationshipType.withName("foo");
    graph.createRelationship(a, b, type);
    assertThat(graph.hasRelationship(a, b, type), is(true));
    assertThat(graph.hasRelationship(b, a, type), is(true));
  }

  @Test
  public void testRelationshipProperty() {
    long a = graph.getNode("a");
    long b = graph.getNode("b");
    RelationshipType type = DynamicRelationshipType.withName("foo");
    long foo = graph.createRelationship(a, b, type);
    graph.setRelationshipProperty(foo, "foo", "bar");
    getGraphDB();
    Relationship rel = graphDb.getRelationshipById(foo);
    assertThat(GraphUtil.getProperty(rel, "foo", String.class).get(), is("bar"));
  }

  @Test
  public void testCreateRelationshipPairwise() {
    long a = graph.getNode("a");
    long b = graph.getNode("b");
    long c = graph.getNode("c");
    RelationshipType type = DynamicRelationshipType.withName("foo");
    graph.createRelationshipPairwise(newHashSet(a, b, c), type);
    getGraphDB();
    assertThat(size(graphDb.getNodeById(a).getRelationships(type)), is(2));
    assertThat(size(graphDb.getNodeById(b).getRelationships(type)), is(2));
    assertThat(size(graphDb.getNodeById(c).getRelationships(type)), is(2));
  }

  @Test
  public void testCollectIndexProperties() {
    System.out.println(graph.collectIndexProperties("prop1", "foo"));
    Map<String, Object> map = new HashMap<>();
    map.put("prop1", "foo");
    map.put("prop2", "bar");
    System.out.println(graph.collectIndexProperties(map));
  }
}
