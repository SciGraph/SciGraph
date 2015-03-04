package edu.sdsc.scigraph.owlapi.loader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;

import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.OntologySetup;

public class OwlOntologyProducerTest {

  BlockingQueue<OWLObject> queue = new LinkedBlockingQueue<>();
  BlockingQueue<OntologySetup> ontologyQueue = new LinkedBlockingQueue<>();
  AtomicInteger numProducersShutdown = new AtomicInteger();
  OwlOntologyProducer producer;

  @Before
  public void setup() {
    producer = new OwlOntologyProducer(queue, ontologyQueue, numProducersShutdown);
  }

  @Test
  public void poisonShutsDownProducer() throws Exception {
    ontologyQueue.put(BatchOwlLoader.POISON_STR);
    producer.call();
    assertThat(numProducersShutdown.get(), is(1));
  }

}
