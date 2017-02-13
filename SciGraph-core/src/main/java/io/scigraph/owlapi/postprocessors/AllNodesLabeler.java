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
package io.scigraph.owlapi.postprocessors;

import java.util.logging.Logger;

import javax.inject.Inject;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;

public class AllNodesLabeler implements Postprocessor {

  private static final Logger logger = Logger.getLogger(EdgeLabeler.class.getName());
  private final GraphDatabaseService graphDb;
  private final Label label;
  private final int batchCommitSize = 100_000;

  @Inject
  public AllNodesLabeler(String label, GraphDatabaseService graphDb) {
    this.label = Label.label(label);
    this.graphDb = graphDb;
  }

  @Override
  public void run() {
    logger.info("Starting all nodes labeling...");
    int processedNodes = 0;

    Transaction tx = graphDb.beginTx();

    ResourceIterable<Node> allNodes = graphDb.getAllNodes();
    for (Node n : allNodes) {
      n.addLabel(label);
      if (processedNodes % batchCommitSize == 0) {
        tx.success();
        tx.close();
        tx = graphDb.beginTx();
      }
      processedNodes++;
    }

    logger.info(processedNodes + " nodes labeled.");
    tx.success();
    tx.close();
  }

}
