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

import io.scigraph.frames.CommonProperties;
import io.scigraph.frames.Concept;
import io.scigraph.neo4j.GraphUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Named;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.graphdb.traversal.Uniqueness;

public class OwlPostprocessor {

  private static final Logger logger = Logger.getLogger(OwlPostprocessor.class.getName());

  private final GraphDatabaseService graphDb;

  private final Map<String, String> categoryMap;

  public OwlPostprocessor(GraphDatabaseService graphDb,
      @Named("owl.categories") Map<String, String> categoryMap) {
    this.graphDb = graphDb;
    this.categoryMap = categoryMap;
  }

  public void postprocess() {
    processSomeValuesFrom();
    processCategories(categoryMap);
  }

  public void processSomeValuesFrom() {
    logger.info("Processing someValuesFrom classes");
    try (Transaction tx = graphDb.beginTx()) {
      Result results =
          graphDb.execute(
              "MATCH (n)-[relationship]->(svf:someValuesFrom)-[:property]->(p) "
                  + "RETURN n, relationship, svf, p");
      while (results.hasNext()) {
        Map<String, Object> result = results.next();
        Node subject = (Node) result.get("n");
        Relationship relationship = (Relationship) result.get("relationship");
        Node svf = (Node) result.get("svf");
        Node property = (Node) result.get("p");
        for (Relationship r : svf.getRelationships(OwlRelationships.FILLER)) {
          Node object = r.getEndNode();
          String relationshipName = GraphUtil.getProperty(property, CommonProperties.IRI, String.class).get();
          RelationshipType type = DynamicRelationshipType.withName(relationshipName);
          String propertyUri =
              GraphUtil.getProperty(property, CommonProperties.IRI, String.class).get();
          Relationship inferred = subject.createRelationshipTo(object, type);
          inferred.setProperty(CommonProperties.IRI, propertyUri);
          inferred.setProperty(CommonProperties.CONVENIENCE, true);
          inferred.setProperty(CommonProperties.OWL_TYPE, relationship.getType().name());
        }
      }
      tx.success();
    }
  }

  long processCategory(Node root, RelationshipType type, Direction direction, String category) {
    long count = 0;
    int batchSize = 100_000;
    Label label = DynamicLabel.label(category);
    Transaction tx = graphDb.beginTx();
    for (Path position : graphDb.traversalDescription().uniqueness(Uniqueness.NODE_GLOBAL)
        .depthFirst().relationships(type, direction)
        .relationships(OwlRelationships.RDF_TYPE, Direction.INCOMING)
        .relationships(OwlRelationships.OWL_EQUIVALENT_CLASS, Direction.BOTH).traverse(root)) {
      Node end = position.endNode();
      GraphUtil.addProperty(end, Concept.CATEGORY, category);
      end.addLabel(label);
      if (0 == ++count % batchSize) {
        logger.fine("Commiting " + count);
        tx.success();
        tx.close();
        tx = graphDb.beginTx();
      }
    }
    tx.success();
    tx.close();
    return count;
  }

  public void processCategories(Map<String, String> categories) {
    logger.info("Processing categories");
    for (Entry<String, String> category : categories.entrySet()) {
      Set<Node> roots = new HashSet<>();
      try (Transaction tx = graphDb.beginTx()) {
        ReadableIndex<Node> nodeIndex = graphDb.index().getNodeAutoIndexer().getAutoIndex();
        Node root = nodeIndex.get(CommonProperties.IRI, category.getKey()).getSingle();
        if (null == root) {
          logger.warning("Failed to locate " + category.getKey() + " while processing categories");
          continue;
        }
        roots.add(root);
        for (Relationship equiv: root.getRelationships(OwlRelationships.OWL_EQUIVALENT_CLASS)) {
          roots.add(equiv.getOtherNode(root));
        }
        tx.success();
      }
      if (roots.isEmpty()) {
        logger.warning("Failed to locate " + category.getKey() + " while processing categories");
      } else {
        for (Node root: roots) {
          logger.info("Processing category: " + category);
          long count = processCategory(root, OwlRelationships.RDFS_SUBCLASS_OF, Direction.INCOMING,
              category.getValue());
          logger.info("Processsed " + count + " nodes for " + category);
        }
      }
    }
  }

}
