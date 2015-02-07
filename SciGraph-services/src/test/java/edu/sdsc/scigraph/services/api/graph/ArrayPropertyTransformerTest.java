package edu.sdsc.scigraph.services.api.graph;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import edu.sdsc.scigraph.frames.CommonProperties;

public class ArrayPropertyTransformerTest {

  TinkerGraph graph;
  Vertex v1, v2;
  Edge e;

  @Before
  public void setup() {
    TinkerGraph graph = new TinkerGraph();
    v1 = graph.addVertex(0);
    v2 = graph.addVertex(1);
    v1.setProperty(CommonProperties.URI, "foo");
    v1.setProperty("foo", "bar");
    e = graph.addEdge(0, v1, v2, "test");
    e.setProperty("foo", 1);
    ArrayPropertyTransformer.transform(graph);
  }

  @Test
  public void singleProperties_areConvertedToArrays() {
    assertThat((String[])v1.getProperty("foo"), is(new String[]{"bar"}));
  }

  @Test
  public void edgeProperties_areConvertedToArrays() {
    assertThat((Integer[])e.getProperty("foo"), is(new Integer[]{1}));
  }

  @Test
  public void protectedProperties_stayPrimitive() {
    assertThat((String)v1.getProperty(CommonProperties.URI), is("foo"));
  }

}
