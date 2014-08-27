package edu.sdsc.scigraph.neo4j;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class TransactionalInterceptorTest {

  TransactionalInterceptor interceptor = new TransactionalInterceptor();
  GraphDatabaseService graphDb;
  Transaction tx;
  MethodInvocation invocation;

  @Before
  public void setup() throws Exception {
    tx = mock(Transaction.class);
    graphDb = mock(GraphDatabaseService.class);
    interceptor.graphDb = graphDb;
    interceptor.inTransaction = new AtomicBoolean();
    when(interceptor.graphDb.beginTx()).thenReturn(tx);
    invocation = mock(MethodInvocation.class);
  }

  @Test
  public void testNormalTransaction() throws Throwable {
    interceptor.invoke(invocation);
    verify(graphDb).beginTx();
    verify(tx).success();
  }

  @Test
  public void testBatchTransaction() throws Throwable {
    interceptor.inTransaction = new AtomicBoolean(true);
    interceptor.invoke(invocation);
    verify(graphDb, never()).beginTx();
    verify(tx, never()).success();
  }

  @Test
  public void testTransactionCounting() throws Throwable {

  }

}
