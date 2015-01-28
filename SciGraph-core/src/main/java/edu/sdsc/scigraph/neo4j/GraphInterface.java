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

public interface GraphInterface {

  long createNode(String id);

  Optional<Long> getNode(String id);

  long createRelationship(long start, long end, RelationshipType type);

  Optional<Long> getRelationship(long start, long end, RelationshipType type);

  Collection<Long> createRelationshipsPairwise(Collection<Long> nodeIds, RelationshipType type);

  void setNodeProperty(long node, String property, Object value);

  void addNodeProperty(long node, String property, Object value);

  <T> Optional<T> getNodeProperty(long node, String property, Class<T> type);

  <T> List<T> getNodeProperties(long node, String property, Class<T> type);

  void setRelationshipProperty(long relationship, String property, Object value);

  void addRelationshipProperty(long relationship, String property, Object value);

  <T> Optional<T> getRelationshipProperty(long relationship, String property, Class<T> type);

  <T> List<T> getRelationshipProperties(long relationship, String property, Class<T> type);

  void setLabel(long node, Label label);

  void addLabel(long node, Label label);

  Collection<Label> getLabels(long node);

  void shutdown();
}
