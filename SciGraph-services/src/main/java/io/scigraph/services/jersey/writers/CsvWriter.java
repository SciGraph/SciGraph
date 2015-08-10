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
package io.scigraph.services.jersey.writers;

import io.scigraph.services.jersey.CustomMediaTypes;

import java.io.IOException;
import java.io.Writer;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

@Produces(CustomMediaTypes.TEXT_CSV)
@Provider
public class CsvWriter extends DelimitedWriter {

  @Override
  CSVPrinter getCsvPrinter(Writer writer) throws IOException {
    return new CSVPrinter(writer, CSVFormat.DEFAULT);
  }

}

