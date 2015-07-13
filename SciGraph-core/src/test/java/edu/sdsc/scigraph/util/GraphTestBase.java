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

import static com.google.common.collect.Sets.newHashSet;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.internal.CypherUtil;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.neo4j.GraphTransactionalImpl;
import edu.sdsc.scigraph.neo4j.Neo4jConfiguration;
import edu.sdsc.scigraph.neo4j.Neo4jModule;
import edu.sdsc.scigraph.neo4j.RelationshipMap;
import edu.sdsc.scigraph.owlapi.OwlLabels;
import edu.sdsc.scigraph.owlapi.curies.CurieUtil;

public class GraphTestBase {

  protected static GraphDatabaseService graphDb;
  protected static CypherUtil cypherUtil;
  protected static Graph graph;
  static ConcurrentHashMap<String, Long> idMap = new ConcurrentHashMap<>();

  Transaction tx;

  static protected Node createNode(String iri) {
    long node = graph.createNode(iri);
    graph.setNodeProperty(node, CommonProperties.IRI, iri);
    if (iri.startsWith("_:")) {
      graph.addLabel(node, OwlLabels.OWL_ANONYMOUS);
    }
    return graphDb.getNodeById(node);
  }

  static protected Relationship addRelationship(String parentIri, String childIri, RelationshipType type) {
    Node parent = createNode(parentIri);
    Node child = createNode(childIri);
    return child.createRelationshipTo(parent, type);
  }

  @BeforeClass
  public static void setupDb() {
    graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
    Neo4jConfiguration config = new Neo4jConfiguration();
    config.getExactNodeProperties().addAll(newHashSet(
        NodeProperties.LABEL,
        Concept.SYNONYM,
        Concept.ABREVIATION,
        Concept.ACRONYM));
    config.getIndexedNodeProperties().addAll(newHashSet(
        NodeProperties.LABEL,
        Concept.CATEGORY, Concept.SYNONYM,
        Concept.ABREVIATION,
        Concept.ACRONYM));
    Neo4jModule.setupAutoIndexing(graphDb, config);
    graph = new GraphTransactionalImpl(graphDb, idMap, new RelationshipMap());
    cypherUtil = new CypherUtil(graphDb, new CurieUtil(Collections.<String, String>emptyMap()));
  }

  @AfterClass
  public static void shutdown() {
    graphDb.shutdown();
  }

  @Before
  public void startTransaction() {
    tx = graphDb.beginTx();
  }

  @After
  public void failTransaction() {
    idMap.clear();
    tx.failure();
    tx.close();
  }

}
