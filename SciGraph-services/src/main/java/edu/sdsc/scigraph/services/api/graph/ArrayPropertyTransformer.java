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
