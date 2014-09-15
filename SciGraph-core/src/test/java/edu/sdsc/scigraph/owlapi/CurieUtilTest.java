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
