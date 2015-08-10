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
package io.scigraph.services.api.graph;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import io.scigraph.frames.CommonProperties;
import io.scigraph.frames.EdgeProperties;

import java.util.Arrays;
import java.util.Collection;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;

public class ArrayPropertyTransformer {

  private static final Collection<String> PROTECTED_PROPERTY_KEYS = newHashSet(
      CommonProperties.CURIE, CommonProperties.CONVENIENCE, CommonProperties.IRI, CommonProperties.OWL_TYPE,
      EdgeProperties.QUANTIFICATION_TYPE, EdgeProperties.REFLEXIVE, EdgeProperties.SYMMETRIC, EdgeProperties.TRANSITIVE);

  static void transform(Iterable<? extends Element> elements) {
    for (Element element: elements) {
      for (String key: element.getPropertyKeys()) {
        if (PROTECTED_PROPERTY_KEYS.contains(key)) {
          continue;
        } else {
          Object value = element.getProperty(key);
          if (value instanceof Iterable) {
            // Leave it
          } else if (value.getClass().isArray()) {
            element.setProperty(key, Arrays.asList(value));
          } else {
            element.setProperty(key, newArrayList(value));
          }
        }
      }
    }
  }

  public static void transform(Graph graph) {
    transform(graph.getVertices());
    transform(graph.getEdges());
  }

}
