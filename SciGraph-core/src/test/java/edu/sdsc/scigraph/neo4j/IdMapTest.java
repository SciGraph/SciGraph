package edu.sdsc.scigraph.neo4j;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class IdMapTest {

  @Test
  public void test() {
    IdMap map = new IdMap();
    assertThat(0L, is(map.get("http://example.org/a")));
  }

}
