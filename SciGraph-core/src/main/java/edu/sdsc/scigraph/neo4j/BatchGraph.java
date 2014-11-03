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

import static com.google.common.collect.Iterables.contains;
import static com.google.common.collect.Sets.newHashSet;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Named;

import org.apache.lucene.analysis.StopAnalyzer;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import edu.sdsc.scigraph.lucene.LuceneUtils;
import edu.sdsc.scigraph.lucene.VocabularyIndexAnalyzer;

@ThreadSafe
public class BatchGraph {

  private static final Logger logger = Logger.getLogger(BatchGraph.class.getName());

  private final static Object graphLock = new Object();

  private final BatchInserter inserter;
  private final BatchInserterIndexProvider indexProvider;
  private final BatchInserterIndex nodeIndex;

  public final String uniqueProperty;

  private final Set<String> indexedProperties;
  private final Set<String> indexedExactProperties;

  private static final Map<String, String> INDEX_CONFIG = MapUtil.stringMap(IndexManager.PROVIDER,
      "lucene", "analyzer", VocabularyIndexAnalyzer.class.getName());

  IdMap idMap = new IdMap();

  RelationshipMap relationshipMap = new RelationshipMap();

  @Inject
  public BatchGraph(BatchInserter inserter, @Named("uniqueProperty") String uniqueProperty,
      @Named("indexedProperties") Set<String> indexedProperties,
      @Named("exactProperties") Set<String> exactIndexedProperties) {
    this.inserter = inserter;
    indexProvider = new LuceneBatchInserterIndexProvider(inserter);
    nodeIndex = indexProvider.nodeIndex("node_auto_index", INDEX_CONFIG);
    this.uniqueProperty = uniqueProperty;
    this.indexedProperties = newHashSet(indexedProperties);
    this.indexedProperties.add(uniqueProperty);
    this.indexedExactProperties = newHashSet(exactIndexedProperties);
  }

  public void shutdown() {
    nodeIndex.flush();
    indexProvider.shutdown();
    inserter.shutdown();
  }

  /***
   * @param id The node's unique id
   * @return A new or existing node identified by id
   */
  public long getNode(String id) {
    long nodeId = idMap.get(id);
    synchronized(graphLock) {
      if (!inserter.nodeExists(nodeId)) {
        inserter.createNode(nodeId, Collections.<String, Object>emptyMap());
        setNodeProperty(nodeId, uniqueProperty, id);
      }
    }
    return nodeId;
  }

  /***
   * @param from
   * @param to
   * @param type
   * @return true if an undirected relationship with type exists between from and to
   */
  public boolean hasRelationship(long from, long to, RelationshipType type) {
    return relationshipMap.containsKey(from, to, type) 
        || relationshipMap.containsKey(to, from, type);
  }

  /***
   * @param from
   * @param to  
   * @param type
   * @return A new or existing relationship with type type between from and to
   */
  public long createRelationship(long from, long to, RelationshipType type) {
    if (!relationshipMap.containsKey(from, to, type)) {
      synchronized (graphLock) {
        long relationshipId =
            inserter.createRelationship(from, to, type, Collections.<String, Object>emptyMap());
        relationshipMap.put(from, to, type, relationshipId);
      }
    }
    return relationshipMap.get(from, to, type);
  }

  /***
   * Create pairwise relationships between all nodes.
   * 
   * @param nodes
   * @param type
   * @return All of the created edges
   */
  public Collection<Long> createRelationshipPairwise(Collection<Long> nodes, RelationshipType type) {
    Set<Long> relationships = new HashSet<>();
    for (Long start : nodes) {
      for (Long end : nodes) {
        if (start.equals(end)) {
          continue;
        } else {
          synchronized (graphLock) {
            if (!hasRelationship(start, end, type)) {
              relationships.add(createRelationship(start, end, type));
            }
          }
        }
      }
    }
    return relationships;
  }

  static boolean ignoreProperty(Object value) {
    // Ignore whitespace properties and stop words
    // HACK: This stop word check should be done at OWL load time
    return (null == value || (value instanceof String && (CharMatcher.WHITESPACE
        .matchesAllOf((String) value) || StopAnalyzer.ENGLISH_STOP_WORDS_SET
        .contains(((String) value).toLowerCase()))));
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

  Map<String, Object> collectIndexProperties(String property, Object value) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(property, value);
    return collectIndexProperties(properties);
  }

  public void addProperty(long node, String property, Object value) {
    if (!ignoreProperty(value)) {
      synchronized(graphLock) {
        if (inserter.getNodeProperties(node).containsKey(property)) {
          // We might be creating or updating an array - read everything into a Set<>
          Object origValue = inserter.getNodeProperties(node).get(property);
          Class<?> clazz = value.getClass();
          Set<Object> valueSet = new LinkedHashSet<>();
          if (origValue.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(origValue); i++) {
              valueSet.add(Array.get(origValue, i));
            }
          } else {
            valueSet.add(origValue);
          }
          valueSet.add(value);

          // Now write the set back if necessary
          if (valueSet.size() > 1) {
            Object newArray = Array.newInstance(clazz, valueSet.size());
            int i = 0;
            for (Object obj : valueSet) {
              Array.set(newArray, i++, clazz.cast(obj));
            }
            Map<String, Object> properties = Maps.newHashMap(inserter.getNodeProperties(node));
            properties.put(property, newArray);
            inserter.setNodeProperties(node, properties);
          }
          Map<String, Object> indexProperties = collectIndexProperties(property, value);
          if (!indexProperties.isEmpty()) {
            logger.fine("Indexing " + indexProperties);
            nodeIndex.add(node, indexProperties);
          }
        } else {
          setNodeProperty(node, property, value);
        }
      }
    }
  }

  public void setNodeProperty(long batchId, String property, Object value) {
    try {
      if (!ignoreProperty(value)) {
        Map<String, Object> properties = null;
        synchronized (graphLock) {
          properties = Maps.newHashMap(inserter.getNodeProperties(batchId));
          properties.put(property, value);
          inserter.setNodeProperties(batchId, properties);
        }
        Map<String, Object> indexProperties = collectIndexProperties(properties);
        if (!indexProperties.isEmpty()) {
          logger.fine("Indexing " + indexProperties);
          synchronized (graphLock) {
            nodeIndex.updateOrAdd(batchId, indexProperties);
          }
        }
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to set " + property + " to " + value + " on " + batchId, e);
    }
  }

  public void setRelationshipProperty(long batchId, String property, Object value) {
    if (!ignoreProperty(value)) {
      synchronized (graphLock) {
        inserter.setRelationshipProperty(batchId, property, value);
      }
    }
  }

  public void setLabel(long node, Label label) {
    synchronized(graphLock) {
      inserter.setNodeLabels(node, label);
    }
  }

  public void addLabel(long node, Label label) {
    synchronized(graphLock) {
      Set<Label> labels = newHashSet(inserter.getNodeLabels(node));
      labels.add(label);
      inserter.setNodeLabels(node, labels.toArray(new Label[labels.size()]));
    }
  }

  public boolean hasLabel(long node, Label label) {
    synchronized(graphLock) {
      return contains(inserter.getNodeLabels(node), label);
    }
  }

}
