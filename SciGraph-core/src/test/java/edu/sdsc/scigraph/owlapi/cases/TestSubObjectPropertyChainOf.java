package edu.sdsc.scigraph.owlapi.cases;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.neo4j.OwlLabels;

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
    Node subp = getNode("http://example.org/subproperty");
    Node superp = getNode("http://example.org/superproperty");
    // TODO
  }

}
