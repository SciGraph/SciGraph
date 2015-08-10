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
package io.scigraph.lucene;

import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.util.CharsRef;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;

class SynonymMapSupplier implements Supplier<SynonymMap> {

  private static final Logger logger = Logger.getLogger(SynonymMapSupplier.class.getName());

  @Override
  public SynonymMap get() {
    try {
      return Resources.readLines(Resources.getResource("lemmatization.txt"), Charsets.UTF_8, new LineProcessor<SynonymMap>() {

        SynonymMap.Builder builder = new SynonymMap.Builder(true);

        @Override
        public boolean processLine(String line) throws IOException {
          List<String> synonyms = newArrayList(Splitter.on(',').trimResults().split(line));
          for (String term: synonyms) {
            for (String synonym: synonyms) {
              if (!term.equals(synonym)) {
                builder.add(new CharsRef(term), new CharsRef(synonym), true);
              }
            }
          }
          return true;
        }

        @Override
        public SynonymMap getResult() {
          try {
            return builder.build();
          } catch (IOException e) {
            e.printStackTrace();
            return null;
          }
        }
      });
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to build synonym map", e);
      return null;
    }
  }

}
