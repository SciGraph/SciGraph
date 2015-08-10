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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Collections.emptyList;
import io.scigraph.lucene.LuceneUtils;
import io.scigraph.lucene.VocabularyIndexAnalyzer;
import io.scigraph.owlapi.loader.bindings.IndicatesExactIndexedProperties;
import io.scigraph.owlapi.loader.bindings.IndicatesIndexedProperties;
import io.scigraph.owlapi.loader.bindings.IndicatesUniqueProperty;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.concurrent.ThreadSafe;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

@ThreadSafe
public class GraphBatchImpl implements Graph {

  private static final Logger logger = Logger.getLogger(GraphBatchImpl.class.getName());

  private final static Object graphLock = new Object();

  private final BatchInserter inserter;
  private final BatchInserterIndexProvider indexProvider;
  private final BatchInserterIndex nodeIndex;

  private final String uniqueProperty;

  private final Set<String> indexedProperties;
  private final Set<String> indexedExactProperties;

  private static final Map<String, String> INDEX_CONFIG = MapUtil.stringMap(IndexManager.PROVIDER,
      "lucene", "analyzer", VocabularyIndexAnalyzer.class.getName());

  private final ConcurrentMap<String, Long> idMap;
  private final RelationshipMap relationshipMap;

  // TODO: this constructor and class should not be public
  @Inject
  public GraphBatchImpl(BatchInserter inserter, @IndicatesUniqueProperty String uniqueProperty,
      @IndicatesIndexedProperties Set<String> indexedProperties,
      @IndicatesExactIndexedProperties Set<String> exactIndexedProperties,
      ConcurrentMap<String, Long> idMap, RelationshipMap relationshioMap) {
    this.inserter = inserter;
    this.idMap = idMap;
    this.relationshipMap = relationshioMap;
    indexProvider = new LuceneBatchInserterIndexProvider(inserter);
    nodeIndex = indexProvider.nodeIndex("node_auto_index", INDEX_CONFIG);
    this.uniqueProperty = uniqueProperty;
    this.indexedProperties = newHashSet(indexedProperties);
    this.indexedProperties.add(uniqueProperty);
    this.indexedExactProperties = newHashSet(exactIndexedProperties);
  }

  @Override
  public long createNode(String id) {
    synchronized(graphLock) {
      if (!idMap.containsKey(id)) {
        long nodeId = inserter.createNode(Collections.<String, Object>emptyMap());
        idMap.put(id, nodeId);
        setNodeProperty(nodeId, uniqueProperty, id);
      }
      return idMap.get(id);
    }
  }

  @Override
  public Optional<Long> getNode(String id) {
    if (idMap.containsKey(id)) {
      return Optional.of(idMap.get(id));
    } else {
      return Optional.absent();
    }
  }

  @Override
  public long createRelationship(long start, long end, RelationshipType type) {
    synchronized (graphLock) {
      if (!relationshipMap.containsKey(start, end, type)) {
        long relationshipId =
            inserter.createRelationship(start, end, type, Collections.<String, Object>emptyMap());
        relationshipMap.put(start, end, type, relationshipId);
      }
    }
    return relationshipMap.get(start, end, type);
  }

  @Override
  public Optional<Long> getRelationship(long start, long end, RelationshipType type) {
    if (relationshipMap.containsKey(start, end, type)) {
      return Optional.of(relationshipMap.get(start, end, type));
    } else {
      return Optional.absent();
    }
  }

  @Override
  public Collection<Long> createRelationshipsPairwise(Collection<Long> nodes, RelationshipType type) {
    Set<Long> relationships = new HashSet<>();
    for (Long start : nodes) {
      for (Long end : nodes) {
        if (start.equals(end)) {
          continue;
        } else {
          synchronized (graphLock) {
            if (!getRelationship(end, start, type).isPresent()) {
              relationships.add(createRelationship(start, end, type));
            }
          }
        }
      }
    }
    return relationships;
  }

  @Override
  public void setNodeProperty(long node, String property, Object value) {
    if (GraphUtil.ignoreProperty(value)) {
      return;
    }
    try {
      synchronized (graphLock) {
        Map<String, Object> properties = Maps.newHashMap(inserter.getNodeProperties(node));
        properties.put(property, value);
        inserter.setNodeProperties(node, properties);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to set " + property + " to " + value + " on " + node, e);
    }
  }

  @Override
  public void addNodeProperty(long node, String property, Object value) {
    if (GraphUtil.ignoreProperty(value)) {
      return;
    }
    synchronized(graphLock) {
      Map<String, Object> properties = inserter.getNodeProperties(node);
      if (properties.containsKey(property)) {
        Object originalValue = properties.get(property);
        Object newValue = GraphUtil.getNewPropertyValue(originalValue, value);
        Map<String, Object> newProperties = Maps.newHashMap(inserter.getNodeProperties(node));
        newProperties.put(property, newValue);
        inserter.setNodeProperties(node, newProperties);
      } else {
        setNodeProperty(node, property, value);
      }
    }
  }

  @Override
  public <T> Optional<T> getNodeProperty(long node, String property, Class<T> type) {
    Map<String, Object> propertyMap;
    synchronized (graphLock) {
      propertyMap = inserter.getNodeProperties(node);
    }
    if (propertyMap.containsKey(property)) {
      return Optional.<T> of(type.cast(propertyMap.get(property)));
    } else {
      return Optional.absent();
    }
  }

  @Override
  public <T> Collection<T> getNodeProperties(long node, String property, Class<T> type) {
    Map<String, Object> propertyMap;
    synchronized (graphLock) {
      propertyMap = inserter.getNodeProperties(node);
    }
    if (propertyMap.containsKey(property)) {
      return GraphUtil.getPropertiesAsSet(propertyMap.get(property), type);
    } else {
      return emptyList();
    }
  }

  @Override
  public void setRelationshipProperty(long batchId, String property, Object value) {
    if (GraphUtil.ignoreProperty(value)) {
      return;
    }
    synchronized (graphLock) {
      inserter.setRelationshipProperty(batchId, property, value);
    }
  }

  @Override
  public void addRelationshipProperty(long relationship, String property, Object value) {
    if (GraphUtil.ignoreProperty(value)) {
      return;
    }
    synchronized(graphLock) {
      Map<String, Object> propertyMap = inserter.getRelationshipProperties(relationship);
      if (propertyMap.containsKey(property)) {
        Object originalValue = propertyMap.get(property);
        Object newValue = GraphUtil.getNewPropertyValue(originalValue, value);
        Map<String, Object> properties = Maps.newHashMap(propertyMap);
        properties.put(property, newValue);
        synchronized (graphLock) {
          inserter.setRelationshipProperties(relationship, properties);
        }
      } else {
        setRelationshipProperty(relationship, property, value);
      }
    }
  }

  @Override
  public <T> Optional<T> getRelationshipProperty(long relationship, String property, Class<T> type) {
    Map<String, Object> propertyMap;
    synchronized (graphLock) {
      propertyMap = inserter.getRelationshipProperties(relationship);
    }
    if (propertyMap.containsKey(property)) {
      return Optional.<T> of(type.cast(propertyMap.get(property)));
    } else {
      return Optional.absent();
    }
  }

  @Override
  public <T> Collection<T> getRelationshipProperties(long relationship, String property, Class<T> type) {
    Map<String, Object> propertyMap;
    synchronized (graphLock) {
      propertyMap = inserter.getRelationshipProperties(relationship);
    }
    if (propertyMap.containsKey(property)) {
      return GraphUtil.getPropertiesAsSet(propertyMap.get(property), type);
    } else {
      return emptyList();
    }
  }

  @Override
  public void setLabel(long node, Label label) {
    synchronized (graphLock) {
      inserter.setNodeLabels(node, label);
    }
  }

  @Override
  public void addLabel(long node, Label label) {
    synchronized (graphLock) {
      Set<Label> labels = newLinkedHashSet(inserter.getNodeLabels(node));
      labels.add(label);
      inserter.setNodeLabels(node, labels.toArray(new Label[labels.size()]));
    }
  }

  @Override
  public Collection<Label> getLabels(long node) {
    synchronized (graphLock) {
      return newHashSet(inserter.getNodeLabels(node));
    }
  }

  Map<String, Object> collectIndexProperties(Map<String, Object> properties) {
    Map<String, Object> indexProperties = new HashMap<>();
    for (Entry<String, Object> entry : properties.entrySet()) {
      if (indexedProperties.contains(entry.getKey())) {
        indexProperties.put(entry.getKey(), entry.getValue());
      }
      if (indexedExactProperties.contains(entry.getKey())) {
        indexProperties.put(entry.getKey() + LuceneUtils.EXACT_SUFFIX, entry.getValue());
      }
    }
    return indexProperties;
  }

  void index() {
    logger.info("Starting indexing");
    List<Long> ids = newArrayList(idMap.values());
    //TODO: Evaluate the performance of using a BTreeMap instead of sorting here.
    Collections.sort(ids);
    for (long id: ids) {
      Map<String, Object> properties = inserter.getNodeProperties(id);
      Map<String, Object> indexProperties = collectIndexProperties(properties);
      if (!indexProperties.isEmpty()) {
        nodeIndex.add(id, indexProperties);
      }
    }
    logger.info("Finished indexing");
  }

  @Override
  public void shutdown() {
    index();
    nodeIndex.flush();
    indexProvider.shutdown();
    inserter.shutdown();
  }

}
