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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;

public class CurieUtilTest {

  CurieUtil util;

  @Before
  public void setup() {
    Map<String, String> map = new HashMap<>();
    map.put("http://example.org/a_", "A");
    map.put("http://example.org/A_", "A");
    map.put("http://example.org/B_", "B");
    util = new CurieUtil(map);
  }

  @Test
  public void curiePrefixes() {
    assertThat(util.getPrefixes(), hasItems("A", "B"));
  }

  @Test
  public void multipleExpansions() {
    assertThat(util.getAllExpansions("A"), hasItems("http://example.org/a_", "http://example.org/A_"));
  }

  @Test
  public void multipleUris_whenThereAreMultipleMappings() {
    assertThat(util.getFullUri("A:foo"), containsInAnyOrder("http://example.org/a_foo", "http://example.org/A_foo"));
  }

  @Test
  public void emptyUris_whenMappingIsNotPresent() {
    assertThat(util.getFullUri("NONE:foo"), is(empty()));
  }

  @Test
  public void emptyUris_whenInputHasNoPrefix() {
    assertThat(util.getFullUri(":foo"), is(empty()));
  }

  @Test
  public void emptyUris_whenNotValidCurie() {
    assertThat(util.getFullUri("A"), is(empty()));
  }

  @Test
  public void currie_whenMappingIsPresent() {
    assertThat(util.getCurie("http://example.org/a_foo"), is(Optional.of("A:foo")));
  }

  @Test
  public void noCurrie_whenMappingIsNotPresent() {
    assertThat(util.getCurie("http://example.org/none"), is(Optional.<String>absent()));
  }

}
