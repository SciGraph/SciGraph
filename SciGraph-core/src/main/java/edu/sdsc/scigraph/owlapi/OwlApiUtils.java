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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.validator.routines.UrlValidator;
import org.coode.owlapi.obo12.parser.OBO12ParserFactory;
import org.coode.owlapi.oboformat.OBOFormatParserFactory;
import org.neo4j.helpers.collection.Iterables;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.io.OWLParserFactoryRegistry;
import org.semanticweb.owlapi.io.XMLUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;

public class OwlApiUtils {

  private static final Logger logger = Logger.getLogger(OwlApiUtils.class.getName());

  private static final String ANONYMOUS_NODE_PREFIX = "http://ontology.neuinfo.org/anon/";

  private static final UrlValidator validator = UrlValidator.getInstance();

  public static URI getURI(String uri) {
    try {
      return new URI(checkNotNull(uri));
    } catch (URISyntaxException e) {
      checkState(false, "URIs passed to this method should always be valid: " + uri);
      return null;
    }
  }

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
      return getURI(ANONYMOUS_NODE_PREFIX + expression.hashCode());
    } else {
      return expression.asOWLClass().getIRI().toURI();
    }
  }

  public static URI getUri(OWLObjectPropertyExpression property) {
    if (property.isAnonymous()) {
      return getURI(ANONYMOUS_NODE_PREFIX + property.hashCode());
    } else {
      return property.asOWLObjectProperty().getIRI().toURI();
    }
  }

  public static URI getUri(OWLIndividual individual) {
    if (individual.isAnonymous()) {
      String id = ((OWLAnonymousIndividual)individual).getID().getID();
      String trueId = Iterables.last(Splitter.on("_:").split(id));
      return getURI(ANONYMOUS_NODE_PREFIX + trueId);
    } else {
      return individual.asOWLNamedIndividual().getIRI().toURI();
    }
  }

  public static URI getUri(OWLAnnotationProperty property) {
    return property.asOWLAnnotationProperty().getIRI().toURI();
  }

  public static String getFragment(OWLRDFVocabulary vocab) {
    return XMLUtils.getNCNameSuffix(vocab.getIRI());
  }

  /***
   * @param manager The ontology manager to use
   * @param ontology A string representing the ontology to load. This can be an URI or a file path
   * @return The loaded ontology
   * @throws OWLOntologyCreationException
   */
  public static OWLOntology loadOntology(OWLOntologyManager manager, String ontology) throws OWLOntologyCreationException {
    logger.info(String.format("Reading ontology: %s", ontology));
    OWLOntology ont;
    if (validator.isValid(ontology)) {
      ont = manager.loadOntology(IRI.create(ontology));
    } else {
      ont = manager.loadOntologyFromOntologyDocument(new File(ontology));
    }
    logger.info(String.format("Finished reasing ontology: %s", ontology));
    return ont;
  }

  public static void silenceOboParser() {
    OWLManager.createOWLOntologyManager();
    /* TODO: Why does this logging never become silent?
     * Logger logger = Logger.getLogger("org.obolibrary");
    logger.setLevel(java.util.logging.Level.SEVERE);
    Handler[] handlers = logger.getHandlers();
    for (Handler handler : handlers) {
      handler.setLevel(Level.SEVERE);
    }*/
    OWLParserFactoryRegistry registry = OWLParserFactoryRegistry.getInstance();
    List<OWLParserFactory> factories = registry.getParserFactories();
    for (OWLParserFactory factory : factories) {
      if (factory instanceof OBOFormatParserFactory ||
          factory instanceof OBO12ParserFactory) {
        registry.unregisterParserFactory(factory);
      }
    }
  }
}
