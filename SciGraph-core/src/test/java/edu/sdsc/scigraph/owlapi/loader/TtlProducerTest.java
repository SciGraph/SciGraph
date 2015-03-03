package edu.sdsc.scigraph.owlapi.loader;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;

import org.junit.Test;

public class TtlProducerTest {

  @Test
  public void test() throws FileNotFoundException {
    TtlProducer producer = new TtlProducer(null);
    producer.produce();
  }

}
