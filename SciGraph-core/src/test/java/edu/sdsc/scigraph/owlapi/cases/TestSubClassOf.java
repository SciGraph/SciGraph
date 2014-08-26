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
 * See https://github.com/SciCrunch/SciGraph/wiki/MappingToOWL#subclassof-axioms
 *
 */
public class TestSubClassOf extends OwlTestCase {

  @Test
  public void testSubclass() {
    Node subclass = getNode("http://example.org/subclass");
    Node superclass = getNode("http://example.org/superclass");
    assertThat("subclass should be a directed relationship",
        GraphUtil.getRelationships(subclass, superclass, OwlLabels.RDF_SUBCLASS_OF),
        is(IsIterableWithSize.<Relationship> iterableWithSize(1)));
  }

}
