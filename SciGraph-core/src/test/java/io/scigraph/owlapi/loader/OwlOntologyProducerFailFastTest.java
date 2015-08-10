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
package io.scigraph.owlapi.loader;

import io.scigraph.owlapi.loader.OWLCompositeObject;
import io.scigraph.owlapi.loader.OwlOntologyProducer;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.OntologySetup;
import io.scigraph.util.GraphTestBase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class OwlOntologyProducerFailFastTest extends GraphTestBase {
  static Server server = new Server(10000);
  static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    server.setStopAtShutdown(true);
    ResourceHandler handler = new ResourceHandler();
    handler.setBaseResource(Resource.newClassPathResource("/ontologies/bad/"));
    server.setHandler(handler);
    server.start();
  }

  @AfterClass
  public static void teardown() throws Exception {
    server.stop();
    server.join();
  }

  @Test(timeout = 11000)
  public void fail_fast() throws InterruptedException, ExecutionException {
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    CompletionService<Long> completionService = new ExecutorCompletionService<Long>(executorService);

    BlockingQueue<OWLCompositeObject> queue = new LinkedBlockingQueue<OWLCompositeObject>();
    BlockingQueue<OntologySetup> ontologyQueue = new LinkedBlockingQueue<OntologySetup>();
    OwlOntologyProducer producer =
        new OwlOntologyProducer(queue, ontologyQueue, new AtomicInteger(), graph);
    OntologySetup ontologyConfig = new OntologySetup();

    ontologyConfig.setUrl("http://localhost:10000/foo.owl");

    List<Future<?>> futures = new ArrayList<>();
    futures.add(completionService.submit(producer));
    futures.add(completionService.submit(producer));
    Thread.sleep(1000);
    ontologyQueue.put(ontologyConfig);

    expectedException.expect(ExecutionException.class);
    while (futures.size() > 0) {
      Future<?> completedFuture = completionService.take();
      futures.remove(completedFuture);
      completedFuture.get();
    }

    executorService.shutdown();
    executorService.awaitTermination(10, TimeUnit.SECONDS);

  }
}
