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
import static edu.sdsc.scigraph.owlapi.OwlApiUtils.getUri;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.RelationshipType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
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
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.MappedProperty;

public class GraphOwlVisitor extends OWLOntologyWalkerVisitor<Void> {

  private static final Logger logger = Logger.getLogger(GraphOwlVisitor.class.getName());

  private final Graph graph;

  private OWLOntology ontology;

  private Map<String, String> mappedProperties;

  public GraphOwlVisitor(Graph graph,
      @Named("owl.mappedProperties") List<MappedProperty> mappedProperties) {
    this(new OWLOntologyWalker(Collections.<OWLOntology>emptySet()), graph, mappedProperties);
  }

  @Inject
  public GraphOwlVisitor(OWLOntologyWalker walker, Graph graph,
      @Named("owl.mappedProperties") List<MappedProperty> mappedProperties) {
    super(walker);
    this.graph = graph;
    this.mappedProperties = new HashMap<>();
    for (MappedProperty mappedProperty : mappedProperties) {
      for (String property : mappedProperty.getProperties()) {
        this.mappedProperties.put(property, mappedProperty.getName());
      }
    }
  }

  public void shutdown() {
    graph.shutdown();
  }

  @Override
  public Void visit(OWLOntology ontology) {
    logger.info("Walking ontology: " + ontology.getOntologyID());
    this.ontology = ontology;
    return null;
  }

  private long getOrCreateNode(URI uri) {
    Optional<Long> node = graph.getNode(uri.toString());
    if (!node.isPresent()) {
      long nodeId = graph.createNode(uri.toString());
      graph.setNodeProperty(nodeId, CommonProperties.URI, uri.toString());
      graph.setNodeProperty(nodeId, CommonProperties.FRAGMENT, GraphUtil.getFragment(uri));
      node = Optional.of(nodeId);
    }
    return node.get();
  }

  @Override
  public Void visit(OWLClass desc) {
    URI uri = getUri(desc);
    long node = getOrCreateNode(uri);
    graph.addLabel(node, OwlLabels.OWL_CLASS);
    return null;
  }

  @Override
  public Void visit(OWLObjectProperty property) {
    URI uri = getUri(property);
    long node = getOrCreateNode(uri);
    graph.addLabel(node, OwlLabels.OWL_OBJECT_PROPERTY);
    return null;
  }

  @Override
  public Void visit(OWLNamedIndividual individual) {
    URI uri = getUri(individual);
    long node = getOrCreateNode(uri);
    graph.addLabel(node, OwlLabels.OWL_NAMED_INDIVIDUAL);
    return null;
  }

  @Override
  public Void visit(OWLDeclarationAxiom axiom) {
    URI uri = getUri(axiom);
    long node = getOrCreateNode(uri);
    if (axiom.getEntity() instanceof OWLClass) {
      graph.addLabel(node, OwlLabels.OWL_CLASS);
    } else if (axiom.getEntity() instanceof OWLNamedIndividual) {
      graph.addLabel(node, OwlLabels.OWL_NAMED_INDIVIDUAL);
    } else if (axiom.getEntity() instanceof OWLObjectProperty) {
      if (!graph.getLabels(node).contains(OwlLabels.OWL_OBJECT_PROPERTY)) {
        graph.addLabel(node, OwlLabels.OWL_OBJECT_PROPERTY);
        OWLObjectProperty property = (OWLObjectProperty) axiom.getEntity();
        graph.setNodeProperty(node, EdgeProperties.SYMMETRIC, !property.isAsymmetric(ontology));
        graph.setNodeProperty(node, EdgeProperties.REFLEXIVE, property.isReflexive(ontology));
        graph.setNodeProperty(node, EdgeProperties.TRANSITIVE, property.isTransitive(ontology));
      }
    } else if (axiom.getEntity() instanceof OWLDataProperty) {
      graph.setLabel(node, OwlLabels.OWL_DATA_PROPERTY);
    } else {
      //logger.warning("Unhandled declaration type " + axiom.getEntity().getClass().getName());
    }
    return null;
  }

  @Override
  public Void visit(OWLAnnotationAssertionAxiom axiom) {
    if ((axiom.getSubject() instanceof IRI)  || (axiom.getValue() instanceof OWLAnonymousIndividual)) {
      long subject = 0L;
      if (axiom.getSubject() instanceof IRI) {
        subject = getOrCreateNode(((IRI) axiom.getSubject()).toURI());
      } else if (axiom.getSubject() instanceof OWLAnonymousIndividual) {
        subject = getOrCreateNode(OwlApiUtils.getUri((OWLAnonymousIndividual)axiom.getSubject()));
      }

      String property = getUri(axiom.getProperty()).toString();
      if (axiom.getValue() instanceof OWLLiteral) {
        //Optional<Object> literal = getTypedLiteralValue((OWLLiteral) (axiom.getValue()));
        OWLLiteral owlLiteral = (OWLLiteral) axiom.getValue();
        if (owlLiteral.hasLang() && !"en".equals(owlLiteral.getLang())) {
          // TODO: Ignore non-english literals for now
          return null;
        }
        //TODO: Store mixed types at some point?
        try {
          graph.addNodeProperty(subject, property, owlLiteral.getLiteral());

          if (mappedProperties.containsKey(property)) {
            graph.addNodeProperty(subject, mappedProperties.get(property), owlLiteral.getLiteral());
          }
        } catch (ClassCastException e) {
          logger.warning("Can't store property arrays with mixed types. Ignoring " + property + " with value "
              + owlLiteral.getLiteral() + " on " + ((IRI) axiom.getSubject()).toURI().toString());
        }
      } else if ((axiom.getValue() instanceof IRI) || (axiom.getValue() instanceof OWLAnonymousIndividual)) {
        long object = 0L;
        if (axiom.getValue() instanceof IRI) {
          object = getOrCreateNode(((IRI) axiom.getValue()).toURI());
        } else if (axiom.getValue() instanceof OWLAnonymousIndividual) {
          object = getOrCreateNode(OwlApiUtils.getUri((OWLAnonymousIndividual)axiom.getValue()));
        }
        URI uri = OwlApiUtils.getURI(property);
        String fragment = GraphUtil.getFragment(uri);
        long assertion =
            graph.createRelationship(subject, object, DynamicRelationshipType.withName(fragment));
        graph.setRelationshipProperty(assertion, CommonProperties.URI, uri.toString());
        graph.setRelationshipProperty(assertion, CommonProperties.OWL_TYPE,
            OwlRelationships.OWL_ANNOTATION.name());
      } else {
        logger.info("Ignoring assertion axiom: " + axiom);
      }
    } else {
      logger.info("Ignoring assertion axiom: " + axiom);
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
  public Void visit(OWLSameIndividualAxiom axiom) {
    List<Long> nodes = transform(axiom.getIndividualsAsList(), new Function<OWLIndividual, Long>() {
      @Override
      public Long apply(OWLIndividual individual) {
        return getOrCreateNode(getUri(individual));
      }
    });
    graph.createRelationshipsPairwise(nodes, OwlRelationships.OWL_SAME_AS);
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
    graph.createRelationshipsPairwise(nodes, OwlRelationships.OWL_DIFFERENT_FROM);
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
    OWLDataProperty property = axiom.getProperty().asOWLDataProperty();
    // TODO: fix this Set<OWLDataRange> ranges = property.getRanges(ontology);
    String propertyName = property.getIRI().toString();
    OWLLiteral owlLiteral = axiom.getObject();
    if (owlLiteral.hasLang() && !"en".equals(owlLiteral.getLang())) {
      // TODO: Ignore non-english literals for now
      return null;
    }
    Optional<Object> literal = Optional.absent();
    // If there's no range assume that this property is a string (#28)
    // Except that without the ontology we can't verify the range...
    literal = Optional.<Object>of(owlLiteral.getLiteral());

    if (literal.isPresent()) {
      graph.setNodeProperty(individual, propertyName, literal.get());
      if (mappedProperties.containsKey(propertyName)) {
        graph.addNodeProperty(individual, mappedProperties.get(propertyName), literal.get());
      }
    }
    return null;
  }

  @Override
  public Void visit(OWLSubClassOfAxiom axiom) {
    long subclass = getOrCreateNode(getUri(axiom.getSubClass()));
    long superclass = getOrCreateNode(getUri(axiom.getSuperClass()));
    graph.createRelationship(subclass, superclass, OwlRelationships.RDFS_SUBCLASS_OF);
    return null;
  }

  @Override
  public Void visit(OWLObjectIntersectionOf desc) {
    long subject = getOrCreateNode(getUri(desc));
    graph.setLabel(subject, OwlLabels.OWL_INTERSECTION_OF);
    graph.addLabel(subject, OwlLabels.OWL_ANONYMOUS);
    for (OWLClassExpression expression : desc.getOperands()) {
      long object = getOrCreateNode(getUri(expression));
      graph.createRelationship(subject, object, OwlRelationships.OPERAND);
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
      graph.createRelationship(subject, object, OwlRelationships.OPERAND);
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
    List<Long> nodes =
        transform(axiom.getClassExpressionsAsList(), new Function<OWLClassExpression, Long>() {

          @Override
          public Long apply(OWLClassExpression expr) {
            return getOrCreateNode(getUri(expr));
          }
        });

    graph.createRelationshipsPairwise(nodes, OwlRelationships.OWL_EQUIVALENT_CLASS);
    return null;
  }

  @Override
  public Void visit(OWLDisjointClassesAxiom axiom) {
    List<Long> nodes =
        transform(axiom.getClassExpressionsAsList(), new Function<OWLClassExpression, Long>() {

          @Override
          public Long apply(OWLClassExpression individual) {
            return getOrCreateNode(getUri(individual));
          }
        });

    graph.createRelationshipsPairwise(nodes, OwlRelationships.OWL_DISJOINT_WITH);
    return null;
  }

  @Override
  public Void visit(OWLObjectComplementOf desc) {
    long subject = getOrCreateNode(getUri(desc));
    graph.setLabel(subject, OwlLabels.OWL_COMPLEMENT_OF);
    graph.addLabel(subject, OwlLabels.OWL_ANONYMOUS);
    long operand = getOrCreateNode(getUri(desc.getOperand()));
    graph.createRelationship(subject, operand, OwlRelationships.OPERAND);
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
      long relationship =
          graph.createRelationship(chain, link, OwlRelationships.OWL_PROPERTY_CHAIN_AXIOM);
      graph.setRelationshipProperty(relationship, "order", i++);
    }
    return null;
  }

  long addCardinalityRestriction(OWLObjectCardinalityRestriction desc) {
    long restriction = getOrCreateNode(getUri(desc));
    graph.addLabel(restriction, OwlLabels.OWL_ANONYMOUS);
    graph.setNodeProperty(restriction, "cardinality", desc.getCardinality());
    long property = getOrCreateNode(getUri(desc.getProperty()));
    graph.createRelationship(restriction, property, OwlRelationships.PROPERTY);
    long cls = getOrCreateNode(getUri(desc.getFiller()));
    graph.createRelationship(restriction, cls, OwlRelationships.CLASS);
    return restriction;
  }

  @Override
  public Void visit(OWLObjectMaxCardinality desc) {
    long restriction = addCardinalityRestriction(desc);
    graph.addLabel(restriction, OwlLabels.OWL_MAX_CARDINALITY);
    return null;
  }

  @Override
  public Void visit(OWLObjectMinCardinality desc) {
    long restriction = addCardinalityRestriction(desc);
    graph.addLabel(restriction, OwlLabels.OWL_MIN_CARDINALITY);
    return null;
  }

  @Override
  public Void visit(OWLObjectExactCardinality desc) {
    long restriction = addCardinalityRestriction(desc);
    graph.addLabel(restriction, OwlLabels.OWL_QUALIFIED_CARDINALITY);
    return null;
  }

  @Override
  public Void visit(OWLObjectSomeValuesFrom desc) {
    long restriction = getOrCreateNode(getUri(desc));
    graph.setLabel(restriction, OwlLabels.OWL_SOME_VALUES_FROM);
    graph.addLabel(restriction, OwlLabels.OWL_ANONYMOUS);
    if (!desc.getProperty().isAnonymous()) {
      long property = getOrCreateNode(getUri(desc.getProperty()));
      graph.createRelationship(restriction, property, OwlRelationships.PROPERTY);
      long cls = getOrCreateNode(getUri(desc.getFiller()));
      graph.createRelationship(restriction, cls, OwlRelationships.FILLER);
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
      graph.createRelationship(restriction, property, OwlRelationships.PROPERTY);
      long cls = getOrCreateNode(getUri(desc.getFiller()));
      graph.createRelationship(restriction, cls, OwlRelationships.FILLER);
    }
    return null;
  }

}
