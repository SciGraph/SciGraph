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
package io.scigraph.neo4j;

import io.scigraph.frames.CommonProperties;
import io.scigraph.frames.Concept;
import io.scigraph.owlapi.OwlLabels;
import io.scigraph.owlapi.OwlRelationships;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import com.google.common.base.Function;

// TODO: Can dozer do this?
public class NodeTransformer implements Function<Node, Concept> {

  static boolean isDeprecated(Node n) {
    if (!n.hasProperty(OWLRDFVocabulary.OWL_DEPRECATED.toString())) {
      return false;
    }
    if (n.getProperty(OWLRDFVocabulary.OWL_DEPRECATED.toString()) instanceof Boolean) {
      return (Boolean) n.getProperty(OWLRDFVocabulary.OWL_DEPRECATED.toString());
    } else {
      return Boolean.valueOf((String)n.getProperty(OWLRDFVocabulary.OWL_DEPRECATED.toString(), "false"));
    }
  }

  @Override
  public Concept apply(Node n) {
    try (Transaction tx = n.getGraphDatabase().beginTx()) {
      Concept concept = new Concept(n.getId());
      concept.setIri((String) n.getProperty(Concept.IRI, null));
      concept.setAnonymous(n.hasLabel(OwlLabels.OWL_ANONYMOUS));
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
        concept.getEquivalentClasses().add((String)equivalence.getProperty(CommonProperties.IRI));
      }

      tx.success();
      return concept;
    }
  }

}
