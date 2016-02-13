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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Uniqueness;

class CategoryProcessor implements Callable<Map<String, Set<Long>>> {

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
  public Map<String, Set<Long>> call() throws Exception {
    logger.info("Processsing " + category);
    Map<String, Set<Long>> map = new HashMap<String, Set<Long>>();
    Set<Long> nodeSet = new HashSet<Long>();
    Transaction tx = graphDb.beginTx();
    try {
      for (Path position : graphDb.traversalDescription().uniqueness(Uniqueness.NODE_GLOBAL).depthFirst()
          .relationships(OwlRelationships.RDFS_SUBCLASS_OF, Direction.INCOMING).relationships(OwlRelationships.RDF_TYPE, Direction.INCOMING)
          .relationships(OwlRelationships.OWL_EQUIVALENT_CLASS, Direction.BOTH).relationships(OwlRelationships.OWL_SAME_AS, Direction.BOTH)
          .traverse(root)) {
        Node end = position.endNode();
        nodeSet.add(end.getId());
      }
      logger.info("Discovered " + nodeSet.size() + " nodes for " + category);
      map.put(category, nodeSet);
    } catch (Exception e) {
      logger.warning("IRI not found for category: " + category);
    } finally {
      tx.success();
      tx.close();
    }
    return map;
  }

}
