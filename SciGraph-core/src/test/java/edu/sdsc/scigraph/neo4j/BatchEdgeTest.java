package edu.sdsc.scigraph.neo4j;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.junit.Test;

public class BatchEdgeTest {

  @Test
  public void equalsContract() {
    EqualsVerifier.forClass(BatchEdge.class).suppress(Warning.NULL_FIELDS).verify();
  }

}
