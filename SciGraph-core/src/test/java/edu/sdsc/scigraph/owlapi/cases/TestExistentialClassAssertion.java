package edu.sdsc.scigraph.owlapi.cases;

import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

public class TestExistentialClassAssertion extends OwlTestCase {

	/**
	 * 
	 * 
	 */
	@Test
	public void testSubclass() {
		Node i = getNode("http://example.org/i");
		Node c = getNode("http://example.org/c");
		RelationshipType p = DynamicRelationshipType.withName( "http://example.org/p" );
		/*
		 * We need the graph structure to be:
		 * 
		 * i TYPE _
		 * _ p c
		 * 
		 * Currently it inserts wierd PROPERTY and CLASS properties
		 * 
		 */
	}

}
