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

import static edu.sdsc.scigraph.owlapi.OwlApiUtils.getFragment;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.RelationshipType;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/***
 * Neo4j relationships for OWL / RDF axioms
 */
public class OwlRelationships {

  public static final RelationshipType RDF_SUBCLASS_OF = DynamicRelationshipType
      .withName(getFragment(OWLRDFVocabulary.RDFS_SUBCLASS_OF));

  public static final RelationshipType RDF_TYPE = DynamicRelationshipType
      .withName(getFragment(OWLRDFVocabulary.RDF_TYPE));

  public static final RelationshipType OWL_SAME_AS = DynamicRelationshipType
      .withName(getFragment(OWLRDFVocabulary.OWL_SAME_AS));

  public static final RelationshipType OWL_DIFFERENT_FROM = DynamicRelationshipType
      .withName(getFragment(OWLRDFVocabulary.OWL_DIFFERENT_FROM));

  public static final RelationshipType OWL_ANNOTATION = DynamicRelationshipType
      .withName(getFragment(OWLRDFVocabulary.OWL_ANNOTATION));

  public static final RelationshipType OWL_EQUIVALENT_CLASS = DynamicRelationshipType
      .withName(getFragment(OWLRDFVocabulary.OWL_EQUIVALENT_CLASS));

  public static final RelationshipType OWL_DISJOINT_WITH = DynamicRelationshipType
      .withName(getFragment(OWLRDFVocabulary.OWL_DISJOINT_WITH));

  public static final RelationshipType RDFS_SUB_PROPERTY_OF = DynamicRelationshipType
      .withName(getFragment(OWLRDFVocabulary.RDFS_SUB_PROPERTY_OF));

  public static final RelationshipType OWL_PROPERTY_CHAIN_AXIOM = DynamicRelationshipType
      .withName(getFragment(OWLRDFVocabulary.OWL_PROPERTY_CHAIN_AXIOM));

  public static final RelationshipType FILLER = DynamicRelationshipType.withName("filler");

  public static final RelationshipType OPERAND = DynamicRelationshipType.withName("operand");

  public static final RelationshipType PROPERTY = DynamicRelationshipType.withName("property");
  
  public static final RelationshipType CLASS = DynamicRelationshipType.withName("class");

}
