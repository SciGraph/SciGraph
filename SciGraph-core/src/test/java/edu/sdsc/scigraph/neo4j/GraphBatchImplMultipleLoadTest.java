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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public class GraphBatchImplMultipleLoadTest {

  private static final RelationshipType TYPE = DynamicRelationshipType.withName("type");

  Path path;
  GraphDatabaseService graphDb;
  ReadableIndex<Node> nodeIndex;
  DB maker;

  @Before
  public void setup() throws IOException {
    path = Files.createTempDirectory("SciGraph-BatchTest");
    maker = DBMaker.newMemoryDB().make();
  }

  @After
  public void teardown() throws IOException {
    // TODO: Why does this fail on Windows?
    // FileUtils.deleteDirectory(path.toFile());
  }

  Graph getBatchGraph() {
    BatchInserter inserter = BatchInserters.inserter(path.toString());
    return new GraphBatchImpl(inserter, "uri", Collections.<String>emptySet(), Collections.<String>emptySet(),
        new IdMap(maker), new RelationshipMap(maker));
  }

  GraphDatabaseService getGraphDB() {
    graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(path.toString());
    return graphDb;
  }

  @Test
  public void testMultipleInserts() {
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
      Iterable<Node> nodes = GlobalGraphOperations.at(graphDb).getAllNodes();
      assertThat(size(nodes), is(3));
      Iterable<Relationship> relationships = GlobalGraphOperations.at(graphDb).getAllRelationships();
      assertThat(size(relationships), is(2));
      tx.success();
    }
  }

}
