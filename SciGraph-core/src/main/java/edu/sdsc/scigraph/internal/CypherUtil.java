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

  private static final String ENTAILMENT_REGEX = "\\:[a-zA-Z0-9_:.|]*!";
  private static Pattern pattern = Pattern.compile(ENTAILMENT_REGEX);

  private final GraphDatabaseService graphDb;
  private final CurieUtil curieUtil;

  @Inject
  public CypherUtil(GraphDatabaseService graphDb, CurieUtil curieUtil) {
    this.graphDb = graphDb;
    this.curieUtil = curieUtil;
  }

  public Result execute(String query, Multimap<String, Object> params) {
    query = substituteRelationships(query, params);
    query = entailRelationships(query);
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

  Set<String> getEntailedRelationshipTypes(Set<String> parents) {
    Set<String> entailedTypes = new HashSet<>();
    for (String parent: parents) {
      if (curieUtil.getIri(parent).isPresent()) {
        parent = GraphUtil.getFragment(curieUtil.getIri(parent).get());
      }
      entailedTypes.add(parent);
      Map<String, Object> params = new HashMap<>();
      params.put("fragment", parent);
      try (Transaction tx = graphDb.beginTx()) {
        Result result = graphDb.execute(
            "START parent=node:node_auto_index(fragment={fragment}) " +
                "MATCH (parent)<-[:subPropertyOf*]-(subProperty) " +
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

  String entailRelationships(String cypher) {
    Matcher m = pattern.matcher(cypher);
    StringBuffer buffer = new StringBuffer();
    while (m.find()) {
      String group = m.group().substring(1, m.group().length() - 1);
      Set<String> parentTypes = newHashSet(Splitter.on('|').split(group));
      String entailedTypes = on('|').join(getEntailedRelationshipTypes(parentTypes));
      m.appendReplacement(buffer, ":" + entailedTypes);
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
