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

public class TestEquivalentToIntersectionOf extends OwlTestCase {

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
		Node definedClass = getNode("http://example.org/definedClass");
		Node fillerClass = getNode("http://example.org/fillerClass");

		RelationshipType p = DynamicRelationshipType.withName( "http://example.org/p" );
		Relationship relationship = getOnlyElement(GraphUtil.getRelationships(definedClass, fillerClass, p));
		
		/*
		 * Should translate
		 * 
		 * X = genus and R some Y
		 * ==>
		 * X = _
		 * _ subClassOfgenus
		 * _ R Y
		 * 
		 * 
		 */
		
	}

}
