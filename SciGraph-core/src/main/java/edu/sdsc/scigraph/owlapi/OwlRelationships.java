package edu.sdsc.scigraph.owlapi;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.RelationshipType;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

public class OwlRelationships {

  /***
   * Relationships
   */

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

  /*
   * public static final RelationshipType OWL_OBJECT_PROPERTY = DynamicRelationshipType
   * .withName(getFragment(OWLRDFVocabulary.OWL_OBJECT_PROPERTY));
   */

  // public static final RelationshipsType ANNOTATION_ASSERTION =
  // DynamicRelationshipType.withName(getFragment(OWLRDFVocabulary.o))

  private static String getFragment(OWLRDFVocabulary vocab) {
    // return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, vocab.getIRI().getFragment());
    return vocab.getIRI().getFragment();
  }

}
