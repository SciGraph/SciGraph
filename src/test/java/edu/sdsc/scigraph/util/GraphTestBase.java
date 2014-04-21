/**
 * Copyright (C) 2014 Christopher Condit (condit@sdsc.edu)
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
package edu.sdsc.scigraph.util;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

public class GraphTestBase {

  protected static GraphDatabaseService graphDb;
  Transaction tx;

  @BeforeClass
  public static void setupDb() {
    graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
  }

  @AfterClass
  public static void shutdownDb() {
    graphDb.shutdown();
  }

  @Before
  public void setUp() throws Exception {
    tx = graphDb.beginTx();
  }

  @After
  public void teardown() throws Exception {
    tx.success();
    tx.finish();
    tx = null;
    cleanDatabase();
  }

  void cleanDatabase() {
    Transaction tx = graphDb.beginTx();
    for (Relationship relationship: GlobalGraphOperations.at(graphDb).getAllRelationships()) {
      relationship.delete();
    }
    for (Node node: GlobalGraphOperations.at(graphDb).getAllNodes()) {
      node.delete();
    }
    tx.success();
    tx.finish();
  }

}
