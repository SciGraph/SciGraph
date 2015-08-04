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
package edu.sdsc.scigraph.owlapi.loader.processor;

import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.owlapi.OwlRelationships;

class SomeValuesFromProcessor implements GraphProcessor {

  private static final String CYPHER_QUERY = 
      "MATCH (n)-[relationship]->(svf:someValuesFrom)-[:property]->(p) " + 
          "RETURN n, relationship, svf, p";

  private static final Logger logger = Logger.getLogger(SomeValuesFromProcessor.class.getName());

  private final GraphDatabaseService graphDb;

  @Inject
  public SomeValuesFromProcessor(GraphDatabaseService graphDb) {
    this.graphDb = graphDb;
  }

  @Override
  public void process() throws Exception {
    logger.info("Processing someValuesFrom axioms...");
    try (Transaction tx = graphDb.beginTx()) {
      Result results = graphDb.execute(CYPHER_QUERY);
      while (results.hasNext()) {
        Map<String, Object> result = results.next();
        Node subject = (Node) result.get("n");
        Relationship relationship = (Relationship) result.get("relationship");
        Node svf = (Node) result.get("svf");
        Node property = (Node) result.get("p");
        for (Relationship r: svf.getRelationships(OwlRelationships.FILLER)) {
          Node object = r.getEndNode();
          String relationshipName = GraphUtil.getProperty(property, CommonProperties.URI, String.class).get();
          RelationshipType type = DynamicRelationshipType.withName(relationshipName);
          Relationship inferred = subject.createRelationshipTo(object, type);
          inferred.setProperty(CommonProperties.URI, relationshipName);
          inferred.setProperty(CommonProperties.CONVENIENCE, true);
          inferred.setProperty(CommonProperties.OWL_TYPE, relationship.getType().name());
        }
      }
      tx.success();
    }
    logger.info("Finished processing someValuesFrom axioms.");
  }

}
