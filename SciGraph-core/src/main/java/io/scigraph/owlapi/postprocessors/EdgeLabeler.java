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

import io.scigraph.frames.NodeProperties;
import io.scigraph.neo4j.GraphUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class EdgeLabeler implements Postprocessor {

  private static final Logger logger = Logger.getLogger(EdgeLabeler.class.getName());
  private final GraphDatabaseService graphDb;
  private final int batchCommitSize = 100_000;
  public static final String edgeProperty = "lbl"; // TinkerGraph edge property "label" is reserved. 

  @Inject
  public EdgeLabeler(GraphDatabaseService graphDb) {
    this.graphDb = graphDb;
  }

  @Override
  public void run() {
    logger.info("Starting edge labeling...");
    Map<String, String> map = new HashMap<String, String>();
    int processedRels = 0;

    Transaction tx = graphDb.beginTx();
    ResourceIterable<Relationship> rels = graphDb.getAllRelationships();

    for (Relationship rel : rels) {

      if (processedRels % batchCommitSize == 0) {
        tx.success();
        tx.close();
        tx = graphDb.beginTx();
      }

      String relName = rel.getType().name();
      if (map.containsKey(relName)) {
        rel.setProperty(edgeProperty, map.get(relName));
      } else {
        String relLabel = relName;
        String query = "START n = node:node_auto_index(iri='" + relName + "') match (n) return n";
        Result result = graphDb.execute(query);
        if (result.hasNext()) {
          Node n = (Node) result.next().get("n");
          if (n.hasProperty(NodeProperties.LABEL)) {
            relLabel =
                GraphUtil.getProperties(n, NodeProperties.LABEL, String.class).iterator().next();
          }
        }
        rel.setProperty(edgeProperty, relLabel);
        map.put(relName, relLabel);
      }

      processedRels++;
    }

    logger.info(processedRels + " relations labeled.");
    tx.success();
    tx.close();
  }

}
