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
package edu.sdsc.scigraph.bbop;

import static com.google.common.collect.Iterables.getFirst;

import java.util.Collection;

import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.internal.TinkerGraphUtil;
import edu.sdsc.scigraph.owlapi.curies.CurieUtil;

public class BbopGraphUtil {

  private final CurieUtil curieUtil;

  private static final ImmutableSet<String> IGNORED_PROPERTY_KEYS =
      ImmutableSet.of(CommonProperties.IRI, NodeProperties.LABEL, CommonProperties.CURIE);

  @Inject
  public BbopGraphUtil(CurieUtil curieUtil) {
    this.curieUtil = curieUtil;
  }

  String getCurieOrIri(Vertex vertex) {
    String iri = (String)vertex.getProperty(CommonProperties.IRI);
    return curieUtil.getCurie(iri).or(iri);
  }
  
  /***
   * @param graph The graph to convert
   * @return a bbop representation of a {@link Graph}
   */
  public BbopGraph convertGraph(Graph graph) {
    BbopGraph bbopGraph = new BbopGraph();
    for (Vertex vertex: graph.getVertices()) {
      BbopNode bbopNode = new BbopNode();
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
    for (Edge edge: graph.getEdges()) {
      BbopEdge bbopEdge = new BbopEdge();
      Vertex subject= edge.getVertex(Direction.OUT);
      Vertex object= edge.getVertex(Direction.IN);
      bbopEdge.setSub(getCurieOrIri(subject));
      bbopEdge.setObj(getCurieOrIri(object));
      bbopEdge.setPred(edge.getLabel());
      bbopGraph.getEdges().add(bbopEdge);
    }
    return bbopGraph;
  }

}
