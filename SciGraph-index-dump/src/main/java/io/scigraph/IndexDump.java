package io.scigraph;

import io.scigraph.frames.NodeProperties;
import io.scigraph.owlapi.OwlRelationships;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.tooling.GlobalGraphOperations;

import com.google.common.collect.Iterators;

public class IndexDump {
  public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
    final RelationshipType IS_SUBCLASS_OF = OwlRelationships.RDFS_SUBCLASS_OF;
    final String path = "/tmp/index-dump/";
    final String subclassPath = path + "subclass/";
    final String superclassPath = path + "superclass/";

    // on startup
    System.out.println("Starting to dump json...");

    File p = new File(subclassPath);
    p.mkdirs();
    p = new File(superclassPath);
    p.mkdirs();

    GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase("/tmp/dipper-test");
    GlobalGraphOperations globalGraphOperations = GlobalGraphOperations.at(graphDb);
    Transaction tx = graphDb.beginTx();

    ResourceIterable<Node> allNodes = globalGraphOperations.getAllNodes();

    int size = Iterators.size(allNodes.iterator());

    for (Node baseNode : allNodes) {
      size -= 1;
      if (size % 1000 == 0) {
        System.out.println(size + " nodes left to process.");
      }

      List<Long> subclassNodes = new ArrayList<Long>();
      List<Long> superclassNodes = new ArrayList<Long>();

      for (Node currentNode : graphDb.traversalDescription().relationships(IS_SUBCLASS_OF, Direction.INCOMING).uniqueness(Uniqueness.NODE_GLOBAL)
          .traverse(baseNode).nodes()) {
        if (baseNode.getId() != currentNode.getId()) {
          subclassNodes.add(currentNode.getId());
        }
      }
      for (Node currentNode : graphDb.traversalDescription().relationships(IS_SUBCLASS_OF, Direction.OUTGOING).uniqueness(Uniqueness.NODE_GLOBAL)
          .traverse(baseNode).nodes()) {
        if (baseNode.getId() != currentNode.getId()) {
          superclassNodes.add(currentNode.getId());
        }
      }

      if (!subclassNodes.isEmpty()) {
        String json =
            "{" + "\"iri\":\"" + baseNode.getProperty(NodeProperties.IRI, baseNode.getId()) + "\",\"neoId\":" + baseNode.getId()
                + ",\"subclassNodes\":[" + StringUtils.join(subclassNodes, ",") + "]}";
        PrintWriter writer = new PrintWriter(subclassPath + baseNode.getId() + ".json", "UTF-8");
        writer.print(json);
        writer.close();
      }

      if (!superclassNodes.isEmpty()) {
        String json =
            "{" + "\"iri\":\"" + baseNode.getProperty(NodeProperties.IRI, baseNode.getId()) + "\",\"neoId\":" + baseNode.getId()
                + ",\"superclassNodes\":[" + StringUtils.join(superclassNodes, ",") + "]" + "}";
        PrintWriter writer = new PrintWriter(superclassPath + baseNode.getId() + ".json", "UTF-8");
        writer.print(json);
        writer.close();
      }

    }

    tx.success();


    // on shutdown
    System.out.println("Finished dumping and shutting down...");
  }
}
