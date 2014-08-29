package edu.sdsc.scigraph.owlapi;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

public class OwlLabels {

  /***
   * Labels
   */
  public static final Label OWL_ANONYMOUS = DynamicLabel.label("anonymous");
  
  public static final Label OWL_CLASS = DynamicLabel.label(getFragment(OWLRDFVocabulary.OWL_CLASS));

  public static final Label OWL_INDIVIDUAL = DynamicLabel
      .label(getFragment(OWLRDFVocabulary.OWL_INDIVIDUAL));

  public static final Label OWL_SOME_VALUES_FROM = DynamicLabel
      .label(getFragment(OWLRDFVocabulary.OWL_SOME_VALUES_FROM));

  public static final Label OWL_ALL_VALUES_FROM = DynamicLabel
      .label(getFragment(OWLRDFVocabulary.OWL_ALL_VALUES_FROM));

  public static final Label OWL_INTERSECTION_OF = DynamicLabel
      .label(getFragment(OWLRDFVocabulary.OWL_INTERSECTION_OF));

  public static final Label OWL_UNION_OF = DynamicLabel
      .label(getFragment(OWLRDFVocabulary.OWL_UNION_OF));

  public static final Label OWL_NAMED_INDIVIDUAL = DynamicLabel
      .label(getFragment(OWLRDFVocabulary.OWL_NAMED_INDIVIDUAL));

  public static final Label OWL_DATA_PROPERTY = DynamicLabel
      .label(getFragment(OWLRDFVocabulary.OWL_DATA_PROPERTY));

  public static final Label OWL_OBJECT_PROPERTY = DynamicLabel
      .label(getFragment(OWLRDFVocabulary.OWL_OBJECT_PROPERTY));

  public static final Label OWL_ANNOTATION_PROPERTY = DynamicLabel
      .label(getFragment(OWLRDFVocabulary.OWL_ANNOTATION_PROPERTY));

  public static final Label OWL_COMPLEMENT_OF = DynamicLabel
      .label(getFragment(OWLRDFVocabulary.OWL_COMPLEMENT_OF));

  public static final Label OWL_MAX_CARDINALITY = DynamicLabel
      .label(getFragment(OWLRDFVocabulary.OWL_MAX_CARDINALITY));

  public static final Label OWL_MIN_CARDINALITY = DynamicLabel
      .label(getFragment(OWLRDFVocabulary.OWL_MIN_CARDINALITY));

  public static final Label OWL_QUALIFIED_CARDINALITY = DynamicLabel
      .label(getFragment(OWLRDFVocabulary.OWL_QUALIFIED_CARDINALITY));

  private static String getFragment(OWLRDFVocabulary vocab) {
    // return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, vocab.getIRI().getFragment());
    return vocab.getIRI().getFragment();
  }

}
