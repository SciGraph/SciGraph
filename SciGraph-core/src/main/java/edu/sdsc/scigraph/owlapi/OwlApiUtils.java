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

import java.net.URI;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.neo4j.Graph;

public class OwlApiUtils {

  private static final String ANONYMOUS_NODE_PREFIX = "http://ontology.neuinfo.org/anon/";

  /*** 
   * @param literal An OWLLiteral
   * @return an optional correctly typed Java object from the OWLLiteral
   */
  public static Optional<Object> getTypedLiteralValue(OWLLiteral literal) {
    Object literalValue = null;
    if (literal.isBoolean()) {
      literalValue = literal.parseBoolean();
    } else if (literal.isInteger()) {
      literalValue = literal.parseInteger();
    } else if (literal.isFloat()) {
      literalValue = literal.parseFloat();
    } else if (literal.isDouble()) {
      literalValue = literal.parseDouble();
    } else {
      //HACK: Ignore non-english literals for now
      if (literal.hasLang() && !literal.getLang().equals("en")) {
        return Optional.absent();
      }
      literalValue = literal.getLiteral();
    }
    return Optional.of(literalValue);
  }

  public static URI getUri(OWLDeclarationAxiom expression) {
    return expression.getEntity().getIRI().toURI();
  }

  public static URI getUri(OWLClassExpression expression) {
    if (expression.isAnonymous()) {
      return Graph.getURI(ANONYMOUS_NODE_PREFIX + expression.hashCode());
    } else {
      return expression.asOWLClass().getIRI().toURI();
    }
  }

  public static URI getUri(OWLObjectPropertyExpression property) {
    if (property.isAnonymous()) {
      return Graph.getURI(ANONYMOUS_NODE_PREFIX + property.hashCode());
    } else {
      return property.asOWLObjectProperty().getIRI().toURI();
    }
  }

  public static URI getUri(OWLIndividual individual) {
    if (individual.isAnonymous()) {
      return Graph.getURI(ANONYMOUS_NODE_PREFIX + individual.hashCode());
    } else {
      return individual.asOWLNamedIndividual().getIRI().toURI();
    }
  }

  public static URI getUri(OWLAnnotationProperty property) {
    return property.asOWLAnnotationProperty().getIRI().toURI();
  }

  @Deprecated
  public static void removeOboParser() {
    silenceOboParser();
  }
  
  public static void silenceOboParser() {
    OWLManager.createOWLOntologyManager();
    Logger logger = Logger.getLogger("org.obolibrary");
    logger.setLevel(Level.SEVERE);
    for(Handler handler : logger.getHandlers()) {
      handler.setLevel(Level.SEVERE);
    }
  }
}
