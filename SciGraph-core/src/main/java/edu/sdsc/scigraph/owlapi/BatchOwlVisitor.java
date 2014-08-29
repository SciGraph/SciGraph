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

import static com.google.common.collect.Lists.transform;
import static edu.sdsc.scigraph.owlapi.OwlApiUtils.getTypedLiteralValue;
import static edu.sdsc.scigraph.owlapi.OwlApiUtils.getUri;
import static java.lang.String.format;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.RelationshipType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
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
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.EdgeProperties;
import edu.sdsc.scigraph.neo4j.BatchGraph;
import edu.sdsc.scigraph.neo4j.EdgeType;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.MappedProperty;

public class BatchOwlVisitor extends OWLOntologyWalkerVisitor<Void> {

  private static final Logger logger = Logger.getLogger(BatchOwlVisitor.class.getName());

  private final BatchGraph graph;

  private OWLOntology ontology;

  private Map<String, String> curieMap;

  private Map<String, String> mappedProperties;

  private OWLOntology parentOntology = null;

  @Inject
  public BatchOwlVisitor(OWLOntologyWalker walker, BatchGraph graph, Map<String, String> curieMap,
      List<MappedProperty> mappedProperties) {
    super(walker);
    this.graph = graph;
    this.curieMap = curieMap;
    this.mappedProperties = new HashMap<>();
    for (MappedProperty mappedProperty : mappedProperties) {
      for (String property : mappedProperty.getProperties()) {
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

  Optional<String> getCurie(String iri) {
    for (Entry<String, String> prefix : curieMap.entrySet()) {
      String key = prefix.getKey();
      if (iri.startsWith(key)) {
        String currie = format("%s:%s", prefix.getValue(), iri.substring(key.length()));
        return Optional.of(currie);
      }
    }
    return Optional.absent();
  }

  private long getOrCreateNode(URI uri) {
    long node = graph.getNode(uri.toString());
    graph.setNodeProperty(node, CommonProperties.FRAGMENT, GraphUtil.getFragment(uri));
    return node;
  }

  @Override
  public Void visit(OWLDeclarationAxiom axiom) {
    URI uri = getUri(axiom);
    long node = getOrCreateNode(uri);
    graph.addLabel(node, OwlLabels.OWL_CLASS);
    return null;
  }

  @Override
  public Void visit(OWLClass desc) {
    URI uri = getUri(desc);
    long node = getOrCreateNode(uri);
    graph.addLabel(node, OwlLabels.OWL_CLASS);
    /*
     * graph.setProperty(node, NodeProperties.ANONYMOUS, false);
     * 
     * if (null != ontology.getOntologyID().getOntologyIRI()) { graph.setProperty(node,
     * NodeProperties.ONTOLOGY, ontology.getOntologyID().getOntologyIRI() .toString()); } if (null
     * != parentOntology.getOntologyID().getOntologyIRI()) { graph.setProperty(node,
     * NodeProperties.PARENT_ONTOLOGY, parentOntology.getOntologyID() .getOntologyIRI().toString());
     * } if (null != ontology.getOntologyID().getVersionIRI()) { graph.setProperty(node,
     * NodeProperties.ONTOLOGY_VERSION, ontology.getOntologyID() .getVersionIRI()); }
     * Optional<String> curie = getCurie(getUri(desc).toString()); if (curie.isPresent()) {
     * graph.setProperty(node, CommonProperties.CURIE, curie.get()); }
     */
    return null;
  }

  @Override
  public Void visit(OWLDataProperty property) {
    long node = getOrCreateNode(property.getIRI().toURI());
    graph.setLabel(node, OwlLabels.OWL_DATA_PROPERTY);
    return null;
  }

  @Override
  public Void visit(OWLObjectProperty property) {
    long node = getOrCreateNode(property.getIRI().toURI());
    graph.setLabel(node, OwlLabels.OWL_OBJECT_PROPERTY);
    graph.setNodeProperty(node, EdgeProperties.SYMMETRIC, !property.isAsymmetric(ontology));
    graph.setNodeProperty(node, EdgeProperties.REFLEXIVE, property.isReflexive(ontology));
    graph.setNodeProperty(node, EdgeProperties.TRANSITIVE, property.isTransitive(ontology));
    return null;
  }

  @Override
  public Void visit(OWLAnnotationAssertionAxiom axiom) {
    if (axiom.getSubject() instanceof IRI) {
      long subject = getOrCreateNode(((IRI) axiom.getSubject()).toURI());

      String property = getUri(axiom.getProperty()).toString();
      if (axiom.getValue() instanceof OWLLiteral) {
        Optional<Object> literal = getTypedLiteralValue((OWLLiteral) (axiom.getValue()));
        if (literal.isPresent()) {
          try {
            graph.addProperty(subject, property, literal.get());

            if (mappedProperties.containsKey(property)) {
              graph.addProperty(subject, mappedProperties.get(property), literal.get());
            }
          } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to add property: " + property + " with value "
                + literal.get().toString(), e);
          }
        }
      } else if (axiom.getValue() instanceof IRI) {
        long object = getOrCreateNode(((IRI) axiom.getValue()).toURI());
        URI uri = Graph.getURI(property);
        String fragment = GraphUtil.getFragment(uri);
        long assertion = graph.createRelationship(subject, object,
            DynamicRelationshipType.withName(fragment));
        graph.setRelationshipProperty(assertion, CommonProperties.URI, uri.toString());
        graph.setRelationshipProperty(assertion, CommonProperties.OWL_TYPE,
            OwlRelationships.OWL_ANNOTATION.name());
      }
    } else {
      logger.fine("Ignoring non IRI assertion axiom: " + axiom.toString());
    }
    return null;
  }

  @Override
  public Void visit(OWLSubAnnotationPropertyOfAxiom axiom) {
    long subProperty = getOrCreateNode(getUri(axiom.getSubProperty()));
    graph.addLabel(subProperty, OwlLabels.OWL_ANNOTATION_PROPERTY);
    long superProperty = getOrCreateNode(getUri(axiom.getSuperProperty()));
    graph.addLabel(superProperty, OwlLabels.OWL_ANNOTATION_PROPERTY);
    graph.createRelationship(subProperty, superProperty, OwlRelationships.RDFS_SUB_PROPERTY_OF);
    return null;
  }

  @Override
  public Void visit(OWLNamedIndividual individual) {
    long node = getOrCreateNode(getUri(individual));
    graph.addLabel(node, OwlLabels.OWL_NAMED_INDIVIDUAL);
    return null;
  }

  @Override
  public Void visit(OWLSameIndividualAxiom axiom) {
    List<Long> nodes = transform(axiom.getIndividualsAsList(), new Function<OWLIndividual, Long>() {
      @Override
      public Long apply(OWLIndividual individual) {
        return getOrCreateNode(getUri(individual));
      }
    });
    graph.createRelationshipPairwise(nodes, OwlRelationships.OWL_SAME_AS);
    return null;
  }

  @Override
  public Void visit(OWLDifferentIndividualsAxiom axiom) {
    List<Long> nodes = transform(axiom.getIndividualsAsList(), new Function<OWLIndividual, Long>() {
      @Override
      public Long apply(OWLIndividual individual) {
        return getOrCreateNode(getUri(individual));
      }
    });
    graph.createRelationshipPairwise(nodes, OwlRelationships.OWL_DIFFERENT_FROM);
    return null;
  }

  @Override
  public Void visit(OWLClassAssertionAxiom axiom) {
    long individual = getOrCreateNode(getUri(axiom.getIndividual()));
    long type = getOrCreateNode(getUri(axiom.getClassExpression()));
    graph.createRelationship(individual, type, OwlRelationships.RDF_TYPE);
    return null;
  }

  @Override
  public Void visit(OWLDataPropertyAssertionAxiom axiom) {
    long individual = getOrCreateNode(getUri(axiom.getSubject()));
    String property = axiom.getProperty().asOWLDataProperty().getIRI().toString();
    Optional<Object> literal = getTypedLiteralValue(axiom.getObject());
    if (literal.isPresent()) {
      graph.setNodeProperty(individual, property, literal.get());
      if (mappedProperties.containsKey(property)) {
        graph.addProperty(individual, mappedProperties.get(property), literal.get());
      }
    }
    return null;
  }

  @Override
  public Void visit(OWLSubClassOfAxiom axiom) {
    long subclass = getOrCreateNode(getUri(axiom.getSubClass()));
    long superclass = getOrCreateNode(getUri(axiom.getSuperClass()));
    graph.createRelationship(subclass, superclass, OwlRelationships.RDF_SUBCLASS_OF);
    return null;
  }

  @Override
  public Void visit(OWLObjectIntersectionOf desc) {
    long subject = getOrCreateNode(getUri(desc));
    graph.setLabel(subject, OwlLabels.OWL_INTERSECTION_OF);
    graph.addLabel(subject, OwlLabels.OWL_ANONYMOUS);
    for (OWLClassExpression expression : desc.getOperands()) {
      long object = getOrCreateNode(getUri(expression));
      graph.createRelationship(subject, object, EdgeType.OPERAND);
    }
    return null;
  }

  @Override
  public Void visit(OWLObjectUnionOf desc) {
    long subject = getOrCreateNode(getUri(desc));
    graph.setLabel(subject, OwlLabels.OWL_UNION_OF);
    graph.addLabel(subject, OwlLabels.OWL_ANONYMOUS);
    for (OWLClassExpression expression : desc.getOperands()) {
      long object = getOrCreateNode(getUri(expression));
      graph.createRelationship(subject, object, EdgeType.OPERAND);
    }
    return null;
  }

  long getObjectPropertyRelationship(
      OWLPropertyAssertionAxiom<OWLObjectPropertyExpression, OWLIndividual> axiom) {
    long subject = getOrCreateNode(getUri(axiom.getSubject()));
    URI property = getUri(axiom.getProperty());
    long object = getOrCreateNode(getUri(axiom.getObject()));
    RelationshipType type = DynamicRelationshipType.withName(property.toString());
    if (null != GraphUtil.getFragment(property)) {
      type = DynamicRelationshipType.withName(GraphUtil.getFragment(property));
    }
    long relationship = graph.createRelationship(subject, object, type);
    graph.setRelationshipProperty(relationship, CommonProperties.URI, property.toString());
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
    List<Long> nodes = transform(axiom.getClassExpressionsAsList(),
        new Function<OWLClassExpression, Long>() {

          @Override
          public Long apply(OWLClassExpression expr) {
            return getOrCreateNode(getUri(expr));
          }
        });

    graph.createRelationshipPairwise(nodes, OwlRelationships.OWL_EQUIVALENT_CLASS);
    return null;
  }

  @Override
  public Void visit(OWLDisjointClassesAxiom axiom) {
    List<Long> nodes = transform(axiom.getClassExpressionsAsList(),
        new Function<OWLClassExpression, Long>() {

          @Override
          public Long apply(OWLClassExpression individual) {
            return getOrCreateNode(getUri(individual));
          }
        });

    graph.createRelationshipPairwise(nodes, OwlRelationships.OWL_DISJOINT_WITH);
    return null;
  }

  @Override
  public Void visit(OWLObjectComplementOf desc) {
    long subject = getOrCreateNode(getUri(desc));
    graph.setLabel(subject, OwlLabels.OWL_COMPLEMENT_OF);
    graph.addLabel(subject, OwlLabels.OWL_ANONYMOUS);
    long operand = getOrCreateNode(getUri(desc.getOperand()));
    graph.createRelationship(subject, operand, EdgeType.OPERAND);
    return null;
  }

  @Override
  public Void visit(OWLSubObjectPropertyOfAxiom axiom) {
    long subProperty = getOrCreateNode(getUri(axiom.getSubProperty()));
    long superProperty = getOrCreateNode(getUri(axiom.getSuperProperty()));
    graph.createRelationship(subProperty, superProperty, OwlRelationships.RDFS_SUB_PROPERTY_OF);
    return null;
  }

  @Override
  public Void visit(OWLSubPropertyChainOfAxiom axiom) {
    long chain = getOrCreateNode(getUri(axiom.getSuperProperty()));
    int i = 0;
    for (OWLObjectPropertyExpression property : axiom.getPropertyChain()) {
      long link = getOrCreateNode(getUri(property));
      long relationship = graph.createRelationship(chain, link,
          OwlRelationships.OWL_PROPERTY_CHAIN_AXIOM);
      graph.setRelationshipProperty(relationship, "order", i++);
    }
    return null;
  }

  /*
   * Node addQuantifiedRestriction(OWLQuantifiedObjectRestriction desc) { Node restriction =
   * graph.getOrCreateNode(getUri(desc)); Node property =
   * graph.getOrCreateNode(getUri(desc.getProperty())); graph.getOrCreateRelationship(restriction,
   * property, EdgeType.PROPERTY); Node cls = graph.getOrCreateNode(getUri(desc.getFiller()));
   * graph.getOrCreateRelationship(restriction, cls, EdgeType.CLASS); return restriction; }
   */
  long addCardinalityRestriction(OWLObjectCardinalityRestriction desc) {
    long restriction = getOrCreateNode(getUri(desc));
    graph.setNodeProperty(restriction, "cardinality", desc.getCardinality());
    long property = getOrCreateNode(getUri(desc.getProperty()));
    graph.createRelationship(restriction, property, EdgeType.PROPERTY);
    long cls = getOrCreateNode(getUri(desc.getFiller()));
    graph.createRelationship(restriction, cls, EdgeType.CLASS);
    return restriction;
  }

  @Override
  public Void visit(OWLObjectMaxCardinality desc) {
    long restriction = addCardinalityRestriction(desc);
    graph.setLabel(restriction, OwlLabels.OWL_MAX_CARDINALITY);
    return null;
  }

  @Override
  public Void visit(OWLObjectMinCardinality desc) {
    long restriction = addCardinalityRestriction(desc);
    graph.setLabel(restriction, OwlLabels.OWL_MIN_CARDINALITY);
    return null;
  }

  @Override
  public Void visit(OWLObjectExactCardinality desc) {
    long restriction = addCardinalityRestriction(desc);
    graph.setLabel(restriction, OwlLabels.OWL_QUALIFIED_CARDINALITY);
    return null;
  }

  @Override
  public Void visit(OWLObjectSomeValuesFrom desc) {
    long restriction = getOrCreateNode(getUri(desc));
    graph.setLabel(restriction, OwlLabels.OWL_SOME_VALUES_FROM);
    graph.addLabel(restriction, OwlLabels.OWL_ANONYMOUS);
    if (!desc.getProperty().isAnonymous()) {
      long property = getOrCreateNode(getUri(desc.getProperty()));
      graph.createRelationship(restriction, property, EdgeType.PROPERTY);
      long cls = getOrCreateNode(getUri(desc.getFiller()));
      graph.createRelationship(restriction, cls, EdgeType.FILLER);
    }
    return null;
  }

  @Override
  public Void visit(OWLObjectAllValuesFrom desc) {
    long restriction = getOrCreateNode(getUri(desc));
    graph.setLabel(restriction, OwlLabels.OWL_ALL_VALUES_FROM);
    graph.addLabel(restriction, OwlLabels.OWL_ANONYMOUS);
    if (!desc.getProperty().isAnonymous()) {
      long property = getOrCreateNode(getUri(desc.getProperty()));
      graph.createRelationship(restriction, property, EdgeType.PROPERTY);
      long cls = getOrCreateNode(getUri(desc.getFiller()));
      graph.createRelationship(restriction, cls, EdgeType.FILLER);
    }
    return null;
  }

}
