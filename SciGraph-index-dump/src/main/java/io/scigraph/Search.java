package io.scigraph;

import java.io.IOException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterators;


public class Search {
  public static void main(String[] args) throws IOException, InterruptedException {
    GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase("/tmp/dipper-test");
    Transaction tx = graphDb.beginTx();

    String phenotype = "http://purl.obolibrary.org/obo/HP_0009283";

    System.out.println("starting");
    Stopwatch stopwatch = Stopwatch.createStarted();
    Result result =
        graphDb.execute("START phenotype = node:node_auto_index(iri=\"" + phenotype
            + "\")  MATCH path = (phenotype)<-[:subClassOf*]-(subclass)-[]-(connectedNode)-[:subClassOf*]->(superclass) RETURN path");
    stopwatch.stop(); // optional
    System.out.println("Finished. Elapsed time ==> " + stopwatch);
    System.out.println(Iterators.size(result));
    tx.success();
    
    // Finished. Elapsed time ==> 3.237 s
    // # results 239691180
  }

}
