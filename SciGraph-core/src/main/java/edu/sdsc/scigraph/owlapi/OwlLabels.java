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

import static edu.sdsc.scigraph.owlapi.OwlApiUtils.getNeo4jName;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/***
 * Neo4j labels for OWL / RDF axioms
 */
public class OwlLabels {

  public static final Label OWL_ONTOLOGY = DynamicLabel.label(getNeo4jName(OWLRDFVocabulary.OWL_ONTOLOGY));
  
  public static final Label OWL_ANONYMOUS = DynamicLabel.label("anonymous");

  public static final Label OWL_CLASS = DynamicLabel.label(getNeo4jName(OWLRDFVocabulary.OWL_CLASS));

  public static final Label OWL_INDIVIDUAL = DynamicLabel
      .label(getNeo4jName(OWLRDFVocabulary.OWL_INDIVIDUAL));

  public static final Label OWL_SOME_VALUES_FROM = DynamicLabel
      .label(getNeo4jName(OWLRDFVocabulary.OWL_SOME_VALUES_FROM));

  public static final Label OWL_ALL_VALUES_FROM = DynamicLabel
      .label(getNeo4jName(OWLRDFVocabulary.OWL_ALL_VALUES_FROM));

  public static final Label OWL_INTERSECTION_OF = DynamicLabel
      .label(getNeo4jName(OWLRDFVocabulary.OWL_INTERSECTION_OF));

  public static final Label OWL_UNION_OF = DynamicLabel
      .label(getNeo4jName(OWLRDFVocabulary.OWL_UNION_OF));

  public static final Label OWL_NAMED_INDIVIDUAL = DynamicLabel
      .label(getNeo4jName(OWLRDFVocabulary.OWL_NAMED_INDIVIDUAL));

  public static final Label OWL_DATA_PROPERTY = DynamicLabel
      .label(getNeo4jName(OWLRDFVocabulary.OWL_DATA_PROPERTY));

  public static final Label OWL_OBJECT_PROPERTY = DynamicLabel
      .label(getNeo4jName(OWLRDFVocabulary.OWL_OBJECT_PROPERTY));

  public static final Label OWL_ANNOTATION_PROPERTY = DynamicLabel
      .label(getNeo4jName(OWLRDFVocabulary.OWL_ANNOTATION_PROPERTY));

  public static final Label OWL_COMPLEMENT_OF = DynamicLabel
      .label(getNeo4jName(OWLRDFVocabulary.OWL_COMPLEMENT_OF));

  public static final Label OWL_MAX_CARDINALITY = DynamicLabel
      .label(getNeo4jName(OWLRDFVocabulary.OWL_MAX_CARDINALITY));

  public static final Label OWL_MIN_CARDINALITY = DynamicLabel
      .label(getNeo4jName(OWLRDFVocabulary.OWL_MIN_CARDINALITY));

  public static final Label OWL_QUALIFIED_CARDINALITY = DynamicLabel
      .label(getNeo4jName(OWLRDFVocabulary.OWL_QUALIFIED_CARDINALITY));

}
