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

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

public class CategoryLabeler implements Callable<Boolean> {

  private final GraphDatabaseService graphDb;
  private final Node node;
  private final String category;

  public CategoryLabeler(GraphDatabaseService graphDb, Node node, String category) {
    this.graphDb = graphDb;
    this.node = node;
    this.category = category;
  }

  @Override
  public Boolean call() throws Exception {
    Label label = DynamicLabel.label(category);
    try (Transaction tx = graphDb.beginTx()) {
      GraphUtil.addProperty(node, Concept.CATEGORY, category);
      node.addLabel(label);
      tx.success();
      tx.close();
    }
    return true;
  }

}
