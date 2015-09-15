package io.scigraph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;


public class Search {
  public static void main(String[] args)  {
    GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase("/tmp/dipper-test");
    Transaction tx = graphDb.beginTx();
  }

}
