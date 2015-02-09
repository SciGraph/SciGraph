package edu.sdsc.scigraph.lexical.pos;

import org.junit.Test;

public class PosTokenTest {

  @Test
  public void toString_doesntFail() {
    PosToken token = new PosToken("foo", "NN", 0, 4);
    token.toString();
  }

}
