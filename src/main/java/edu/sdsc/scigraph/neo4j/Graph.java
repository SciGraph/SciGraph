/**
 * Copyright (C) 2014 Christopher Condit (condit@sdsc.edu)
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;

import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.apache.lucene.analysis.StopAnalyzer;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.index.AutoIndexer;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.helpers.collection.MapUtil;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.oupls.jung.GraphJung;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphConfiguration;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.AbstractModule;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.frames.util.MultiPropertyMethodHandler;
import edu.sdsc.scigraph.lucene.LuceneUtils;
import edu.sdsc.scigraph.lucene.VocabularyIndexAnalyzer;

public class Graph<N> {

  private static final Logger logger = Logger.getLogger(Graph.class.getName()); 

  public static final String UNIQUE_PROPERTY = CommonProperties.URI;

  private static final Set<String> NODE_PROPERTIES_TO_INDEX = 
      newHashSet(CommonProperties.URI, NodeProperties.LABEL, 
          NodeProperties.LABEL + LuceneUtils.EXACT_SUFFIX, CommonProperties.CURIE,
          CommonProperties.ONTOLOGY,
          CommonProperties.FRAGMENT, Concept.CATEGORY, Concept.SYNONYM, Concept.SYNONYM + LuceneUtils.EXACT_SUFFIX);
  private static final Set<String> RELATIONSHIP_PROPERTIES_TO_INDEX = newHashSet(CommonProperties.URI);
  private static final Set<String> EXACT_PROPERTIES = newHashSet(NodeProperties.LABEL, Concept.SYNONYM);

  private static final Map<String, String> INDEX_CONFIG = MapUtil.stringMap(
      IndexManager.PROVIDER, "lucene",
      "analyzer", VocabularyIndexAnalyzer.class.getName());

  private final GraphDatabaseService graphDb;
  private final ExecutionEngine engine;
  private final ReadableIndex<Node> nodeAutoIndex;
  private final FramedGraph<com.tinkerpop.blueprints.Graph> framedGraph;
  private final GraphJung<com.tinkerpop.blueprints.Graph> jungGraph;

  private final Class<?> nodeType;

  @Inject
  public Graph(GraphDatabaseService graphDb, Class<?> nodeType) {
    this.graphDb = graphDb;
    this.nodeType = nodeType;
    this.engine = new ExecutionEngine(graphDb);
    if (!graphDb.index().getNodeAutoIndexer().isEnabled()) {
      setupAutoIndexing();
    }
    this.nodeAutoIndex = graphDb.index().getNodeAutoIndexer().getAutoIndex();

    FramedGraphFactory factory = new FramedGraphFactory(new AbstractModule() {
      @Override
      protected void doConfigure(FramedGraphConfiguration config) {
        config.addMethodHandler(new MultiPropertyMethodHandler());
      }
    });
    Neo4jGraph neo4jGraph = new Neo4jGraph(graphDb);
    framedGraph = factory.create((com.tinkerpop.blueprints.Graph) (neo4jGraph));
    jungGraph = new GraphJung<com.tinkerpop.blueprints.Graph>((com.tinkerpop.blueprints.Graph) (neo4jGraph));
  }

  private void setupIndex(AutoIndexer<?> index, Set<String> properties) {
    for (String property: properties) {
      index.startAutoIndexingProperty(property);
    }
    index.setEnabled(true);
  }

  private void setupAutoIndexing() {
    graphDb.index().forNodes("node_auto_index", INDEX_CONFIG);
    setupIndex(graphDb.index().getNodeAutoIndexer(), NODE_PROPERTIES_TO_INDEX);
    graphDb.index().forRelationships("relationship_auto_index", INDEX_CONFIG);
    setupIndex(graphDb.index().getRelationshipAutoIndexer(), RELATIONSHIP_PROPERTIES_TO_INDEX);
  }

  public void shutdown() {
    graphDb.shutdown();
  }

  public static URI getURI(String uri) {
    checkNotNull(uri);
    try {
      return new URI(uri);
    } catch (URISyntaxException e) {
      checkState(false, "URIs passed to this method should always be valid: " + uri);
      return null;
    }
  }

  public GraphDatabaseService getGraphDb() {
    return graphDb;
  }

  public ExecutionEngine getExecutionEngine() {
    return engine;
  }

  public ReadableIndex<Node> getNodeAutoIndex() {
    return nodeAutoIndex;
  }

  public GraphJung<com.tinkerpop.blueprints.Graph> getJungGraph() {
    return jungGraph;
  }

  public boolean nodeExists(String uri) {
    return nodeExists(getURI(uri));
  }

  public boolean nodeExists(URI uri) {
    checkNotNull(uri);
    Node node = nodeAutoIndex.get(CommonProperties.URI, uri.toString()).getSingle();
    return (null != node);
  }

  public Node getOrCreateNode(String uri) {
    return getOrCreateNode(getURI(uri));
  }

  static String getLastPathFragment(URI uri) {
    return uri.getPath().replaceFirst(".*/([^/?]+).*", "$1");
  }

  public Node getOrCreateNode(final URI uri) {
    checkNotNull(uri);

    UniqueFactory<Node> factory = new UniqueFactory.UniqueNodeFactory(graphDb, "uniqueNodeIndex") {
      @Override
      protected void initialize(Node created, Map<String, Object> properties) {
        logger.fine("Creating node: " + properties.get(UNIQUE_PROPERTY));
        created.setProperty(UNIQUE_PROPERTY, properties.get(UNIQUE_PROPERTY));
        if (null != uri.getFragment()) {
          created.setProperty(CommonProperties.FRAGMENT, uri.getFragment());
        } else if (uri.toString().startsWith("mailto:")) {
          created.setProperty(CommonProperties.FRAGMENT, uri.toString().substring("mailto:".length()));
        } else {
          created.setProperty(CommonProperties.FRAGMENT, getLastPathFragment(uri));
        }
      }
    };

    return factory.getOrCreate(UNIQUE_PROPERTY, uri.toString());
  }

  public Optional<Node> getNode(String uri) {
    return getNode(getURI(uri));
  }

  public Optional<Node> getNode(final URI uri) {
    if (nodeExists(uri)) {
      return Optional.of(getOrCreateNode(uri));
    }
    return Optional.absent();
  }

  @SuppressWarnings("unchecked")
  public N getOrCreateFramedNode(String uri) {
    Node n = getOrCreateNode(uri);
    return (N) framedGraph.getVertex(n.getId(), nodeType);
  }

  @SuppressWarnings("unchecked")
  public N getOrCreateFramedNode(Node node) {
    return (N) framedGraph.getVertex(node.getId(), nodeType);
  }

  public Iterable<N> getOrCreateFramedNodes(Iterable<Node> nodes) {
    return transform(nodes, new Function<Node, N>() {

      @Override
      public N apply(Node node) {
        return getOrCreateFramedNode(node);
      }

    });
  }

  public Optional<N> getFramedNode(String uri) {
    if (nodeExists(uri)) {
      return Optional.of(getOrCreateFramedNode(uri));
    }
    return Optional.absent();
  }

  public boolean hasRelationship(Node a, Node b, RelationshipType type) {
    return hasRelationship(a, b, type, Optional.<URI>absent());
  }

  public boolean hasRelationship(Node a, Node b, RelationshipType type, String uri) {
    return hasRelationship(a, b, type, Optional.of(getURI(uri)));
  }

  public boolean hasRelationship(Node a, Node b, RelationshipType type, Optional<URI> uri) {
    checkNotNull(a); checkNotNull(b); checkNotNull(type); checkNotNull(uri);
    for (Relationship r: a.getRelationships(type)) {
      if (uri.isPresent() && (r.getEndNode().equals(b))) {
        if (r.getProperty(CommonProperties.URI).equals(uri.get().toString())) {
          return true;
        }
      } else if (!uri.isPresent() && r.getEndNode().equals(b)) {
        return true;
      }
    }
    return false;
  }

  public Relationship getOrCreateRelationship(Node a, Node b, RelationshipType type) { 
    return getOrCreateRelationship(a, b, type, Optional.<URI>absent());
  }
  public Relationship getOrCreateRelationship(Node a, Node b, RelationshipType type, String uri) { 
    return getOrCreateRelationship(a, b, type, Optional.of(getURI(uri)));
  }

  public Relationship getOrCreateRelationship(final Node a, final Node b, final RelationshipType type, final Optional<URI> uri) {
    checkNotNull(a); checkNotNull(b); checkNotNull(type); checkNotNull(uri);

    UniqueFactory<Relationship> factory = new UniqueFactory.UniqueRelationshipFactory(graphDb, "uniqueRelationshipIndex") {
      @Override
      protected Relationship create(Map<String, Object> properties) {
        Relationship r =  a.createRelationshipTo(b, type);
        if (uri.isPresent()) {
          r.setProperty(CommonProperties.URI, uri.get().toString());
          if (null != uri.get().getFragment()) {
            r.setProperty(CommonProperties.FRAGMENT, uri.get().getFragment());
          }
        }
        return r;
      }
    };

    return factory.getOrCreate("relationship", a.getProperty(CommonProperties.URI) + type.name() + b.getProperty(CommonProperties.URI));
  }

  public Collection<Relationship> getOrCreateRelationshipPairwise(Collection<Node> nodes, RelationshipType type, Optional<URI> uri) {
    Set<Relationship> relationships = new HashSet<>();
    for (Node start: nodes) {
      for (Node end: nodes) {
        if (start.equals(end)) {
          continue;
        }
        relationships.add(getOrCreateRelationship(start, end, type, uri));
      }
    }
    return relationships;
  }

  /***
   * Set property to single valued value for node or relationship
   * @param container node or relationship
   * @param property
   * @param value
   */
  @Transactional
  public void setProperty(PropertyContainer container, String property, Object value) {
    if (value instanceof String) {
      // Ignore whitespace properties and stop words
      if (CharMatcher.WHITESPACE.matchesAllOf((String)value) 
          || StopAnalyzer.ENGLISH_STOP_WORDS_SET.contains((String)value)) {
        return;
      }
    }
    container.setProperty(property, value);
    if (EXACT_PROPERTIES.contains(property)) {
      container.setProperty(property + LuceneUtils.EXACT_SUFFIX, value);
    }
  }

  /***
   * Add value to property for a node or relationship.
   * <p>If necessary this will concatenate value to an array. 
   * <ul>
   * <li>Duplicate values for the same property will be ignored.</li>
   * <li>Property value insertion order will be preserved.</li>
   * </ul>
   * @param container node or relationship
   * @param property
   * @param value
   */
  @Transactional
  public void addProperty(PropertyContainer container, String property, Object value) {
    if (value instanceof String) {
      // Ignore whitespace properties and stop words
      if (CharMatcher.WHITESPACE.matchesAllOf((String)value) 
          || StopAnalyzer.ENGLISH_STOP_WORDS_SET.contains((String)value)) {
        return;
      }
    }
    if (container.hasProperty(property)) {
      // We might be creating or updating an array - read everything into a Set<>
      Object origValue = (Object)container.getProperty(property);
      Class<?> clazz = value.getClass();
      Set<Object> valueSet = new LinkedHashSet<>();
      if (container.getProperty(property).getClass().isArray()) {
        for (int i = 0; i < Array.getLength(origValue); i++) {
          valueSet.add(Array.get(origValue, i));
        }
      } else {
        valueSet.add((Object)origValue);
      }
      valueSet.add(value);

      // Now write the set back if necessary
      if (valueSet.size() > 1) {
        Object newArray = Array.newInstance(clazz, valueSet.size());
        int i = 0;
        for (Object obj: valueSet) {
          Array.set(newArray, i++, clazz.cast(obj));
        }
        container.setProperty(property, newArray);
      }
    } else {
      container.setProperty(property, value);
    }
  }

  /***
   * @param container
   * @param property
   * @param type
   * @return the single property value for node with the supplied type
   */
  public <T> Optional<T> getProperty(PropertyContainer container, String property, Class<T> type)  {
    Optional<T> value = Optional.<T>absent();
    if (container.hasProperty(property)) {
      value = Optional.<T>of(type.cast(container.getProperty(property)));
    }
    return value;
  }

  /***
   * @param container
   * @param property
   * @param type
   * @return a list of properties for node with the supplied type
   */
  public <T> List<T> getProperties(PropertyContainer container, String property, Class<T> type) {
    List<T> list = new ArrayList<>();
    if (container.hasProperty(property)) {
      if (container.getProperty(property).getClass().isArray()) {
        for (Object o: (Object[])container.getProperty(property)) {
          list.add(type.cast(o));
        }
      } else {
        list.add(type.cast(container.getProperty(property)));
      }
    }

    return list;
  }

  public ResourceIterator<Map<String,Object>> runCypherQuery(String query) {
    ExecutionResult result = engine.execute(query);
    return result.iterator();
  }

}
