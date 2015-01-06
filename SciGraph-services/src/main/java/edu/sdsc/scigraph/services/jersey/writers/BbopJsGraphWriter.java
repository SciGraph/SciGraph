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

import io.dropwizard.jackson.Jackson;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.services.api.graph.BbopGraph;

@Produces(MediaType.APPLICATION_JSON)
@Provider
public class BbopJsGraphWriter implements MessageBodyWriter<Graph> {

  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();
  
  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return Graph.class.isAssignableFrom(type);
  }

  @Override
  public long getSize(Graph data, Class<?> type, Type genericType, Annotation annotations[], MediaType mediaType) {
    return -1;
  }

  @Override
  public void writeTo(Graph data, Class<?> type, Type genericType, Annotation[] annotations,
      MediaType mediaType, MultivaluedMap<String, Object> headers, OutputStream out) throws IOException {
    BbopGraph bbopGraph = new BbopGraph();
    for (Vertex vertex: data.getVertices()) {
      BbopGraph.BbopNode bbopNode = new BbopGraph.BbopNode();
      bbopNode.setId((String)vertex.getProperty(CommonProperties.FRAGMENT));
      bbopNode.setLbl((String)vertex.getProperty(NodeProperties.LABEL));
      bbopGraph.getNodes().add(bbopNode);
    }
    for (Edge edge: data.getEdges()) {
      BbopGraph.BbopEdge bbopEdge = new BbopGraph.BbopEdge();
      Vertex subject= edge.getVertex(Direction.OUT);
      Vertex object= edge.getVertex(Direction.IN);
      bbopEdge.setSub((String)subject.getProperty(CommonProperties.FRAGMENT));
      bbopEdge.setObj((String)object.getProperty(CommonProperties.FRAGMENT));
      bbopEdge.setPred(edge.getLabel());
      bbopGraph.getEdges().add(bbopEdge);
    }
    MAPPER.writeValue(out, bbopGraph);
    out.flush();
  }

}

