/**
 * Copyright (C) 2014 The SciGraph authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.scigraph.neo4j;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.scigraph.neo4j.TransactionalInterceptor;

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
    when(interceptor.graphDb.beginTx()).thenReturn(tx);
    invocation = mock(MethodInvocation.class);
  }

  @Test
  public void testNormalTransaction() throws Throwable {
    interceptor.invoke(invocation);
    verify(graphDb).beginTx();
    verify(tx).success();
  }

}
