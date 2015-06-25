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
package edu.sdsc.scigraph.internal;

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
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.owlapi.curies.CurieUtil;

/***
 * A utility for more expressive Cypher queries.
 * 
 * <p>
 */
public class CypherUtil {

  private static final String ENTAILMENT_REGEX = "\\:[a-zA-Z0-9_:.|]*!?";
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
    for (Entry<String, Collection<Object>> entry: paramMap.asMap().entrySet()) {
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
          return DynamicRelationshipType.withName(name);
        }
      });
      tx.success();
    }
    return new HashSet<RelationshipType>(relationshipTypes);
  }

  Set<String> getEntailedRelationshipNames(Collection<String> parents) {
    Set<String> entailedTypes = new HashSet<>();
    for (String parent: parents) {
      entailedTypes.add(parent);
      Map<String, Object> params = new HashMap<>();
      params.put("fragment", parent);
      try (Transaction tx = graphDb.beginTx()) {
        Result result = graphDb.execute(
            "START parent=node:node_auto_index(fragment={fragment}) " +
                "MATCH (parent)<-[:subPropertyOf|equivalentProperty*]-(subProperty) " +
                "RETURN distinct subProperty.fragment as subProperty", params);
        while (result.hasNext()) {
          Map<String, Object> map = result.next();
          entailedTypes.add((String) map.get("subProperty"));
        }
        tx.success();
      }
    }
    return entailedTypes;
  }

  String resolveRelationships(String cypher) {
    Matcher m = ENTAILMENT_PATTERN.matcher(cypher);
    StringBuffer buffer = new StringBuffer();
    while (m.find()) {
      String group = m.group().substring(1, m.group().length());
      boolean entail = false;
      if (group.endsWith("!")) {
        entail = true;
        group = group.substring(0, group.length() - 1);
      }
      Collection<String> parentTypes = transform(newHashSet(Splitter.on('|').split(group)), new Function<String, String>() {
        @Override
        public String apply(String type) {
          if (curieUtil.getIri(type).isPresent()) {
            return GraphUtil.getFragment(curieUtil.getIri(type).get());
          } else {
            return type;
          }
        }

      });
      if (entail) {
        String entailedTypes = on('|').join(getEntailedRelationshipNames(parentTypes));
        m.appendReplacement(buffer, ":" + entailedTypes);
      } else {
        String resolvedTypes = on('|').join(parentTypes);
        m.appendReplacement(buffer, ":" + resolvedTypes);
      }
    }
    m.appendTail(buffer);
    return buffer.toString();
  }

  String substituteRelationships(String query, final Multimap<String, Object> valueMap) {
    StrSubstitutor substitutor = new StrSubstitutor(new StrLookup<String>() {
      @Override
      public String lookup(String key) {
        Collection<String> resolvedRelationshipTypes = transform(valueMap.get(key), new Function<Object, String>() {
          @Override
          public String apply(Object input) {
            if (input.toString().matches(".*(\\s).*")) {
              throw new IllegalArgumentException("Cypher relationship templates must not contain spaces");
            }
            Optional<String> iri = curieUtil.getIri(input.toString());
            if (iri.isPresent()) {
              return GraphUtil.getFragment(iri.get());
            } else {
              return input.toString();
            }
          }

        });
        return on('|').join(resolvedRelationshipTypes);
      }
    });
    return substitutor.replace(query);
  }

}
