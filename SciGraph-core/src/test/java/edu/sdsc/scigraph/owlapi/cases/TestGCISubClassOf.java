package edu.sdsc.scigraph.owlapi.cases;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.owlapi.OwlLabels;

public class TestGCISubClassOf extends OwlTestCase {

  /**
   */
  @Test
  public void testSubclass() {
    Node subclass = getNode("http://example.org/subclass");
    Node superclass = getNode("http://example.org/superclass");

    RelationshipType p = DynamicRelationshipType.withName("p");
    // TODO
    // test for this:
    // _:1 -[p]-> subclass
    // _:1 subClassOf _:2
    // _:2 -[p]-> superclass
    
  }

}
