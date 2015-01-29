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
package edu.sdsc.scigraph.neo4j;

import java.util.Collection;
import java.util.List;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;

import com.google.common.base.Optional;

public interface Graph {

  /***
   * Create a node
   * 
   * @param id The String ID of the node
   * @return The Neo4j ID of a newly created node or the ID of an existing node
   */
  long createNode(String id);

  /***
   * Get a node
   * 
   * @param id
   * @return An optional Neo4j node ID with this ID
   */
  Optional<Long> getNode(String id);

  /***
   * Create a relationship
   * 
   * @param start The ID of the start node 
   * @param end The ID of the end node
   * @param type The type of relationship
   * @return The Neo4j ID of a newly created relationship or the ID of an existing relationship
   */
  long createRelationship(long start, long end, RelationshipType type);

  /***
   * Get a relationship
   * 
   * @param start The ID of the start node
   * @param end The ID of the end node
   * @param type The type of relationship
   * @return An optional Neo4j relationship ID
   */
  Optional<Long> getRelationship(long start, long end, RelationshipType type);

  /***
   * Creates relationships pairwise.
   * 
   * This assumes that the relationships type is undirected in your domain. 
   * 
   * @param nodeIds The Neo4j IDs of the nodes
   * @param type The type of relationship
   * @return The Neo4j IDs of the relationships involved
   */
  Collection<Long> createRelationshipsPairwise(Collection<Long> nodeIds, RelationshipType type);

  /***
   * Set a node property
   * 
   * This will erase a previous value.
   * 
   * @param node The Neo4j node ID
   * @param property The property key
   * @param value The property value
   */
  void setNodeProperty(long node, String property, Object value);

  /***
   * Add a node property
   * 
   * This will append the value to a list
   * 
   * @param node The Neo4j node ID
   * @param property The property key
   * @param value The property value
   */
  void addNodeProperty(long node, String property, Object value);

  /***
   * Get a node property
   * 
   * @param node The Neo4j node ID
   * @param property The property key
   * @param type The expected type of the property value
   * @return An optional single valued value of the property
   */
  <T> Optional<T> getNodeProperty(long node, String property, Class<T> type);

  /***
   * Get node properties
   * 
   * @param node The Neo4j node ID
   * @param property The property key
   * @param type The expected type of the property values
   * @return A list (possibly empty) with the values of the property
   */
  <T> List<T> getNodeProperties(long node, String property, Class<T> type);

  /***
   * Set a relationship property
   * 
   * This will erase a previous value.
   * 
   * @param relationship The Neo4j relationship ID
   * @param property The property key
   * @param value The property value
   */
  void setRelationshipProperty(long relationship, String property, Object value);

  /***
   * Add a relationship property
   * 
   * This will append the value to a list
   * 
   * @param relationship The Neo4j relationship ID
   * @param property The property key
   * @param value The property value
   */
  void addRelationshipProperty(long relationship, String property, Object value);

  /***
   * Get a relationship property
   * 
   * @param relationship The Neo4j relationship ID
   * @param property The property key
   * @param type The expected type of the property value
   * @return An optional single valued value of the property
   */
  <T> Optional<T> getRelationshipProperty(long relationship, String property, Class<T> type);

  /***
   * Get relationship properties
   * 
   * @param relationship The Neo4j relationship ID
   * @param property The property key
   * @param type The expected type of the property values
   * @return A list (possibly empty) with the values of the property
   */
  <T> List<T> getRelationshipProperties(long relationship, String property, Class<T> type);

  /***
   * Set a label on a node
   * 
   * @param node The Neo4j node ID
   * @param label The label to set
   */
  void setLabel(long node, Label label);

  /***
   * Add a label to a node
   * 
   * @param node The Neo4j node ID
   * @param label The label to add
   */
  void addLabel(long node, Label label);

  /***
   * Get the labels of a node
   * 
   * @param node The Neo4j node ID
   * @return the labels
   */
  Collection<Label> getLabels(long node);

  /***
   * Perform any implementation specific graph cleanup
   */
  void shutdown();
}
