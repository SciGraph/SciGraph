package edu.sdsc.scigraph.neo4j;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import com.google.common.base.Function;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.owlapi.OwlRelationships;

public class NodeTransformer implements Function<Node, Concept> {

  static boolean isDeprecated(Node n) {
    return Boolean.valueOf((String)n.getProperty(OWLRDFVocabulary.OWL_DEPRECATED.toString(), "false"));
  }

  @Override
  public Concept apply(Node n) {
    Concept concept = new Concept();
    try (Transaction tx = n.getGraphDatabase().beginTx()) {
      concept.setId(n.getId());
      concept.setUri((String) n.getProperty(Concept.URI, null));
      concept.setAnonymous((boolean) n.getProperty(Concept.ANONYMOUS, false));
      concept.setFragment((String) n.getProperty(Concept.FRAGMENT, null));
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
        concept.getEquivalentClasses().add((String)equivalence.getProperty(CommonProperties.URI));
      }

      tx.success();
    }

    return concept;
  }

}
