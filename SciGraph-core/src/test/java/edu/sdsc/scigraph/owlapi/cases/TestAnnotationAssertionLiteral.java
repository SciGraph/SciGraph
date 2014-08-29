package edu.sdsc.scigraph.owlapi.cases;

import static com.google.common.collect.Iterables.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.neo4j.OwlLabels;

/***
 * See https://github.com/SciCrunch/SciGraph/wiki/MappingToOWL#subclassof-axioms
 *
 */
public class TestAnnotationAssertionLiteral extends OwlTestCase {

	@Test
	public void testAnnotationAssertion() {
		Node i = getNode("http://example.org/i");
		assertThat("property value is set to foo",
				i.getProperty("http://example.org/p").toString(),
				is("foo"));

	}

}
