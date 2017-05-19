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

import java.util.Collection;
import java.util.Optional;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;


/***
 * Abstract methods for dealing with an underlying graph.
 * 
 * <p><em>NOTE:</em> the ID of nodes and relationships is assumed to be {@link Long}
 * 
 */
public interface Graph {

  /***
   * Create a node
   * <p>
   * This method is idempotent.
   * 
   * @param id the string ID of the node
   * @return the internal ID of a the node
   */
  long createNode(String id);

  /***
   * Get a node
   * 
   * @param id
   * @return an {@link Optional} node ID with this ID
   */
  Optional<Long> getNode(String id);

  /***
   * Create a relationship
   * 
   * @param start The ID of the start node 
   * @param end The ID of the end node
   * @param type The type of relationship
   * @return The ID of a newly created relationship or the ID of an existing relationship
   */
  long createRelationship(long start, long end, RelationshipType type);

  /***
   * Get a relationship
   * 
   * @param start The ID of the start node
   * @param end The ID of the end node
   * @param type The type of relationship
   * @return An optional relationship ID
   */
  Optional<Long> getRelationship(long start, long end, RelationshipType type);

  /***
   * Creates relationships pairwise.
   * 
   * This assumes that the relationships type is undirected in your domain. 
   * 
   * @param nodeIds The IDs of the nodes
   * @param type The type of relationship
   * @return The IDs of the relationships involved
   */
  Collection<Long> createRelationshipsPairwise(Collection<Long> nodeIds, RelationshipType type);

  /***
   * Set a node property
   * 
   * This will erase a previous value.
   * 
   * @param node The node ID
   * @param property The property key
   * @param value The property value
   */
  void setNodeProperty(long node, String property, Object value);

  /***
   * Add a node property
   * 
   * This will append the value to a list
   * 
   * @param node The node ID
   * @param property The property key
   * @param value The property value
   */
  void addNodeProperty(long node, String property, Object value);

  /***
   * Get a node property
   * 
   * @param node The node ID
   * @param property The property key
   * @param type The expected type of the property value
   * @return An optional single valued value of the property
   */
  <T> Optional<T> getNodeProperty(long node, String property, Class<T> type);

  /***
   * Get node properties
   * 
   * @param node The node ID
   * @param property The property key
   * @param type The expected type of the property values
   * @return A collection (possibly empty) with the values of the property
   */
  <T> Collection<T> getNodeProperties(long node, String property, Class<T> type);

  /***
   * Set a relationship property
   * 
   * This will erase a previous value.
   * 
   * @param relationship The relationship ID
   * @param property The property key
   * @param value The property value
   */
  void setRelationshipProperty(long relationship, String property, Object value);

  /***
   * Add a relationship property
   * 
   * This will append the value to a list
   * 
   * @param relationship The relationship ID
   * @param property The property key
   * @param value The property value
   */
  void addRelationshipProperty(long relationship, String property, Object value);

  /***
   * Get a relationship property
   * 
   * @param relationship The relationship ID
   * @param property The property key
   * @param type The expected type of the property value
   * @return An optional single valued value of the property
   */
  <T> Optional<T> getRelationshipProperty(long relationship, String property, Class<T> type);

  /***
   * Get relationship properties
   * 
   * @param relationship The relationship ID
   * @param property The property key
   * @param type The expected type of the property values
   * @return A collection (possibly empty) with the values of the property
   */
  <T> Collection<T> getRelationshipProperties(long relationship, String property, Class<T> type);

  /***
   * Set a label on a node
   * 
   * @param node The node ID
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
   * @param node The node ID
   * @return the labels
   */
  Collection<Label> getLabels(long node);

  /***
   * Perform any implementation specific graph cleanup
   */
  void shutdown();
}
