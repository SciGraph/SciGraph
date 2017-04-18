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

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import jersey.repackaged.com.google.common.base.Splitter;

public class MultivaluedMapUtils {

  /***
   * Converts a {@link MultivaluedMap} to a {@link Multimap}.
   * 
   * @param map the original map
   * @param separator an optional separator to further split the values in the map
   * @return the new multimap
   */
  public static Multimap<String, Object> multivaluedMapToMultimap(MultivaluedMap<String, String> map, Optional<Character> separator) {
    Multimap<String, Object> merged = ArrayListMultimap.create();
    for (Entry<String, List<String>> entry: map.entrySet()) {
      for (String value: entry.getValue()) {
        if (separator.isPresent()) {
          merged.putAll(entry.getKey(), Splitter.on(separator.get()).split(value));
        } else {
          merged.put(entry.getKey(), value);
        }
      }
    }
    return merged;
  }

  /***
   * Converts a {@link MultivaluedMap} to a {@link Multimap}.
   * 
   * @param map the original map
   * @return the new multimap
   */
  public static Multimap<String, Object> multivaluedMapToMultimap(MultivaluedMap<String, String> map) {
    return multivaluedMapToMultimap(map, Optional.<Character>empty());
  }

  /***
   * Extracts both the path and query parameters from {@link UriInfo} into a {@link Multimap}
   * 
   * <p>Path parameters are additionally split on '+' to support multiple values 
   * 
   * @param uriInfo the uriinfo
   * @return the merged multimap
   */
  public static Multimap<String, Object> merge(UriInfo uriInfo) {
    Multimap<String, Object> merged = ArrayListMultimap.create();
    merged.putAll(multivaluedMapToMultimap(uriInfo.getPathParameters(), Optional.of('+')));
    merged.putAll(multivaluedMapToMultimap(uriInfo.getQueryParameters()));
    return merged;
  }

}
