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
package edu.sdsc.scigraph.util;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.test.TestGraphDatabaseFactory;

import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.neo4j.GraphTransactionalImpl;
import edu.sdsc.scigraph.neo4j.Neo4jModule;
import edu.sdsc.scigraph.neo4j.RelationshipMap;
import edu.sdsc.scigraph.owlapi.OwlApiUtils;

public class GraphTestBase {

  protected GraphDatabaseService graphDb;
  protected Graph graph;

  protected Node createNode(String uri) {
    long node = graph.createNode(uri);
    graph.setNodeProperty(node, "uri", uri);
    graph.setNodeProperty(node, "fragment", OwlApiUtils.getIri(uri).getFragment());
    return graphDb.getNodeById(node);
  }

  @Before
  public void setupDb() {
    graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
    Neo4jModule.setupAutoIndexing(graphDb);
    graph = new GraphTransactionalImpl(graphDb, new ConcurrentHashMap<String, Long>(), new RelationshipMap());
  }

  @Before
  public void setUp() throws Exception {
    graphDb.beginTx();
  }

}
