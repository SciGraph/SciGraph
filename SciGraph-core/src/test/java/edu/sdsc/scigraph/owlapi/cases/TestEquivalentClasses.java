package edu.sdsc.scigraph.owlapi.cases;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Iterator;

import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.neo4j.OwlLabels;

public class TestEquivalentClasses extends OwlTestCase {

	/**
	 * 
	 * See
	 * https://github.com/SciCrunch/SciGraph/wiki/MappingToOWL#subclassof-axioms
	 * 
	 * Reduction step should give us a simple edge {sub p super}
	 * 
	 */
	@Test
	public void testEquivalentToIntersectionOf() {
		Node x = getNode("http://example.org/x");
		Node y = getNode("http://example.org/y");
		Node z = getNode("http://example.org/z");

		testEdge(x,y);
		testEdge(y,x);
		testEdge(x,z);
		testEdge(z,x);
		testEdge(y,z);
		testEdge(z,y);


	}
	
	private void testEdge(Node x, Node y) {
		Relationship relationship = getOnlyElement(GraphUtil.getRelationships(x, y, OwlLabels.OWL_EQUIVALENT_CLASS));
		assertThat("equivalence is symmetric and holds between all members.",
				relationship.getStartNode(), is(x));
		assertThat("equivalence is symmetric and holds between all members.",
				relationship.getEndNode(), is(y));
	}

}
