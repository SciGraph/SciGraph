package edu.sdsc.scigraph.services.health;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

public class Neo4jHealthCheckTest {

  GraphDatabaseService graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
  Neo4jHealthCheck healthCheck = new Neo4jHealthCheck(graphDb);

  @Test
  public void whenNoNodes_unhealty() throws Exception {
    assertThat(healthCheck.check().isHealthy(), is(false));
  }

  @Test
  public void whenNodes_healty() throws Exception {
    try (Transaction tx = graphDb.beginTx()) {
      graphDb.createNode();
      tx.success();
    }
    assertThat(healthCheck.check().isHealthy(), is(true));
  }

}
