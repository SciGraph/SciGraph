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
package edu.sdsc.scigraph.neo4j;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class IdMapTest {

  static long FIRST_ID_VALUE = 0L;

  IdMap map;

  @Before
  public void setup() {
    map = new IdMap();
  }

  @Test
  public void idValuesAreReturned() {
    assertThat(FIRST_ID_VALUE, is(map.get("http://example.org/a")));
  }

  @Test
  public void subsequentIdValuesAreReturned_whenDifferentKeysAreAlreadyAdded() {
    assertThat(FIRST_ID_VALUE, is(map.get("http://example.org/a")));
    assertThat(FIRST_ID_VALUE + 1, is(map.get("http://example.org/b")));
  }

  @Test
  public void identicalIdValuesAreReturned_whenKeysAreAlreadyPresent() {
    map.get("http://example.org/a");
    assertThat(FIRST_ID_VALUE, is(map.get("http://example.org/a")));
  }

}
