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
package edu.sdsc.scigraph.owlapi.loader;

import java.io.File;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.visualization.graphviz.GraphvizWriter;
import org.neo4j.walk.Walker;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import edu.sdsc.scigraph.neo4j.Neo4jConfiguration;
import edu.sdsc.scigraph.owlapi.loader.OwlLoadConfiguration.OntologySetup;

public class BatchOwlLoaderIT {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

  static Server server = new Server(10000);

  @BeforeClass
  public static void setup() throws Exception {
    ResourceHandler handler = new ResourceHandler();
    handler.setBaseResource(Resource.newClassPathResource("/ontologies/import/"));
    server.setHandler(handler);
    server.start();
  }

  @AfterClass
  public static void teardown() throws Exception {
    server.stop();
  }

  @Test
  public void test() throws Exception {
    OwlLoadConfiguration config = new OwlLoadConfiguration();
    Neo4jConfiguration neo4jConfig = new Neo4jConfiguration();
    neo4jConfig.setLocation(folder.getRoot().getAbsolutePath());
    config.setGraphConfiguration(neo4jConfig);
    OntologySetup ontSetup = new OntologySetup();
    ontSetup.setUrl("http://127.0.0.1:10000/main.owl");
    config.getOntologies().add(ontSetup);
    BatchOwlLoader.load(config);

    GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(folder.getRoot().toString());
    graphDb.beginTx();
    GraphvizWriter writer = new GraphvizWriter();
    Walker walker = Walker.fullGraph(graphDb);
    writer.emit(new File("/tmp/test.dot"), walker);
  }


}
