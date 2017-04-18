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
package io.scigraph.internal;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.prefixcommons.CurieUtil;

/***
 * A utility for more expressive Cypher queries.
 * 
 * <p>
 */
public class CypherUtil {

  /***
   * <dl>
   * <b>REGEX Groupings</b>
   * <dt>1</dt>
   * <dd>The variable name</dd>
   * <dt>2</dt>
   * <dd>The relationship type(s)</dd>
   * <dt>3</dt>
   * <dd>The query modifiers</dd>
   * </dl>
   */
  private static final String ENTAILMENT_REGEX = "\\[(\\w*):?([\\w:|\\.\\/#]*)([!*\\.\\d]*)\\]";
  private static Pattern ENTAILMENT_PATTERN = Pattern.compile(ENTAILMENT_REGEX);

  private final GraphDatabaseService graphDb;
  private final CurieUtil curieUtil;

  @Inject
  public CypherUtil(GraphDatabaseService graphDb, CurieUtil curieUtil) {
    this.graphDb = graphDb;
    this.curieUtil = curieUtil;
  }

  public Result execute(String query, Multimap<String, Object> params) {
    query = substituteRelationships(query, params);
    query = resolveRelationships(query);
    return graphDb.execute(query, flattenMap(params));
  }

  public Result execute(String query) {
    return execute(query, HashMultimap.<String, Object>create());
  }

  static Map<String, Object> flattenMap(Multimap<String, Object> paramMap) {
    Map<String, Object> flatMap = new HashMap<>();
    for (Entry<String, Collection<Object>> entry : paramMap.asMap().entrySet()) {
      flatMap.put(entry.getKey(), getFirst(entry.getValue(), null));
    }
    return flatMap;
  }

  public Set<RelationshipType> getEntailedRelationshipTypes(Collection<String> parents) {
    Set<String> relationshipNames = getEntailedRelationshipNames(parents);
    Collection<RelationshipType> relationshipTypes = Collections.emptySet();
    try (Transaction tx = graphDb.beginTx()) {
      relationshipTypes = transform(relationshipNames, new Function<String, RelationshipType>() {
        @Override
        public RelationshipType apply(String name) {
          return RelationshipType.withName(name);
        }
      });
      tx.success();
    }
    return new HashSet<RelationshipType>(relationshipTypes);
  }

  Set<String> getEntailedRelationshipNames(Collection<String> parents) {
    Set<String> entailedTypes = new HashSet<>();
    for (String parent : parents) {
      entailedTypes.add(parent);
      Map<String, Object> params = new HashMap<>();
      params.put("iri", parent);
      try (Transaction tx = graphDb.beginTx()) {
        Result result =
            graphDb.execute("START parent=node:node_auto_index(iri={iri}) "
                + "MATCH (parent)<-[:subPropertyOf|equivalentProperty*]-(subProperty) "
                + "RETURN distinct subProperty.iri as subProperty", params);
        while (result.hasNext()) {
          Map<String, Object> map = result.next();
          entailedTypes.add((String) map.get("subProperty"));
        }
        tx.success();
      }
    }
    return entailedTypes;
  }

  Collection<String> resolveTypes(String types, boolean entail) {
    Collection<String> resolvedTypes =
        transform(newHashSet(Splitter.on('|').omitEmptyStrings().split(types)),
            new Function<String, String>() {
              @Override
              public String apply(String type) {
                return curieUtil.getIri(type).orElse(type);
              }
            });
    if (entail) {
      return getEntailedRelationshipNames(resolvedTypes);
    } else {
      return resolvedTypes;
    }
  }

  /**
   * 
   * @param cypher
   * @return cypher with resolved STARTs
   * 
   *         Resolves CURIEs to full IRIs in the section between a START and a MATCH. e.g. from
   *         START n = node:node_auto_index(iri='DOID:4') match (n) return n to START n =
   *         node:node_auto_index(iri='http://purl.obolibrary.org/obo/DOID_4') match (n) return n
   */
  public String resolveStartQuery(String cypher) {
    String resolvedCypher = cypher;
    Pattern p = Pattern.compile("\\(\\s*iri\\s*=\\s*['|\"]([\\w:/\\?=]+)['|\"]\\s*\\)");
    Matcher m = p.matcher(cypher);
    while (m.find()) {
      String curie = m.group(1);
      String iri = curieUtil.getIri(curie).orElse(curie);
      resolvedCypher = resolvedCypher.replace(curie, iri);
    }

    return resolvedCypher;
  }

  public String resolveRelationships(String cypher) {
    Matcher m = ENTAILMENT_PATTERN.matcher(cypher);
    StringBuffer buffer = new StringBuffer();
    while (m.find()) {
      String varName = m.group(1);
      String types = m.group(2);
      String modifiers = m.group(3);
      Collection<String> resolvedTypes = resolveTypes(types, modifiers.contains("!"));
      modifiers = modifiers.replaceAll("!", "");
      String typeString = resolvedTypes.isEmpty() ? "" : ":`" + on("`|`").join(resolvedTypes) + "`";
      m.appendReplacement(buffer, "[" + varName + typeString + modifiers + "]");
    }
    m.appendTail(buffer);
    return buffer.toString();
  }

  String substituteRelationships(String query, final Multimap<String, Object> valueMap) {
    StrSubstitutor substitutor = new StrSubstitutor(new StrLookup<String>() {
      @Override
      public String lookup(String key) {
        Collection<String> resolvedRelationshipTypes =
            transform(valueMap.get(key), new Function<Object, String>() {
              @Override
              public String apply(Object input) {
                if (input.toString().matches(".*(\\s).*")) {
                  throw new IllegalArgumentException(
                      "Cypher relationship templates must not contain spaces");
                }
                return curieUtil.getIri(input.toString()).orElse(input.toString());
              }

            });
        return on("|").join(resolvedRelationshipTypes);
      }
    });
    return substitutor.replace(query);
  }

  public Map<String, String> getCurieMap() {
    return curieUtil.getCurieMap();
  }

}
