package edu.sdsc.scigraph.neo4j;

import static com.google.common.collect.Sets.newHashSet;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.concurrent.NotThreadSafe;
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
import org.neo4j.unsafe.batchinsert.BatchRelationship;

import com.google.common.base.CharMatcher;
import com.google.inject.Inject;

import edu.sdsc.scigraph.lucene.LuceneUtils;
import edu.sdsc.scigraph.lucene.VocabularyIndexAnalyzer;

@NotThreadSafe
public class BatchGraph {

  private static final Logger logger = Logger.getLogger(BatchGraph.class.getName());

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
   * 
   * @param uri
   * @return
   */
  public long getOrCreateNode(String uri) {
    long nodeId = idMap.get(uri);
    if (!inserter.nodeExists(nodeId)) {
      inserter.createNode(nodeId, Collections.<String, Object> emptyMap());
      setNodeProperty(nodeId, uniqueProperty, uri);
    }
    return nodeId;
  }

  public long getOrCreateNode(URI uri) {
    return getOrCreateNode(uri.toString());
  }

  public boolean hasRelationship(long from, long to, RelationshipType type) {
    for (BatchRelationship rel : inserter.getRelationships(from)) {
      if ((rel.getEndNode() == to) || (rel.getStartNode() == to)
          && rel.getType().name().equals(type.name())) {
        return true;
      }
    }
    return false;
  }

  public long createRelationship(long from, long to, RelationshipType type) {
    if (!relationshipMap.containsKey(from, to, type)) {
      long relationshipId = inserter.createRelationship(from, to, type, Collections.<String, Object> emptyMap());
      relationshipMap.put(from, to, type, relationshipId);
    }
    return relationshipMap.get(from, to, type);
  }

  public Collection<Long> createRelationshipPairwise(Collection<Long> nodes, RelationshipType type) {
    Set<Long> relationships = new HashSet<>();
    for (Long start : nodes) {
      for (Long end : nodes) {
        if (start.equals(end)) {
          continue;
        } else {
          if (!hasRelationship(start, end, type)) {
            relationships.add(createRelationship(start, end, type));
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
    Map<String, Object> indexProperties = new HashMap<>();
    if (indexedProperties.contains(property)) {
      indexProperties.put(property, value);
    }
    if (indexedExactProperties.contains(property)) {
      indexProperties.put(property + LuceneUtils.EXACT_SUFFIX, value);
    }
    return indexProperties;
  }

  public void addProperty(long node, String property, Object value) {
    if (!ignoreProperty(value)) {
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
          inserter.getNodeProperties(node).put(property, newArray);
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

  public void setNodeProperty(long batchId, String property, Object value) {
    if (!ignoreProperty(value)) {
      inserter.setNodeProperty(batchId, property, value);
      Map<String, Object> indexProperties = collectIndexProperties(inserter
          .getNodeProperties(batchId));
      if (!indexProperties.isEmpty()) {
        logger.fine("Indexing " + indexProperties);
        nodeIndex.updateOrAdd(batchId, indexProperties);
      }
    }
  }

  public void setRelationshipProperty(long batchId, String property, Object value) {
    if (!ignoreProperty(value)) {
      inserter.setRelationshipProperty(batchId, property, value);
    }
  }

  public void setLabel(long node, Label label) {
    inserter.setNodeLabels(node, label);
  }

  public void addLabel(long node, Label label) {
    Set<Label> labels = newHashSet(inserter.getNodeLabels(node));
    labels.add(label);
    inserter.setNodeLabels(node, labels.toArray(new Label[labels.size()]));
  }

}
