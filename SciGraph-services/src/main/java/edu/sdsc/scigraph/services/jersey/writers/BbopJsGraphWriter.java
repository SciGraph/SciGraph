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

import static com.google.common.collect.Iterables.getFirst;
import io.dropwizard.jackson.Jackson;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.internal.TinkerGraphUtil;
import edu.sdsc.scigraph.services.api.graph.BbopGraph;

@Produces(MediaType.APPLICATION_JSON)
@Provider
public class BbopJsGraphWriter extends GraphWriter {

  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

  private static final ImmutableSet<String> IGNORED_PROPERTY_KEYS =
      ImmutableSet.of(CommonProperties.IRI, NodeProperties.LABEL, CommonProperties.CURIE);

  static String getCurieOrIri(Vertex vertex) {
    return TinkerGraphUtil.getProperty(vertex, CommonProperties.CURIE, String.class).or((String)vertex.getProperty(CommonProperties.IRI));
  }

  @Override
  public void writeTo(Graph data, Class<?> type, Type genericType, Annotation[] annotations,
      MediaType mediaType, MultivaluedMap<String, Object> headers, OutputStream out) throws IOException {
    BbopGraph bbopGraph = new BbopGraph();
    for (Vertex vertex: data.getVertices()) {
      BbopGraph.BbopNode bbopNode = new BbopGraph.BbopNode();
      bbopNode.setId(getCurieOrIri(vertex));
      String label = getFirst(TinkerGraphUtil.getProperties(vertex, NodeProperties.LABEL, String.class), null);
      bbopNode.setLbl(label);
      for (String key: vertex.getPropertyKeys()) {
        if (IGNORED_PROPERTY_KEYS.contains(key)) {
          continue;
        }
        Collection<Object> values = TinkerGraphUtil.getProperties(vertex, key, Object.class);
        bbopNode.getMeta().put(key, values);
      }
      bbopGraph.getNodes().add(bbopNode);
    }
    for (Edge edge: data.getEdges()) {
      BbopGraph.BbopEdge bbopEdge = new BbopGraph.BbopEdge();
      Vertex subject= edge.getVertex(Direction.OUT);
      Vertex object= edge.getVertex(Direction.IN);
      bbopEdge.setSub(getCurieOrIri(subject));
      bbopEdge.setObj(getCurieOrIri(object));
      bbopEdge.setPred(edge.getLabel());
      bbopGraph.getEdges().add(bbopEdge);
    }
    MAPPER.writeValue(out, bbopGraph);
    out.flush();
  }

}

