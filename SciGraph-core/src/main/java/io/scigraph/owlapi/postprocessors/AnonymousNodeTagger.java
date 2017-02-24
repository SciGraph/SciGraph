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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;

import io.scigraph.owlapi.OwlLabels;

public class AnonymousNodeTagger implements Postprocessor {

  private static final Logger logger = Logger.getLogger(AnonymousNodeTagger.class.getName());
  private final GraphDatabaseService graphDb;
  private final String anonymousProperty;
  private final int batchCommitSize = 100_000;


  public AnonymousNodeTagger(String anonymousProperty, GraphDatabaseService graphDb) {
    this.anonymousProperty = anonymousProperty;
    this.graphDb = graphDb;
  }

  @Override
  public void run() {
    logger.info("Starting anonymous nodes tagger...");
    int taggedNodes = 0;
    Transaction tx = graphDb.beginTx();

    ResourceIterable<Node> allNodes = graphDb.getAllNodes();
    for (Node n : allNodes) {
      if (n.hasProperty(anonymousProperty)) {
        n.addLabel(OwlLabels.OWL_ANONYMOUS);
        taggedNodes++;
      }
      if (taggedNodes % batchCommitSize == 0) {
        tx.success();
        tx.close();
        tx = graphDb.beginTx();
      }
    }

    logger.info(taggedNodes + " nodes tagged.");
    tx.success();
    tx.close();
  }

}
