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
package io.scigraph.owlapi.curies;

import java.util.Optional;

import javax.inject.Inject;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.prefixcommons.CurieUtil;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

import io.scigraph.frames.CommonProperties;

final class CurieAdder implements MethodInterceptor {

  @Inject
  CurieUtil curieUtil;

  void addCuries(Graph graph) {
    for (Vertex vertex: graph.getVertices()) {
      String iri = (String)vertex.getProperty(CommonProperties.IRI);
      Optional<String> curie = curieUtil.getCurie(iri);
      if (curie.isPresent()) {
        vertex.setProperty(CommonProperties.CURIE, curie.get());
      }
    }
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Object result = invocation.proceed();
    if (result instanceof Graph) {
      addCuries((Graph)result);
    }
    return result;
  }

}
