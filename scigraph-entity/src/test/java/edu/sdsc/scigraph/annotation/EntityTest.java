package edu.sdsc.scigraph.annotation;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.Test;

public class EntityTest {

  @Test
  public void equalsContract() {
    EqualsVerifier.forClass(Entity.class).verify();
  }

}
