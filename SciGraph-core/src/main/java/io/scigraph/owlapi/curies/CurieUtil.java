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
package io.scigraph.owlapi.curies;

import static com.google.common.base.Preconditions.checkNotNull;
import io.scigraph.neo4j.bindings.IndicatesCurieMapping;
import io.scigraph.owlapi.curies.trie.Trie;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;

/***
 * Utilities for resolving CURIEs
 */
public class CurieUtil {

  private final ImmutableBiMap<String, String> curieMap;
  private final Trie trie;

  /***
   * @param curieMap A map from CURIE prefix to IRI prefix
   */
  @Inject
  public CurieUtil(@IndicatesCurieMapping Map<String, String> curieMap) {
    this.curieMap = ImmutableBiMap.copyOf(checkNotNull(curieMap));
    this.trie = new Trie();
    for (String key : this.curieMap.inverse().keySet()) {
      this.trie.insert(key);
    }
  }

  /***
   * @return all of the CURIE prefixes
   */
  public Collection<String> getPrefixes() {
    return curieMap.keySet();
  }

  /***
   * Expand a CURIE prefix to the corresponding IRI prefix.
   * 
   * @param curiePrefix
   * @return mapped IRI prefix
   */
  public String getExpansion(String curiePrefix) {
    return curieMap.get(curiePrefix);
  }

  /***
   * Returns the CURIE of an IRI, if mapped.
   * 
   * @param iri
   * @return An {@link Optional} CURIE
   */
  public Optional<String> getCurie(String iri) {
    String prefix = trie.getMatchingPrefix(iri);
    if (prefix.equals("")) {
      return Optional.absent();
    } else {
      String curiePrefix = curieMap.inverse().get(prefix);
      return Optional.of(String.format("%s:%s", curiePrefix,
          iri.substring(prefix.length(), iri.length())));
    }
  }

  /***
   * Expands a CURIE to a full IRI, if mapped.
   * 
   * @param curie
   * @return an {@link Optional} IRI
   */
  public Optional<String> getIri(String curie) {
    String[] parts = checkNotNull(curie).split(":");
    if (parts.length > 1) {
      String prefix = parts[0];
      if (curieMap.containsKey(prefix)) {
        return Optional.of(String.format("%s%s", curieMap.get(prefix),
            curie.substring(curie.indexOf(':') + 1)));
      }
    }
    return Optional.absent();
  }

  /***
   * Returns the curie map.
   * 
   * @return the curie map
   */
  public Map<String, String> getCurieMap() {
    return curieMap;
  }

}
