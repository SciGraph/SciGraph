package edu.sdsc.scigraph.owlapi;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.graphdb.traversal.Uniqueness;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.neo4j.EdgeType;
import edu.sdsc.scigraph.neo4j.GraphUtil;

public class OwlPostprocessor {

  private static final Logger logger = Logger.getLogger(OwlPostprocessor.class.getName());

  private final GraphDatabaseService graphDb;
  private final ExecutionEngine engine;
  private final ReadableIndex<Node> nodeIndex;

  @Inject
  public OwlPostprocessor(GraphDatabaseService graphDb) {
    this.graphDb = graphDb;
    this.nodeIndex = graphDb.index().getNodeAutoIndexer().getAutoIndex();
    engine = new ExecutionEngine(graphDb);
    
  }

  public void processSomeValuesFrom() {
    logger.info("Processing someValuesFrom classes");
    try (Transaction tx = graphDb.beginTx()) {
      ResourceIterator<Map<String, Object>> results = engine.execute(
          "MATCH (n)-[:subClassOf]->(svf:someValuesFrom)-[:PROPERTY]->(p) RETURN n, svf, p")
          .iterator();
      while (results.hasNext()) {
        Map<String, Object> result = results.next();
        Node subject = (Node) result.get("n");
        Node svf = (Node) result.get("svf");
        Node property = (Node) result.get("p");
        for (Relationship r : svf.getRelationships(EdgeType.FILLER)) {
          Node object = r.getEndNode();
          String relationshipName = GraphUtil.getProperty(property, CommonProperties.FRAGMENT,
              String.class).get();
          RelationshipType type = DynamicRelationshipType.withName(relationshipName);
          String propertyUri = GraphUtil.getProperty(property, CommonProperties.URI, String.class)
              .get();
          Relationship inferred = subject.createRelationshipTo(object, type);
          inferred.setProperty(CommonProperties.URI, propertyUri);
          inferred.setProperty(CommonProperties.CONVENIENCE, true);
        }
      }
      tx.success();
    }
  }

  void processCategory(Node root, RelationshipType type, String category) {
    for (Path position : graphDb.traversalDescription()
        .uniqueness(Uniqueness.NODE_GLOBAL).depthFirst().relationships(type, Direction.OUTGOING)
        .traverse(root)) {
      Node end = position.endNode();
      GraphUtil.addProperty(end, Concept.CATEGORY, category);
    }
  }

  public void processCategories(Map<String, String> categoryMap) {
    for (Entry<String, String> category : categoryMap.entrySet()) {
      Node root = nodeIndex.get(CommonProperties.URI, category.getKey()).getSingle();
      processCategory(root, EdgeType.SUPERCLASS_OF, category.getValue());
    }
  }

}
