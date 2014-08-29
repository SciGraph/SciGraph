package edu.sdsc.scigraph.owlapi.cases;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.owlapi.OwlRelationships;

public class TestEquivalentClasses extends OwlTestCase {

  /**
   * See https://github.com/SciCrunch/SciGraph/wiki/MappingToOWL#equivalence-axioms
   */
  @Test
  public void testEquivalentToIntersectionOf() {
    Node x = getNode("http://example.org/x");
    Node y = getNode("http://example.org/y");
    Node z = getNode("http://example.org/z");

    assertThat("equivalence is symmetric and holds between all members.",
        GraphUtil.getRelationships(x, y, OwlRelationships.OWL_EQUIVALENT_CLASS, false),
        is(IsIterableWithSize.<Relationship> iterableWithSize(1)));
    assertThat("equivalence is symmetric and holds between all members.",
        GraphUtil.getRelationships(x, z, OwlRelationships.OWL_EQUIVALENT_CLASS, false),
        is(IsIterableWithSize.<Relationship> iterableWithSize(1)));
    assertThat("equivalence is symmetric and holds between all members.",
        GraphUtil.getRelationships(y, z, OwlRelationships.OWL_EQUIVALENT_CLASS, false),
        is(IsIterableWithSize.<Relationship> iterableWithSize(1)));
  }

}
