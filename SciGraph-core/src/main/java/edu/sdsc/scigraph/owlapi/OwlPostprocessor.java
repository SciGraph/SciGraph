package edu.sdsc.scigraph.owlapi;

import static com.google.common.collect.Iterables.getFirst;

import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.neo4j.EdgeType;
import edu.sdsc.scigraph.neo4j.GraphUtil;

public class OwlPostprocessor {

  private static final Logger logger = Logger.getLogger(OwlPostprocessor.class.getName());

  private final GraphDatabaseService graphDb;
  private final ExecutionEngine engine;

  @Inject
  public OwlPostprocessor(GraphDatabaseService graphDb) {
    this.graphDb = graphDb;
    engine = new ExecutionEngine(graphDb);
  }

  public void processSomeValuesFrom() {
    logger.info("Processing someValuesFrom classes");
    try (Transaction tx = graphDb.beginTx()) {
      ResourceIterator<Map<String, Object>> results = engine.execute(
          "MATCH (n)-[:subClassOf]->(svf:someValuesFrom) RETURN n, svf").iterator();
      while (results.hasNext()) {
        Map<String, Object> result = results.next();
        Node subject = (Node) result.get("n");
        Node svf = (Node) result.get("svf");
        Node property = getFirst(svf.getRelationships(EdgeType.PROPERTY), null).getEndNode();
        for (Relationship r : svf.getRelationships(EdgeType.CLASS)) {
          Node object = r.getEndNode();
          String relationshipName = GraphUtil.getProperty(property, CommonProperties.FRAGMENT,
              String.class).get();
          RelationshipType type = DynamicRelationshipType.withName(relationshipName);
          String propertyUri = GraphUtil.getProperty(property, CommonProperties.URI, String.class)
              .get();
          Relationship inferred = subject.createRelationshipTo(object, type);
          inferred.setProperty(CommonProperties.URI, propertyUri);
          inferred.setProperty(CommonProperties.ASSERTED, true);
        }
      }
      tx.success();
    }
  }

}
