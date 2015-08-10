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

import static org.hamcrest.MatcherAssert.assertThat;
import io.scigraph.services.jersey.CustomMediaTypes;
import io.scigraph.services.jersey.MediaTypeMappings;

import javax.ws.rs.core.MediaType;

import org.hamcrest.collection.IsMapContaining;
import org.junit.Test;

public class MediaTypeMappingsTest {

  @Test
  public void verifyMap() {
    MediaTypeMappings mappings = new MediaTypeMappings();
    assertThat(mappings, IsMapContaining.<String, MediaType>hasEntry("graphson", CustomMediaTypes.APPLICATION_GRAPHSON_TYPE));
    assertThat(mappings, IsMapContaining.<String, MediaType>hasEntry("xml", MediaType.APPLICATION_XML_TYPE));
  }

}
