package edu.sdsc.scigraph.frames;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.Test;

public class ConceptTest {

  @Test
  public void conceptEqualsContract() {
    EqualsVerifier.forClass(Concept.class).verify();
  }

  @Test
  public void commonPropertiesEqualsContract() {
    EqualsVerifier.forClass(CommonProperties.class).verify();
  }

  @Test
  public void edgePropertiesEqualsContract() {
    EqualsVerifier.forClass(EdgeProperties.class).verify();
  }

  @Test
  public void nodePropertiesEqualsContract() {
    EqualsVerifier.forClass(NodeProperties.class).verify();
  }

}
