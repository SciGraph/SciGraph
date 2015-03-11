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
package edu.sdsc.scigraph.services.jersey.writers;

import static com.google.common.collect.Lists.newArrayList;
import io.dropwizard.jackson.Jackson;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import jersey.repackaged.com.google.common.collect.Iterables;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.services.api.graph.BbopGraph;

@Produces(MediaType.APPLICATION_JSON)
@Provider
public class BbopJsGraphWriter extends GraphWriter {

  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

  // TODO: Move these next three methods someplace common
  String getCurieOrFragment(Vertex vertex) {
    String property = vertex.getPropertyKeys().contains(CommonProperties.CURIE) 
        ? CommonProperties.CURIE : CommonProperties.FRAGMENT; 
    return (String)vertex.getProperty(property);
  }

  static Optional<String> getLabel(Vertex vertex) {
    Optional<String> label = Optional.absent();
    if (vertex.getPropertyKeys().contains(NodeProperties.LABEL)) {
      Object value = vertex.getProperty(NodeProperties.LABEL);
      if (value.getClass().isArray()) {
        label = Optional.of((String)Array.get(value, 0));
      } else if (value instanceof Iterable) {
        label = Optional.of((String)Iterables.getFirst((Iterable<?>)value, null));
      } else {
        label = Optional.of((String) value);
      }
    }
    return label;
  }

  static Collection<String> getCategories(Vertex vertex) {
    Collection<String> categories = new HashSet<>();
    if (vertex.getPropertyKeys().contains(Concept.CATEGORY)) {
      Object value = vertex.getProperty(Concept.CATEGORY);
      if (value.getClass().isArray()) {
        for (int i = 0; i < Array.getLength(value); i++) {
          categories.add((String)Array.get(value, i));
        }
      } else if (value instanceof Iterable) {
        categories.addAll(newArrayList((Iterable<String>)value)); 
      } else {
        categories.add((String) value);
      }
    }
    return categories;
  }

  @Override
  public void writeTo(Graph data, Class<?> type, Type genericType, Annotation[] annotations,
      MediaType mediaType, MultivaluedMap<String, Object> headers, OutputStream out) throws IOException {
    BbopGraph bbopGraph = new BbopGraph();
    for (Vertex vertex: data.getVertices()) {
      BbopGraph.BbopNode bbopNode = new BbopGraph.BbopNode();
      bbopNode.setId(getCurieOrFragment(vertex));
      Optional<String> label = getLabel(vertex);
      if (label.isPresent()) {
        bbopNode.setLbl(label.get());
      }
      bbopNode.getMeta().put(Concept.CATEGORY, getCategories(vertex));
      bbopGraph.getNodes().add(bbopNode);
    }
    for (Edge edge: data.getEdges()) {
      BbopGraph.BbopEdge bbopEdge = new BbopGraph.BbopEdge();
      Vertex subject= edge.getVertex(Direction.OUT);
      Vertex object= edge.getVertex(Direction.IN);
      bbopEdge.setSub(getCurieOrFragment(subject));
      bbopEdge.setObj(getCurieOrFragment(object));
      bbopEdge.setPred(edge.getLabel());
      bbopGraph.getEdges().add(bbopEdge);
    }
    MAPPER.writeValue(out, bbopGraph);
    out.flush();
  }

}

