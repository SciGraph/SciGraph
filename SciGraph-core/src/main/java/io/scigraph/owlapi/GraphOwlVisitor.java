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
package io.scigraph.owlapi;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.transform;
import static io.scigraph.owlapi.OwlApiUtils.getIri;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
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
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
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
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import io.scigraph.frames.CommonProperties;
import io.scigraph.frames.EdgeProperties;
import io.scigraph.neo4j.Graph;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.MappedProperty;

/***
 * The core of the code for translating owlapi axioms into Neo4j structure.
 */
public class GraphOwlVisitor extends OWLOntologyWalkerVisitor<Void> {

  private static final Logger logger = Logger.getLogger(GraphOwlVisitor.class.getName());

  private final Graph graph;

  private Optional<OWLOntology> ontology = Optional.empty();

  private String definingOntology;

  private Map<String, String> mappedProperties;

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

  public void setOntology(String ontology) {
    this.definingOntology = checkNotNull(ontology);
  }

  @Override
  public Void visit(OWLOntology ontology) {
    this.ontology = Optional.of(ontology);
    this.definingOntology = OwlApiUtils.getIri(ontology);
    Long versionNodeID = null;
    Long ontologyNodeID = null;
    OWLOntologyID id = ontology.getOntologyID();
    if (null == id.getOntologyIRI()) {
      logger.fine("Ignoring null ontology ID for " + ontology.toString());
    } else {
      ontologyNodeID = getOrCreateNode(id.getOntologyIRI().toString(), OwlLabels.OWL_ONTOLOGY);
    }
    if (null != id.getVersionIRI()){
      versionNodeID = getOrCreateNode(id.getVersionIRI().toString(), OwlLabels.OWL_ONTOLOGY);
    }
    if (null != ontologyNodeID && null != versionNodeID) {
      graph.createRelationship(ontologyNodeID, versionNodeID, OwlRelationships.OWL_VERSION_IRI);
    }
    return null;
  }

  private long addDefinedBy(Long node) {
    long ontologyNode = graph.getNode(definingOntology).get();
    return graph.createRelationship(node, ontologyNode, OwlRelationships.RDFS_IS_DEFINED_BY);
  }

  private long getOrCreateNode(String iri, Label... labels) {
    Optional<Long> node = graph.getNode(iri);
    if (!node.isPresent()) {
      long nodeId = graph.createNode(iri);
      graph.setNodeProperty(nodeId, CommonProperties.IRI, iri);
      node = Optional.of(nodeId);
    }
    for (Label label : labels) {
      graph.addLabel(node.get(), label);
    }
    return node.get();
  }

  private long getOrCreateRelationship(long start, long end, RelationshipType type) {
    long relationship = graph.createRelationship(start, end, type);
    graph.addRelationshipProperty(relationship, OwlRelationships.RDFS_IS_DEFINED_BY.name(),
        definingOntology);
    return relationship;
  }

  private Collection<Long> getOrCreateRelationshipPairwise(Collection<Long> nodeIds,
      RelationshipType type) {
    Collection<Long> relationships = graph.createRelationshipsPairwise(nodeIds, type);
    for (long relationship : relationships) {
      graph.addRelationshipProperty(relationship, OwlRelationships.RDFS_IS_DEFINED_BY.name(),
          definingOntology);
    }
    return relationships;
  }

  @Override
  public Void visit(OWLClass desc) {
    String iri = getIri(desc);
    getOrCreateNode(iri, OwlLabels.OWL_CLASS);
    return null;
  }

  @Override
  public Void visit(OWLObjectProperty property) {
    String iri = getIri(property);
    getOrCreateNode(iri, OwlLabels.OWL_OBJECT_PROPERTY);
    return null;
  }

  @Override
  public Void visit(OWLNamedIndividual individual) {
    String iri = getIri(individual);
    long individualNode = getOrCreateNode(iri, OwlLabels.OWL_NAMED_INDIVIDUAL);
    if (individual.isAnonymous()) {
      graph.addLabel(individualNode, OwlLabels.OWL_ANONYMOUS);
    }
    return null;
  }

  @Override
  public Void visit(OWLDeclarationAxiom axiom) {
    String iri = getIri(axiom);
    long node = getOrCreateNode(iri);
    addDefinedBy(node);
    if (axiom.getEntity() instanceof OWLClass) {
      graph.addLabel(node, OwlLabels.OWL_CLASS);
    } else if (axiom.getEntity() instanceof OWLNamedIndividual) {
      graph.addLabel(node, OwlLabels.OWL_NAMED_INDIVIDUAL);
    } else if (axiom.getEntity() instanceof OWLObjectProperty) {
      if (!graph.getLabels(node).contains(OwlLabels.OWL_OBJECT_PROPERTY)) {
        graph.addLabel(node, OwlLabels.OWL_OBJECT_PROPERTY);
        if (ontology.isPresent()) {
          OWLObjectProperty property = (OWLObjectProperty) axiom.getEntity();
          graph.setNodeProperty(node, EdgeProperties.SYMMETRIC,
              !property.isAsymmetric(ontology.get()));
          graph.setNodeProperty(node, EdgeProperties.REFLEXIVE,
              property.isReflexive(ontology.get()));
          graph.setNodeProperty(node, EdgeProperties.TRANSITIVE,
              property.isTransitive(ontology.get()));
        }
      }
    } else if (axiom.getEntity() instanceof OWLDataProperty) {
      graph.setLabel(node, OwlLabels.OWL_DATA_PROPERTY);
    } else {
      // logger.warning("Unhandled declaration type " + axiom.getEntity().getClass().getName());
    }
    return null;
  }

  @Override
  public Void visit(OWLAnnotationAssertionAxiom axiom) {
    if ((axiom.getSubject() instanceof IRI)
        || (axiom.getSubject() instanceof OWLAnonymousIndividual)) {
      long subject = 0L;
      if (axiom.getSubject() instanceof IRI) {
        subject = getOrCreateNode(((IRI) axiom.getSubject()).toString());
      } else if (axiom.getSubject() instanceof OWLAnonymousIndividual) {
        subject = getOrCreateNode(OwlApiUtils.getIri((OWLAnonymousIndividual) axiom.getSubject()));
      }

      String property = getIri(axiom.getProperty()).toString();
      if (axiom.getValue() instanceof OWLLiteral) {
        Optional<Object> literal =
            OwlApiUtils.getTypedLiteralValue((OWLLiteral) (axiom.getValue()));
        if (literal.isPresent()) {
          graph.addNodeProperty(subject, property, literal.get());
          if (mappedProperties.containsKey(property)) {
            graph.addNodeProperty(subject, mappedProperties.get(property), literal.get());
          }
        }
      } else if ((axiom.getValue() instanceof IRI)
          || (axiom.getValue() instanceof OWLAnonymousIndividual)) {
        long object = 0L;
        if (axiom.getValue() instanceof IRI) {
          object = getOrCreateNode(((IRI) axiom.getValue()).toString());
        } else if (axiom.getValue() instanceof OWLAnonymousIndividual) {
          object = getOrCreateNode(OwlApiUtils.getIri((OWLAnonymousIndividual) axiom.getValue()));
        }
        long assertion =
            getOrCreateRelationship(subject, object, RelationshipType.withName(property));
        graph.setRelationshipProperty(assertion, CommonProperties.IRI, property);
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
    long subProperty =
        getOrCreateNode(getIri(axiom.getSubProperty()), OwlLabels.OWL_ANNOTATION_PROPERTY);
    long superProperty =
        getOrCreateNode(getIri(axiom.getSuperProperty()), OwlLabels.OWL_ANNOTATION_PROPERTY);
    getOrCreateRelationship(subProperty, superProperty, OwlRelationships.RDFS_SUB_PROPERTY_OF);
    return null;
  }

  @Override
  public Void visit(OWLSameIndividualAxiom axiom) {
    List<Long> nodes = transform(axiom.getIndividualsAsList(), new Function<OWLIndividual, Long>() {
      @Override
      public Long apply(OWLIndividual individual) {
        return getOrCreateNode(getIri(individual));
      }
    });
    getOrCreateRelationshipPairwise(nodes, OwlRelationships.OWL_SAME_AS);
    return null;
  }

  @Override
  public Void visit(OWLDifferentIndividualsAxiom axiom) {
    List<Long> nodes = transform(axiom.getIndividualsAsList(), new Function<OWLIndividual, Long>() {
      @Override
      public Long apply(OWLIndividual individual) {
        return getOrCreateNode(getIri(individual));
      }
    });
    getOrCreateRelationshipPairwise(nodes, OwlRelationships.OWL_DIFFERENT_FROM);
    return null;
  }

  @Override
  public Void visit(OWLClassAssertionAxiom axiom) {
    long individual = getOrCreateNode(getIri(axiom.getIndividual()));
    if (axiom.getIndividual().isAnonymous()) {
      graph.addLabel(individual, OwlLabels.OWL_ANONYMOUS);
    }
    long type = getOrCreateNode(getIri(axiom.getClassExpression()));
    getOrCreateRelationship(individual, type, OwlRelationships.RDF_TYPE);
    return null;
  }

  @Override
  public Void visit(OWLDataPropertyAssertionAxiom axiom) {
    long individual = getOrCreateNode(getIri(axiom.getSubject()));
    OWLDataProperty property = axiom.getProperty().asOWLDataProperty();
    // TODO: fix this Set<OWLDataRange> ranges = property.getRanges(ontology);
    // Except without the ontology we can't verify the range...
    String propertyName = property.getIRI().toString();
    Optional<Object> literal = OwlApiUtils.getTypedLiteralValue(axiom.getObject());

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
    long subclass = getOrCreateNode(getIri(axiom.getSubClass()));
    long superclass = getOrCreateNode(getIri(axiom.getSuperClass()));
    long relationship =
        getOrCreateRelationship(subclass, superclass, OwlRelationships.RDFS_SUBCLASS_OF);
    for (OWLAnnotation annotation : axiom.getAnnotations()) {
      // TODO: Is there a more elegant way to process these annotations?
      String property = annotation.getProperty().getIRI().toString();
      if (annotation.getValue() instanceof OWLLiteral) {
        Optional<Object> value =
            OwlApiUtils.getTypedLiteralValue((OWLLiteral) annotation.getValue());
        if (value.isPresent()) {
          graph.addRelationshipProperty(relationship, property, value.get());
        }
      }
    }
    return null;
  }

  @Override
  public Void visit(OWLObjectIntersectionOf desc) {
    long subject =
        getOrCreateNode(getIri(desc), OwlLabels.OWL_INTERSECTION_OF, OwlLabels.OWL_ANONYMOUS);
    for (OWLClassExpression expression : desc.getOperands()) {
      long object = getOrCreateNode(getIri(expression));
      getOrCreateRelationship(subject, object, OwlRelationships.OPERAND);
    }
    return null;
  }

  @Override
  public Void visit(OWLObjectUnionOf desc) {
    long subject = getOrCreateNode(getIri(desc), OwlLabels.OWL_UNION_OF, OwlLabels.OWL_ANONYMOUS);
    for (OWLClassExpression expression : desc.getOperands()) {
      long object = getOrCreateNode(getIri(expression));
      getOrCreateRelationship(subject, object, OwlRelationships.OPERAND);
    }
    return null;
  }

  long getObjectPropertyRelationship(
      OWLPropertyAssertionAxiom<OWLObjectPropertyExpression, OWLIndividual> axiom) {
    long subject = getOrCreateNode(getIri(axiom.getSubject()));
    String property = getIri(axiom.getProperty());
    long object = getOrCreateNode(getIri(axiom.getObject()));
    RelationshipType type = RelationshipType.withName(property.toString());

    long relationship = getOrCreateRelationship(subject, object, type);
    graph.setRelationshipProperty(relationship, CommonProperties.IRI, property.toString());
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
            return getOrCreateNode(getIri(expr));
          }
        });

    getOrCreateRelationshipPairwise(nodes, OwlRelationships.OWL_EQUIVALENT_CLASS);
    return null;
  }

  @Override
  public Void visit(OWLDisjointClassesAxiom axiom) {
    List<Long> nodes =
        transform(axiom.getClassExpressionsAsList(), new Function<OWLClassExpression, Long>() {

          @Override
          public Long apply(OWLClassExpression individual) {
            return getOrCreateNode(getIri(individual));
          }
        });

    getOrCreateRelationshipPairwise(nodes, OwlRelationships.OWL_DISJOINT_WITH);
    return null;
  }

  @Override
  public Void visit(OWLObjectComplementOf desc) {
    long subject =
        getOrCreateNode(getIri(desc), OwlLabels.OWL_COMPLEMENT_OF, OwlLabels.OWL_ANONYMOUS);
    long operand = getOrCreateNode(getIri(desc.getOperand()));
    getOrCreateRelationship(subject, operand, OwlRelationships.OPERAND);
    return null;
  }

  @Override
  public Void visit(OWLSubObjectPropertyOfAxiom axiom) {
    long subProperty = getOrCreateNode(getIri(axiom.getSubProperty()));
    long superProperty = getOrCreateNode(getIri(axiom.getSuperProperty()));
    getOrCreateRelationship(subProperty, superProperty, OwlRelationships.RDFS_SUB_PROPERTY_OF);
    return null;
  }

  @Override
  public Void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
    Set<OWLObjectPropertyExpression> properties = axiom.getProperties();
    boolean anonymousPropertyExists = false;
    for (OWLObjectPropertyExpression property : properties) {
      anonymousPropertyExists = anonymousPropertyExists || property.isAnonymous();
    }

    // #217 - in case of EquivalentObjectProperties(:p ObjectInverseOf(:q))
    if (!anonymousPropertyExists) {
      Collection<Long> nodes = Collections2.transform(axiom.getObjectPropertiesInSignature(),
          new Function<OWLObjectProperty, Long>() {

            @Override
            public Long apply(OWLObjectProperty objectProperty) {
              return getOrCreateNode(getIri(objectProperty));
            }
          });

      getOrCreateRelationshipPairwise(nodes, OwlRelationships.OWL_EQUIVALENT_OBJECT_PROPERTY);
    }
    return null;
  }

  @Override
  public Void visit(OWLSubPropertyChainOfAxiom axiom) {
    long chain = getOrCreateNode(getIri(axiom.getSuperProperty()));
    int i = 0;
    for (OWLObjectPropertyExpression property : axiom.getPropertyChain()) {
      long link = getOrCreateNode(getIri(property));
      long relationship =
          getOrCreateRelationship(chain, link, OwlRelationships.OWL_PROPERTY_CHAIN_AXIOM);
      graph.setRelationshipProperty(relationship, "order", i++);
    }
    return null;
  }

  long addCardinalityRestriction(OWLObjectCardinalityRestriction desc) {
    long restriction = getOrCreateNode(getIri(desc), OwlLabels.OWL_ANONYMOUS);
    graph.setNodeProperty(restriction, "cardinality", desc.getCardinality());
    long property = getOrCreateNode(getIri(desc.getProperty()));
    getOrCreateRelationship(restriction, property, OwlRelationships.PROPERTY);
    long cls = getOrCreateNode(getIri(desc.getFiller()));
    getOrCreateRelationship(restriction, cls, OwlRelationships.CLASS);
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
    long restriction =
        getOrCreateNode(getIri(desc), OwlLabels.OWL_SOME_VALUES_FROM, OwlLabels.OWL_ANONYMOUS);
    if (!desc.getProperty().isAnonymous()) {
      long property = getOrCreateNode(getIri(desc.getProperty()));
      getOrCreateRelationship(restriction, property, OwlRelationships.PROPERTY);
      long cls = getOrCreateNode(getIri(desc.getFiller()));
      getOrCreateRelationship(restriction, cls, OwlRelationships.FILLER);
    }
    return null;
  }

  @Override
  public Void visit(OWLObjectAllValuesFrom desc) {
    long restriction =
        getOrCreateNode(getIri(desc), OwlLabels.OWL_ALL_VALUES_FROM, OwlLabels.OWL_ANONYMOUS);
    if (!desc.getProperty().isAnonymous()) {
      long property = getOrCreateNode(getIri(desc.getProperty()));
      getOrCreateRelationship(restriction, property, OwlRelationships.PROPERTY);
      long cls = getOrCreateNode(getIri(desc.getFiller()));
      getOrCreateRelationship(restriction, cls, OwlRelationships.FILLER);
    }
    return null;
  }

}
