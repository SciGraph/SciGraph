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

import io.scigraph.frames.Concept;
import io.scigraph.neo4j.GraphUtil;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Uniqueness;

class CategoryProcessor implements Callable<Long> {

  private static final Logger logger = Logger.getLogger(CategoryProcessor.class.getName());

  private final GraphDatabaseService graphDb;
  private final Node root;
  private final String category;

  CategoryProcessor(GraphDatabaseService graphDb, Node root, String category) {
    this.graphDb = graphDb;
    this.root = root;
    this.category = category;
    Thread.currentThread().setName("category processor - " + category);
  }

  @Override
  public Long call() throws Exception {
    long count = 0;
    int batchSize = 10;
    Label label = DynamicLabel.label(category);
    logger.info("Processsing " + category);
    Transaction tx = graphDb.beginTx();
    for (Path position : graphDb.traversalDescription().uniqueness(Uniqueness.NODE_GLOBAL)
        .depthFirst().relationships(OwlRelationships.RDFS_SUBCLASS_OF, Direction.INCOMING)
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
    logger.info("Processsed " + count + " nodes for " + category);
    return count;
  }

}