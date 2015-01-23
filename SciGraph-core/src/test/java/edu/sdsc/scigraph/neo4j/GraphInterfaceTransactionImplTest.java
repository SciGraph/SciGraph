package edu.sdsc.scigraph.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

public class GraphInterfaceTransactionImplTest extends GraphTestBase<GraphInterfaceTransactionImpl> {

  @Override
  protected GraphInterfaceTransactionImpl createInstance() {
    GraphDatabaseService graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
    IdMap idMap = new IdMap();
    RelationshipMap relationahipMap = new RelationshipMap();
    return new GraphInterfaceTransactionImpl(graphDb, idMap, relationahipMap);
  }

}
