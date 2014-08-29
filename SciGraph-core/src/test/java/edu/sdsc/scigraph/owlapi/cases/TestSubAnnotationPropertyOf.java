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
public class TestSubAnnotationPropertyOf extends OwlTestCase {

  @Test
  public void testSubclass() {
    Node subp = getNode("http://example.org/subproperty");
    Node superp = getNode("http://example.org/superproperty");
    assertThat("subclass should be a directed relationship",
        GraphUtil.getRelationships(subp, superp, OwlLabels.RDFS_SUB_PROPERTY_OF),
        is(IsIterableWithSize.<Relationship> iterableWithSize(1)));
  }

}
