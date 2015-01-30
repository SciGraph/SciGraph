package edu.sdsc.scigraph.owlapi;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import edu.sdsc.scigraph.neo4j.GraphTransactionalImpl;
import edu.sdsc.scigraph.neo4j.IdMap;
import edu.sdsc.scigraph.neo4j.Neo4jModule;
import edu.sdsc.scigraph.neo4j.RelationshipMap;

public class OwlVisitorTransactionalGraphTest extends OwlVisitorTestBase<GraphTransactionalImpl> {

  @Override
  protected GraphTransactionalImpl createInstance() throws Exception {
    GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(path.toString());
    Neo4jModule.setupAutoIndexing(graphDb);
    IdMap idMap = new IdMap();
    RelationshipMap relationahipMap = new RelationshipMap();
    return new GraphTransactionalImpl(graphDb, idMap, relationahipMap);
  }

}
