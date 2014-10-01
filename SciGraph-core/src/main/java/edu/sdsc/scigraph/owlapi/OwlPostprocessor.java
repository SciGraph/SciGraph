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

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.inject.Named;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.graphdb.traversal.Uniqueness;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.neo4j.GraphUtil;

public class OwlPostprocessor {

  private static final Logger logger = Logger.getLogger(OwlPostprocessor.class.getName());

  private final GraphDatabaseService graphDb;
  private final ExecutionEngine engine;
  private final ReadableIndex<Node> nodeIndex;

  private final Map<String, String> categoryMap;

  public OwlPostprocessor(GraphDatabaseService graphDb,
      @Named("owl.categories") Map<String, String> categoryMap) {
    this.graphDb = graphDb;
    this.categoryMap = categoryMap;
    this.nodeIndex = graphDb.index().getNodeAutoIndexer().getAutoIndex();
    engine = new ExecutionEngine(graphDb);

  }

  public void postprocess() {
    processSomeValuesFrom();
    processCategories(categoryMap);
  }

  public void processSomeValuesFrom() {
    logger.info("Processing someValuesFrom classes");
    try (Transaction tx = graphDb.beginTx()) {
      ResourceIterator<Map<String, Object>> results =
          engine.execute(
              "MATCH (n)-[relationship]->(svf:someValuesFrom)-[:property]->(p) "
                  + "RETURN n, relationship, svf, p").iterator();
      while (results.hasNext()) {
        Map<String, Object> result = results.next();
        Node subject = (Node) result.get("n");
        Relationship relationship = (Relationship) result.get("relationship");
        Node svf = (Node) result.get("svf");
        Node property = (Node) result.get("p");
        for (Relationship r : svf.getRelationships(OwlRelationships.FILLER)) {
          Node object = r.getEndNode();
          String relationshipName =
              GraphUtil.getProperty(property, CommonProperties.FRAGMENT, String.class).get();
          RelationshipType type = DynamicRelationshipType.withName(relationshipName);
          String propertyUri =
              GraphUtil.getProperty(property, CommonProperties.URI, String.class).get();
          Relationship inferred = subject.createRelationshipTo(object, type);
          inferred.setProperty(CommonProperties.URI, propertyUri);
          inferred.setProperty(CommonProperties.CONVENIENCE, true);
          inferred.setProperty(CommonProperties.OWL_TYPE, relationship.getType().name());
        }
      }
      tx.success();
    }
  }

  void processCategory(Node root, RelationshipType type, Direction direction, String category) {
    for (Path position : graphDb.traversalDescription().uniqueness(Uniqueness.NODE_GLOBAL)
        .depthFirst().relationships(type, direction).traverse(root)) {
      Node end = position.endNode();
      GraphUtil.addProperty(end, Concept.CATEGORY, category);
    }
  }

  public void processCategories(Map<String, String> categories) {
    try (Transaction tx = graphDb.beginTx()) {
      for (Entry<String, String> category : categories.entrySet()) {
        Node root = nodeIndex.get(CommonProperties.URI, category.getKey()).getSingle();
        if (null == root) {
          logger.warning("Failed to locate " + category.getKey() + " while processing categories");
        } else {
          processCategory(root, OwlRelationships.RDF_SUBCLASS_OF, Direction.INCOMING,
              category.getValue());
        }
      }
      tx.success();
    }
  }

}
