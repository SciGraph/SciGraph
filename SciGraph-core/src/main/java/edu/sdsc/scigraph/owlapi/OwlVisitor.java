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
package edu.sdsc.scigraph.owlapi;

import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Lists.transform;
import static edu.sdsc.scigraph.owlapi.OwlApiUtils.getTypedLiteralValue;
import static edu.sdsc.scigraph.owlapi.OwlApiUtils.getUri;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLQuantifiedObjectRestriction;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.EdgeProperties;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.neo4j.EdgeType;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.MappedProperty;

public class OwlVisitor extends OWLOntologyWalkerVisitor<Void> {

  private static final Logger logger = Logger.getLogger(OwlVisitor.class.getName());

  private final Graph graph;

  private OWLOntology ontology;

  private Map<String, String> categoryMap;

  private Map<String, String> mappedProperties;

  private OWLOntology parentOntology = null;

  @Inject
  OwlVisitor(OWLOntologyWalker walker, Graph graph,
      Map<String, String> categoryMap,
      List<MappedProperty> mappedProperties) {
    super(walker);
    this.graph = graph;
    this.categoryMap = categoryMap;
    this.mappedProperties = new HashMap<>();
    for (MappedProperty mappedProperty: mappedProperties) {
      for (String property: mappedProperty.getProperties()) {
        this.mappedProperties.put(property, mappedProperty.getName());
      }
    }
  }

  @Override
  public Void visit(OWLOntology ontology) {
    logger.info("Walking ontology: " + ontology.getOntologyID());
    this.ontology = ontology;
    if (null == parentOntology) {
      parentOntology = ontology;
    }
    return null;
  }

  @Override
  public Void visit(OWLClass desc) {
    logger.fine(desc.toString());
    Node node = graph.getOrCreateNode(getUri(desc));
    //TODO: Move this to the object creation:
    node.addLabel(OwlLabels.OWL_CLASS);
    graph.setProperty(node, NodeProperties.ANONYMOUS, false);
    if (null != ontology.getOntologyID().getOntologyIRI()) {
      graph.setProperty(node, NodeProperties.ONTOLOGY, ontology.getOntologyID().getOntologyIRI().toString());
    }
    if (null != parentOntology.getOntologyID().getOntologyIRI()) {
      graph.setProperty(node, CommonProperties.PARENT_ONTOLOGY, parentOntology.getOntologyID().getOntologyIRI().toString());
    }
    if (null != ontology.getOntologyID().getVersionIRI()) {
      graph.setProperty(node, NodeProperties.ONTOLOGY_VERSION, ontology.getOntologyID().getVersionIRI().toString());
    }
    return null;
  }

  @Override
  public Void visit(OWLDataProperty property) {
    Node node = graph.getOrCreateNode(property.getIRI().toURI());
    node.addLabel(OwlLabels.OWL_DATA_PROPERTY);
    return null;
  }

  @Override
  public Void visit(OWLObjectProperty property) {
    Node node = graph.getOrCreateNode(property.getIRI().toURI());
    node.addLabel(OwlLabels.OWL_OBJECT_PROPERTY);
    return null;
  }

  @Override
  public Void visit(OWLAnnotationAssertionAxiom axiom) {
    if (axiom.getSubject() instanceof IRI) {
      Node subject = graph.getOrCreateNode(((IRI)axiom.getSubject()).toURI());
      String property = getUri(axiom.getProperty()).toString();
      if (axiom.getValue() instanceof OWLLiteral) {
        Optional<Object> literal = getTypedLiteralValue((OWLLiteral)(axiom.getValue()));
        if (literal.isPresent()) {
          try {
            graph.addProperty(subject, property, literal.get());

            if (mappedProperties.containsKey(property)) {
              graph.addProperty(subject, mappedProperties.get(property), literal.get());
            }
          } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to add property: " + property + " with value " + literal.get().toString(), e);
          }
        }
      } else if (axiom.getValue() instanceof IRI){
        Node object = graph.getOrCreateNode(((IRI)axiom.getValue()).toURI());
        URI uri = Graph.getURI(property);

        RelationshipType type = DynamicRelationshipType.withName(GraphUtil.getFragment(uri));
        Relationship r = graph.getOrCreateRelationship(subject, object, type, property);
        r.setProperty(CommonProperties.TYPE, OWLAnnotationAssertionAxiom.class.getSimpleName());
      }
    } else {
      logger.fine("Ignoring non IRI assertion axiom: " + axiom.toString());
    }
    return null;
  }

  @Override
  public Void visit(OWLNamedIndividual individual) {
    Node node = graph.getOrCreateNode(getUri(individual));
    node.addLabel(OwlLabels.OWL_NAMED_INDIVIDUAL);
    return null;
  }

  @Override
  public Void visit(OWLSameIndividualAxiom axiom) {
    List<Node> nodes = transform(axiom.getIndividualsAsList(), new Function<OWLIndividual, Node>() {
      @Override
      public Node apply(OWLIndividual individual) {
        return graph.getOrCreateNode(getUri(individual));
      }
    });
    graph.getOrCreateRelationshipPairwise(nodes, OwlRelationships.OWL_SAME_AS, Optional.<URI>absent());
    return null;
  }

  @Override
  public Void visit(OWLDifferentIndividualsAxiom axiom) {
    List<Node> nodes = transform(axiom.getIndividualsAsList(), new Function<OWLIndividual, Node>() {
      @Override
      public Node apply(OWLIndividual individual) {
        return graph.getOrCreateNode(getUri(individual));
      }
    });

    graph.getOrCreateRelationshipPairwise(nodes, OwlRelationships.OWL_DIFFERENT_FROM, Optional.<URI>absent());
    return null;
  }

  @Override
  public Void visit(OWLClassAssertionAxiom axiom) {
    Node individual = graph.getOrCreateNode(getUri(axiom.getIndividual()));
    Node type = graph.getOrCreateNode(getUri(axiom.getClassExpression()));
    graph.getOrCreateRelationship(individual, type, EdgeType.IS_A);
    return null;
  }

  @Override
  public Void visit(OWLDataPropertyAssertionAxiom axiom) {
    Node individual = graph.getOrCreateNode(getUri(axiom.getSubject()));
    String property = axiom.getProperty().asOWLDataProperty().getIRI().toString();
    Optional<Object> literal = getTypedLiteralValue(axiom.getObject());
    if (literal.isPresent()) {
      graph.setProperty(individual, property, literal.get());
      if (mappedProperties.containsKey(property)) {
        graph.addProperty(individual, mappedProperties.get(property), literal.get());
      }
    }
    return null;
  }

  @Override
  public Void visit(OWLSubClassOfAxiom axiom) {
    Node subjectNode = graph.getOrCreateNode(getUri(axiom.getSubClass()));
    Node objectNode = graph.getOrCreateNode(getUri(axiom.getSuperClass()));
    graph.getOrCreateRelationship(subjectNode, objectNode, OwlRelationships.RDF_SUBCLASS_OF);
    return null;
  }

  @Override
  public Void visit(OWLObjectIntersectionOf desc) {
    Node subjectNode = graph.getOrCreateNode(getUri(desc));
    graph.setProperty(subjectNode, CommonProperties.TYPE, OWLObjectIntersectionOf.class.getSimpleName());
    graph.setProperty(subjectNode, NodeProperties.ANONYMOUS, true);
    for (OWLClassExpression expression: desc.getOperands()) {
      Node object = graph.getOrCreateNode(getUri(expression));
      graph.getOrCreateRelationship(subjectNode, object, EdgeType.REL);
    }
    return null;
  }

  @Override
  public Void visit(OWLObjectUnionOf desc) {
    Node subjectNode = graph.getOrCreateNode(getUri(desc));
    graph.setProperty(subjectNode, CommonProperties.TYPE, OWLObjectUnionOf.class.getSimpleName());
    graph.setProperty(subjectNode, NodeProperties.ANONYMOUS, true);
    for (OWLClassExpression expression: desc.getOperands()) {
      Node object = graph.getOrCreateNode(getUri(expression));
      graph.getOrCreateRelationship(subjectNode, object, EdgeType.REL);
    }
    return null;
  }

  Relationship getObjectPropertyRelationship(OWLPropertyAssertionAxiom<OWLObjectPropertyExpression,OWLIndividual> axiom) {
    Node subject = graph.getOrCreateNode(getUri(axiom.getSubject()));
    URI property = getUri(axiom.getProperty());
    Node object = graph.getOrCreateNode(getUri(axiom.getObject()));
    RelationshipType type = EdgeType.OWLObjectPropertyAssertionAxiom;
    if (null != property.getFragment()) {
      type = DynamicRelationshipType.withName(property.getFragment());
    }
    Relationship relationship = graph.getOrCreateRelationship(subject, object, type, Optional.of(property));
    graph.setProperty(relationship, EdgeProperties.NEGATED, false);
    graph.setProperty(relationship, EdgeProperties.SYMMETRIC, !axiom.getProperty().isAsymmetric(ontology));
    graph.setProperty(relationship, EdgeProperties.REFLEXIVE, axiom.getProperty().isReflexive(ontology));
    graph.setProperty(relationship, EdgeProperties.TRANSITIVE, axiom.getProperty().isTransitive(ontology));
    return relationship;
  }

  @Override
  public Void visit(OWLObjectPropertyAssertionAxiom axiom) {
    getObjectPropertyRelationship(axiom);
    return null;
  }

  @Override
  public Void visit(OWLEquivalentClassesAxiom axiom) {
    logger.fine(axiom.toString());
    List<Node> nodes = transform(axiom.getClassExpressionsAsList(), new Function<OWLClassExpression, Node>() {
      @Override
      public Node apply(OWLClassExpression expr) {
        return graph.getOrCreateNode(getUri(expr));
      }
    });

    graph.getOrCreateRelationshipPairwise(nodes, OwlRelationships.OWL_EQUIVALENT_CLASS, Optional.<URI>absent());
    return null;
  }

  @Override
  public Void visit(OWLDisjointClassesAxiom axiom) {
    List<Node> nodes = transform(axiom.getClassExpressionsAsList(), new Function<OWLClassExpression, Node>() {
      @Override
      public Node apply(OWLClassExpression individual) {
        return graph.getOrCreateNode(getUri(individual));
      }
    });

    graph.getOrCreateRelationshipPairwise(nodes, OwlRelationships.OWL_DISJOINT_WITH, Optional.<URI>absent());
    return null;
  }

  @Override
  public Void visit(OWLObjectComplementOf desc) {
    Node subject = graph.getOrCreateNode(getUri(desc));
    graph.setProperty(subject, CommonProperties.TYPE, OWLObjectComplementOf.class.getSimpleName());
    graph.setProperty(subject, NodeProperties.ANONYMOUS, true);
    Node operand = graph.getOrCreateNode(getUri(desc.getOperand()));
    graph.getOrCreateRelationship(subject, operand, EdgeType.REL);
    return null;
  }

  @Override
  public Void visit(OWLSubObjectPropertyOfAxiom axiom) {
    Node subProperty = graph.getOrCreateNode(getUri(axiom.getSubProperty()));
    Node superProperty = graph.getOrCreateNode(getUri(axiom.getSuperProperty()));
    graph.getOrCreateRelationship(subProperty, superProperty, OwlRelationships.RDFS_SUB_PROPERTY_OF, Optional.<URI>absent());
    return null;
  }

  @Override
  public Void visit(OWLSubPropertyChainOfAxiom axiom) {
    Node chain = graph.getOrCreateNode(getUri(axiom.getSuperProperty()));
    int i = 0;
    for (OWLObjectPropertyExpression property: axiom.getPropertyChain()) {
      Node link = graph.getOrCreateNode(getUri(property));
      Relationship relationship = graph.getOrCreateRelationship(chain, link, EdgeType.REL);
      graph.setProperty(relationship, "order", i++);
    }
    return null;
  }

  Node addQuantifiedRestriction(OWLQuantifiedObjectRestriction desc) {
    Node restriction = graph.getOrCreateNode(getUri(desc));
    Node property = graph.getOrCreateNode(getUri(desc.getProperty()));
    graph.getOrCreateRelationship(restriction, property, EdgeType.PROPERTY);
    Node cls = graph.getOrCreateNode(getUri(desc.getFiller()));
    graph.getOrCreateRelationship(restriction, cls, EdgeType.CLASS);
    return restriction;
  }

  Node addCardinalityRestriction(OWLObjectCardinalityRestriction desc) {
    Node restriction = graph.getOrCreateNode(getUri(desc));
    graph.setProperty(restriction, "cardinality", desc.getCardinality());
    Node property = graph.getOrCreateNode(getUri(desc.getProperty()));
    graph.getOrCreateRelationship(restriction, property, EdgeType.PROPERTY);
    Node cls = graph.getOrCreateNode(getUri(desc.getFiller()));
    graph.getOrCreateRelationship(restriction, cls, EdgeType.CLASS);
    return restriction;
  }

  @Override
  public Void visit(OWLObjectMaxCardinality desc) {
    Node restriction = addCardinalityRestriction(desc);
    graph.setProperty(restriction, CommonProperties.TYPE, OWLObjectMaxCardinality.class.getSimpleName());
    return null;
  }

  @Override
  public Void visit(OWLObjectMinCardinality desc) {
    Node restriction = addCardinalityRestriction(desc);
    graph.setProperty(restriction, CommonProperties.TYPE, OWLObjectMinCardinality.class.getSimpleName());
    return null;
  }

  @Override
  public Void visit(OWLObjectExactCardinality desc) {
    Node restriction = addCardinalityRestriction(desc);
    graph.setProperty(restriction, CommonProperties.TYPE, OWLObjectExactCardinality.class.getSimpleName());
    return null;
  }

  @Override
  public Void visit(OWLObjectSomeValuesFrom desc) {
    Node restriction = graph.getOrCreateNode(getUri(desc));
    graph.setProperty(restriction, CommonProperties.TYPE, OWLObjectSomeValuesFrom.class.getSimpleName());
    if (!desc.getProperty().isAnonymous()) {
      Node property = graph.getOrCreateNode(getUri(desc.getProperty()));
      graph.getOrCreateRelationship(restriction, property, EdgeType.PROPERTY);
      Node cls = graph.getOrCreateNode(getUri(desc.getFiller()));
      graph.getOrCreateRelationship(restriction, cls, EdgeType.CLASS);
    }
    return null;
  }

  @Override
  public Void visit(OWLObjectAllValuesFrom desc) {
    Node restriction = graph.getOrCreateNode(getUri(desc));
    graph.setProperty(restriction, CommonProperties.TYPE, OWLObjectAllValuesFrom.class.getSimpleName());
    if (!desc.getProperty().isAnonymous()) {
      Node property = graph.getOrCreateNode(getUri(desc.getProperty()));
      graph.getOrCreateRelationship(restriction, property, EdgeType.PROPERTY);
      Node cls = graph.getOrCreateNode(getUri(desc.getFiller()));
      graph.getOrCreateRelationship(restriction, cls, EdgeType.CLASS);
    }
    return null;
  }

  public void processSomeValuesFrom() {
    logger.info("Processing someValuesFrom classes");
    ResourceIterator<Map<String, Object>> results = graph.runCypherQuery(
        "START svf = node(*) " +
            "MATCH n-[:SUBCLASS_OF]->svf " +
 "WHERE (has(svf.type) AND svf.type = 'OWLObjectSomeValuesFrom') " +
        "RETURN n, svf");
    while (results.hasNext()) {
      Map<String, Object> result = results.next();
      Node subject = (Node)result.get("n");
      Node svf = (Node)result.get("svf");
      Node property = getFirst(svf.getRelationships(EdgeType.PROPERTY), null).getEndNode();
      // TODO: This could contain multiple nodes
      for (Relationship r : svf.getRelationships(EdgeType.CLASS)) {
        Node object = r.getEndNode();
        String relationshipName = graph.getProperty(property, CommonProperties.FRAGMENT,
            String.class).get();
        RelationshipType type = DynamicRelationshipType.withName(relationshipName);
        String propertyUri = graph.getProperty(property, CommonProperties.URI, String.class).get();
        graph.getOrCreateRelationship(subject, object, type, propertyUri);
      }
    }
  }

  public void processCategories(Node root, RelationshipType type, String category) {
    for (Path position : graph.getGraphDb().traversalDescription()
        .uniqueness(Uniqueness.NODE_GLOBAL)
        .depthFirst()
        .relationships(type, Direction.OUTGOING)
        .traverse(root)) {
      Node end = position.endNode();
      graph.addProperty(end, Concept.CATEGORY, category);
    }
  }

  public void postProcess() {
    processSomeValuesFrom();

    logger.info("Processing categories");
    for (Entry<String, String> category: categoryMap.entrySet()) {
      Node root = graph.getOrCreateNode(category.getKey());
      processCategories(root, OwlRelationships.RDF_SUBCLASS_OF, category.getValue());
    }

  }

}
