/**
 * Copyright (C) 2014 Christopher Condit (condit@sdsc.edu)
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
package edu.sdsc.scigraph.neo4j;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import edu.sdsc.scigraph.neo4j.bindings.IndicatesNeo4j;

public class TransactionalInterceptor implements MethodInterceptor {

  private static final Logger logger = Logger.getLogger(TransactionalInterceptor.class.getName());

  @Inject
  GraphDatabaseService graphDb;

  @Inject
  @IndicatesNeo4j
  AtomicBoolean inTransaction;

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    logger.fine("Intercepting transaction");
    Object result = null;
    if (inTransaction.compareAndSet(false, true)) {
      Transaction tx = graphDb.beginTx();
      try {
        result = invocation.proceed();
        tx.success();
      } finally {
        tx.finish();
      }
      inTransaction.set(false);
    } else {
      result = invocation.proceed();
    }
    return result;
  }

}
