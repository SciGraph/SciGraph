package edu.sdsc.scigraph.services.api.graph;

import static com.google.common.collect.Sets.newHashSet;

import java.lang.reflect.Array;
import java.util.Collection;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.EdgeProperties;

public class ArrayPropertyTransformer {

  private static final Collection<String> protectedKeys = newHashSet(
      CommonProperties.CONVENIENCE, CommonProperties.FRAGMENT, CommonProperties.URI, CommonProperties.OWL_TYPE,
      EdgeProperties.QUANTIFICATION_TYPE, EdgeProperties.REFLEXIVE, EdgeProperties.SYMMETRIC, EdgeProperties.TRANSITIVE);

  static void transform(Iterable<? extends Element> elements) {
    for (Element element: elements) {
      for (String key: element.getPropertyKeys()) {
        if (protectedKeys.contains(key)) {
          continue;
        } else {
          Object value = element.getProperty(key);
          if (value instanceof Iterable) {
            // TODO: Should these be transformed to arrays (or vice versa?)
          } else if (value.getClass().isArray()) {
          } else {
            Object newValue = Array.newInstance(value.getClass(), 1);
            Array.set(newValue, 0, value);
            element.setProperty(key, newValue);
          }
        }
      }
    }
  }

  public static void transform(TinkerGraph graph) {
    transform(graph.getVertices());
    transform(graph.getEdges());
  }

}
