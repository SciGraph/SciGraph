package edu.sdsc.scigraph.owlapi.cases;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.owlapi.OwlRelationships;

/***
 * Rule: p1 o p2 -> p3
 * 
 * Expected graph structure
 * 
 * p3 -[subPropertyOf]-> _:1
 * _:1 ...?
 * 
 * How to store lists in neo4j?
 *
 */
public class TestSubObjectPropertyChainOf extends OwlTestCase {

  @Test
  public void testSubclass() {
    Node p1 = getNode("http://example.org/p1");
    Node p2 = getNode("http://example.org/p2");
    Node p3 = getNode("http://example.org/p3");

    Relationship first = getOnlyElement(GraphUtil.getRelationships(p3, p1,
        OwlRelationships.OWL_PROPERTY_CHAIN_AXIOM));
    assertThat(GraphUtil.getProperty(first, "order", Integer.class), is(Optional.of(0)));
    Relationship second = getOnlyElement(GraphUtil.getRelationships(p3, p2,
        OwlRelationships.OWL_PROPERTY_CHAIN_AXIOM));
    assertThat(GraphUtil.getProperty(second, "order", Integer.class), is(Optional.of(1)));
  }

}
