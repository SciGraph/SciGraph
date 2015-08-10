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
package io.scigraph.lucene;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import io.scigraph.lucene.LuceneUtils;

import org.junit.Test;

public class LuceneUtilsTest {

  @Test
  public void testIsAllStopwords() {
    assertThat(LuceneUtils.isAllStopwords(newArrayList("the", "a")), is(true));
    assertThat(LuceneUtils.isAllStopwords(newArrayList("the", "cat")), is(false));
  }

}
