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
package edu.sdsc.scigraph.owlapi;

import static com.google.common.collect.Sets.newHashSet;

import java.io.File;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.neo4j.GraphTransactionalImpl;
import edu.sdsc.scigraph.neo4j.IdMap;
import edu.sdsc.scigraph.neo4j.Neo4jConfiguration;
import edu.sdsc.scigraph.neo4j.Neo4jModule;
import edu.sdsc.scigraph.neo4j.RelationshipMap;

public class GraphOwlVisitorTransactionalGraphTest extends GraphOwlVisitorTestBase<GraphTransactionalImpl> {

  @Override
  protected GraphTransactionalImpl createInstance() throws Exception {
    GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(path));
    Neo4jConfiguration config = new Neo4jConfiguration();
    config.getExactNodeProperties().addAll(newHashSet(
      NodeProperties.LABEL,
      Concept.SYNONYM,
      Concept.ABREVIATION,
      Concept.ACRONYM));
    config.getIndexedNodeProperties().addAll(newHashSet(
        NodeProperties.LABEL,
        CommonProperties.FRAGMENT,
        Concept.CATEGORY, Concept.SYNONYM,
        Concept.ABREVIATION,
        Concept.ACRONYM));
    Neo4jModule.setupAutoIndexing(graphDb, config);
    IdMap idMap = new IdMap();
    RelationshipMap relationahipMap = new RelationshipMap();
    return new GraphTransactionalImpl(graphDb, idMap, relationahipMap);
  }

}
