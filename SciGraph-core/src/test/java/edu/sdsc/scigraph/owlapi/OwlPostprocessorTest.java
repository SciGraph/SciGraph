package edu.sdsc.scigraph.owlapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.AutoIndexer;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.neo4j.GraphUtil;

public class OwlPostprocessorTest {

  GraphDatabaseService graphDb;
  Node parent, child;
  OwlPostprocessor postprocessor;

  @Before
  public void setup() {
    graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
    Transaction tx = graphDb.beginTx();
    AutoIndexer<Node> nodeIndex = graphDb.index().getNodeAutoIndexer();
    nodeIndex.startAutoIndexingProperty(CommonProperties.URI);
    nodeIndex.setEnabled(true);
    parent = graphDb.createNode();
    parent.setProperty(CommonProperties.URI, "http://example.org/a");
    child = graphDb.createNode();
    child.createRelationshipTo(parent, OwlRelationships.RDF_SUBCLASS_OF);
    tx.success();
    postprocessor = new OwlPostprocessor(graphDb, Collections.<String, String>emptyMap());
  }

  @Test(expected = IllegalStateException.class)
  public void testUnknownUri() {
    Map<String, String> categoryMap = new HashMap<>();
    categoryMap.put("http://example.org/unknown", "foo");
    postprocessor.processCategories(categoryMap);
  }

  @Test
  public void testCategories() {
    Map<String, String> categoryMap = new HashMap<>();
    categoryMap.put("http://example.org/a", "foo");
    postprocessor.processCategories(categoryMap);
    assertThat("parent category should be set",
        GraphUtil.getProperty(parent, Concept.CATEGORY, String.class), is(Optional.of("foo")));
    assertThat("child category should be set",
        GraphUtil.getProperty(child, Concept.CATEGORY, String.class), is(Optional.of("foo")));
  }

}
