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

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserters;

/***
 * Verify some assumptions about how Neo4j handles indexing.
 */
public class Neo4jIndexingTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  
  String path;
  GraphDatabaseService graphDb;

  @Before
  public void setup() throws IOException {
    path = folder.newFolder().getAbsolutePath();
  }

  GraphDatabaseService getGraphDb() {
    return new GraphDatabaseFactory().newEmbeddedDatabase(new File(path));
  }

  void addIndexing() {
    graphDb.index().getNodeAutoIndexer().startAutoIndexingProperty("foo");
    graphDb.index().getNodeAutoIndexer().setEnabled(true);
  }

  @Test
  public void testAddingIndexAfterGraphCreation() {
    graphDb = getGraphDb();
    try (Transaction tx = graphDb.beginTx()) {
      Node node = graphDb.createNode();
      node.setProperty("foo", "bar");
      tx.success();
    }
    graphDb.shutdown();
    graphDb = getGraphDb();
    addIndexing();
    try (Transaction tx = graphDb.beginTx()) {
      Node node = graphDb.createNode();
      node.setProperty("foo", "baz");
      assertThat("Values added prior to enabling index are not in the index", graphDb.index()
          .getNodeAutoIndexer().getAutoIndex().get("foo", "bar").getSingle(), is(nullValue()));
      assertThat("Values added after enabling index are in the index", graphDb.index()
          .getNodeAutoIndexer().getAutoIndex().get("foo", "baz").getSingle(), is(node));
      tx.success();
    }
    graphDb.shutdown();
  }

  @Test
  public void testGraphAutoIndexingStatus() {
    graphDb = getGraphDb();
    addIndexing();
    assertThat("AutoIndex is enabled", graphDb.index().getNodeAutoIndexer().isEnabled(), is(true));
    assertThat("Properties are indexed", graphDb.index().getNodeAutoIndexer()
        .getAutoIndexedProperties(), contains("foo"));
    try (Transaction tx = graphDb.beginTx()) {
      Node node = graphDb.createNode();
      node.setProperty("foo", "bar");
      assertThat("Values added after enabling index are in the index", graphDb.index()
          .getNodeAutoIndexer().getAutoIndex().get("foo", "bar").getSingle(), is(node));
      tx.success();
    }
    graphDb.shutdown();
    graphDb = getGraphDb();
    assertThat("AutoIndex is not enabled after reopening the graph", graphDb.index()
        .getNodeAutoIndexer().isEnabled(), is(false));
    assertThat("AutoIndexed properties are not maintained after closing the graph", graphDb.index()
        .getNodeAutoIndexer().getAutoIndexedProperties(), is(empty()));
    graphDb.shutdown();
  }

  @Test
  public void testBatchIndexToAutoIndex() throws IOException {
    BatchInserter inserter = BatchInserters.inserter(new File(path));
    BatchInserterIndexProvider indexProvider = new LuceneBatchInserterIndexProvider(inserter);
    BatchInserterIndex index =
        indexProvider.nodeIndex("node_auto_index", MapUtil.stringMap("type", "exact"));
    long node = inserter.createNode(MapUtil.map("foo", "bar"));
    index.add(node, MapUtil.map("foo", "bar"));
    index.flush();
    assertThat("Batch indexed node can be retrieved", index.get("foo", "bar").next(), is(node));
    indexProvider.shutdown();
    inserter.shutdown();
    graphDb = getGraphDb();
    try (Transaction tx = graphDb.beginTx()) {
      assertThat("AutoIndex is not enabled after reopening the graph", graphDb.index()
          .getNodeAutoIndexer().isEnabled(), is(false));
      assertThat("AutoIndexed properties are not maintained after closing the graph", graphDb
          .index().getNodeAutoIndexer().getAutoIndexedProperties(), is(empty()));
      assertThat("Batch index properties are in the index", graphDb.index().getNodeAutoIndexer()
          .getAutoIndex().query("foo", "bar").size(), is(1));
      tx.success();
    }
  }

}
