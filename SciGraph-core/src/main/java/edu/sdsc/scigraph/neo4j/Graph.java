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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.transform;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.ReadableIndex;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.owlapi.OwlRelationships;

public class Graph {

  private final GraphDatabaseService graphDb;
  private final ExecutionEngine engine;
  private final ReadableIndex<Node> nodeAutoIndex;

  @Inject
  public Graph(GraphDatabaseService graphDb) {
    this.graphDb = graphDb;
    this.engine = new ExecutionEngine(graphDb);
    this.nodeAutoIndex = graphDb.index().getNodeAutoIndexer().getAutoIndex();
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

  public boolean nodeExists(String uri) {
    return nodeExists(getURI(uri));
  }

  @Transactional
  public boolean nodeExists(URI uri) {
    checkNotNull(uri);
    try (Transaction tx = graphDb.beginTx()) {
      boolean exists = null != nodeAutoIndex.get(CommonProperties.URI, uri.toString()).getSingle();
      tx.success();
      return exists;
    }
  }

  public Node getOrCreateNode(String uri) {
    return getOrCreateNode(getURI(uri));
  }

  //TODO: This makes the class not threadsafe. Ideally this should be coordinated with 
  // the IdMap in BatchGraph so that the two can co-exist.
  public Node getOrCreateNode(final URI uri) {
    checkNotNull(uri);
    try (Transaction tx = graphDb.beginTx()) {
      Node node = nodeAutoIndex.get(CommonProperties.URI, uri.toString()).getSingle();
      if (null == node) {
        node = graphDb.createNode();
        node.setProperty(CommonProperties.URI, uri.toString());
        node.setProperty(CommonProperties.FRAGMENT, GraphUtil.getFragment(uri));
      }
      tx.success();
      return node;
    }
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

  public Node getNode(Concept framedNode) {
    long id = framedNode.getId();
    return graphDb.getNodeById(id);
  }

  public boolean isDeprecated(Node n) {
    boolean owlDeprecated = Boolean.valueOf((String)n.getProperty(OWLRDFVocabulary.OWL_DEPRECATED.toString(), "false"));
    return owlDeprecated;
  }
  
  Concept getVertex(long id) {
    Concept concept = new Concept();
    try (Transaction tx = graphDb.beginTx()) {
      Node n = graphDb.getNodeById(id);
      concept.setAnonymous((boolean) n.getProperty(Concept.ANONYMOUS, false));
      concept.setInferred((boolean) n.getProperty(Concept.INFERRED, false));
      concept.setNegated((boolean) n.getProperty(Concept.NEGATED, false));

      concept.setFragment((String) n.getProperty(Concept.FRAGMENT, null));
      concept.setId(id);
      concept.setPreferredLabel((String) n.getProperty(Concept.PREFERRED_LABEL, null));
      concept.setUri((String) n.getProperty(Concept.URI, null));
      concept.setDeprecated(isDeprecated(n));

      for (String definition : GraphUtil.getProperties(n, Concept.DEFINITION, String.class)) {
        concept.addDefinition(definition);
      }
      for (String abbreviation : GraphUtil.getProperties(n, Concept.ABREVIATION, String.class)) {
        concept.addAbbreviation(abbreviation);
      }
      for (String acronym : GraphUtil.getProperties(n, Concept.ACRONYM, String.class)) {
        concept.addAcronym(acronym);
      }
      for (String category : GraphUtil.getProperties(n, Concept.CATEGORY, String.class)) {
        concept.addCategory(category);
      }
      for (String label : GraphUtil.getProperties(n, Concept.LABEL, String.class)) {
        concept.addLabel(label);
      }
      for (String synonym : GraphUtil.getProperties(n, Concept.SYNONYM, String.class)) {
        concept.addSynonym(synonym);
      }
      for (Label type : n.getLabels()) {
        concept.addType(type.name());
      }

      for (Relationship r: n.getRelationships(OwlRelationships.OWL_EQUIVALENT_CLASS)) {
        Node equivalence = r.getStartNode().equals(n) ? r.getEndNode() : r.getStartNode();
        concept.getEquivalentClasses().add((String)equivalence.getProperty(CommonProperties.URI));
      }

      tx.success();


    }

    return concept;
  }

  public Concept getOrCreateFramedNode(String uri) {
    Node n = getOrCreateNode(uri);
    return getVertex(n.getId());
  }

  public Concept getOrCreateFramedNode(Node node) {
    return getVertex(node.getId());
  }

  public Iterable<Concept> getOrCreateFramedNodes(Iterable<Node> nodes) {
    return transform(nodes, new Function<Node, Concept>() {

      @Override
      public Concept apply(Node node) {
        return getOrCreateFramedNode(node);
      }

    });
  }

  public Optional<Concept> getFramedNode(String uri) {
    if (nodeExists(uri)) {
      return Optional.of(getOrCreateFramedNode(uri));
    }
    return Optional.absent();
  }

}
