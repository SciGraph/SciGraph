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
package io.scigraph.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

public class GraphDump {

  public static void dumpProperties(PropertyContainer container) {
    for (String key: container.getPropertyKeys()) {
      System.out.println(key + ": " + container.getProperty(key));
    }
  }

  public static void dumpNode(Vertex node) {
    System.out.println(String.format("%s", node.getId()));
    dumpProperties(node);
  }

  public static void dumpRelationship(Relationship relationship) {
    System.out.println(String.format("%d [%d->%d] (%s)", 
        relationship.getId(), relationship.getStartNode().getId(),
        relationship.getEndNode().getId(),
        relationship.getType().name()));
    dumpProperties(relationship);
  }

  public static void dumpGraph(com.tinkerpop.blueprints.Graph graphDb) {
    for (Vertex node: graphDb.getVertices()) {
      dumpNode(node);
    }
    for (Edge relationship: graphDb.getEdges()) {
      dumpRelationship(relationship);
    }
  }

  public static void dumpProperties(Element container) {
    for (String key: container.getPropertyKeys()) {
      System.out.println(key + ": " + container.getProperty(key));
    }
  }

  public static void dumpNode(Node node) {
    System.out.println(String.format("%d (%s)", node.getId(), Iterables.toString(node.getLabels())));
    dumpProperties(node);
  }

  public static void dumpRelationship(Edge relationship) {
    System.out.println(String.format("%s [%s->%s] (%s)", 
        relationship.getId(), relationship.getVertex(Direction.OUT).getId(),
        relationship.getVertex(Direction.IN).getId(),
        relationship.getLabel()));
    dumpProperties(relationship);
  }

  public static void dumpGraph(GraphDatabaseService graphDb) {
    for (Node node: graphDb.getAllNodes()) {
      dumpNode(node);
    }
    for (Relationship relationship: graphDb.getAllRelationships()) {
      dumpRelationship(relationship);
    }
  }


}
