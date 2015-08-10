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
import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.csv.CSVPrinter;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.internal.TinkerGraphUtil;

abstract public class DelimitedWriter extends GraphWriter {

  abstract CSVPrinter getCsvPrinter(Writer writer) throws IOException;

  static String getCurieOrIri(Vertex vertex) {
    return TinkerGraphUtil.getProperty(vertex, CommonProperties.CURIE, String.class).or((String)vertex.getProperty(CommonProperties.IRI));
  }

  @Override
  public void writeTo(Graph data, Class<?> type, Type genericType, Annotation[] annotations,
      MediaType mediaType, MultivaluedMap<String, Object> headers, OutputStream out) throws IOException {
    try (Writer writer = new OutputStreamWriter(out);
        CSVPrinter printer = getCsvPrinter(writer)) {
      List<String> header = newArrayList("id", "label", "categories");
      printer.printRecord(header);
      List<String> vals = new ArrayList<>();
      for (Vertex vertex: data.getVertices()) {
        vals.clear();
        vals.add(getCurieOrIri(vertex));
        String label = getFirst(TinkerGraphUtil.getProperties(vertex, NodeProperties.LABEL, String.class), null);
        vals.add(label);
        vals.add(TinkerGraphUtil.getProperties(vertex, Concept.CATEGORY, String.class).toString());
        printer.printRecord(vals);
      }
    }
  }

}

