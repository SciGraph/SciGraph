package edu.sdsc.scigraph.owlapi.cases;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import edu.sdsc.scigraph.owlapi.OwlRelationships;

@Ignore
public class TestInferredEdges extends OwlTestCase {

  @Test
  public void testInferredEdges() {
    Node cx = getNode("http://example.org/cx");
    Node dx = getNode("http://example.org/dx");

    Iterable<Relationship> superclasses = dx.getRelationships(OwlRelationships.RDF_SUBCLASS_OF, Direction.OUTGOING);
    Relationship r = getOnlyElement(superclasses);
    assertThat(r.getOtherNode(dx), is(cx));
  }

}
