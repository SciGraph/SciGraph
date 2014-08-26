package edu.sdsc.scigraph.owlapi.cases;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.neo4j.OwlLabels;

public class TestClassAssertion extends OwlTestCase {

  @Test
  public void testObjectPropertyAssertion() {
    Node i = getNode("http://example.org/i");
    Node c = getNode("http://example.org/c");

    Relationship relationship = getOnlyElement(GraphUtil.getRelationships(i, c, OwlLabels.RDF_TYPE));
    assertThat("OPE edge should start with the subject.", relationship.getStartNode(), is(i));
    assertThat("OPE edge should start with the target.", relationship.getEndNode(), is(c));
  }

}
