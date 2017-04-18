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

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.isOneOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.hamcrest.collection.IsMapContaining;
import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.prefixcommons.CurieUtil;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.scigraph.owlapi.OwlRelationships;
import io.scigraph.util.GraphTestBase;

public class CypherUtilTest extends GraphTestBase {

  CypherUtil util;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setup() {
    CurieUtil curieUtil = mock(CurieUtil.class);
    when(curieUtil.getIri(anyString())).thenReturn(Optional.<String>empty());
    when(curieUtil.getIri("FOO:foo")).thenReturn(Optional.of("http://x.org/#foo"));
    when(curieUtil.getIri("FOO:fizz")).thenReturn(Optional.of("http://x.org/#fizz"));
    util = new CypherUtil(graphDb, curieUtil);
    addRelationship("http://x.org/#foo", "http://x.org/#fizz",
        OwlRelationships.RDFS_SUB_PROPERTY_OF);
    addRelationship("http://x.org/#bar", "http://x.org/#baz", OwlRelationships.RDFS_SUB_PROPERTY_OF);
    addRelationship("http://x.org/#fizz", "http://x.org/#fizz_equiv",
        OwlRelationships.OWL_EQUIVALENT_OBJECT_PROPERTY);
    addRelationship("http://x.org/#1", "http://x.org/#2",
        RelationshipType.withName("http://x.org/#fizz"));
  }

  @Test
  public void simpleQuery() {
    String query = util.resolveRelationships("(a)-[r]-(b)");
    assertThat(query, is("(a)-[r]-(b)"));
  }

  @Test
  public void simpleQueryWithDepth() {
    String query = util.resolveRelationships("(a)-[:r*0..1]-(b)");
    assertThat(query, is("(a)-[:`r`*0..1]-(b)"));
  }

  @Test
  public void naiveInjectionPrevention() {
    Multimap<String, Object> valueMap = HashMultimap.create();
    valueMap.put("node_id", "HP_123");
    valueMap.put("rel_id", "DELETE *");
    exception.expect(IllegalArgumentException.class);
    util.substituteRelationships("({node_id})-[:${rel_id}!]-(end)", valueMap);
  }

  @Test
  public void jsonPropertiesAreNotRegexed() {
    String query = util.resolveRelationships("(a {foo: 'bar'})-[:fizz*]-(end)");
    assertThat(query, is("(a {foo: 'bar'})-[:`fizz`*]-(end)"));
  }

  @Test
  public void substituteSingleRelationship() {
    Multimap<String, Object> valueMap = HashMultimap.create();
    valueMap.put("node_id", "HP_123");
    valueMap.put("rel_id", "RO_1");
    String actual = util.substituteRelationships("({node_id})-[:${rel_id}!]-(end)", valueMap);
    assertThat(actual, is("({node_id})-[:RO_1!]-(end)"));
  }

  @Test
  public void substituteCurieRelationship() {
    Multimap<String, Object> valueMap = HashMultimap.create();
    valueMap.put("node_id", "HP_123");
    valueMap.put("rel_id", "FOO:foo");
    String actual = util.substituteRelationships("({node_id})-[:${rel_id}!]-(end)", valueMap);
    assertThat(actual, is("({node_id})-[:http://x.org/#foo!]-(end)"));
  }

  @Test
  public void replaceCurieRelationship() {
    String actual = util.resolveRelationships("(start)-[:FOO:foo]-(end)");
    assertThat(actual, is("(start)-[:`http://x.org/#foo`]-(end)"));
  }

  @Test
  public void substituteMultipleRelationships() {
    Multimap<String, Object> valueMap = HashMultimap.create();
    valueMap.put("node_id", "HP_123");
    valueMap.put("rel_id", "RO_1");
    valueMap.put("rel_id", "RO_2");
    String actual = util.substituteRelationships("({node_id})-[:${rel_id}!]-(end)", valueMap);
    // TODO: A bit of a hack to get Java 7 and Java 8 to pass tests
    assertThat(actual,
        isOneOf("({node_id})-[:RO_2|RO_1!]-(end)", "({node_id})-[:RO_1|RO_2!]-(end)"));
  }

  @Test
  public void entailmentRegex() {
    Collection<String> resolvedTypes = util.resolveTypes("http://x.org/#foo", true);
    assertThat(resolvedTypes,
        containsInAnyOrder("http://x.org/#fizz", "http://x.org/#foo", "http://x.org/#fizz_equiv"));
  }

  @Test
  public void curiesAreEntailed() {
    Collection<String> resolvedTypes = util.resolveTypes("FOO:foo", true);
    assertThat(resolvedTypes,
        containsInAnyOrder("http://x.org/#fizz", "http://x.org/#foo", "http://x.org/#fizz_equiv"));
  }

  @Test
  public void multipleEntailmentRegex() {
    Set<String> types =
        util.getEntailedRelationshipNames(newHashSet("http://x.org/#fizz", "http://x.org/#bar"));
    assertThat(
        types,
        containsInAnyOrder("http://x.org/#bar", "http://x.org/#fizz", "http://x.org/#fizz_equiv",
            "http://x.org/#baz"));
  }

  @Test
  public void multipleEntailmentRegex_types() {
    Set<RelationshipType> types =
        util.getEntailedRelationshipTypes(newHashSet("http://x.org/#foo", "http://x.org/#bar"));
    Collection<String> typeNames = transform(types, new Function<RelationshipType, String>() {
      @Override
      public String apply(RelationshipType arg0) {
        return arg0.name();
      }
    });
    assertThat(
        typeNames,
        containsInAnyOrder("http://x.org/#foo", "http://x.org/#bar", "http://x.org/#fizz",
            "http://x.org/#fizz_equiv", "http://x.org/#baz"));
  }

  @Test
  public void flattenMap() {
    Multimap<String, Object> multiMap = HashMultimap.create();
    multiMap.put("foo", "bar");
    multiMap.put("foo", "baz");
    assertThat(CypherUtil.flattenMap(multiMap), IsMapContaining.hasKey("foo"));
  }

  @Test
  public void executeExecutes() {
    Multimap<String, Object> params = HashMultimap.create();
    params.put("iri", "http://x.org/#1");
    params.put("rel", "FOO:fizz");
    Result result = util.execute("MATCH (n)<-[:${rel}!]-(n2) WHERE n.iri = {iri} RETURN n", params);
    assertThat(result.next().size(), is(1));
  }

  @Test
  public void resolveStartQueryWithSingleMatch() {
    String cypher = "START n = node:node_auto_index(iri='FOO:foo') match (n) return n";
    assertThat(util.resolveStartQuery(cypher),
        IsEqual
            .equalTo("START n = node:node_auto_index(iri='http://x.org/#foo') match (n) return n"));
  }

  @Test
  public void resolveStartQueryWithMultipleMatches() {
    String cypher =
        "START n = node:node_auto_index(iri='FOO:foo') match (n) UNION START m = node:node_auto_index(iri='FOO:fizz') match (m) return n,m";
    assertThat(
        util.resolveStartQuery(cypher),
        IsEqual
            .equalTo("START n = node:node_auto_index(iri='http://x.org/#foo') match (n) UNION START m = node:node_auto_index(iri='http://x.org/#fizz') match (m) return n,m"));
  }

  @Test
  public void notResolveStartQueryWithIris() {
    String cypher =
        "START n = node:node_auto_index(iri='http://x.org/#foo') match (n) UNION START m = node:node_auto_index(iri='http://x.org/#fizz') match (m) return n,m";
    assertThat(
        util.resolveStartQuery(cypher),
        IsEqual
            .equalTo("START n = node:node_auto_index(iri='http://x.org/#foo') match (n) UNION START m = node:node_auto_index(iri='http://x.org/#fizz') match (m) return n,m"));
  }

  @Test
  public void unalterRandomString() {
    String cypher = "foo";
    assertThat(util.resolveStartQuery(cypher), IsEqual.equalTo("foo"));
  }
}
