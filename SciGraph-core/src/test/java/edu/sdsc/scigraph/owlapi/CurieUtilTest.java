package edu.sdsc.scigraph.owlapi;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;

public class CurieUtilTest {

  CurieUtil util;

  @Before
  public void setup() {
    util =
        new CurieUtil(ImmutableBiMap.<String, String>builder().put("http://example.org/a_", "A")
            .build());
  }

  @Test
  public void testGetFullUri() {
    assertThat(util.getFullUri("A:foo"), is(Optional.of("http://example.org/a_foo")));
  }

  @Test
  public void testUnknownCurie() {
    assertThat(util.getFullUri("NONE:foo"), is(Optional.<String>absent()));
  }

  @Test
  public void testGetCurie() {
    assertThat(util.getCurie("http://example.org/a_foo"), is(Optional.of("A:foo")));
  }

  @Test
  public void testUnknownUri() {
    assertThat(util.getFullUri("http://example.org/none_foo"), is(Optional.<String>absent()));
  }

}
