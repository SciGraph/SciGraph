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
package io.scigraph.services.jersey.dynamic;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriInfo;

import io.swagger.models.Path;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.prefixcommons.CurieUtil;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

public class DynamicResourceModuleIT {

  static GraphDatabaseService graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();

  static ContainerRequestContext context = mock(ContainerRequestContext.class);
  static UriInfo uriInfo = mock(UriInfo.class);

  Injector i = Guice.createInjector(new TestModule());
  Path path = new Path();
  
  static class TestModule extends AbstractModule {

    @Override
    protected void configure() {
      install(new DynamicResourceModule());
      bind(GraphDatabaseService.class).toInstance(graphDb);
      bind(CurieUtil.class).toInstance(new CurieUtil(new HashMap<String, String>()));
    }

  }

  @BeforeClass
  public static void setup() {
    try (Transaction tx = graphDb.beginTx()) {
      Node node = graphDb.createNode();
      Node node2 = graphDb.createNode();
      node.createRelationshipTo(node2, RelationshipType.withName("foo"));
      node.setProperty("foo", "bar");
      tx.success();
    }
    when(context.getUriInfo()).thenReturn(uriInfo);
    MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
    map.put("foo", newArrayList("bar"));
    when(uriInfo.getQueryParameters()).thenReturn(map);
    when(uriInfo.getPathParameters()).thenReturn(new MultivaluedHashMap<String, String>());
  }

  @Test
  public void nodesAreReturned() {
    path.setVendorExtension("x-query", "MATCH (n) RETURN n");
    CypherInflector inflector = i.getInstance(CypherInflectorFactory.class).create("foo", path);
    Graph graph = (Graph) inflector.apply(context).getEntity();
    assertThat(graph.getVertices(), Matchers.<Vertex>iterableWithSize(2));
    assertThat(graph.getEdges(), Matchers.<Edge>iterableWithSize(0));
  }

  @Test
  public void edgesAreReturned() {
    path.setVendorExtension("x-query", "MATCH (n)-[r]-(m) RETURN n, r, m");
    CypherInflector inflector = i.getInstance(CypherInflectorFactory.class).create("foo", path);
    Graph graph = (Graph) inflector.apply(context).getEntity();
    assertThat(graph.getVertices(), Matchers.<Vertex>iterableWithSize(2));
    assertThat(graph.getEdges(), Matchers.<Edge>iterableWithSize(1));
  }
  
  @Test
  public void propertiesAreSubstituted() {
    path.setVendorExtension("x-query", "MATCH (n)-[r]-(m) WHERE n.foo = {foo} RETURN n, r, m");
    CypherInflector inflector = i.getInstance(CypherInflectorFactory.class).create("foo", path);
    Graph graph = (Graph) inflector.apply(context).getEntity();
    assertThat(graph.getVertices(), Matchers.<Vertex>iterableWithSize(2));
    assertThat(graph.getEdges(), Matchers.<Edge>iterableWithSize(1));
  }

}
