package edu.sdsc.scigraph.services.jersey.dynamic;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

public class CypherInflectorTest {

  GraphDatabaseService graphDb = mock(GraphDatabaseService.class);
  ExecutionEngine engine = mock(ExecutionEngine.class);
  CypherResourceConfig config = new CypherResourceConfig();
  ContainerRequestContext context = mock(ContainerRequestContext.class);
  UriInfo uriInfo = mock(UriInfo.class);
  Transaction tx = mock(Transaction.class);

  @Before
  public void setup() {
    when(context.getUriInfo()).thenReturn(uriInfo);
    MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
    when(uriInfo.getQueryParameters()).thenReturn(map);
    when(graphDb.beginTx()).thenReturn(tx);
  }

  static class MockResult extends ExecutionResult {

    MockResult() {
      this(null);
    }

    public MockResult(org.neo4j.cypher.ExecutionResult projection) {
      super(projection);
    }

    @Override
    public ResourceIterator<Map<String, Object>> iterator() {
      return new ResourceIterator<Map<String,Object>>() {

        @Override
        public boolean hasNext() {
          return false;
        }

        @Override
        public Map<String, Object> next() {
          return null;
        }

        @Override
        public void remove() {}

        @Override
        public void close() {}};
    }

  }

  @Test
  @SuppressWarnings(value = {"unchecked"})
  public void smoke() {
    config.setQuery("MATCH (n) RETURN n");
    CypherInflector inflector = new CypherInflector(graphDb, engine, config);
    ExecutionResult result = new MockResult();
    when(engine.execute(anyString(), anyMap())).thenReturn(result);
    inflector.apply(context);
  }

}
