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
package io.scigraph.services.jersey;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;

import com.google.common.collect.Multimap;

public class MapUtilsTest {

  UriInfo uriInfo = mock(UriInfo.class);

  @Test
  public void mergesPathAndQueryParams() {
    MultivaluedHashMap<String, String> pathParamMap = new MultivaluedHashMap<>();
    pathParamMap.put("pathParam", newArrayList("paramValue"));
    MultivaluedHashMap<String, String> queryParamMap = new MultivaluedHashMap<>();
    pathParamMap.put("rel_id", newArrayList("fizz"));
    when(uriInfo.getPathParameters()).thenReturn(pathParamMap);
    when(uriInfo.getQueryParameters()).thenReturn(queryParamMap);
    Multimap<String, Object> actual = MultivaluedMapUtils.merge(uriInfo);
    assertThat(actual.get("pathParam"), contains((Object)"paramValue"));
    assertThat(actual.get("rel_id"), contains((Object)"fizz"));
  }

  @Test
  public void splitsMultivaluedPathParams() {
    MultivaluedHashMap<String, String> paramMap = new MultivaluedHashMap<>();
    paramMap.put("key", newArrayList("value1+value2"));
    Multimap<String, Object> actual = MultivaluedMapUtils.multivaluedMapToMultimap(paramMap, Optional.of('+'));
    assertThat(actual.get("key"), contains((Object)"value1", (Object)"value2"));
  }

}
