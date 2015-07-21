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

import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinkerpop.blueprints.Graph;

import edu.sdsc.scigraph.bbop.BbopGraph;
import edu.sdsc.scigraph.bbop.BbopGraphUtil;

@Produces(MediaType.APPLICATION_JSON)
@Provider
public class BbopJsGraphWriter extends GraphWriter {

  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

  private final BbopGraphUtil graphUtil;

  @Inject
  public BbopJsGraphWriter(BbopGraphUtil graphUtil) {
    this.graphUtil = graphUtil;
  }

  @Override
  public void writeTo(Graph data, Class<?> type, Type genericType, Annotation[] annotations,
      MediaType mediaType, MultivaluedMap<String, Object> headers, OutputStream out) throws IOException {
    BbopGraph bbopGraph = graphUtil.convertGraph(data);
    MAPPER.writeValue(out, bbopGraph);
    out.flush();
  }

}

