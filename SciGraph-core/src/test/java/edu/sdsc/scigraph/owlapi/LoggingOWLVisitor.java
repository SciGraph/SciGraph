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

import java.util.logging.Logger;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataComplementOf;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataIntersectionOf;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataOneOf;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDataUnionOf;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFacetRestriction;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

public class LoggingOWLVisitor extends OWLOntologyWalkerVisitor<Object> {

  private static final Logger logger = Logger.getLogger(LoggingOWLVisitor.class.getName());
  
  private OWLOntology ontology;
  
  public LoggingOWLVisitor(OWLOntologyWalker walker) {
    super(walker);
  }

  @Override
  public Object visit(OWLAnnotationAssertionAxiom axiom) {
    logger.info(axiom.getSubject().toString());
    logger.info(axiom.getClass().getName() + ": " + axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLAsymmetricObjectPropertyAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLClassAssertionAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLDataPropertyAssertionAxiom axiom) {
    logger.info("Data subject " + axiom.getSubject().toString());
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLDataPropertyDomainAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLDataPropertyRangeAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLDeclarationAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLDifferentIndividualsAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLDisjointClassesAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLDisjointDataPropertiesAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLDisjointObjectPropertiesAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLDisjointUnionAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLEquivalentClassesAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLEquivalentDataPropertiesAxiom axiom) {
    // TODO Auto-generated method stub
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLEquivalentObjectPropertiesAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLFunctionalDataPropertyAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLFunctionalObjectPropertyAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLHasKeyAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLInverseObjectPropertiesAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLObjectPropertyAssertionAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLSubPropertyChainOfAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLObjectPropertyDomainAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLObjectPropertyRangeAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLReflexiveObjectPropertyAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLSameIndividualAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLSubClassOfAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    logger.info("Annotations: " + axiom.getAnnotations().toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLSubDataPropertyOfAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLSubObjectPropertyOfAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLSymmetricObjectPropertyAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLTransitiveObjectPropertyAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLClass desc) {
    logger.info(desc.getClass().getName() + desc.toString());
    logger.info(desc.asOWLClass().getAnnotations(ontology).toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLDataAllValuesFrom desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLDataExactCardinality desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLDataMaxCardinality desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLDataMinCardinality desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLDataSomeValuesFrom desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLDataHasValue desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLObjectAllValuesFrom desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLObjectComplementOf desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLObjectExactCardinality desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLObjectHasSelf desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLObjectHasValue desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLObjectIntersectionOf desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLObjectMaxCardinality desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLObjectMinCardinality desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLObjectOneOf desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLObjectSomeValuesFrom desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLObjectUnionOf desc) {
    logger.info(desc.toString());
    return super.visit(desc);
  }

  @Override
  public Object visit(OWLDataComplementOf node) {
    logger.info(node.getClass().getName() + " " + node.toString());
    return super.visit(node);
  }

  @Override
  public Object visit(OWLDataIntersectionOf node) {
    logger.info(node.getClass().getName() + " " + node.toString());
    return super.visit(node);
  }

  @Override
  public Object visit(OWLDataOneOf node) {
    logger.info(node.getClass().getName() + " " + node.toString());
    return super.visit(node);
  }

  @Override
  public Object visit(OWLDatatype node) {
    logger.info(node.getClass().getName() + " " + node.toString());
    return super.visit(node);
  }

  @Override
  public Object visit(OWLDatatypeRestriction node) {
    logger.info(node.getClass().getName() + " " + node.toString());
    return super.visit(node);
  }

  @Override
  public Object visit(OWLDataUnionOf node) {
    logger.info(node.getClass().getName() + " " + node.getClass().getName() + " " + node.toString());
    return super.visit(node);
  }

  @Override
  public Object visit(OWLFacetRestriction node) {
    logger.info(node.getClass().getName() + " " + node.toString());
    return super.visit(node);
  }

  @Override
  public Object visit(OWLDataProperty property) {
    logger.info("OWLDataProperty: " + property.toString());
    return super.visit(property);
  }

  @Override
  public Object visit(OWLObjectProperty property) {
    logger.info("OWLObjectProperty: " + property.toString());
    return super.visit(property);
  }

  @Override
  public Object visit(OWLObjectInverseOf property) {
    logger.info(property.toString());
    return super.visit(property);
  }

  @Override
  public Object visit(OWLNamedIndividual individual) {
    logger.info(individual.toString());
    return super.visit(individual);
  }

  @Override
  public Object visit(OWLAnnotationProperty property) {
    logger.info("Annotation Property " + property.toString());
    return super.visit(property);
  }

  @Override
  public Object visit(OWLAnnotation annotation) {
    logger.info("Annotation " + annotation.toString());
    return super.visit(annotation);
  }

  @Override
  public Object visit(OWLAnnotationPropertyDomainAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLAnnotationPropertyRangeAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLSubAnnotationPropertyOfAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

  @Override
  public Object visit(OWLAnonymousIndividual individual) {
    logger.info(individual.toString());
    return super.visit(individual);
  }

  @Override
  public Object visit(IRI iri) {
    logger.info(iri.toString());
    return super.visit(iri);
  }

  @Override
  public Object visit(OWLLiteral literal) {
    logger.info(literal.toString());
    return super.visit(literal);
  }

  @Override
  public Object visit(OWLOntology ontology) {
    logger.info("Entering ontology: " + ontology.getOntologyID());
    this.ontology = ontology;
    return super.visit(ontology);
  }

  @Override
  public Object visit(OWLDatatypeDefinitionAxiom axiom) {
    logger.info(axiom.getClass().getName() + ": " + axiom.toString());
    return super.visit(axiom);
  }

}
