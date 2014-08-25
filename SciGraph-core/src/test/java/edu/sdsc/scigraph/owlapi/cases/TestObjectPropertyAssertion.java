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

public class TestObjectPropertyAssertion extends OwlTestCase {

	@Test
	public void testObjectPropertyAssertion() {
		Node i = getNode("http://example.org/i");
		Node j = getNode("http://example.org/j");

		RelationshipType p = DynamicRelationshipType.withName( "http://example.org/p" );
		Relationship relationship = getOnlyElement(GraphUtil.getRelationships(i, j, p));
		assertThat("subclassOf relationship should start with the subclass.",
				relationship.getStartNode(), is(i));
		assertThat("subclassOf relationship should end with the subclass.",
				relationship.getEndNode(), is(j));

	}

}
