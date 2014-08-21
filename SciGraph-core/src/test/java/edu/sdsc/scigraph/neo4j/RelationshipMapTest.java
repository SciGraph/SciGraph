package edu.sdsc.scigraph.neo4j;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;

public class RelationshipMapTest {

  @Test
  public void test() {
    RelationshipMap map = new RelationshipMap();
    assertThat(map.containsKey(0L, 1L, DynamicRelationshipType.withName("foo")), is(false));
    map.put(0L, 1L, DynamicRelationshipType.withName("foo"), 1L);
    assertThat(map.containsKey(0L, 1L, DynamicRelationshipType.withName("foo")), is(true));
    assertThat(map.get(0L, 1L, DynamicRelationshipType.withName("foo")), is(1L));
  }

}
