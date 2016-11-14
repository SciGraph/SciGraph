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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import io.scigraph.neo4j.Graph;
import io.scigraph.neo4j.GraphBatchImpl;
import io.scigraph.neo4j.IdMap;
import io.scigraph.neo4j.RelationshipMap;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public class GraphBatchImplMultipleLoadTest {

  private static final RelationshipType TYPE = RelationshipType.withName("type");

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  
  String path;
  GraphDatabaseService graphDb;
  ReadableIndex<Node> nodeIndex;
  DB maker;

  @Before
  public void setup() throws IOException {
    path = folder.newFolder().getAbsolutePath();
    maker = DBMaker.newMemoryDB().make();
  }

  Graph getBatchGraph() throws IOException {
    BatchInserter inserter = BatchInserters.inserter(new File(path));
    return new GraphBatchImpl(inserter, "uri", Collections.<String>emptySet(), Collections.<String>emptySet(),
        new IdMap(maker), new RelationshipMap(maker));
  }

  GraphDatabaseService getGraphDB() {
    graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(path));
    return graphDb;
  }

  @Test
  public void testMultipleInserts() throws IOException {
    Graph batchGraph = getBatchGraph();
    long a = batchGraph.createNode("a");
    long b = batchGraph.createNode("b");
    batchGraph.createRelationship(a, b, TYPE);
    batchGraph.shutdown();
    batchGraph = getBatchGraph();
    a = batchGraph.createNode("a");
    long c = batchGraph.createNode("c");
    batchGraph.createRelationship(a, c, TYPE);
    batchGraph.shutdown();
    GraphDatabaseService graphDb = getGraphDB();
    try (Transaction tx = graphDb.beginTx()) {
      Iterable<Node> nodes = graphDb.getAllNodes();
      assertThat(size(nodes), is(3));
      Iterable<Relationship> relationships = graphDb.getAllRelationships();
      assertThat(size(relationships), is(2));
      tx.success();
    }
  }

}
