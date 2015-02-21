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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/***
 * Utilities for resolving CURIEs
 */
public class CurieUtil {

  private final Map<String, String> curieMap;
  private final Multimap<String, String> uriMap = HashMultimap.create();

  @Inject
  CurieUtil(@Named("neo4j.curieMap") Map<String, String> curieMap) {
    this.curieMap = ImmutableMap.copyOf(curieMap);
    for (Entry<String, String> curie: curieMap.entrySet()) {
      uriMap.put(curie.getValue(), curie.getKey());
    }
  }

  /***
   * @return all of the CURIE prefixes
   */
  public Collection<String> getPrefixes() {
    return uriMap.keySet();
  }

  /***
   * Expand a CURIE prefix to the its corresponding IRI prefix(es).
   * 
   * @param curiePrefix the CURIE prefix
   * @return mapped IRI prefix(es)
   */
  public Collection<String> getAllExpansions(final String curiePrefix) {
    return uriMap.get(curiePrefix);
  }

  /***
   * Returns the first CURIE found for {@code iri}
   * 
   * @param iri 
   * @return An {@link Optional} CURIE
   */
  public Optional<String> getCurie(final String iri) {
    checkNotNull(iri);
    for (Entry<String, String> entry: curieMap.entrySet()) {
      if (iri.startsWith(entry.getKey())) {
        return Optional.of(String.format("%s:%s", entry.getValue(), iri.substring(entry.getKey().length(), iri.length())));
      }
    }
    return Optional.absent();
  }

  /***
   * Expand a CURIE to an IRI
   * 
   * @param curie the CURIE
   * @return all possible IRIs
   */
  public Collection<String> getFullUri(final String curie) {
    String[] parts = checkNotNull(curie).split(":");
    if (parts.length > 1) {
      String prefix = parts[0];
      if (uriMap.containsKey(prefix)) {
        return transform(uriMap.get(prefix), new Function<String, String>() {
          @Override
          public String apply(String uriPrefix) {
            return String.format("%s%s", uriPrefix, curie.substring(curie.indexOf(':') + 1));
          }
        });
      }
    }
    return Collections.emptySet();
  }

}
