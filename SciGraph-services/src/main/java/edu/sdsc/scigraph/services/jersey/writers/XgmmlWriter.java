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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import jersey.repackaged.com.google.common.collect.Iterables;

import com.google.common.base.Optional;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.services.jersey.CustomMediaTypes;
import edu.sdsc.scigraph.services.xgmml.Att;
import edu.sdsc.scigraph.services.xgmml.ObjectFactory;
import edu.sdsc.scigraph.services.xgmml.SimpleEdge;
import edu.sdsc.scigraph.services.xgmml.SimpleGraph;
import edu.sdsc.scigraph.services.xgmml.SimpleNode;

@Produces(CustomMediaTypes.APPLICATION_XGMML)
@Provider
public class XgmmlWriter extends GraphWriter {

  private static final Logger logger = Logger.getLogger(XgmmlWriter.class.getName());

  private static final ObjectFactory factory = new ObjectFactory();
  private final JAXBContext context;

  public XgmmlWriter() throws JAXBException {
    context = JAXBContext.newInstance(SimpleGraph.class);
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

  List<Att> getAtts(Element elt) {
    List<Att> attList = new ArrayList<>();
    for (String key: elt.getPropertyKeys()) {
      Att att = factory.createAtt();
      att.setName(key);
      att.setValue(elt.getProperty(key).toString());
      attList.add(att);
    }
    return attList;
  }

  @Override
  public void writeTo(Graph data, Class<?> type, Type genericType, Annotation[] annotations,
      MediaType mediaType, MultivaluedMap<String, Object> headers, OutputStream out) throws IOException {
    SimpleGraph graph = factory.createSimpleGraph();
    graph.setDirected(BigInteger.ONE);
    for (Vertex vertex: data.getVertices()) {
      SimpleNode node = factory.createSimpleNode();
      node.setId(BigInteger.valueOf(Long.parseLong((String)vertex.getId())));
      Optional<String> label = getLabel(vertex);
      if (label.isPresent()) {
        node.setLabel(label.get());
      }
      node.getAtt().addAll(getAtts(vertex));
      graph.getNodeOrEdge().add(node);
    }
    for (Edge edge: data.getEdges()) {
      SimpleEdge e = factory.createSimpleEdge();
      Vertex subject= edge.getVertex(Direction.OUT);
      Vertex object= edge.getVertex(Direction.IN);
      e.setSource(BigInteger.valueOf(Long.parseLong((String)subject.getId())));
      e.setTarget(BigInteger.valueOf(Long.parseLong((String)object.getId())));
      e.setType(edge.getLabel());
      e.getAtt().addAll(getAtts(edge));
      graph.getNodeOrEdge().add(e);
    }
    try {
      Marshaller marshaller = context.createMarshaller();
      JAXBElement<SimpleGraph> root = new JAXBElement<SimpleGraph>(new QName("http://www.cs.rpi.edu/XGMML","graph"), SimpleGraph.class, graph);
      marshaller.marshal(root, out);
      out.flush();
    } catch (JAXBException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }
  }

}

