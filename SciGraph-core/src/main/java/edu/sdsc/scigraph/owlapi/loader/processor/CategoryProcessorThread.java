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

import java.util.Collection;
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.owlapi.OwlRelationships;

class CategoryProcessorThread implements Callable<Long> {

  private static final Logger logger = Logger.getLogger(CategoryProcessorThread.class.getName());

  private final GraphDatabaseService graphDb;
  private final Node root;
  private final Collection<String> categories;
  private final Collection<Label> labels;
  private final int batchSize;

  CategoryProcessorThread(GraphDatabaseService graphDb, Node root, Collection<String> categories, int batchSize) {
    this.graphDb = graphDb;
    this.root = root;
    this.categories = ImmutableSet.copyOf(categories);
    this.batchSize = batchSize;
    Builder<Label> builder = ImmutableSet.builder();
    for (String category: categories) {
      builder.add(DynamicLabel.label(category));
    }
    labels = builder.build();
  }

  @Override
  public Long call() throws Exception {
    Thread.currentThread().setName("category processor - " + categories);
    long count = 0;
    Transaction tx = graphDb.beginTx();
    for (Path position : graphDb.traversalDescription()
        .uniqueness(Uniqueness.NODE_GLOBAL)
        .depthFirst()
        .relationships(OwlRelationships.RDFS_SUBCLASS_OF, Direction.INCOMING)
        .relationships(OwlRelationships.RDF_TYPE, Direction.INCOMING)
        .relationships(OwlRelationships.OWL_EQUIVALENT_CLASS, Direction.BOTH)
        .traverse(root)) {
      Node end = position.endNode();
      for (String category: categories) {
        GraphUtil.addProperty(end, Concept.CATEGORY, category);
      }
      for (Label label: labels) {
        end.addLabel(label);
      }
      if (0 == ++count % batchSize) {
        logger.fine("Commiting " + count);
        tx.success();
        tx.close();
        tx = graphDb.beginTx();
      }
    }
    tx.success();
    tx.close();
    logger.info("Processsed " + count + " nodes for " + categories);
    return count;
  }

}