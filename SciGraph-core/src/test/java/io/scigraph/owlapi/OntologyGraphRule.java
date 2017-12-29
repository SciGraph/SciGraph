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
package io.scigraph.owlapi;

import io.scigraph.neo4j.Graph;
import io.scigraph.neo4j.GraphTransactionalImpl;
import io.scigraph.neo4j.IdMap;
import io.scigraph.neo4j.RelationshipMap;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.MappedProperty;

import java.util.Collections;

import org.junit.rules.ExternalResource;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

public class OntologyGraphRule extends ExternalResource {

  OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
  final String ontologyLocation;
  GraphDatabaseService graphDb;

  public OntologyGraphRule(String ontologyLocation) {
    this.ontologyLocation = ontologyLocation;
  }

  @Override
  protected void before() throws Throwable {
    OwlApiUtils.loadOntology(manager, ontologyLocation);
    OWLOntologyWalker walker = new OWLOntologyWalker(manager.getOntologies());
    graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
    Graph graph = new GraphTransactionalImpl(graphDb, new IdMap(), new RelationshipMap());
    GraphOwlVisitor visitor = new GraphOwlVisitor(walker, graph, Collections.<MappedProperty>emptyList());
    walker.walkStructure(visitor);
  }

  @Override
  protected void after() {
    graphDb.shutdown();
  }

  public GraphDatabaseService getGraphDb() {
    return graphDb;
  }

}
